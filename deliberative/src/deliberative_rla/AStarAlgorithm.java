package deliberative_rla;

import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology;

import java.util.Comparator;
import java.util.List;

public class AStarAlgorithm extends BaseAlgorithm {
    public AStarAlgorithm(int capacity, long costPerKm) {
        super(capacity, costPerKm, true);
        System.out.println("running astar algorithm");
    }

    public static double astarHeuristic(State state, State nextState, long costPerKm) {
        var distance_to_all_tasks = nextState.currentTasks.stream().mapToDouble(Task::pathLength).sum();
        // nett cost = cost - reward
//        return (state.city.distanceTo(nextState.city) + distance_to_all_tasks) * costPerKm -
//                nextState.currentTasks.stream().mapToDouble(t -> t.reward).sum();
        return (state.city.distanceTo(nextState.city) + distance_to_all_tasks) * costPerKm;
    }

    public static State Astar(State state, long costPerKm) {
        State bestState = null;
        double minCost = Double.MAX_VALUE;
        double cost;

        return state.city.neighbors().stream()
                .map(state::moveTo)
                .min(Comparator.comparing(x -> astarHeuristic(state, x, costPerKm)))
                .get();

//        return state.city.neighbors().stream()
//                .map(state::moveTo)
//                .sorted(Comparator.comparing(x -> astarHeuristic(state, x, costPerKm)))
//                .findFirst()
//

    }
}
