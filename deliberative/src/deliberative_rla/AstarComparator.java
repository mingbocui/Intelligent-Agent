package deliberative_rla;

import logist.task.Task;

import java.util.Comparator;

public class AstarComparator implements Comparator<State>{

    @Override
    public int compare(State s1, State s2){
        return Double.compare(s1.getAStarDistance(), s2.getAStarDistance());
    }

//    private double calculateHeuristic(State state){
//        double maxDistance = -1;
//        for(Task currentTask : state.currentTasks){
//            double distance = state.city.distanceTo(currentTask.deliveryCity);
//            if(distance > maxDistance) maxDistance = distance;
//        }
//        for(Task unPickedTask : state.unpickedTasks){
//            double distance = state.city.distanceTo(unPickedTask.pickupCity) + unPickedTask.pathLength();
//            if(distance > maxDistance) maxDistance = distance;
//        }
//        return maxDistance;
//    }

}
