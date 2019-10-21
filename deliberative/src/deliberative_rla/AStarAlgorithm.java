package deliberative_rla;

import logist.task.Task;
import logist.topology.Topology;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AStarAlgorithm extends AstarBaseAlgorithm {
    public AStarAlgorithm(int capacity, long costPerKm) {
        super(capacity, costPerKm, true, true);
    }

    public static double astarHeuristic(State state, State nextState, long costPerKm) {

        //all unpicked task existed in nextState
        //
        var maxDistancenOfUnpickedTasks = nextState.unpickedTasks.stream().mapToDouble(ut->state.city.distanceTo(ut.pickupCity)).max().getAsDouble()
                + nextState.unpickedTasks.stream().mapToDouble(Task::pathLength).max().getAsDouble();

        if(state.currentTasks.isEmpty()) return maxDistancenOfUnpickedTasks;
        else{
            var maxDistanceOfCurrentTasks = state.currentTasks.stream().mapToDouble(ct->state.city.distanceTo(ct.deliveryCity)).max().getAsDouble();
            return Math.max(maxDistancenOfUnpickedTasks, maxDistanceOfCurrentTasks);
        }

    }

    public static State Astar(State state, long costPerKm) {
        State bestState = null;
        double minCost = Double.MAX_VALUE;
        double cost;
        var neighborStates = state.city.neighbors().stream().map(state::moveTo).collect(Collectors.toList());

        for(State neighborState : neighborStates){
//            System.out.println(state.city.name);

            double distance = astarHeuristic(state, neighborState, costPerKm);

            System.out.println("city " + neighborState.city.name + " has current tasks " + neighborState.currentTasks.size() + " with distance " + distance);

            if(distance < minCost){
                minCost = distance;
                bestState = neighborState;
            }
        }

//        System.out.println(bestState.city.name);
        return bestState;


    }
}
