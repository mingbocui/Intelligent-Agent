package reactive_rla;

import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;

import java.util.HashMap;
import java.util.Random;

public class ReactiveAgent implements ReactiveBehavior {
    private HashMap<State, AgentAction> lookupTable;

    private int id;
    private double discountFactor;
    private double costPerKm;


    @Override
    public void setup(Topology topology, TaskDistribution taskDistribution, Agent agent) {
        if (Config.TESTING && Config.VERBOSITY_LEVEL >= 20) {
            // reachable cities test
            System.out.println(String.format("setting up agent with id: %d", agent.id()));
            final var aCity = topology.parseCity("Paris");
            System.out.println(String.format("Testing getting all destinations for city: %d, %s", aCity.id, aCity.name));
            Utils.getReachableCities(aCity).forEach(c -> System.out.println("\t" + c.name));
            System.out.println("that was all of them");

            // Dummy state action creation test
            final var dummyState = new State(topology.randomCity(new Random()), null);
            dummyState.createActions(taskDistribution, 0.0);
            System.out.println("Creating dummy state with no destination: " + dummyState);
        }


        // Reads the discount factor from the agents.xml file.
        // If the property is not present it defaults to 0.95
        discountFactor = agent.readProperty("discount-factor", Double.class, 0.95);
        costPerKm = agent.vehicles().get(0).costPerKm(); // TODO is there a better way?
        id = agent.id();

        System.out.println(String.format("Running value iteration for agent %d now", agent.id()));
        this.lookupTable = new ReactiveWorld(topology, taskDistribution, discountFactor, costPerKm).valueIteration();
    }

    /**
     * This method is called every time the agent arrives in a new city and is not
     * carrying a task. The agent can see at most one available task in the city and
     * has to decide whether or not to accept the task. It is possible that there is
     * no task in which case availableTask is null.
     * <p>
     * - If the agent decides to pick up the task, the platform will take over the
     * control of the vehicle and deliver the task on the shortest path. The
     * next time this method is called the vehicle will have dropped the task
     * at its destination.
     * - If the agent decides to refuse the task, it chooses a neighboring city to
     * move to. A refused task disappears and will not be available the next
     * time the agent visits the city.
     * Note: If multiple tasks are available then the LogistPlatform randomly selects
     * the task that is shown to the agent. If no task is available then the agent
     * must return a move action.
     *
     * @param vehicle
     * @param availableTask
     * @return
     */
    @Override
    public Action act(Vehicle vehicle, Task availableTask) {
        if (Config.TESTING && Config.VERBOSITY_LEVEL >= 10) {
            System.out.println("Agent " + id + " act called with " + availableTask);
        }
        State state;

        // the lookup table states indicate the difference between a task (then destination is not null)
        // and moving (dest. is null)
        // NB.: Kotlin would have been much nicer here... again
        if (availableTask != null) {
            state = new State(vehicle.getCurrentCity(), availableTask.deliveryCity);
        } else {
            state = new State(vehicle.getCurrentCity(), null);
        }

        var proposedAction = lookupTable.get(state); // proposed learned solution

        // the Config.ACTION_ACCEPTANCE_PERCENTAGE * benefit allows us to be a bit more flexible with the acceptance
        // of a real world task.
        // TODO compare the task to the given distribution of the task
        // TODO refuse to work if reward is lower than cost

        // This should never be the case, but just in case...
        if (proposedAction == null) {
            System.out.println("Huh? no action, given state is: " + state
                    + " this is of course a bit weird... so let's go with a heuristic");
            if (availableTask != null) {
                return new Pickup(availableTask);
            } else {
                return new Move(lookupTable.get(new State(vehicle.getCurrentCity(), null)).getDestination());
            }
        }

        if (availableTask != null
                && Utils.benefit(availableTask, costPerKm) >= proposedAction.getBenefit()) {
            if (Config.TESTING && Config.VERBOSITY_LEVEL >= 10) {
                System.out.println("Agent " + id + " decided to pick something up: " + availableTask);
            }
            return new Pickup(availableTask);
        } else {
            if (Config.TESTING && Config.VERBOSITY_LEVEL >= 10) {
                System.out.println("Agent " + id + " decided to move to " + proposedAction.getDestination()
                        + " with proposed action " + proposedAction);
            }
            return new Move(proposedAction.getDestination());
        }
    }
}
