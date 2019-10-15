package deliberative_rla;

import logist.plan.Action;
import logist.plan.Plan;
import logist.task.Task;
import logist.topology.Topology.City;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class State {
    public City city; // the city where the Vehicle locates now
    public List<Task> currentTasks;
    public List<City> pathTaken; // includes origin
    public List<Task> completedTasks;
    public Plan plan;
    
    public State(City currentCity) {
        this.currentTasks = new ArrayList<>();
        this.pathTaken = new ArrayList<>(Arrays.asList(currentCity));
        this.completedTasks = new ArrayList<>();
        
        this.city = currentCity;
        this.plan = new Plan(currentCity);
    }
    
    public State(State other) {
        // we need to make a copy, the elements themselves are final
        city = other.city;
        pathTaken = new ArrayList<>(other.pathTaken);
        currentTasks = new ArrayList<>(other.currentTasks);
        completedTasks = new ArrayList<>(other.completedTasks);
        
        // shitty way to copy... uag
        plan = new Plan(other.pathTaken.get(0));
        if (other.plan.iterator().hasNext()) {
            other.plan.forEach(plan::append);
        }
    }
    
    public int currentTaskWeights() {
        return currentTasks.stream().mapToInt(t -> t.weight).sum();
    }
    
    public State moveTo(City city) {
        State newState = new State(this);
        newState.pathTaken.add(city);
        newState.city = city;
    
        newState.plan.appendMove(city);
        newState.currentTasks.stream().filter(t -> t.deliveryCity == city).forEach(t -> {
            newState.plan.appendDelivery(t);
            newState.completedTasks.add(t);
            
        });
    
        newState.currentTasks.removeIf(t -> t.deliveryCity == city);
        return newState;
    }
    
    public State pickUp(Task task) {
        State s = new State(this);
        s.currentTasks.add(task);
        s.plan.appendPickup(task);
        
        return s;
    }
    
    public long costOfTravel(long costPerKm) {
        if (this.pathTaken.size() <= 2) {
            return 0;
        }
        
        long cost = 0;
    
        for (int i = 1; i < this.pathTaken.size(); i++) {
            cost += this.pathTaken.get(i - 1).distanceTo(this.pathTaken.get(i)) * costPerKm;
        }
        
        return cost;
    }
    
    public long profit(long costPerKm) {
        return this.completedTasks.stream().mapToLong(t -> t.reward).sum() - this.costOfTravel(costPerKm);
    }
    
    // This is being done for the circle detection. We want an hash-collision in the HashSet, this will then trigger the
    // .equals() method in which we look for a path
    @Override
    public int hashCode() {
        return Objects.hash(this.completedTasks, this.currentTasks, this.city);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof State)) return false;
        State state = (State) obj;
        
        // just a sanity check, should not be necessary
        if (this.hashCode() != state.hashCode()) return false;
        
        // TODO look for a path overlap
        // find divergence in path
        // check if rest is just moving
        var thisIter = plan.iterator();
        var otherIter = state.plan.iterator();
        
        boolean diverged = false;
        while (thisIter.hasNext() && otherIter.hasNext()) {
            if (!thisIter.next().equals(otherIter.next())) {
                diverged = true;
                break;
            }
        }
        
        if (diverged) {
            while (otherIter.hasNext()) {
                // TODO don't know if this is correct
                if (otherIter.next().getClass() != Action.Move.class) {
                    return false;
                }
            }
            
            return true;
        }
        
        return false;
    }
}
