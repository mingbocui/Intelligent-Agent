package deliberative_rla;

import logist.task.Task;

import java.util.Comparator;
import java.util.List;

public class AStarAlgorithm extends AstarBaseAlgorithm {
    public AStarAlgorithm(int capacity, long costPerKm) {
        super(capacity, costPerKm, true, true);
    }

    public static double astarHeuristic(State state, State nextState, long costPerKm) {

        //all unpicked task existed in nextState
        //
        var maxDistancenOfUnpickedTasks = nextState.unpickedTasks.stream().mapToDouble(ut->state.city.distanceTo(ut.pickupCity)).max().getAsDouble()
                + nextState.unpickedTasks.stream().mapToDouble(Task::pathLength).max().getAsDouble();

        var maxDistanceOfCurrentTasks = state.currentTasks.stream().mapToDouble(ct->state.city.distanceTo(ct.deliveryCity)).max().getAsDouble();
        //only consider the maximal distance
//        var max_distance_among_all_tasks = nextState.currentTasks.stream().mapToDouble(Task::pathLength).max().getAsDouble();
//        return (state.city.distanceTo(nextState.city) + distance_to_all_tasks) * costPerKm -
//                nextState.currentTasks.stream().mapToDouble(t -> t.reward).sum();
//        return (state.constructPlan().totalDistance() + state.city.distanceTo(nextState.city) + max_distance_among_all_tasks)*costPerKm;

        return Math.max(maxDistancenOfUnpickedTasks, maxDistanceOfCurrentTasks);
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
