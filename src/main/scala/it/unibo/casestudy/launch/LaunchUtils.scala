package it.unibo.casestudy.launch

import it.unibo.casestudy.DesIncarnation.ID
import it.unibo.casestudy.Simulation
import it.unibo.casestudy.Simulation.TicksAndOutput
import it.unibo.casestudy.DesIncarnation._

object LaunchUtils {
  def buildSimulation(simulationName: String, fireLogic: ID => RoundEvent, simId: String): Simulation[TicksAndOutput] =
    SimulationFactory.simulationFromString(simulationName, simId)(fireLogic)
}
