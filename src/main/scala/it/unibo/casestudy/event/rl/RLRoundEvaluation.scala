package it.unibo.casestudy.event.rl

import it.unibo.casestudy.DesIncarnation
import it.unibo.casestudy.DesIncarnation._
import it.unibo.casestudy.RLAgent.{Normal, State, _}
import it.unibo.casestudy.event.rl.RLRoundEvaluation._
import it.unibo.casestudy.utils.ExperimentConstant
import it.unibo.casestudy.utils.RichDouble._
import it.unibo.rl.model.QRL
import RLConfiguration._
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
    rlConfig: RLConfiguration,
    val seed: Int = 0,
    val q: QRLFamily.QFunction = RLRoundEvaluation.globalQ
) extends RoundEvent {
  self =>
  import rlConfig._
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
    val action = if (learn.value) {
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
    val nextDt = when.plusMillis(action.next.toMillis).plusNanos(random.nextInt(nextFireNoise))
    val nextEvent =
      new RLRoundEvaluation(node, program, nextDt, temporalWindow, weightForConvergence, rlConfig, q = q) {
        this.oldValue = currentValue
        this.state = nextState
        this.reinforcementLearningProcess = self.reinforcementLearningProcess
      }
    val updatedRoundCount = network.context(node).sense[Int](ExperimentConstant.RoundCount).get + 1
    network.chgSensorValue(ExperimentConstant.RoundCount, Set(node), updatedRoundCount)
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
    val nextDt = when.plusMillis(millisToAdd)
    new RLRoundEvaluation(node, program, nextDt, temporalWindow, weightForConvergence, rlConfig, q = q) {
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
  private[RLRoundEvaluation] var random: Random = _
  private[RLRoundEvaluation] var globalQ: QRLFamily.QFunction = _
  reset(0)

  def reset(seed: Int): Random = {
    random = new Random(seed)
    globalQ = createQ
    //QRLFamily.loadFromMap(globalQ, os.read(os.pwd / "q"))
    random
  }
  def updateRandom(seed: Int): Random = {
    random = new Random(seed)
    random
  }

  def printCurrentTable(): String = QRLFamily.storeQ(globalQ)

  val nextFireNoise = 1000 // increase randomness in next device fire
}
