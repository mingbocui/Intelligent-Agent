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
	private HashMap<State, Topology.City> lookupTable;

	private double discountFactor;
	private double costPerKm;


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
	    this.lookupTable = new ReactiveWorld(topology, td, agent, discountFactor, costPerKm).valueIteration();
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
		State state;

		// the lookup table states indicate the difference between a task (then destination is not null)
		// and moving (dest. is null)
		if (availableTask != null) {
			state = new State(vehicle.getCurrentCity(), availableTask.deliveryCity);
		} else {
			state = new State(vehicle.getCurrentCity(), null);
		}

		// use `availableTask.reward`  to compare it with a Move to any of the neighbouring cities
		// then make a decision

		Topology.City dest = lookupTable.get(state);

	    if (availableTask != null && availableTask.deliveryCity == dest) {
	    	return new Pickup(availableTask);
		} else {
	    	return new Move(dest);
		}
	}
}
