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
        var statesToProcess = new ArrayList<State>();
        statesToProcess.add(rootState);
    
        HashMap<Topology.City, ArrayList<Task>> tasksPerCity = Utils.taskPerCity(newTasks);
        
        // 1. building tree
        
        // TODO currently this get's us a timeout
        long reachedDepth = 0;
        // either we don't have new states to process or
        // we reached the max search depth, but not we haven't found a solution which haven't delivered eveything.
        while (!statesToProcess.isEmpty()
                && (reachedDepth < this.depthLimit
                    || allStates.stream().noneMatch(s -> s.completedTasks.containsAll(newTasks)))) {
            // there are two steps
            // 1. decide to pick anything up
            // 2. decide where to move
            var pickedUpStates = new ArrayList<State>();
            
            for (final var state : statesToProcess) {
                // 1. do not pick anything up
                pickedUpStates.add(state);
    
                // 2. picking up multiple tasks is possible, by taking the power-set
                // easiest to create a list of possible tasks to pick up, then filter them (if totalNewWeight <= capacity)
                if (tasksPerCity.containsKey(state.city)) {
                    for (final var selectedTasks : Utils.powerSet(new LinkedHashSet<>(tasksPerCity.get(state.city)))) {
                        // We do not allow (done in state.pickup):
                        //  - picking up a task that does not fit the capacity
                        //  - already completed tasks
                        //  - already picked up tasks
                        // In case we can't generate a new state, we're going to filter it out later.
                        pickedUpStates.add(state.pickUp(selectedTasks, this.capacity));
                    }
                }
            }
    
            var nextStatesToProcess = new ArrayList<State>();
            for (final var state: pickedUpStates) {
                for (final var neighbor: state.city.neighbors()) {
                    if (!state.wouldMoveInACircle(neighbor)) {
                        nextStatesToProcess.add(state.moveTo(neighbor));
                    }
                }
            }
            
            // remove the circles
            //System.out.print("in depth " + reachedDepth + " new states pre / post circle detection " + nextStatesToProcess.size() + " / ");
            nextStatesToProcess.removeIf(allStates::contains); // remove duplicates and circles
            //System.out.println(nextStatesToProcess.size());
            // TODO super aggressive optim, but we probably remove too many states, check the other optim comment above
            //allStates = new HashSet<>(nextStatesToProcess);
            allStates.addAll(nextStatesToProcess);
            statesToProcess = nextStatesToProcess;
            reachedDepth += 1;
            
            System.out.println("In depth " + reachedDepth + ", total nb of states: " + allStates.size() + " new states to check: " + nextStatesToProcess.size());
        }
    
    
        var statesWhichTakeAll = allStates.stream().filter(newTasks::contains).collect(Collectors.toList());
        var longestPath = allStates.stream().max(Comparator.comparing(s -> s.pathTaken.size()));
        
        
        System.out.println("Out of the loop in depth " + reachedDepth + ", total nb of states: " + allStates.size());
        // 2. getting best path
        var theChosenOneV0 = allStates.stream().filter(s -> s.completedTasks.containsAll(newTasks)).max(Comparator.comparing(s -> s.profit(this.costPerKm)));
        var theChosenOne = allStates.stream().max(Comparator.comparing(s -> s.profit(this.costPerKm)));
        System.out.println("he has a profit of " + theChosenOne.get().profit(this.costPerKm));
        
        if (theChosenOne.isPresent()) {
            return theChosenOne.get().plan;
        } else {
            // TODO set breakpoint here
            throw new IllegalStateException("woops");
        }
    }
}

