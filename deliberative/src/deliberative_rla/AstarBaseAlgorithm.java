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

        State initState = new State(startingCity, carryingTasks, newTasks);
        var allStates = new HashSet<State>();
        var statesToProcess = new ArrayList<State>();
        statesToProcess.add(initState);

        // this includes the power set of the available tasks
        HashMap<Topology.City, Set<Set<Task>>> tasksPerCity = Utils.taskPerCity(newTasks);

        System.out.println("initial city is " + initState.city.name);

        long reachedDepth = 0;

        var startTime = LocalDateTime.now();


        // 1. build tree
        while (!statesToProcess.isEmpty()) {

            var pickedUpStates = statesToProcess.parallelStream()
                    .filter(s -> tasksPerCity.containsKey(s.city))
                    .flatMap(s -> tasksPerCity.get(s.city).stream()
                            // due to the above filter, we could also have filled tasksPerCity with empty lists...
                            // this should not be necessary, as the set-of-all-tasks should take care of duplicates
                            .filter(ts -> !ts.isEmpty())
                            .map(ts -> s.pickUp(ts, this.capacity))
                            .filter(Objects::nonNull))
                    .collect(Collectors.toList());

            pickedUpStates.addAll(statesToProcess);

            List<State> nextStatesToProcess;


           // the city of nextStatesToProcess is the current city
           nextStatesToProcess = pickedUpStates.parallelStream().collect(Collectors.toCollection(ArrayList::new));

           // best state is evaluated with minimum cost, the city of the bestNextState is one of the neighbor city
           State bestNextState = null;
           double distance = Double.MAX_VALUE;
           for(State state : nextStatesToProcess){
               // I moved the Astar function from AStarAlgorithm to the State
               state.Astar(); //to calculate the distance
               if(state.hueristicDistance < distance){
                   distance = state.hueristicDistance;
                   bestNextState = state.Astar();
               }
           }



            System.out.println("before the size of states to Process is " + nextStatesToProcess.size());

            System.out.println("ASTAR is running!");
            var dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            System.out.print(">>> " + dtf.format(LocalDateTime.now()) + " >>> in depth " + reachedDepth
                    + " new states pre / post circle & duplicate detection " + nextStatesToProcess.size() + " / ");


            // 1.2.1. remove duplicates and circles
            nextStatesToProcess.removeIf(allStates::contains);


            // 1.2.2. keeping track of all new states, used for "circle detection"
            // Note that our implementation of hashCode and equals has been overwritten, now only states with
            // a lower cost but same result will be added to the set.
//            statesToProcess.poll();
            allStates.add(bestNextState);

//            System.out.println("size of allStates is " + allStates.size());

            // 1.2.3. we don't need to spawn new states originating from these ones which already completed all tasks
            var tempStatesToProcess = nextStatesToProcess.parallelStream()
                    .filter(s -> !s.completedTasks.containsAll(newTasks))
                    .collect(Collectors.toCollection(ArrayList::new));


            // TODO AStar sort next states
                // statesToProcess is a PriorityQueue, so the it will ordering all the states with order of increasing cost
//            statesToProcess.removeAll(State);
            // the size of stateToProcess should always be 1;
            statesToProcess.remove(0);
            statesToProcess.add(bestNextState);
            if(statesToProcess.size() != 1){
                throw new IllegalArgumentException("the size of stateToProcess should always be 1");
            }

            reachedDepth += 1;

            System.out.println("In depth " + reachedDepth + ", total nb of states: "
                    + allStates.size() + " new states to check: " + nextStatesToProcess.size());

            if (this.useEarlyStop) {
                var theChosenOne = nextStatesToProcess.stream()
                        .filter(s -> s.completedTasks.containsAll(newTasks))
                        .max(Comparator.comparing(s -> s.profit(this.costPerKm)));

                if (theChosenOne.isPresent()) {
                    printReport(theChosenOne.get(), reachedDepth, allStates.size(), startTime);
                    return theChosenOne.get().constructPlan();
                }
            }
        }

        // 2. getting best plan, taking the one with max profit
        var theChosenOne = allStates.stream()
                .filter(s -> s.completedTasks.containsAll(newTasks))
                .max(Comparator.comparing(s -> s.profit(this.costPerKm)));

        if (theChosenOne.isPresent()) {
            printReport(theChosenOne.get(), reachedDepth, allStates.size(), startTime);
            return theChosenOne.get().constructPlan();
        } else {
            throw new IllegalStateException("woops");
        }

    }

    private void printReport(State goal, long reachedDepth, long nStates, LocalDateTime startTime) {
        System.out.print("reached goal at depth: " + reachedDepth);
        System.out.print(", total nb of states: " + nStates);
        System.out.print(", took " + Utils.humanReadableFormat(Duration.between(startTime, LocalDateTime.now())));
        System.out.print(", profit of: " + goal.profit(this.costPerKm));
        System.out.println(", using early stopping: " + this.useEarlyStop);
    }
}

