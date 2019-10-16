package deliberative_rla;

import logist.plan.Action;
import logist.plan.Plan;
import logist.task.Task;
import logist.topology.Topology.City;

import java.sql.Array;
import java.util.*;
import java.util.stream.StreamSupport;

public class State {
    public City city; // the city where the Vehicle locates now
    public List<Task> currentTasks;
    public List<City> pathTaken; // includes origin
    public List<Task> completedTasks;
    public Plan plan;
    private Integer _hash; // storing the hash, as the members above are "final" (not enforced)
                           // we don't need to recompute the hash every time
    
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
    
    private boolean stupidCircle(ArrayList<Action> plan, int begin, int end) {
        return plan.subList(begin, end).stream().allMatch(a -> a instanceof Action.Move);
    }
    
    public boolean wouldMoveInACircle(City proposedCity) {
        if (city != proposedCity) return false;
        
        ArrayList<Action> planAsList = Utils.planAsList(plan);
        planAsList.add(new Action.Move(proposedCity));
    
        for (int i = 0; i < planAsList.size(); i++) {
            if (planAsList.get(i) instanceof Action.Move) {
                Action.Move move = (Action.Move)planAsList.get(i);
                String currentCity = Utils.getCityString(move);
                // possible circle
                if (currentCity.equals(proposedCity.toString())) {
                    if (stupidCircle(planAsList, i, planAsList.size())) {
                        return true;
                    }
                }
            }
        }
        
        return false;
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
        //System.out.println("dropping of tasks, I carry now " + newState.currentTasks.size());
        return newState;
    }
    
    public State pickUp(Task task) {
        State s = new State(this);
        s.currentTasks.add(task);
        s.plan.appendPickup(task);
    
        //System.out.println("picking up task, now I carry " + s.currentTasks.size());
        
        return s;
    }
    
    public State pickUp(Set<Task> tasks, long capacity) {
        State s = new State(this);
        for (final var t : tasks) {
            if (s.currentTaskWeights() + t.weight <= capacity
                    && !s.completedTasks.contains(t)
                    && !s.currentTasks.contains(t)) {
                s.currentTasks.add(t);
                s.plan.appendPickup(t);
            }
        }
        
        return s;
    }
    
    public long costOfTravel(long costPerKm) {
        if (this.pathTaken.size() <= 1) {
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
        if (this._hash == null) {
            this._hash = Objects.hash(this.currentTasks, this.city);
        }
        return this._hash;
    }
    
    // This is the check for the circle.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof State)) return false;
        State state = (State) obj;
    
        // just sanity checks, should not be necessary
        if (this.city != state.city) return false;
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
                if (otherIter.next().getClass() != Action.Move.class) {
                    return false;
                }
            }
            
            return true;
        }
        
        return false;
    }
    
}
