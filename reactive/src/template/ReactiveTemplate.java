package template;

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
import logist.topology.Topology.City;

public class ReactiveTemplate implements ReactiveBehavior {

	private HashMap<State, AgentAction> stateActionTable;
	private HashMap<StateActionPair, Double> valueTable;

	public void valueIteration(Topology topology, Agent agent, TaskDistribution td){

		stateActionTable = new HashMap<State, AgentAction>();
		valueTable = new HashMap<StateActionPair, Double>();

		AgentManager agentManager = new AgentManager(agent, td, topology);
		List<State> states = agentManager.initStates(topology);
		List<AgentAction> actions = agentManager.initActions(topology);


		double difference = 100;
		// iterating until converges
		while(difference > Config.THRESHOLD){
			for(State statePrev : states){
				double currValue;
				for(AgentAction action : actions){
					StateActionPair stateActionPair = new StateActionPair(statePrev, action);
					currValue = agentManager.rewardTable.get(stateActionPair); // just query from the created table
					for(State stateNext : states){
						TransitionSequence transitionSequence = new TransitionSequence(statePrev, action, stateNext);
						double prob = agentManager.transitionProbTable.get(transitionSequence); // query from the second
						// TODO not sure, implementation of value iteration
						AgentAction bestAction = stateActionTable.get(stateNext);
						double futureValue = valueTable.get(new StateActionPair(stateNext, bestAction));
						currValue += Config.DISCOUNTFACTOR * prob * futureValue;
					}

					// check the convergence after update the table
					AgentAction bestAction = stateActionTable.get(statePrev); // best action of the current state
					StateActionPair currBestStateActionPair = new StateActionPair(statePrev,bestAction); // best (s,a) pair of current state
					// if currentSate is not in the transition table
					if(valueTable.get(currBestStateActionPair) == null){
						StateActionPair temp = new StateActionPair(statePrev, action);
						valueTable.put(temp, currValue);
						stateActionTable.put(statePrev,action);
					}
					else{
						double preValue = valueTable.get(currBestStateActionPair);
						if(preValue < currValue){
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

		valueIteration(topology, agent, td);

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);

	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {

		Action action;

		State currState = new State(null,null, false);
		City vehicleCurrCity = vehicle.getCurrentCity();
		currState.setFromCity(vehicleCurrCity);
		if(availableTask != null){
			currState.setToCity(availableTask.deliveryCity);
		}
		AgentAction agentAction = stateActionTable.get(currState);
		if(agentAction.isHasPickup())
			action = new Pickup(availableTask);
		else{
			action = new Move(agentAction.getDestCity());
		}

		return action;


	}
}
