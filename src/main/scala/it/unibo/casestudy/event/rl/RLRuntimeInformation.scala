package it.unibo.casestudy.event.rl
import upickle.default._
case class RLRuntimeInformation(q: String, window: Int)
object RLRuntimeInformation {
  implicit def macroRWInfo: upickle.default.ReadWriter[RLRuntimeInformation] = macroRW
}
