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
import java.util.stream.Collectors;

public class BFSAlgorithm implements IAlgorithm {
    private int capacity;
    private long costPerKm;
    
    public BFSAlgorithm(int capacity, long costPerKm) {
        this.capacity = capacity;
        this.costPerKm = costPerKm;
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
        var allStates = new HashSet<State>();
        var statesToProcess = new HashSet<>(Set.of(new State(startingCity, carryingTasks)));
        
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
                            .filter(ts -> !ts.isEmpty())
                            .map(ts -> s.pickUp(ts, this.capacity))
                            .filter(Objects::nonNull))
                    .collect(Collectors.toSet());
            
            // 1.1.2. don't pick anything up
            pickedUpStates.addAll(statesToProcess);
            
            // 1.1.3. moving to a new city
            var nextStatesToProcess = pickedUpStates.parallelStream()
                    .flatMap(s -> s.city.neighbors().stream()
                            .map(s::moveTo)
                            .filter(ns -> !Utils.hasUselessCircle(ns)))
                    .collect(Collectors.toSet());
            
            var dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            System.out.print(">>> " + dtf.format(LocalDateTime.now()) + " >>> in depth " + reachedDepth
                    + " new states pre / post circle & duplicate detection " + nextStatesToProcess.size() + " / ");
            
            // 1.2.1. remove duplicates and circles
            nextStatesToProcess.removeIf(allStates::contains);
            
            System.out.println(nextStatesToProcess.size());
            
            // 1.2.2. keeping track of all new states, used for "circle detection"
            // Note that our implementation of hashCode and equals has been overwritten, now only states with
            // a lower cost but same result will be added to the set.
            allStates.addAll(nextStatesToProcess);
            
            // 1.2.3. we don't need to spawn new states originating from these ones which already completed all tasks
            statesToProcess = nextStatesToProcess.parallelStream()
                    .filter(s -> !s.completedTasks.containsAll(newTasks))
                    .collect(Collectors.toCollection(HashSet::new));
            
            reachedDepth += 1;
            
            System.out.println("In depth " + reachedDepth + ", total nb of states: "
                    + allStates.size() + " new states to check: " + nextStatesToProcess.size());
        }
        
        System.out.println("Out of the loop in depth " + reachedDepth + ", total nb of states: " + allStates.size());
        
        // 2. getting best plan, taking the one with max profit
        var theChosenOne = allStates.stream()
                .filter(s -> s.completedTasks.containsAll(newTasks))
                .max(Comparator.comparing(s -> s.profit(this.costPerKm)));
        
        System.out.println("Took " + Utils.humanReadableFormat(Duration.between(startTime, LocalDateTime.now())) + " to run BFS");
        
        if (theChosenOne.isPresent()) {
            System.out.println("he has a profit of " + theChosenOne.get().profit(this.costPerKm));
            return theChosenOne.get().constructPlan();
        } else {
            throw new IllegalStateException("woops");
        }
    }
}

