package deliberative_rla;

import logist.task.Task;

import java.util.Comparator;

public class AstarComparator implements Comparator<State>{

    private long costPerKm;

    public AstarComparator(long costPerKm){
        this.costPerKm = costPerKm;
    }

    @Override
    public int compare(State s1, State s2){


        var costs1 = s1.currentTasks.stream().mapToDouble(Task::pathLength).sum();
        var costs2 = s2.currentTasks.stream().mapToDouble(Task::pathLength).sum();

        return Double.compare(costs1, costs2);

    }


//    public static double astarHeuristic(State state, State nextState, long costPerKm) {
//        var distance_to_all_tasks = nextState.currentTasks.stream().mapToDouble(Task::pathLength).sum();
//        // nett cost = cost - reward
////        return (state.city.distanceTo(nextState.city) + distance_to_all_tasks) * costPerKm -
////                nextState.currentTasks.stream().mapToDouble(t -> t.reward).sum();
//        return (state.city.distanceTo(nextState.city) + distance_to_all_tasks) * costPerKm;
//    }
//
//    public static State Astar(State state, long costPerKm) {
//        State bestState = null;
//        double minCost = Double.MAX_VALUE;
//        double cost;
//
////        return state.city.neighbors().stream()
////                .map(state::moveTo)
////                .min(Comparator.comparing(x -> astarHeuristic(state, x, costPerKm)))
////                .get();
//
//        return state.city.neighbors().stream()
//                .map(state::moveTo)
//                .sorted(Comparator.comparing(x -> astarHeuristic(state, x, costPerKm)))
//                .findFirst()
//                .orElse(null);
//    }

}
