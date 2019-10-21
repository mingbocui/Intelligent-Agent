package deliberative_rla;

import logist.task.TaskSet;
import logist.topology.Topology;

import java.util.Objects;

public class AStarState extends State {
    public AStarState(Topology.City startingCity, TaskSet carryingTasks) {
        super(startingCity, carryingTasks);
    }
    
    public AStarState(State other) {
        super(other);
    }
    
    public AStarState(Topology.City currentCity) {
        super(currentCity);
    }
    
    // This is being done for the circle detection. We want an hash-collision in the HashSet, this will then trigger the
    // .equals() method in which we look for a path
    @Override
    public int hashCode() {
        return Objects.hash(this.completedTasks, this.currentTasks, this.city);
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
