package it.unibo.casestudy.event
import it.unibo.casestudy.{DesIncarnation, RLAgent}
import it.unibo.casestudy.DesIncarnation._
import it.unibo.casestudy.utils.ExperimentConstant

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.time.Instant
import javax.imageio.ImageIO
import scala.concurrent.duration.FiniteDuration

case class Render(when: Instant, dt: Long, id: String, episode: Int, maxTime: Long = 1000) extends Event {
  override def act(network: DesIncarnation.NetworkSimulator): Option[DesIncarnation.Event] = {
    val sim = network.asInstanceOf[SpaceAwareSimulator]
    val contextAndPosition = sim.ids.map(id => (network.context(id), sim.space.getLocation(id)))
    val minPosition = sim.space.elemPositions.minBy(_._2)._2
    val maxPosition = sim.space.elemPositions.maxBy(_._2)._2
    val width = (maxPosition.x - minPosition.x).toInt
    val height = (maxPosition.y - minPosition.y).toInt
    val image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val g = image.getGraphics
    g.fillRect(0, 0, width, height)
    val data = contextAndPosition.map { case (ctx, pos) =>
      pos -> (ctx.sense[FiniteDuration]("dt").get.toMillis,
      ctx.sense[Boolean](ExperimentConstant.Source).get,
      ctx.exports().toMap.get(sim.space.getAt(pos).get).map(_.root[Double]()).getOrElse(Double.PositiveInfinity))
    }
    val remap = data
      .map { case (pos, (deltaTime, _, _)) => pos -> (deltaTime / maxTime.toFloat) }
      .map { case (pos, intensity) => pos -> Color.getHSBColor(0, 1 - intensity, 1 - intensity) }

    g.setColor(Color.BLUE)

    data
      .collect { case (pos, (_, _, gradient)) => (pos, gradient) }
      .map {
        case (pos, g) if g > 1000 => (pos, 1000.toFloat)
        case (pos, g) => (pos, g.toFloat)
      }
      .foreach { case (pos, gradient) =>
        g.setColor(Color.getHSBColor(0.2f + gradient / 1000 * 4, 1, 1))
        g.fillRect(pos.x.toInt, pos.y.toInt, 8, 8)
      }
    remap.foreach { case (pos, color) =>
      g.setColor(color)
      g.fillRect(pos.x.toInt, pos.y.toInt, 5, 5)
    }
    if (!os.exists(os.pwd / "video" / s"$id" / s"$episode")) {
      os.makeDir.all(os.pwd / "video" / s"$id" / s"$episode")
    }

    val outFile = s"${when.toEpochMilli.toInt}".reverse.padTo(20, '0').reverse
    val out = new File(s"video/$id/$episode/$outFile.png")

    ImageIO.write(image, "png", out)
    Some(this.copy(when = when.plusMillis(dt)))
  }
}
