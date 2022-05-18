package it.unibo.casestudy.event
import it.unibo.casestudy.DesIncarnation
import it.unibo.casestudy.DesIncarnation._
import it.unibo.casestudy.event.RLRoundEvaluation._
import it.unibo.casestudy.RLAgent.{Normal, State, _}
import it.unibo.casestudy.utils.{ExperimentConstant, Variable}
import it.unibo.casestudy.utils.RichDouble._
import it.unibo.casestudy.utils.Variable.V
import it.unibo.rl.model.{QRL, QRLImpl}

import java.time.Instant
import scala.concurrent.duration.FiniteDuration
import scala.language.postfixOps
import scala.util.Random

/** Round evaluation that deploys reinforcement learning to tune the round frequency
  * @param node
  *   the target node
  * @param program
  *   the reference program
  * @param when
  *   the initial tick time
  * @param temporalWindow
  *   the temporal window used to build the state
  * @param weightForConvergence
  *   the weight given for the convergence
  * @param rlConfig
  *   the reinforcement learning configuration
  * @param seed
  *   the random seed
  */
class RLRoundEvaluation(
    val node: ID,
    val program: EXECUTION,
    val when: Instant,
    val temporalWindow: Int = 5,
    val weightForConvergence: Double = 0.99,
    rlConfig: Configuration,
    val seed: Int = 0
) extends RoundEvent {
  self =>
  import rlConfig._
  protected var q: QRLFamily.QFunction = globalQ
  protected var oldValue: Double = Double.PositiveInfinity
  protected var state: State = State(FullSpeed, Seq.empty[OutputDirection])
  protected var reinforcementLearningProcess: QRLFamily.RealtimeQLearning =
    QRLFamily.RealtimeQLearning(gamma, q, QRL.StaticParameters(epsilon, alpha, beta))
  override def act(network: DesIncarnation.NetworkSimulator): Option[DesIncarnation.Event] = {
    implicit val random: Random = RLRoundEvaluation.random
    // ARRANGE
    val context = network.context(node)
    // PAY ATTENTION! THE DELTA TIME MUST BE PERCEIVED BEFORE THE ROUND EXECUTION!
    val deltaTime = context.sense[FiniteDuration]("LSNS_DELTA_TIME").get
    val currentHistory = state.history
    reinforcementLearningProcess.setState(state)
    // ROUND EVALUATION
    network.progress(node, program)
    // EVAL
    val currentValue = network.`export`(node).map(_.root[Double]()).getOrElse(Double.PositiveInfinity)
    val direction = outputTemporalDirection(currentValue)
    val action = if (learn) {
      reinforcementLearningProcess.takeEpsGreedyAction(q)
    } else {
      reinforcementLearningProcess.takeGreedyAction(q)
    }

    network.chgSensorValue("dt", Set(node), action.next)
    val nextState = State(action, (direction +: currentHistory).take(temporalWindow))
    val rewardValue = reward(deltaTime)
    // IMPROVE
    if (learn) { reinforcementLearningProcess.observeEnvAndUpdateQ(q, nextState, rewardValue) }
    // ACT
    val nextEvent =
      new RLRoundEvaluation(
        node,
        program,
        when.plusMillis(action.next.toMillis).plusNanos(random.nextInt(nextFireNoise)),
        temporalWindow,
        weightForConvergence,
        rlConfig
      ) {
        this.oldValue = currentValue
        this.state = nextState
        this.q = self.q
        this.reinforcementLearningProcess = self.reinforcementLearningProcess
      }
    network.chgSensorValue(
      ExperimentConstant.RoundCount,
      Set(node),
      network.context(node).sense[Int](ExperimentConstant.RoundCount).get + 1
    )
    Some(nextEvent)
  }

  def reset(): RLRoundEvaluation = {
    oldValue = Double.PositiveInfinity
    reinforcementLearningProcess = QRLFamily.RealtimeQLearning(gamma, q, QRL.StaticParameters(epsilon, alpha, beta))
    state = State(FullSpeed, Seq.empty[OutputDirection])
    this
  }

  def updateVariables(): RLRoundEvaluation = {
    rlConfig.update()
    this
  }

  def moveTickTo(millisToAdd: Int): RLRoundEvaluation = {
    new RLRoundEvaluation(
      node,
      program,
      when.plusMillis(millisToAdd),
      temporalWindow,
      weightForConvergence,
      rlConfig
    ) {
      this.q = self.q
      this.reinforcementLearningProcess = self.reinforcementLearningProcess
    }
  }

  private def reward(deltaTime: FiniteDuration): Double = {
    val result = if (state.history.headOption.exists(_ != Same)) { // before: state.history.exists(_ != Same)
      -weightForConvergence * (deltaTime / EnergySaving.next)
    } else {
      -(1 - (deltaTime / EnergySaving.next)) * (1 - weightForConvergence)
    }
    result
  }

  private def outputTemporalDirection(current: Double): OutputDirection = if (
    (current ~= oldValue) || current.isInfinite && oldValue.isInfinite
  ) {
    Same
  } else if (current > oldValue) {
    RisingUp
  } else {
    RisingDown
  }
}

object RLRoundEvaluation {
  private[RLRoundEvaluation] val QRLFamily: QRLImpl[State, WeakUpAction] = new QRLImpl[State, WeakUpAction] {}
  private[RLRoundEvaluation] var random: Random = _
  private[RLRoundEvaluation] var globalQ: QRLFamily.QFunction = _
  reset(0)

  def reset(seed: Int): Random = {
    random = new Random(seed)
    globalQ = QRLFamily.QFunction(Set(Sleep, EnergySaving, FullSpeed, Normal))
    random
  }

  def printCurrentTable(): String = QRLFamily.storeQ(globalQ)

  class Configuration(
      val gamma: V[Double],
      val alpha: V[Double],
      val beta: V[Double],
      val epsilon: V[Double],
      val learn: V[Boolean] = true
  ) {
    def update(): Unit = gamma :: alpha :: epsilon :: learn :: beta :: Nil foreach (_.next())
    override def toString = s"Configuration($gamma, $alpha, $beta, $epsilon, $learn)"

    def canEqual(other: Any): Boolean = other.isInstanceOf[Configuration]

    override def equals(other: Any): Boolean = other match {
      case that: Configuration =>
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

  object Configuration {
    def apply(
        gamma: V[Double],
        alpha: V[Double],
        beta: V[Double],
        epsilon: V[Double],
        learn: V[Boolean] = true
    ): Configuration =
      new Configuration(gamma, alpha, beta, epsilon, learn)
  }
  val nextFireNoise = 1000 // increase randomness in next device fire
}
