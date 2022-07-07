package it.unibo.casestudy

import it.unibo.casestudy.DesIncarnation.ID
import it.unibo.casestudy.event.Render
import it.unibo.casestudy.utils.ExperimentTrace

/** A generic interface of simulation
  * @tparam E
  *   the data exported from one simulated episode
  */
trait Simulation[E] {
  type RenderFactory = (Int, String) => Render
  protected var renderFactory: Option[RenderFactory] = None
  def perform(episode: Int): E
  final def repeat(times: Int)(progressEvaluation: (E, Int) => Unit): Seq[E] =
    LazyList
      .iterate(0)(_ + 1)
      .map(i => (perform(i), i))
      .tapEach { case (index, elem) => progressEvaluation(index, elem) }
      .tapEach(_ => updateAfter())
      .map(_._1)
      .take(times)
  def updateAfter(): Unit
  def attachRender(renderFactory: (Int, String) => Render): Unit = this.renderFactory = Some(renderFactory)
}

object Simulation {
  case class WorldSetting(size: Int, range: Double)
  // standard export
  type TicksAndOutput = (ExperimentTrace[Map[ID, Int]], ExperimentTrace[Map[ID, Double]])
}
