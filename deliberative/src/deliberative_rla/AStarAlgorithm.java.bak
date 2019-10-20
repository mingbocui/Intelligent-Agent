package deliberative_rla;

import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology;

import java.util.*;
import java.util.stream.Collectors;

public class AStarAlgorithm implements IAlgorithm {

    private int capacity;
    private long costPerKm;

    public AStarAlgorithm(int capacity, long costPerKm) {
        this.capacity = capacity;
        this.costPerKm = costPerKm;
    }


    @Override
    public Plan optimalPlan(Topology.City startingCity, TaskSet carryingTasks, TaskSet newTasks) {
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
                                    .map(s -> s.Astar(this.costPerKm))
                                    .collect(Collectors.toCollection(ArrayList::new));

            System.out.print("in depth " + reachedDepth + " new states pre / post circle & duplicate detection " + nextStatesToProcess.size() + " / ");

            // remove duplicates and circles
            if (nextStatesToProcess.size() >= 500000 || allStates.size() > 40000) {
                nextStatesToProcess = nextStatesToProcess.parallelStream()
                        .filter(s -> !allStates.contains(s))
                        .collect(Collectors.toCollection(ArrayList::new));
            } else {
                nextStatesToProcess.removeIf(allStates::contains);
            }

            System.out.println(nextStatesToProcess.size());
            System.out.println("I am running ASTAR");

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
            return theChosenOne.get().constructPlan();
        } else {
            throw new IllegalStateException("woops");
        }
    }




}
