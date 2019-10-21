package deliberative_rla;

import logist.task.TaskSet;
import logist.topology.Topology;

public class BFSAlgorithm extends BaseAlgorithm {
    public BFSAlgorithm(int capacity, long costPerKm) {
        super(capacity, costPerKm, false);
        System.out.println("running bfs algorithm");
    }
}
