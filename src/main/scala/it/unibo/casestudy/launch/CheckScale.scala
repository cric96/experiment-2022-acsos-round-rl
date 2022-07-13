package it.unibo.casestudy.launch

import it.unibo.casestudy.DesIncarnation._
import it.unibo.casestudy.Simulation.TicksAndOutput
import it.unibo.casestudy.event.rl.{RLConfiguration, RLRoundEvaluation, RLRuntimeInformation}
import it.unibo.casestudy.event.{AdjustableEvaluation, RoundAtEach}
import it.unibo.casestudy.launch.LaunchConstant.resFolder
import it.unibo.casestudy.utils.ExperimentTrace
import it.unibo.casestudy.{MultipleSwapSimulation, Simulation, SwapSimulation}
import upickle.default.read

import java.time.Instant
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.language.postfixOps
import scala.math.Numeric.Implicits.infixNumericOps
import scala.util.Random

object CheckScale extends App {
  val delta = 100 milliseconds
  val max = 1 seconds
  val switchAt = 100 seconds
  val endWhen = 200 seconds
  def simulationUsing(kind: String, id: String, size: Int): (ID => RoundEvent) => Simulation[TicksAndOutput] = {
    val config = Simulation.WorldSetting(size, 10)
    kind match {
      case "plain" =>
        val scenario = SwapSimulation.SimulationConfiguration(config, endWhen = endWhen, switchAt = switchAt)
        f => new SwapSimulation(f, config = scenario, id)
      case "multiswap" =>
        val scenario = MultipleSwapSimulation.SimulationConfiguration(config, endWhen = endWhen, switchAt = switchAt)
        f => new MultipleSwapSimulation(f, config = scenario, id)
    }
  }
  val mappedArgs = args.zipWithIndex.toMap.map(_.swap)

  if (mappedArgs.isEmpty) {
    scribe.error("You should pass the result file at minimum (located at res/<experiment-name>/<config>/runtime.json")
  }

  val random = new Random(0)
  val runTimeInfo = mappedArgs(0) // q table file
  val name = mappedArgs.getOrElse(1, "gradient")
  val scenario = mappedArgs.getOrElse(2, "plain")

  val program = SimulationFactory.programFromString(name)
  val zero = Instant.ofEpochMilli(0)
  val emptyConfig = RLConfiguration(0, 0, 0, 0, false)

  val rlRuntimeConfiguration =
    read[RLRuntimeInformation](os.read(os.pwd / os.RelPath(runTimeInfo)))

  val q = RLConfiguration.createQ

  RLConfiguration.QRLFamily.loadFromMap(q, rlRuntimeConfiguration.q)
  val lines = (10 to 30 by 1) map { size =>
    scribe.info(s"Check ok: ${size * size} nodes")
    val rlRoundFunction = (id: ID) => {
      val window = rlRuntimeConfiguration.window
      new RLRoundEvaluation(id, program, zero, rlConfig = emptyConfig, temporalWindow = window, q = q)
    }
    val (standardTicks, standardOutput) =
      simulationUsing(scenario, s"gradient-$size", size)(id => RoundAtEach(id, program, zero, delta)).perform(0)

    val (ticksRl, outputRl) =
      simulationUsing(scenario, s"gradient-$size", size)(rlRoundFunction).perform(0)
    val (adHocTicks, adHocOutput) =
      simulationUsing(scenario, s"gradient-$size", size)(id =>
        AdjustableEvaluation(id, program, zero, delta, max, delta)
      ).perform(0)

    val ticksRlPercentage = percentage(standardTicks, ticksRl)
    val ticksAdHocPercentage = percentage(standardTicks, adHocTicks)
    val errorAdHocPercentage =
      accumulatedError(standardOutput, adHocOutput).values.map(_._2).sum / standardOutput.values.size
    val errorRlPercentage = accumulatedError(standardOutput, outputRl).values.map(_._2).sum / standardOutput.values.size
    s"${size * size};$ticksRlPercentage;$ticksAdHocPercentage;$errorRlPercentage;$errorAdHocPercentage\n"
  }
  val head = "size;rlticks;adhocticks;rlerror;adhocerror\n"
  os.write.over(resFolder / s"$scenario-$name.csv", Seq(head) ++ lines)
  def percentage[E: Numeric](ticksA: ExperimentTrace[Map[ID, E]], ticksB: ExperimentTrace[Map[ID, E]]): Double = {
    def allTicks(trace: ExperimentTrace[Map[ID, E]]) = trace.values.last._2.values.sum.toDouble
    (allTicks(ticksA) - allTicks(ticksB)) / allTicks(ticksA)
  }

  def accumulatedError(
      reference: ExperimentTrace[Map[ID, Double]],
      other: ExperimentTrace[Map[ID, Double]]
  ): ExperimentTrace[Double] = {
    val errorPerTime = reference.values
      .zip(other.values)
      .map { case ((i, reference), (_, other)) =>
        val sumReference = reference.values.filter(_.isFinite).sum
        val sumOther = other.values.filter(_.isFinite).sum
        i -> (if (sumReference == sumOther) { 0 }
              else { math.abs((sumReference - sumOther)) / sumReference })
      }
    val errorPerTimeTrace = new ExperimentTrace[Double](other.name + "point-wise")
    errorPerTimeTrace.values = errorPerTime
    errorPerTimeTrace
  }
}
