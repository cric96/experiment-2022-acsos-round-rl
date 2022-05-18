package it.unibo.casestudy.launch

import com.github.tototoshi.csv.CSVReader
import it.unibo.casestudy.launch.Analysis.{imageFolder, resultFolder}
import it.unibo.casestudy.launch.LaunchConstant._
import org.nspl._
import org.nspl.awtrenderer._
import scribe.Level
import it.unibo.casestudy.utils.UnsafeProduct._

import java.io.File

/** A script used to produce plot and a brief analysis file. By default, it produces:
  *   - plot of error progression for a certain RL configuration
  *   - plot of ticks progression for a certain RL configuration
  *   - a plot for each episode in which are showed the total ticks, the average ticks per seconds, and the error
  *     percentage and the plot percentage If you pass "sample" as a argument, it produces RL plot only each 50 episode
  *
  * the images are stored in img/ the folder name represents the configuration value combination.
  */
object Analysis extends App {
  // Prepare
  type PlainData = (Double, Double, Double)
  // Rl and Adhoc
  type GeneratedData = (Double, Double, Double, Double)
  // Constants
  private val width = 400
  // line constants
  private val redLine = line(color = Color.red)
  private val greenLine = line(color = Color.green)
  private val blueLine = line(color = Color.blue)
  private val darkBlueLine = line(color = Color(0, 0, 139))
  private val darkGreenLine = line(color = Color(0, 139, 0))
  private val bluishGreen = line(color = Color(0, 158, 115))
  // extract all error and ticks at the end
  private var experimentLinesResult: Seq[String] = Seq("name,ticks,error")
  private val toSample = 10 // one plot each 100 experiments
  private val regex = raw"(.*)rl-(\d+)(.*)".r

  def sample(name: String): Boolean = if (args.length == 1 && args(0) == "sample") {
    name match { case regex(_, number, _) => number.toInt % toSample == 0 }
  } else {
    true
  }
  private val toSecondConversion = 1000.0
  private val resultFolder = LaunchConstant.resFolder
  private val imageFolder = os.pwd / LaunchConstant.imageFolder

  if (os.exists(imageFolder)) { os.remove.all(imageFolder) }
  os.makeDir.all(imageFolder)

  os.list(resultFolder).filter(os.isDir).foreach { path =>
    producePlotIn(path, imageFolder / path.last)
  }

  def producePlotIn(resultFolder: os.Path, imageFolder: os.Path): Unit = {
    val allFiles = os.list(resultFolder).filter(os.isFile).filter(_.toString.contains(".csv"))

    // Load data
    val (_, fixed) = load(allFiles, fixedName, convertPlain).head
    val (_, adHoc) = load(allFiles, adhocName, convertOther).head

    LoggerUtil.disableIf(os.list(resultFolder).size > 1)
    // One folder for each configuration
    allExperiment(resultFolder).foreach { rlFolder =>
      val experimentName = rlFolder.toIO.getName
      scribe.warn(s"Handle: $experimentName")
      val allFiles = os
        .list(rlFolder)
        .filter(os.isFile)
        .filter(_.toString.contains(".csv"))
      // One file foreach episode
      val rl = load(allFiles, rlName, convertOther, sample)
      val (_, error) = load(allFiles, errorName, convertSingle).head
      val (_, totalTicks) = load(allFiles, totalTicksName, convertSingle).head
      // pass as constants
      if (totalTicks.last > 1000 || error.last > 1500) { println("skip " + experimentName) }
      else {
        // Plots preparation
        val errorPlot = xyplot(
          (error, List(redLine), InLegend("Error"))
        )(
          par(xlab = "episode", ylab = "Root Mean Squared Error")
        )
        val totalTickPlot = xyplot(
          (totalTicks, List(bluishGreen), InLegend("Average ticks per second"))
        )(
          par(xlab = "episode", ylab = "Ticks per seconds")
        )
        os.makeDir.all(imageFolder / experimentName)
        // Plot storage
        rl.foreach { case (name, data) =>
          scribe.info(s"process: $name")
          plotRl(imageFolder / experimentName, data, fixed, adHoc, name)
        }
        val all = load(allFiles, rlName, convertOther)
        aggregate(imageFolder / experimentName, all.map(_._2), fixed, adHoc, "aggreagete-view")
        store(
          svgToFile(tempFile, sequence(List(errorPlot, totalTickPlot), TableLayout(2)), width),
          imageFolder / experimentName / s"error-and-ticks.svg"
        )
        store(svgToFile(tempFile, errorPlot, width), imageFolder / experimentName / s"error.svg")
        store(svgToFile(tempFile, totalTickPlot, width), imageFolder / experimentName / s"ticks.svg")

        experimentLinesResult = experimentLinesResult :+ s"$experimentName,${totalTicks.last},${error.last}"
        scribe.warn(s"End: $experimentName")
      }
    }

    os.write.over(imageFolder / "analysis.csv", experimentLinesResult.mkString("\n"))
  }
  // Utility functions
  def convertPlain(data: List[String]): PlainData =
    (data.head.toLong / toSecondConversion, data(1).toDouble, data(2).toDouble)

  // Utility functions
  def convertOther(data: List[String]): GeneratedData =
    (data.head.toLong / toSecondConversion, data(1).toDouble, data(2).toDouble, data(3).toDouble)

  def convertSingle(data: List[String]): Double = data.head.toDouble

