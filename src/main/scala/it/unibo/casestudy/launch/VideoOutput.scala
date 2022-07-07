package it.unibo.casestudy.launch

import it.unibo.casestudy.DesIncarnation._
import it.unibo.casestudy.event.Render
import it.unibo.casestudy.event.rl.{RLConfiguration, RLRoundEvaluation, RLRuntimeInformation}
import it.unibo.casestudy.launch.LaunchUtils.buildSimulation
import upickle.default._

import java.time.Instant
import scala.util.Random

object VideoOutput extends App {
  val mappedArgs = args.zipWithIndex.toMap.map(_.swap)
  if (mappedArgs.isEmpty) {
    scribe.error("You should pass the result file at minimum (located at res/<experiment-name>/<config>/runtime.json")
    scribe.error(
      "arguments: file !mandatory! program [gradient | cblock], scenario [plain | multiswap], snapshotDt [int] "
    )
  }
  val random = new Random(0)
  val runTimeInfo = mappedArgs(0) // q table file
  val name = mappedArgs.getOrElse(1, "gradient")
  val scenario = mappedArgs.getOrElse(2, "plain")
  val each = mappedArgs.getOrElse(3, "100").toLong
  val program = SimulationFactory.programFromString(name)
  val zero = Instant.ofEpochMilli(0)
  val emptyConfig = RLConfiguration(0, 0, 0, 0, false)
  val rlRuntimeConfiguration =
    read[RLRuntimeInformation](os.read(os.pwd / os.RelPath(runTimeInfo)))
  val q = RLConfiguration.createQ
  RLConfiguration.QRLFamily.loadFromMap(q, rlRuntimeConfiguration.q)
  val rlRoundFunction = (id: ID) =>
    new RLRoundEvaluation(
      id,
      program,
      zero,
      rlConfig = emptyConfig,
      temporalWindow = rlRuntimeConfiguration.window,
      q = q
    ).moveTickTo(random.nextInt(RLRoundEvaluation.nextFireNoise))
  val simulation = buildSimulation(
    scenario,
    fireLogic = rlRoundFunction,
    name + scenario
  )
  simulation.attachRender((simulationIndex, id) => Render(zero, each, id, simulationIndex))
  simulation.perform(0)
}
