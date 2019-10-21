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

public class BaseAlgorithm implements IAlgorithm {
    private int capacity;
    private long costPerKm;
    private boolean applyAStar = true;
    
    public BaseAlgorithm(int capacity, long costPerKm, boolean applyAStar) {
        this.capacity = capacity;
        this.costPerKm = costPerKm;
        this.applyAStar = applyAStar;
    }
    
    public BaseAlgorithm(int capacity, long costPerKm) {
        this(capacity, costPerKm, true);
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
        final Set allStates = new HashSet<State>();
        // forcing an order, mostly important for AStar
        List<State> statesToProcess = List.of(rootState(startingCity, carryingTasks));
        
        // this includes the power set of the available tasks
        HashMap<Topology.City, Set<Set<Task>>> tasksPerCity = Utils.taskPerCity(newTasks);
        
        long reachedDepth = 0;
        
        var startTime = LocalDateTime.now();
        
        // 1. build tree
        while (!statesToProcess.isEmpty()) {
            // 1.1.1. picking up packages (if available)
            var pickedUpStates = statesToProcess.parallelStream()
                    .filter(s -> tasksPerCity.containsKey(s.city))
                    .flatMap(s -> tasksPerCity.get(s.city).stream()
                            // due to the above filter, we could also have filled tasksPerCity with empty lists...
                            // this should not be necessary, as the set-of-all-tasks should take care of duplicates
                            .filter(Predicate.not(Set::isEmpty))
                            .map(ts -> s.pickUp(ts, this.capacity))
                            .filter(Objects::nonNull))
                    .collect(Collectors.toList());
            
            // 1.1.2. don't pick anything up
            pickedUpStates.addAll(statesToProcess);
            
            // 1.1.3. moving to a new city
            List<State> nextStatesToProcess;
            if (this.applyAStar) {
                nextStatesToProcess = pickedUpStates.parallelStream()
                        .map(s -> AStarAlgorithm.Astar(s, this.costPerKm))
                        .collect(Collectors.toList());
            } else {
                nextStatesToProcess = pickedUpStates.parallelStream()
                        .flatMap(s -> s.city.neighbors().stream()
                                .map(s::moveTo)
                                .filter(ns -> !Utils.hasUselessCircle(ns)))
                        .collect(Collectors.toList());
            }
            
            var dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            System.out.print(">>> " + dtf.format(LocalDateTime.now()) + " >>> in depth " + reachedDepth
                    + " new states pre / post circle & duplicate detection " + nextStatesToProcess.size() + " / ");
            
            // 1.2.1. remove states that achieve the same but take longer, only for A* algorithm
            if (this.applyAStar) {
                nextStatesToProcess.removeIf(allStates::contains);
            }

            System.out.println(nextStatesToProcess.size());
            
            // 1.2.2. keeping track of all new states, used for "circle detection"
            // Note that our implementation of hashCode and equals has been overwritten, now only states with
            // a lower cost but same result will be added to the set.
            //nextStatesToProcess.stream().filter(s -> !allStates.contains(s)).forEach(s -> allStates.add(s));
            // TODO: bug here
            allStates.addAll(nextStatesToProcess);
            
            // 1.2.3. we don't need to spawn new states originating from these ones which already completed all tasks
            // TODO: bug here
            statesToProcess = nextStatesToProcess.parallelStream()
                    .filter(s -> !s.completedTasks.containsAll(newTasks))
                    .collect(Collectors.toCollection(ArrayList::new));
            
            // TODO AStar sort next states
            
            reachedDepth += 1;
            
            System.out.println("In depth " + reachedDepth + ", total nb of states: "
                    + allStates.size() + " new states to check: " + nextStatesToProcess.size());
            
            var theChosenOne = nextStatesToProcess.stream()
                    .filter(s -> s.completedTasks.containsAll(newTasks))
                    .max(Comparator.comparing(s -> s.profit(this.costPerKm)));

            if (theChosenOne.isPresent()) {
                printReport(theChosenOne.get(), reachedDepth, allStates.size(), startTime);
                return theChosenOne.get().constructPlan();
            }
        }
        // reached goal at depth: 13, total nb of states: 979885, took 1.77541s, profit of: 254657.0
        
        // 2. getting best plan, taking the one with max profit
        // TODO why the fuck does it need the casts down here but in the loop?
        var theChosenOne = allStates.stream()
                .filter(s -> ((State)s).completedTasks.containsAll(newTasks))
                .max(Comparator.comparing(s -> ((State)s).profit(this.costPerKm)));
        
        if (theChosenOne.isPresent()) {
            printReport((State) theChosenOne.get(), reachedDepth, allStates.size(), startTime);
            return ((State)theChosenOne.get()).constructPlan();
        } else {
            throw new IllegalStateException("woops");
        }
    }

    /**
     * used for the check in A* algorithm (not adding states
     * @param startingCity
     * @param carryingTasks
     * @return
     */
    public State rootState(City startingCity, TaskSet carryingTasks) {
        if (this.applyAStar) {
            return new AStarState(startingCity, carryingTasks);
        } else {
            return new State(startingCity, carryingTasks);
        }
    }
    
    private void printReport(State goal, long reachedDepth, long nStates, LocalDateTime startTime) {
        var endTime = LocalDateTime.now();
        System.out.print("reached goal at depth: " + reachedDepth);
        System.out.print(", total nb of states: " + nStates);
        System.out.print(", took " + Utils.humanReadableFormat(Duration.between(startTime, endTime)));
        System.out.print(", profit of: " + goal.profit(this.costPerKm));
    }
}

