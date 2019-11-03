package centralized_rla;

import logist.plan.Action;
import logist.plan.Plan;

import java.util.ArrayList;
import java.util.List;

public class StochasticLocalSearch {
    private enum ActionType {
        DELIVERY,
        PICKUP
    }
    
    private class TaskEncoded {
        public int nextTask;
        public ActionType type;
    
        public TaskEncoded(int nextTask, ActionType type) {
            this.nextTask = nextTask;
            this.type = type;
        }
    }
    /**
     *  - it is no longer pick up of t_k directly followed by delivery of t_k.
     *  - instead it is pick up of t_m up to t_n, with m <= n,
     *    followed by delivery of t_i, for every i in set(next tasks to deliver)
     *  - but we can't encode a delivery as a pick up and a delivery, we have to separate them
     *  => nextTask is an array of 2 * N_T + N_V
     *
     *  nextTask(t_i) match t_i
     *   PickUp(t_j)    -> some vehicle will pick up task t_j after what ever it did with task t_i
     *   Delivery(t_j)  -> some vehicle will deliver task t_j after what ever it did with task t_i
     *  nextTask(v_k) stays the same
     *
     *  time needs two arrays:
     *
     *  timePickUp:
     *      if nextTask(v_k) = t_j => t_j is first to be picked up by vehicle v_k -> timePickUp(t_j) = 1
     *      if nextTask(t_i) = PickUp(t_j) => ...                                 -> timePickUp(t_j) = TODO timePickUp(t_i) can also be another
     *
     *
     *  maybe just without time?
     *
     */
    /*
    public List encoding(int nTasks, int nVehicles) {
        List nextTask = new ArrayList<TaskEncoded>();
        for (int i = 0; i < nTasks ; i++) {
            nextTask.add(new TaskEncoded(i + 1, ActionType.PICKUP));
            if (i == nTasks - 1) {
i               nextTask.add(new TaskEncoded(i, ActionType.DELIVERY));
            } else {
                // super shitty handling of the case that the vehicle no longer moves
                nextTask.add(new TaskEncoded(-1, ActionType.DELIVERY));
            }
        }
        
        nextTask.add()
        while (nextTask.size() < 2 * nTasks + nVehicles) nextTask.add(null);
        
        List timePickUp = new ArrayList<Integer>();
        List timeDelivery = new ArrayList<Integer>();
        while (timePickUp.size() < 2 * nTasks) timePickUp.add(null);
        while (timeDelivery.size() < 2 * nTasks) timeDelivery.add(null);
        
        return nextTask;
    }
    
    public List<SolutionSpace> chooseNeighbours(SolutionSpace old) {
        
        
        
        return null
    }
    
    
    
    private List<Plan> initialSolution() {
        return null;
    }
    
    private Plan localChoice(List<Plan> neighbors) {
        return null;
    }
    
    private List<Plan> chooseNeighbors(Plan p) {
        return null;
    }
    
    public List<Plan> solution() {
        var init = initialSolution();
        
        while(!terminationConditionMet()){
            init = localChoice(chooseNeighbors(init));
        }
        
        return null;
    }
    
    private boolean terminationConditionMet() {
        return false;
    }
    
     */
}
