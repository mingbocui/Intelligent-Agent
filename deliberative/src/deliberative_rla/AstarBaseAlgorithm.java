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
//        var allStates = new HashSet<State>();
        AstarComparator astarComparator = new AstarComparator();
        //TODO from the output is unordered, check my implementation of comparator
        // I updated the AStarDistance in the moveTo() and pickUp() also
        Queue<State> statesToProcess = new PriorityQueue<>(astarComparator);
        statesToProcess.add(initState);

        Map<State, Double> visitedStates = new HashMap<>();

        // this includes the power set of the available tasks
        HashMap<Topology.City, Set<Set<Task>>> tasksPerCity = Utils.taskPerCity(newTasks);


        long reachedDepth = 0;

        var startTime = LocalDateTime.now();

        // 1. build tree
        while (!statesToProcess.isEmpty()) {

            System.out.println("depth " + reachedDepth + " starts");
            statesToProcess.stream().forEach(s->{System.out.println(s.getAStarDistance());});

            State stateIsProcessing = statesToProcess.poll();
            System.out.println("state located in city " + stateIsProcessing.city.name + " with current tasks " + stateIsProcessing.currentTasks.size()
                                                        + ", with completed tasks " + stateIsProcessing.completedTasks.size() + ", with unpicked tasks " + stateIsProcessing.unpickedTasks.size());
            if (stateIsProcessing.completedTasks.containsAll(newTasks))
                return stateIsProcessing.constructPlan();

            // TODO very stupid implementation, just want to deploy the stream()
            List<State> tempList = new ArrayList<>();
            tempList.add(stateIsProcessing);

            var pickedUpStates = tempList.parallelStream()
                    .filter(s -> tasksPerCity.containsKey(s.city))
                    .flatMap(s -> tasksPerCity.get(s.city).stream()
                            // due to the above filter, we could also have filled tasksPerCity with empty lists...
                            // this should not be necessary, as the set-of-all-tasks should take care of duplicates
                            .filter(ts -> !ts.isEmpty())
                            .map(ts -> s.pickUp(ts, this.capacity))
                            .filter(Objects::nonNull))
                    .collect(Collectors.toList());

            var nextStatesToProcess = pickedUpStates.parallelStream()
                    .flatMap(s -> s.city.neighbors().stream()
                            .map(s::moveTo)
                            .filter(ns -> !Utils.hasUselessCircle(ns)))
                    .collect(Collectors.toCollection(ArrayList::new));

            //TODO not sure
            //pickedUpStates.addAll(statesToProcess);
            //TODO the equals function of the state
            if (!visitedStates.containsKey(stateIsProcessing)) {
                visitedStates.put(stateIsProcessing, stateIsProcessing.getAStarDistance());
                statesToProcess.addAll(nextStatesToProcess);
            }
            // maintain smaller cost
            else if(visitedStates.get(stateIsProcessing) >= stateIsProcessing.getAStarDistance()){
                visitedStates.replace(stateIsProcessing, stateIsProcessing.getAStarDistance());
            }

            System.out.println("depth " + reachedDepth + " ended \n");
            reachedDepth++;

        }
        return Plan.EMPTY;
    }
}


