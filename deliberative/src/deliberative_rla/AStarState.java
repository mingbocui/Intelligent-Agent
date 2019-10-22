package deliberative_rla;

import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class AStarState extends State {
    public ArrayList<Task> unpickedTasks; // tasks this state should still pick up

    public AStarState(City startingCity, TaskSet carryingTasks, TaskSet newTasks) {
        super(startingCity, carryingTasks, newTasks);
        
        this.unpickedTasks = new ArrayList<>(newTasks);
        this.unpickedTasks.removeAll(this.currentTasks);
        this.unpickedTasks.removeAll(this.completedTasks);
    }
    
    public AStarState(AStarState other) {
        super(other);
        initialCity = other.initialCity;
        city = other.city;
        plan = new ArrayList<>(other.plan);
        currentTasks = new ArrayList<>(other.currentTasks);
        completedTasks = new ArrayList<>(other.completedTasks);
        unpickedTasks = new ArrayList<>(other.unpickedTasks);
    }
    
    public AStarState(State other, List unpickedTasks) {
        super(other);
        
        this.unpickedTasks = new ArrayList<>(unpickedTasks);
    }

    public double calculateHeuristic() {
        // choose the maximal single distance as the heuristic distance
        double dist = -1;
        if (this.currentTasks.size() > 0) {
            double maxDistanceToNextDelivery = this.currentTasks.stream()
                    .mapToDouble(t -> this.city.distanceTo(t.deliveryCity))
                    .max()
                    .getAsDouble();
            
            if (dist < maxDistanceToNextDelivery) {
                dist = maxDistanceToNextDelivery;
            }
        }
        
        if (this.unpickedTasks.size() > 0) {
            double maxDistanceToUnpickedTask = this.unpickedTasks.stream()
                    .mapToDouble(t -> this.city.distanceTo(t.pickupCity) + t.pathLength())
                    .max()
                    .getAsDouble();
            
            if (dist < maxDistanceToUnpickedTask) {
                dist = maxDistanceToUnpickedTask;
            }
        }
        
        return dist;
    }

    public double fScore() {
        double gScore = constructPlan().totalDistance();

        return gScore + calculateHeuristic();
    }
    
    @Override
    public AStarState moveTo(City city) {
        AStarState s = new AStarState(super.moveTo(city), unpickedTasks);
        s.unpickedTasks.removeIf(s.completedTasks::contains);
        return s;
    }
    
    @Override
    public AStarState pickUp(Set<Task> tasks, long capacity) {
        //double cost = AStarDistance - calculateHeuristic(); // getting original cost g(n)
        State s = super.pickUp(tasks, capacity);
        
        if (s != null) {
            // get the g value in the formula of g(n) + f(n) before update the tasks;
            AStarState as = new AStarState(s, unpickedTasks);
            as.unpickedTasks.removeIf(tasks::contains);
            
            return as;
        }
        
        return null;
    }

    // This is being done for the circle detection. We want an hash-collision in the HashSet, this will then trigger the
    // .equals() method in which we look for a path
    @Override
    public int hashCode() {
        return Objects.hash(this.unpickedTasks, this.completedTasks, this.currentTasks, this.city);
    }
    
    // This is the check for the "circle".
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof State)) return false;
        State state = (State) obj;
        
        // just sanity checks, should not be necessary
        if (this.city != state.city) return false;
        if (this.hashCode() != state.hashCode()) return false;
        
        // basically if the plan takes longer to achieve the same, we return equals to trigger a collision,
        // discarding the new but worse plan.
        return this.constructPlan().totalDistance() >= state.constructPlan().totalDistance();
    }
}
