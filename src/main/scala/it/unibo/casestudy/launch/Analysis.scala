package it.unibo.casestudy.launch

import com.github.tototoshi.csv.CSVReader
import it.unibo.casestudy.launch.LaunchConstant._
import org.nspl._
import org.nspl.awtrenderer._
import scribe.Level
import it.unibo.casestudy.utils.UnsafeProduct._
import scala.language.postfixOps

import java.io.File

/** A script used to produce plot and a brief analysis file. By default, it produces:
  *   - plot of error progression for a certain RL configuration
  *   - plot of ticks progression for a certain RL configuration
  *   - a plot for each episode sample (first argument) in which are showed the total ticks, the average ticks per
  *     seconds, and the error percentage and the plot percentage.
  *   - the second argument is used to define the learning episodes the images are stored in img/ the folder name
  *     represents the configuration value combination.
  */
object Analysis extends App {
  // Prepare
  type PlainData = (Double, Double, Double)
  // Rl and Adhoc
  type GeneratedData = (Double, Double, Double, Double)
  val mappedArgs = args.indices.zip(args).toMap
  // Constants
  private val width = 400
  private val training = mappedArgs.get(1).map(_.toInt).getOrElse(0)
  private val greedyBoundError = mappedArgs.get(2).map(_.toInt).getOrElse(Int.MaxValue)
  private val greedyBoundTick = mappedArgs.get(3).map(_.toInt).getOrElse(Int.MaxValue)
  // line constants
  private val redLine = line(color = Color.red)
  private val greenLine = line(color = Color.green)
  private val blueLine = line(color = Color.blue)
  private val darkBlueLine = line(color = Color(0, 0, 139))
  private val darkGreenLine = line(color = Color(0, 139, 0))
  private val bluishGreen = line(color = Color(0, 158, 115))
  // extract all error and ticks at the end
  private var experimentLinesResult: Seq[String] = Seq("name,ticks,error")
  private val toSample = mappedArgs.get(0).map(_.toInt).getOrElse(1) // one plot each experiment by default
  private val regex = raw"(.*)rl-(\d+)(.*)".r

  def sample(name: String): Boolean =
    name match { case regex(_, number, _) => number.toInt % toSample == 0 }

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
    var experimentsResult = Seq.empty[(String, Double, Double)]

    val errorAndTicks = allExperiment(resultFolder)
      .map(allCsvFile)
      .map(files => load(files, errorName, convertSingle).head -> load(files, totalTicksName, convertSingle).head)
      .map { case ((_, error), (_, totalTicks)) => (error.drop(training), totalTicks.drop(training)) }

    val error = errorAndTicks.map(_._1)
    val ticks = errorAndTicks.map(_._2)
    val (meanError, varianceError) = meanAndVariance(error)
    val (meanTicks, varianceTicks) = meanAndVariance(ticks)
    val t = meanError.indices.toList.map(_.toDouble)
    val resTicks = xyplot(
      (t.zip(meanTicks), List(greenLine), InLegend("Average Ticks")),
      (injectTime2(varianceTicks, t), List(area(yCol2 = Some(2), color = Color.apply(0, 255, 0, 50))))
    )(
      par(
        xlab = "Time",
        ylab = "Average Ticks",
        legendDistance = 0.01 fts,
        rightPadding = 3 fts,
        leftPadding = 3 fts
      )
    )
    val resError = xyplot(
      (t.zip(meanError), List(redLine), InLegend("Average Error percentage")),
      (injectTime2(varianceError, t), List(area(yCol2 = Some(2), color = Color.apply(255, 0, 0, 50))))
    )(
      par(
        xlab = "Time",
        ylab = "Average Percentage",
        legendDistance = 0.01 fts,
        rightPadding = 3 fts,
        leftPadding = 3 fts
      )
    )

    val result = sequence(
      List(resTicks, resError),
      TableLayout(2)
    )
    os.makeDir.all(imageFolder)
    store(svgToFile(tempFile, result, width), imageFolder / s"image-seeds.svg")

