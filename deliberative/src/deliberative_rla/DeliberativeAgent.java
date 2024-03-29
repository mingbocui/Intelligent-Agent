package deliberative_rla;

/* import table */

import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An optimal planner for one vehicle.
 */
public class DeliberativeAgent implements DeliberativeBehavior {
    /* Environment */
    Topology topology;
    TaskDistribution td;
    /* the properties of the agent */
    Agent agent;
    IAlgorithm algorithm;

    @Override
    public void setup(Topology topology, TaskDistribution td, Agent agent) {
        this.topology = topology;
        this.td = td;
        this.agent = agent;

        // running some tests
        // tests();

        // initialize the planner
        int capacity = agent.vehicles().get(0).capacity();
        String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");


        // Throws IllegalArgumentException if algorithm is unknown
        switch (EAlgorithm.valueOf(algorithmName.toUpperCase())) {
            case ASTAR:
                this.algorithm = new AStarAlgorithm(capacity, this.agent.vehicles().get(0).costPerKm());
                break;
            case BFS:
                boolean useSaneState = agent.readProperty("use-sane-state", Boolean.class, false);
                this.algorithm = new BFSAlgorithm(capacity, this.agent.vehicles().get(0).costPerKm(), useSaneState);
                break;
            default:
                throw new IllegalArgumentException("no such algorithm known");
        }
    }

    @Override
    public Plan plan(Vehicle vehicle, TaskSet tasks) {
        return this.algorithm.optimalPlan(vehicle.getCurrentCity(), vehicle.getCurrentTasks(), tasks);
    }

    @Override
    public void planCancelled(TaskSet carriedTasks) {
        System.out.println("Agent " + agent.id() + " had to reevaluate his plan, using " + algorithm);

        if (!carriedTasks.isEmpty()) {
            // This cannot happen for this simple agent, but typically
            // you will need to consider the carriedTasks when the next
            // plan is computed.
        }
    }

    private void tests() {
        var longerPath = new State(topology.parseCity("Lausanne"))
                .moveTo(topology.parseCity("Sion"))
                .moveTo(topology.parseCity("Thun"))
                .moveTo(topology.parseCity("Bern"));

        var shorterPath = new State(topology.parseCity("Lausanne"))
                .moveTo(topology.parseCity("Fribourg"))
                .moveTo(topology.parseCity("Bern"));

        // we don't want to add longer plans with the same result
        assert new HashSet<>(Set.of(shorterPath)).contains(longerPath);
        var testSet = new HashSet<>(Set.of(longerPath));
        // we want to find shorter solutions, so this should be added
        assert testSet.contains(shorterPath);
        testSet.add(shorterPath);
        System.out.println("what is returned by the shortpath now? " + testSet.contains(shorterPath) + " "
                + " the longer path? " + testSet.contains(longerPath) + " "
                + testSet.stream().filter(t -> t.equals(shorterPath)).collect(Collectors.toList()));

        var dummyState = Utils.hasUselessCircle(new State(topology.parseCity("Genève"))
                .moveTo(topology.parseCity("Lausanne"))
                .moveTo(topology.parseCity("Neuchâtel"))
                .moveTo(topology.parseCity("Bern"))
                //.pickUp(Set.of(new Task(0,
                //        topology.parseCity("Bern"),
                //        topology.parseCity("Lausanne"),
                //        2,
                //        1 )), 100)
                .moveTo(topology.parseCity("Fribourg"))
                .moveTo(topology.parseCity("Lausanne")));
        System.out.println("does it detect the circle? " + dummyState);
    }

    enum EAlgorithm {BFS, ASTAR}
}
