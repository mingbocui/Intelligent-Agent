package deliberative_rla;

import logist.plan.Action;
import logist.plan.Action.Delivery;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

import java.util.*;
import java.util.stream.Collectors;

public class State {
    public City city; // the city where the Vehicle locates now
    public City initialCity;
    public List<Task> currentTasks;
    public List<Task> completedTasks;
    public List<Task> unpickedTasks; // unpicked tasks in the world
    public ArrayList<Action> plan;

    public State(City currentCity) {
        this.currentTasks = new ArrayList<>();
        this.completedTasks = new ArrayList<>();
        this.unpickedTasks = new ArrayList<>();

        this.initialCity = currentCity;
        this.city = currentCity;
        this.plan = new ArrayList<>();
    }

    public State(City startingCity, TaskSet carryingTasks) {
        this(startingCity);
        this.currentTasks.addAll(carryingTasks);
    }

    // constructor for A star
    public State(City startingCity, TaskSet carryingTasks, TaskSet newTasks) {
        this(startingCity);
        this.currentTasks.addAll(carryingTasks);
        // unpicked Task existed in the world, unpicked task = initial_all_tasks - picked_tasks - completed_tasks
        this.unpickedTasks.addAll(newTasks);
        this.unpickedTasks.removeAll(carryingTasks);
        this.unpickedTasks.removeAll(this.completedTasks);
    }

    public State(State other) {
        // we need to make a copy, the elements themselves are final
        initialCity = other.initialCity;
        city = other.city;
        plan = new ArrayList<>(other.plan);
        currentTasks = new ArrayList<>(other.currentTasks);
        completedTasks = new ArrayList<>(other.completedTasks);
        unpickedTasks = new ArrayList<>(other.unpickedTasks);
    }


    public int currentTaskWeights() {
        return currentTasks.stream().mapToInt(t -> t.weight).sum();
    }

    public Plan constructPlan() {
        return new Plan(initialCity, plan);
    }

    public State moveTo(City city) {
        State s = new State(this);

        s.plan.add(new Move(city));
        s.city = city;

        s.currentTasks.stream().filter(t -> t.deliveryCity == city).forEach(t -> {
            s.plan.add(new Delivery(t));
            s.completedTasks.add(t);
            s.unpickedTasks.remove(t);
        });
        s.currentTasks.removeIf(t -> t.deliveryCity == city);
        s.currentTasks.stream().forEach(t -> {s.unpickedTasks.remove(t);});

        //System.out.println("dropping of tasks, I carry now " + newState.currentTasks.size());
        return s;
    }

    private Set<Task> allTasks() {
        var r = new HashSet<Task>();

        r.addAll(currentTasks);
        r.addAll(completedTasks);

        return r;
    }

    public State pickUp(Set<Task> tasks, long capacity) {
        boolean tooFull = tasks.stream().mapToInt(t -> t.weight).sum() + currentTaskWeights() > capacity;
        var ats = allTasks();
        boolean alreadyPickedUp = tasks.stream().anyMatch(ats::contains);
        if (tooFull || alreadyPickedUp) {
            return null;
        }

        State s = new State(this);
        s.plan.addAll(tasks.stream()
                .map(Pickup::new)
                .collect(Collectors.toList()));
        s.currentTasks.addAll(tasks);
        s.unpickedTasks.removeAll(tasks);

        return s;
    }

    public double profit(long costPerKm) {
        return this.completedTasks.stream().mapToLong(t -> t.reward).sum() - constructPlan().totalDistance() * costPerKm;
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