  def allExperiment(resultFolder: os.Path): Seq[os.Path] = os.list(resultFolder).filter(os.isDir)
  def load[E](
      allFiles: Seq[os.Path],
      target: String,
      conversion: List[String] => E,
      filter: String => Boolean = _ => true
  ): (Seq[(String, Seq[E])]) =
    allFiles
      .filter(_.baseName.contains(target))
      .filter(f => filter(f.baseName))
      .map(f => f.baseName -> CSVReader.open(f.toIO))
      .map { case (name, reader) => (name, reader.all().tail, reader) }
      .tapEach(_._3.close())
      .map { case (name, dataL, _) => name -> dataL.map(conversion) }

  def plotRl(
      where: os.Path,
      rl: Seq[GeneratedData],
      fixed: Seq[PlainData],
      adhoc: Seq[GeneratedData],
      label: String = ""
  ): Unit = {
    val outputPlot = xyplot(
      (convert(fixed, _._3[Double]), List(redLine), InLegend("Periodic")),
      (convert(adhoc, _._3[Double]), List(greenLine), InLegend("Ad Hoc")),
      (convert(rl, _._3[Double]), List(blueLine), InLegend("Rl"))
    )(
      par(xlab = "time", ylab = "total output")
    )

    val totalTicksPlot = xyplot(
      (convert(fixed, _._2[Double]), List(redLine), InLegend("Periodic")),
      (convert(adhoc, _._2[Double]), List(greenLine), InLegend("Ad Hoc")),
      (convert(rl, _._2[Double]), List(blueLine), InLegend("Rl"))
    )(
      par(xlab = "time", ylab = "total ticks")
    )

    val frequencyPlot = xyplot(
      (tickPerSeconds(fixed), List(redLine), InLegend("Periodic")),
      (tickPerSeconds(adhoc), List(greenLine), InLegend("Ad Hoc")),
      (tickPerSeconds(rl), List(blueLine), InLegend("Rl"))
    )(
      par(xlab = "time", ylab = "ticks per second")
    )

    val errorPerSecond = xyplot(
      (convert(adhoc, _._4[Double]), List(greenLine), InLegend("Ad Hoc")),
      (convert(rl, _._4[Double]), List(blueLine), InLegend("Rl"))
    )(
      par(xlab = "time", ylab = "ticks per second")
    )
    val percentageOfRoundAndOutput = xyplot(
      (percentage(fixed, adhoc, _._3[Double]), List(greenLine), InLegend("Ad Hoc Error Percentage")),
      (percentage(fixed, rl, _._3[Double]), List(blueLine), InLegend("Rl Error Percentage")),
      (percentage(fixed, adhoc, _._2[Double]), List(darkGreenLine), InLegend("Ad Hoc Energy Saving Percentage")),
      (percentage(fixed, rl, _._2[Double]), List(darkBlueLine), InLegend("Rl Energy Saving Percentage"))
    )(
      par(xlab = "time", ylab = "Percentage")
    )
    val elements = sequence(
      List(
        outputPlot,
        totalTicksPlot,
        frequencyPlot,
        errorPerSecond,
        percentageOfRoundAndOutput
      ),
      TableLayout(3)
    )
    store(
      svgToFile(tempFile, elements, width),
      where / s"image-$label.svg"
    )
  }

  def aggregate(
      where: os.Path,
      rl: Seq[Seq[GeneratedData]],
      fixed: Seq[PlainData],
      adhoc: Seq[GeneratedData],
      label: String = ""
  ): Unit = {
    val lastGreedy = rl.drop(10) // number of training

    val tickPerSecond = lastGreedy.map(tickPerSeconds(_))
    val mean =
      tickPerSecond
        .reduce((acc, left) => acc.zip(left).map { case ((t, data), (_, other)) => (t, data + other) })
        .map { case (t, data) => (t, data / tickPerSecond.size) }

    val first = tickPerSecond.head.zip(mean).map { case ((t, data), (_, mu)) =>
      (t, math.pow(data - mu, 2))
    }
    val variance =
      tickPerSecond
        .foldLeft(first) { (acc, left) =>
          val zipped = acc.lazyZip(left).lazyZip(mean).toList
          zipped.map { case ((t, data), (_, other), (_, mu)) =>
            (t, data + math.pow(other - mu, 2))
          }
        }
        .map { case (t, data) => (t, math.sqrt(data / tickPerSecond.size)) }

    val upper = mean.zip(variance).map { case ((t, acc), (_, left)) => (t, acc + left, acc - left) }
    val res = xyplot(
      (mean, List(greenLine), InLegend("Average Plot")),
      (upper, List(area(yCol2 = Some(2), color = Color.apply(0, 255, 0, 50))))
    )(
      par(xlab = "time", ylab = "Average Ticks")
    )
    store(
      svgToFile(tempFile, res, width),
      where / s"image-$label.svg"
    )
  }
  private def store(file: File, path: os.Path): Unit =
    os.copy.over(os.Path(file.toPath), path)

  private def tempFile = java.io.File.createTempFile("nspl", ".svg")

  private def convert(data: Seq[Product], select: (Product) => Double) = data.map { t =>
    (t._1[Double], select(t))
  }

  private def tickPerSeconds(trace: Seq[Product]): Seq[(Double, Double)] = {
    trace.dropRight(1).zip(trace.tail).map { case (first, second) =>
      (first._1[Double], second._2[Double] - first._2[Double])
    }
  }

  private def percentage(ref: Seq[Product], current: Seq[Product], select: Product => Double): Seq[(Double, Double)] = {
    ref.zip(current).tail.map { case (l, r) =>
      (l._1, Math.abs((select(l) - select(r))) / select(l))
    }
  }
}
