package deliberative_rla;

import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.*;
import java.util.stream.Collector;
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
        if (!carryingTasks.isEmpty()) {
            throw new IllegalArgumentException("carryingtasks is currently not supported");
        }
        
        var rootState = new State(startingCity);
        
        // TODO optimisation idea: store only the new states or states that have collected and distributed all tasks
        // -> otherwise they still need to move -> circle detection needs to be done inside of state
        var allStates = new HashSet<State>();
        var statesToProcess = new ArrayList<State>(Arrays.asList(rootState));
    
        // TODO move the powerset computation here
        HashMap<Topology.City, Set<Set<Task>>> tasksPerCity = Utils.taskPerCity(newTasks);
        
        // 1. building tree
        
        // TODO currently this get's us a timeout
        long reachedDepth = 0;
        // either we don't have new states to process or
        // we reached the max search depth, but not we haven't found a solution which haven't delivered eveything.
        long prevStateSize = -1;
        while (!statesToProcess.isEmpty()){
                //&& (reachedDepth < this.depthLimit
                //    || allStates.stream().noneMatch(s -> s.completedTasks.containsAll(newTasks)))
                //&& prevStateSize != allStates.size()) {
            // there are two steps
            // 1. decide to pick anything up
            // 2. decide where to move
    
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
    
            // remove the circles
            System.out.print("in depth " + reachedDepth + " new states pre / post circle & duplicate detection " + nextStatesToProcess.size() + " / ");
            nextStatesToProcess.removeIf(allStates::contains); // remove duplicates and circles
            System.out.println(nextStatesToProcess.size());
            prevStateSize = allStates.size();
            allStates.addAll(nextStatesToProcess);
            statesToProcess = nextStatesToProcess;
            reachedDepth += 1;
            
            
            System.out.println("In depth " + reachedDepth + ", total nb of states: " + allStates.size() + " new states to check: " + nextStatesToProcess.size());
        }
    
        //var statesWhichTakeAll = allStates.stream().filter(newTasks::contains).collect(Collectors.toList());
        //var longestPath = allStates.stream().max(Comparator.comparing(s -> s.pathTaken.size()));
        
        System.out.println("Out of the loop in depth " + reachedDepth + ", total nb of states: " + allStates.size());
        // 2. getting best path
        var theChosenOne = allStates.stream().filter(s -> s.completedTasks.containsAll(newTasks)).max(Comparator.comparing(s -> s.profit(this.costPerKm)));
        //var theChosenOne = allStates.stream().max(Comparator.comparing(s -> s.profit(this.costPerKm)));
        System.out.println("he has a profit of " + theChosenOne.get().profit(this.costPerKm));
        
        if (theChosenOne.isPresent()) {
            return theChosenOne.get().plan;
        } else {
            // TODO set breakpoint here
            throw new IllegalStateException("woops");
        }
    }
}