    allExperiment(resultFolder).foreach { rlFolder =>
      val experimentName = rlFolder.toIO.getName
      scribe.warn(s"Handle: $experimentName")
      val allFiles = allCsvFile(rlFolder)
      // One file foreach episode
      val rl = load(allFiles, rlName, convertOther, sample)
      val (_, error) = load(allFiles, errorName, convertSingle).head
      val (_, totalTicks) = load(allFiles, totalTicksName, convertSingle).head
      val greedyAverageError = error.drop(training).sum / error.drop(training).size
      val greedyAverageTick = totalTicks.drop(training).sum / totalTicks.drop(training).size
      // pass as constants
      if (greedyAverageError > greedyBoundError || greedyAverageTick > greedyBoundTick) {
        println("skip " + experimentName)
      } else {
        // Plots preparation
        val errorPlot = xyplot(
          (error, List(redLine), InLegend("Error"))
        )(
          par(
            xlab = "Episode",
            ylab = "Root Mean Squared Error",
            legendDistance = 0.01 fts,
            rightPadding = 3 fts,
            leftPadding = 3 fts
          )
        )
        val totalTickPlot = xyplot(
          (totalTicks, List(bluishGreen), InLegend("Average ticks per second"))
        )(
          par(
            xlab = "Episode",
            ylab = "Ticks per seconds",
            legendDistance = 0.01 fts,
            rightPadding = 3 fts,
            leftPadding = 3 fts
          )
        )
        os.makeDir.all(imageFolder / experimentName)
        // Plot storage
        rl.foreach { case (name, data) =>
          scribe.info(s"process: $name")
          plotRl(imageFolder / experimentName, data, fixed, adHoc, name)
        }
        val all = load(allFiles, rlName, convertOther).sortBy(_._1.drop(3).toInt)

        aggregate(imageFolder / experimentName, all.map(_._2), fixed, "aggreagete-view")
        store(
          svgToFile(tempFile, sequence(List(errorPlot, totalTickPlot), TableLayout(2)), width),
          imageFolder / experimentName / s"error-and-ticks.svg"
        )
        store(svgToFile(tempFile, errorPlot, width), imageFolder / experimentName / s"error.svg")
        store(svgToFile(tempFile, totalTickPlot, width), imageFolder / experimentName / s"ticks.svg")
        experimentsResult = experimentsResult :+ (experimentName, greedyAverageTick, greedyAverageError)
        //experimentLinesResult = experimentLinesResult :+ s"$experimentName,${totalTicks.last},${error.last}"
        scribe.warn(s"End: $experimentName")
      }
    }
    def nameToBeta(name: String): Double = name.split("-").reverse.tail.head.toDouble
    def nameToGamma(name: String): Double = name.split("-").head.toDouble
    def nameToWindow(name: String): Double = name.split("-").reverse.tail.tail.head.toDouble

