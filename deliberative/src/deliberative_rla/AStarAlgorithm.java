package deliberative_rla;

import logist.task.Task;

import java.util.Comparator;

public class AStarAlgorithm extends BaseAlgorithm {
    public AStarAlgorithm(int capacity, long costPerKm) {
        super(capacity, costPerKm, true, true);
    }
    
    public static double astarHeuristic(State state, State nextState, long costPerKm) {
        var distance_to_all_tasks = nextState.currentTasks.stream().mapToDouble(Task::pathLength).sum();
        // nett cost = cost - reward
        return (state.city.distanceTo(nextState.city) + distance_to_all_tasks) * costPerKm -
                nextState.currentTasks.stream().mapToDouble(t -> t.reward).sum();
    }
    
    public static State Astar(State state, long costPerKm) {
        State bestState = null;
        double minCost = Double.MAX_VALUE;
        double cost;
        
        return state.city.neighbors().stream()
                .map(state::moveTo)
                .min(Comparator.comparing(x -> astarHeuristic(state, x, costPerKm)))
                .get();
    }
}
