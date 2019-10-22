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

// Algorithm:BFS, reached goal at depth: 17, total nb of states: 173857, took 1.39698s, profit of: 389802.0
// Algorithm:ASTAR, reached goal at depth: 30686, total nb of states: 44585, took 2.030551s, profit of: 389802.0

public class BFSAlgorithm implements IAlgorithm {
    private int capacity;
    private long costPerKm;
    private boolean useSaneState;

    public BFSAlgorithm(int capacity, long costPerKm, boolean useSaneState) {
        this.capacity = capacity;
        this.costPerKm = costPerKm;
        this.useSaneState = useSaneState;

        System.out.println("Using BFS with sane state? " + useSaneState);
    }

    public BFSAlgorithm(int capacity, long costPerKm) {
        this(capacity, costPerKm, false);
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
        Set<Task> taskToProcess = new HashSet<>();
        taskToProcess.addAll(newTasks);
        taskToProcess.addAll(carryingTasks);

        Set<State> allStates = new HashSet<>();
        Set<State> statesToProcess;
        if (this.useSaneState) {
            statesToProcess = Set.of(new AStarState(startingCity, carryingTasks, newTasks));
        } else {
            statesToProcess = Set.of(new State(startingCity, carryingTasks));
        }

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
                    .collect(Collectors.toList());

            // 1.1.2. don't pick anything up
            pickedUpStates.addAll(statesToProcess);

            // 1.1.3. moving to a new city
            List<State> nextStatesToProcess = pickedUpStates.parallelStream()
                    .flatMap(s -> s.city.neighbors().stream()
                            .map(s::moveTo)
                            .filter(ns -> !Utils.hasUselessCircle(ns)))
                    .collect(Collectors.toList());

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
                    .filter(s -> !s.completedTasks.containsAll(taskToProcess))
                    .collect(Collectors.toSet());

            reachedDepth += 1;

            System.out.println("In depth " + reachedDepth + ", total nb of states: "
                    + allStates.size() + " new states to check: " + nextStatesToProcess.size());

            var theChosenOne = nextStatesToProcess.stream()
                    .filter(s -> s.completedTasks.containsAll(taskToProcess))
                    .max(Comparator.comparing(s -> s.profit(this.costPerKm)));

            if (theChosenOne.isPresent()) {
                Utils.printReport("BFS", theChosenOne.get(), reachedDepth, allStates.size(), startTime, this.costPerKm);
                return theChosenOne.get().constructPlan();
            }
        }

        // 2. getting best plan, taking the one with max profit
        var theChosenOne = allStates.stream()
                .filter(s -> s.completedTasks.containsAll(taskToProcess))
                .max(Comparator.comparing(s -> s.profit(this.costPerKm)));

        if (theChosenOne.isPresent()) {
            Utils.printReport("BFS", theChosenOne.get(), reachedDepth, allStates.size(), startTime, this.costPerKm);
            return theChosenOne.get().constructPlan();
        } else {
            throw new IllegalStateException("woops");
        }
    }

}