    val storeAnalysis = experimentsResult
      .sortBy(_._2)
      .map { case (name, error, tick) => (name, error, tick, nameToBeta(name)) }
      .map { case (name, error, tick, beta) => s"$name,$error,$tick,$beta,${nameToGamma(name)},${nameToWindow(name)}" }
      .mkString("\n")
    experimentLinesResult = Seq("name,ticks,error,theta,gamma,w")
    os.write.over(imageFolder / "analysis.csv", experimentLinesResult.mkString("\n") + "\n" + storeAnalysis)
  }
  /* plotting functions */
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
      par(
        xlab = "Time",
        ylab = "Total output",
        legendDistance = 0.01 fts,
        rightPadding = 3 fts,
        leftPadding = 3 fts
      )
    )

    val totalTicksPlot = xyplot(
      (convert(fixed, _._2[Double]), List(redLine), InLegend("Periodic")),
      (convert(adhoc, _._2[Double]), List(greenLine), InLegend("Ad Hoc")),
      (convert(rl, _._2[Double]), List(blueLine), InLegend("Rl"))
    )(
      par(
        xlab = "Time",
        ylab = "Total ticks",
        legendDistance = 0.01 fts,
        rightPadding = 3 fts,
        leftPadding = 3 fts
      )
    )

    val frequencyPlot = xyplot(
      (tickPerSeconds(fixed), List(redLine), InLegend("Periodic")),
      (tickPerSeconds(adhoc), List(greenLine), InLegend("Ad Hoc")),
      (tickPerSeconds(rl), List(blueLine), InLegend("Rl"))
    )(
      par(
        xlab = "Time",
        ylab = "ticks per second",
        legendDistance = 0.01 fts,
        rightPadding = 3 fts,
        leftPadding = 3 fts
      )
    )

    val errorPerSecond = xyplot(
      (convert(adhoc, _._4[Double]), List(greenLine), InLegend("Ad Hoc")),
      (convert(rl, _._4[Double]), List(blueLine), InLegend("Rl"))
    )(
      par(
        xlab = "Time",
        ylab = "ticks per second",
        legendDistance = 0.01 fts,
        rightPadding = 3 fts,
        leftPadding = 3 fts
      )
    )

    val percentageOfRoundAndOutput = xyplot(
      (percentage(fixed, adhoc, _._3[Double]), List(greenLine), InLegend("Ad Hoc Error Percentage")),
      (percentage(fixed, rl, _._3[Double]), List(blueLine), InLegend("Rl Error Percentage")),
      (percentage(fixed, adhoc, _._2[Double]), List(darkGreenLine), InLegend("Ad Hoc Energy Saving Percentage")),
      (percentage(fixed, rl, _._2[Double]), List(darkBlueLine), InLegend("Rl Energy Saving Percentage"))
    )(
      par(
        xlab = "Time",
        ylab = "Percentage",
        legendDistance = 0.01 fts,
        rightPadding = 3 fts,
        leftPadding = 3 fts
      )
    )

    val elements = sequence(
      List(
        //outputPlot,
        totalTicksPlot,
        frequencyPlot,
        //errorPerSecond,
        percentageOfRoundAndOutput
      ),
      TableLayout(3)
    )
    store(svgToFile(tempFile, elements, width), where / s"image-$label.svg")
  }

  def aggregate(
      where: os.Path,
      rl: Seq[Seq[GeneratedData]],
      fixed: Seq[PlainData],
      label: String = ""
  ): Unit = {
    val lastGreedy = rl.drop(training) // number of training
    val tickPerSecond = lastGreedy.map(tickPerSeconds(_))
    val t = tickPerSecond.map(_.map(_._1)).head
    val (meanTicks, varianceTicks) = meanAndVariance(tickPerSecond.map(_.map(_._2)))
    val (meanError, varianceError) = meanAndVariance(rl.map(percentage(fixed, _, _._3[Double])).map(_.map(_._2)))

    val resTicks = xyplot(
      (t.zip(meanTicks), List(bluishGreen), InLegend("Average Ticks")),
      (injectTime2(varianceTicks, t), List(area(yCol2 = Some(2), color = Color.apply(0, 158, 115, 50))))
    )(
      par(
        xlab = "Time",
        ylab = "Average Ticks",
        legendDistance = 0.01 fts,
        rightPadding = 3 fts,
        leftPadding = 3 fts
      )
    )
    val resError = xyplot(
      (t.zip(meanError), List(redLine), InLegend("Average Error percentage")),
      (injectTime2(varianceError, t), List(area(yCol2 = Some(2), color = Color.apply(255, 0, 0, 50))))
    )(
      par(
        xlab = "Time",
        ylab = "Average Percentage",
        legendDistance = 0.01 fts,
        rightPadding = 3 fts,
        leftPadding = 3 fts
      )
    )
    val result = sequence(
      List(resError, resTicks),
      TableLayout(2)
    )
    store(svgToFile(tempFile, result, width), where / s"image-$label.svg")
  }

  /** os utilities */
  private def store(file: File, path: os.Path): Unit = {
    os.copy.over(os.Path(file.toPath), path)
    os.remove(os.Path(file.toPath()))
  }

  private def tempFile = java.io.File.createTempFile("nspl", ".svg")

  private def allExperiment(resultFolder: os.Path): Seq[os.Path] = os.list(resultFolder).filter(os.isDir)

  private def allCsvFile(folder: os.Path) = os
    .list(folder)
    .filter(os.isFile)
    .filter(_.toString.contains(".csv"))

  private def load[E](
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

  def convertPlain(data: List[String]): PlainData =
    (data.head.toLong / toSecondConversion, data(1).toDouble, data(2).toDouble)

  /* data utilities  */
  def convertOther(data: List[String]): GeneratedData =
    (data.head.toLong / toSecondConversion, data(1).toDouble, data(2).toDouble, data(3).toDouble)

  def convertSingle(data: List[String]): Double = data.head.toDouble

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

  private def meanAndVariance(
      greedyData: Seq[Seq[Double]]
  ): (Seq[Double], Seq[(Double, Double)]) = {
    val mean =
      greedyData
        .reduce((acc, left) =>
          acc
            .zip(left)
            .map { case (left, right) => left + right }
        )
        .map(_ / greedyData.size)
    val first = greedyData.head.zip(mean).map { case (data, mu) => math.pow(data - mu, 2) }
    val variance =
      greedyData
        .foldLeft(first) { (acc, left) =>
          val zipped = acc.lazyZip(left).lazyZip(mean).toList
          zipped.map { case (data, other, mu) => (data + math.pow(other - mu, 2)) }
        }
        .map(data => math.sqrt(data / greedyData.size))
    val upper = mean.zip(variance).map { case (acc, left) => (acc + left, acc - left) }

    (mean, upper)
  }

  def injectTime2(seq: Seq[(Double, Double)], time: Seq[Double]): Seq[(Double, Double, Double)] =
    seq.zip(time).map { case ((l, r), t) => (t, l, r) }
}
