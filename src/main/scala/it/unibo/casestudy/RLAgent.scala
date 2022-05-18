package it.unibo.casestudy

import it.unibo.casestudy.launch.SimulationDescriptions

import scala.concurrent.duration.{FiniteDuration, _}
import scala.language.postfixOps
import upickle.default._

object RLAgent {
  sealed abstract class WeakUpAction(val next: FiniteDuration)
  case object Sleep extends WeakUpAction(1 seconds)
  case object FullSpeed extends WeakUpAction(100 milliseconds)
  case object Normal extends WeakUpAction(200 milliseconds)
  case object EnergySaving extends WeakUpAction(500 milliseconds)

  sealed trait OutputDirection
  case object Same extends OutputDirection
  case object RisingUp extends OutputDirection
  case object RisingDown extends OutputDirection

  case class State(currentSetting: WeakUpAction, history: Seq[OutputDirection])

  implicit val stateRW: ReadWriter[State] = macroRW[State]
  implicit val outputRW: ReadWriter[OutputDirection] = macroRW[OutputDirection]
  implicit val actionRW: ReadWriter[WeakUpAction] = macroRW[WeakUpAction]
}
