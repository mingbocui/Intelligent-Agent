package deliberative_rla;

import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.*;
import java.util.stream.Collectors;

public class BFSAlgorithm implements IAlgorithm {
    private int depthLimit;
    private int capacity;
    private long costPerKm;
    
    public BFSAlgorithm(int capacity, long costPerKm, int depthLimit) {
        this.capacity = capacity;
        this.depthLimit = depthLimit;
        this.costPerKm = costPerKm;
    }
    
    /**
     * The basic idea is:
     *  - at each state the agent can:
     *      - make a move to another city, and
     *      - pick something up
     *  - dropping of a package is done automatically
     *  - the cost of each node
     *
     * @param startingCity Starting city to branch to look for a new solution.
     * @param carryingTasks The tasks that the agent is currently holding, initially an empty set.
     * @param newTasks The new tasks that should be added to the plan. Can be empty later (if no new task is available but we have to change our path) (if no new task is available but we have to change our path).
     * @return
     */
    @Override
    public Plan optimalPlan(City startingCity, TaskSet carryingTasks, TaskSet newTasks) {
        // TODO optimisation idea: store only the new states or states that have collected and distributed all tasks
        // -> otherwise they still need to move -> circle detection needs to be done inside of state
        var allStates = new HashSet<State>();
        var statesToProcess = new ArrayList<State>(Arrays.asList(new State(startingCity, carryingTasks)));
        
        HashMap<Topology.City, Set<Set<Task>>> tasksPerCity = Utils.taskPerCity(newTasks);
        
        long reachedDepth = 0;
        
        // 1. build tree
        while (!statesToProcess.isEmpty()) {
            var pickedUpStates = statesToProcess.parallelStream()
                    .filter(s -> tasksPerCity.containsKey(s.city))
                    .flatMap(s -> tasksPerCity.get(s.city).stream()
                            .map(ts -> s.pickUp(ts, this.capacity)))
                    .collect(Collectors.toCollection(ArrayList::new));
            
            pickedUpStates.addAll(statesToProcess);
            
            var nextStatesToProcess = pickedUpStates.parallelStream()
                    .flatMap(s -> s.city.neighbors().stream()
                            .map(s::moveTo)
                            .filter(ns -> !ns.movesInACircle()))
                    .collect(Collectors.toCollection(ArrayList::new));
            
            System.out.print("in depth " + reachedDepth + " new states pre / post circle & duplicate detection " + nextStatesToProcess.size() + " / ");
            nextStatesToProcess.removeIf(allStates::contains); // remove duplicates and circles
            System.out.println(nextStatesToProcess.size());
            
            allStates.addAll(nextStatesToProcess);
            statesToProcess = nextStatesToProcess;
            reachedDepth += 1;
            
            System.out.println("In depth " + reachedDepth + ", total nb of states: " + allStates.size() + " new states to check: " + nextStatesToProcess.size());
        }
        
        System.out.println("Out of the loop in depth " + reachedDepth + ", total nb of states: " + allStates.size());
        
        // 2. getting best path
        var theChosenOne = allStates.stream()
                .filter(s -> s.completedTasks.containsAll(newTasks))
                .max(Comparator.comparing(s -> s.profit(this.costPerKm)));
        
        if (theChosenOne.isPresent()) {
            System.out.println("he has a profit of " + theChosenOne.get().profit(this.costPerKm));
            return theChosenOne.get().plan;
        } else {
            throw new IllegalStateException("woops");
        }
    }
}

