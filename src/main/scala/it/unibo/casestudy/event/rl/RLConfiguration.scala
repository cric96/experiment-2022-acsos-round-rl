package it.unibo.casestudy.event.rl

import it.unibo.casestudy.RLAgent.{EnergySaving, FullSpeed, Normal, Sleep, State, WeakUpAction}
import it.unibo.casestudy.utils.Variable.V
import it.unibo.rl.model.QRLImpl

class RLConfiguration(
    val gamma: V[Double],
    val alpha: V[Double],
    val beta: V[Double],
    val epsilon: V[Double],
    val learn: V[Boolean] = true
) {
  def update(): Unit = gamma :: alpha :: epsilon :: learn :: beta :: Nil foreach (_.next())

  override def toString = s"Configuration($gamma, $alpha, $beta, $epsilon, $learn)"

  def canEqual(other: Any): Boolean = other.isInstanceOf[RLConfiguration]

  override def equals(other: Any): Boolean = other match {
    case that: RLConfiguration =>
      (that canEqual this) &&
        gamma == that.gamma &&
        alpha == that.alpha &&
        beta == that.beta &&
        epsilon == that.epsilon &&
        learn == that.learn
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(gamma, alpha, beta, epsilon, learn)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

object RLConfiguration {
  def apply(
      gamma: V[Double],
      alpha: V[Double],
      beta: V[Double],
      epsilon: V[Double],
      learn: V[Boolean] = true
  ): RLConfiguration =
    new RLConfiguration(gamma, alpha, beta, epsilon, learn)

  val QRLFamily: QRLImpl[State, WeakUpAction] = new QRLImpl[State, WeakUpAction] {}
  def createQ: QRLFamily.QFunction = QRLFamily.QFunction(Set(Sleep, EnergySaving, FullSpeed, Normal))
}
