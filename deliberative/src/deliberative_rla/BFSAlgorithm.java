package deliberative_rla;

import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.*;

public class BFSAlgorithm implements IAlgorithm {
    private int depthLimit;
    private SearchTree tree;
    private int capacity;
    private long costPerKm;
    
    public BFSAlgorithm(int capacity, long costPerKm, int depthLimit) {
        this.capacity = capacity;
        this.depthLimit = depthLimit;
        this.costPerKm = costPerKm;
        this.tree = new SearchTree();
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
        // TODO add nodes to tree etc...
        // TODO circle detection maybe inside of SearchTree?
        
        if (!carryingTasks.isEmpty()) {
            throw new IllegalArgumentException("carryingtasks is currently not supported");
        }
        
        var rootState = new State(startingCity);
        tree = new SearchTree(rootState);
        
        var allStates = new HashSet<State>();
        var statesToProcess = new ArrayList<State>();
        statesToProcess.add(rootState);
    
        HashMap<Topology.City, ArrayList<Task>> tasksPerCity = Utils.taskPerCity(newTasks);
        
        // 1. building tree
        
        // TODO add depthLimit
        while (!statesToProcess.isEmpty()) {
            // there are two steps
            // 1. decide to pick anything up
            // 2. decide where to move
            var nextStatesToProcess = new ArrayList<State>();
            var pickedUpStates = new ArrayList<State>();
            
            for (final var state : statesToProcess) {
                // picking up, if total weight allows it
                
                // only picking up a single item for now
                pickedUpStates.add(state); // do not pick anything up
    
                // picking up multiple tasks -> TODO create product(tasks), including the empty set (no picking up)
                // easiest to create a list of possible tasks to pick up, then filter them (if totalNewWeight <= capacity)
                if (tasksPerCity.containsKey(state.city)) {
                    for (final var task : tasksPerCity.get(state.city)) {
                        // We do not allow:
                        //  - picking up a task that does not fit the capacity
                        //  - already completed tasks
                        //  - already picked up tasks
                        if (state.currentTaskWeights() + task.weight <= this.capacity
                                && !state.completedTasks.contains(task)
                                && !state.currentTasks.contains(task)) {
                            pickedUpStates.add(state.pickUp(task));
                        }
                    }
                }
            }
            
            for (final var state: pickedUpStates) {
                for (final var neighbor: state.city.neighbors()) {
                    nextStatesToProcess.add(state.moveTo(neighbor));
                }
            }
            
            // remove the circles
            nextStatesToProcess.removeIf(allStates::contains);
            allStates.addAll(nextStatesToProcess);
            statesToProcess = nextStatesToProcess;
        }
        
        // 2. getting best path
        var theChosenOne = allStates.stream().filter(s -> s.completedTasks.containsAll(newTasks)).max(Comparator.comparing(s -> s.profit(this.costPerKm)));
        
        if (theChosenOne.isPresent()) {
            return theChosenOne.get().plan;
        } else {
            // TODO set breakpoint here
            throw new IllegalStateException("woops");
        }
    }
}

