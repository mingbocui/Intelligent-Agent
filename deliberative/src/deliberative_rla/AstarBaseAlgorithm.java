package deliberative_rla;

import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AstarBaseAlgorithm implements IAlgorithm {
    private int capacity;
    private long costPerKm;
    private boolean useEarlyStop = true;
    private boolean applyAStar = true;

    public AstarBaseAlgorithm(int capacity, long costPerKm, boolean useEarlyStop, boolean applyAStar) {
        this.capacity = capacity;
        this.costPerKm = costPerKm;
        this.useEarlyStop = useEarlyStop;
        this.applyAStar = applyAStar;
    }

    public AstarBaseAlgorithm(int capacity, long costPerKm) {
        this(capacity, costPerKm, true, true);
    }

    public AstarBaseAlgorithm(int capacity, long costPerKm, boolean useEarlyStop) {
        this(capacity, costPerKm, useEarlyStop, true);
    }

    /**
     * The basic idea is:
     * - at each state the agent can:
     * - make a move to another city, and
     * - pick something up
     * - dropping of a package is done automatically
     * - the cost of each node
     *
     * @param startingCity  Starting city to branch to look for a new solution.
     * @param carryingTasks The tasks that the agent is currently holding, initially an empty set.
     * @param newTasks      The new tasks that should be added to the plan. Can be empty later (if no new task is available but we have to change our path) (if no new task is available but we have to change our path).
     * @return
     */
    @Override
    public Plan optimalPlan(City startingCity, TaskSet carryingTasks, TaskSet newTasks) {

        State initState = new State(startingCity, carryingTasks, newTasks, 0.0);
        var allStates = new HashSet<State>();
        //TODO from the output is unordered, check my implementation of comparator
        // I updated the AStarDistance in the moveTo() and pickUp() also
        Queue<State> stateQueue = new PriorityQueue<State>(new AstarComparator());
        stateQueue.add(initState);

        // note that visitedStates will say that it contains the key according to given equal method
        // check with the tests in DeliberativeAgent to see the examples.
        // It might be a good choice to provide a new class CircleState or something that does the equals and hashCode
        // as it is right now, and change the normal hashCode and equals method for State that does only the most basic stuff
        Map<State, Double> visitedStates = new HashMap<>();

        // this includes the power set of the available tasks
        HashMap<City, Set<Set<Task>>> tasksPerCity = Utils.taskPerCity(newTasks);

        long reachedDepth = 0;

        var startTime = LocalDateTime.now();

        while (!stateQueue.isEmpty()) {
            System.out.println("depth " + reachedDepth + " starts");
            //stateQueue.stream().forEach(s->{System.out.println(s.getAStarDistance());});

            State currentState = stateQueue.poll();
            System.out.println("state located in city " + currentState.city.name + " with current tasks " + currentState.currentTasks.size()
                                                        + ", with completed tasks " + currentState.completedTasks.size() + ", with unpicked tasks " + currentState.unpickedTasks.size());
            if (currentState.completedTasks.containsAll(newTasks))
                return currentState.constructPlan();

            // Spawn new states
            List<State> succ;
            if (tasksPerCity.containsKey(currentState.city)) {
                succ = tasksPerCity.get(currentState.city).stream()
                        .filter(ts -> !ts.isEmpty())
                        .map(ts -> currentState.pickUp(ts, this.capacity))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                succ.add(currentState);
            } else {
                succ = List.of(currentState);
            }

            succ = succ.stream()
                    .flatMap(s -> s.city.neighbors().stream()
                            .map(s::moveTo)
                            .filter(Predicate.not(State::hasUselessCircle)))
                    .collect(Collectors.toList());
            
            succ.removeIf(allStates::contains);
            allStates.addAll(succ);
            
            if (!visitedStates.containsKey(currentState)) {
                visitedStates.put(currentState, currentState.getAStarDistance());
                stateQueue.addAll(succ);
            } else if(visitedStates.get(currentState) >= currentState.getAStarDistance()){
                visitedStates.replace(currentState, currentState.getAStarDistance());
            }

            System.out.println("depth " + reachedDepth + " ended \n");
            reachedDepth++;

        }
        return Plan.EMPTY;
    }
}


