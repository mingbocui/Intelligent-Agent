package deliberative_rla;

import logist.task.TaskSet;
import logist.topology.Topology;

public class BFSAlgorithm extends BaseAlgorithm<State> {
    public BFSAlgorithm(int capacity, long costPerKm) {
        super(capacity, costPerKm, true, false);
        System.out.println("running bfs algorithm");
    }
    
    @Override
    public State rootState(Topology.City startingCity, TaskSet carryingTasks) {
        return new State(startingCity, carryingTasks);
    }
}
