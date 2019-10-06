package reactive_rla;

import java.util.HashMap;
import java.util.List;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;

public class ReactiveAgent implements ReactiveBehavior {

	private HashMap<State, AgentAction> stateActionTable;
	private HashMap<StateActionPair, Double> valueTable;

	private double discountFactor;
	private double costPerKm;

	public void valueIteration(Topology topology, Agent agent, TaskDistribution td){
		stateActionTable = new HashMap<State, AgentAction>();
		valueTable = new HashMap<StateActionPair, Double>();

		AgentHelper agentHelper = new AgentHelper(agent, td, topology);
		List<State> states = agentHelper.initStates(topology);
		List<AgentAction> actions = agentHelper.initActions(topology);

		var difference = 100.0;
		while (difference > Config.VALUE_INTERATION_THRESHOLD) {
			for (var statePrev : states) {
				double currValue;
				// TODO create function: `List<AgentAction> getValidActionsForState(State state)`
				for (var action : actions) {
					var stateActionPair = new StateActionPair(statePrev, action);
					currValue = agentHelper.rewardTable.get(stateActionPair); // just query from the created table
					for (var stateNext : states) {
						var transitionSequence = new TransitionSequence(statePrev, action, stateNext);
						var prob = agentHelper.transitionProbTable.get(transitionSequence); // query from the second

						// TODO not sure, implementation of value iteration
						AgentAction bestAction = stateActionTable.get(stateNext);
						var futureValue = valueTable.get(new StateActionPair(stateNext, bestAction));
						// TODO something is null here... super odd...
						currValue += discountFactor * prob * futureValue;
					}

					// check the convergence after update the table
					var bestAction = stateActionTable.get(statePrev); // best action of the current state
					var currBestStateActionPair = new StateActionPair(statePrev,bestAction); // best (s,a) pair of current state
					// if currentSate is not in the transition table
					if (valueTable.get(currBestStateActionPair) == null) {
						valueTable.put(new StateActionPair(statePrev, action), currValue);
						stateActionTable.put(statePrev, action);
					} else {
						var preValue = valueTable.get(currBestStateActionPair);
						if (preValue < currValue) {
							valueTable.remove(currBestStateActionPair);
							valueTable.put(new StateActionPair(statePrev, action), currValue);
							stateActionTable.remove(statePrev);
							stateActionTable.put(statePrev,action);
							difference = (currValue - preValue) / preValue;
						}
					}
				}
			}
		}
	}


	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
	    if (Config.TESTING) {
			System.out.println(String.format("setting up agent with id: %d", agent.id()));
			var aCity = topology.cities().get(0);
			System.out.println(String.format("Testing getting all destinations for city: %d, %s", aCity.id, aCity.name));
			for (final var possibleDest : Utils.getReachableCities(aCity)) {
				System.out.println("\t" + possibleDest.name);
			}
			System.out.println("that was all of them");
		}

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		discountFactor = agent.readProperty("discount-factor", Double.class, 0.95);

	    System.out.println(String.format("Running value iteration for agent %d now", agent.id()));
		valueIteration(topology, agent, td);
	}

	/**
	 * This method is called every time the agent arrives in a new city and is not
	 * carrying a task. The agent can see at most one available task in the city and
	 * has to decide whether or not to accept the task. It is possible that there is
	 * no task in which case availableTask is null.
     *
	 * - If the agent decides to pick up the task, the platform will take over the
	 *   control of the vehicle and deliver the task on the shortest path. The
	 *   next time this method is called the vehicle will have dropped the task
	 *   at its destination.
	 * - If the agent decides to refuse the task, it chooses a neighboring city to
	 *   move to. A refused task disappears and will not be available the next
	 *   time the agent visits the city.
	 * Note: If multiple tasks are available then the LogistPlatform randomly selects
	 *   the task that is shown to the agent. If no task is available then the agent
	 *   must return a move action.
	 * @param vehicle
	 * @param availableTask
	 * @return
	 */
	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		var currState = new State(vehicle.getCurrentCity(),null, false);

		if (availableTask != null) {
			// 1. select the best neighbor and move there
			currState.setToCity(availableTask.deliveryCity);
		}

		// 1. get real reward for this given action in Task
		// 2. compare that to a the task of moving to the best neighbour
		// 3. pick the one with a higher reward

		var agentAction = stateActionTable.get(currState);
		if (agentAction.isHasPickup()) {
			return new Pickup(availableTask);
		} else {
			return new Move(agentAction.getDestCity());
		}
	}
}
