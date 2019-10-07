package reactive_rla;

import logist.task.TaskDistribution;
import logist.topology.Topology;

import java.util.*;
import java.util.stream.Collectors;

public class ReactiveWorld {
    private Topology topology;
    private TaskDistribution taskDistribution;
    private double discountFactor;
    private double costPerKm;
    private List<State> states;


    public ReactiveWorld(Topology topology, TaskDistribution taskDistribution, double discountFactor, double costPerKm) {
        this.topology = topology;
        this.taskDistribution = taskDistribution;
        this.discountFactor = discountFactor;
        this.costPerKm = costPerKm;

        this.states = initStates();

        if (Config.TESTING && Config.VERBOSITY_LEVEL >= 20) {
            System.out.println("All possible states:");
            this.states.forEach(s -> System.out.println(s));
        }
    }

    private List<State> initStates() {
        var states = new ArrayList<State>();
        for (final var origin : topology.cities()) {
            for (final var destination : Utils.getReachableCities(origin)) {
                // using Utils.getReachableCities(origin) is a bit of an overkill but this will reflect the topology
                states.add(new State(origin, destination).createActions(this.taskDistribution, this.costPerKm));
            }

            // the "pick up" state
            states.add(new State(origin, null).createActions(this.taskDistribution, this.costPerKm));
        }

        return states;
    }

    /**
     * Uses value iteration to find the optimal strategy.
     * Algorithm:
     * <pre>
     * repeat
     *   for s ∈ State do
     *     for a ∈ Actions do
     *       Q(s, a) ← R(s, a) + γ * Σ(s'∈S)(T(s, a, s') * V(s'))
     *     end for
     *     V(S) ← max(a)(Q(s, a))
     *   end for
     * until good enough
     * </pre>
     * In our case:
     * T(s, a, s') = Pr[s -(a)-> s'], this is given by the library
     * R(s, a) = 1(task accepted) * r(s, a) - cost(s, a);  1(...) denoting an indicator function
     * note that the cost is always added.
     * TODO this might be wrong? it will lead to an agent that prefers picking up a lot, no?
     *
     * @return
     */
    public HashMap<State, AgentAction> valueIteration() {
        var valueTable = new HashMap<State, Double>();     // State, reward
        var prevValueTable = new HashMap<State, Double>();
        var best = new HashMap<State, AgentAction>();
        var qTable = new HashMap<State, HashMap<AgentAction, Double>>(); // TODO not sure if AgentAction should be replaced with City

        // Init of tables
        for (final var state : states) {
            valueTable.put(state, 0.0);
            best.put(state, new AgentAction());

            var t = new HashMap<AgentAction, Double>();
            t.putAll(state.getActions().stream().collect(Collectors.toMap(a -> a, a -> 0.0)));

            if (t.size() != state.getActions().size()) {
                throw new IllegalStateException("some double keys in the actions" + state);
            }

            if (qTable.containsKey(state)) {
                throw new IllegalStateException("qTable already has that key: " + state);
            }
            qTable.put(state, t);
        }

        if (Config.TESTING && Config.VERBOSITY_LEVEL >= 10) {
            final var magicCity = "Paris";
            final var magicTarget = "Brest";
            var dummyState = new State(topology.parseCity(magicCity), topology.parseCity(magicTarget));
            var sNull = qTable.containsKey(dummyState);
            var sNullWithActions = qTable.containsKey(dummyState.createActions(taskDistribution, 0.0));

            System.out.println("did we get " + magicCity + "? " + sNull + ", " + sNullWithActions + dummyState);
        }

        // Value iteration
        do {
            // super important to make a deep copy of the previous vTable
            prevValueTable = new HashMap<>(valueTable.entrySet().stream()
                    .collect(Collectors.toMap(v -> v.getKey(), v -> v.getValue().doubleValue())));

            for (final var state : states) {
                if (Config.TESTING && Config.VERBOSITY_LEVEL >= 20) {
                    System.out.println("considering state " + state);
                }
                for (final var action : state.getActions()) {
                    if (Config.TESTING && Config.VERBOSITY_LEVEL >= 20) {
                        System.out.println("considering state's action" + state + " " + action);
                    }

                    double sum = 0.0;
                    double sumProba = 0.0; // Used to calculate the `not taking the package` case

                    // pick up selected
                    for (final var possibleDestination : Utils.getReachableCities(state.getCurrentCity())) {
                        final var v = valueTable.get(new State(state.getCurrentCity(), possibleDestination));
                        final var p = taskDistribution.probability(state.getCurrentCity(), possibleDestination);
                        sum += v * p;
                        sumProba += p;
                    }

                    // moving on
                    sum += valueTable.get(new State(state.getCurrentCity(), null)) * (1 - sumProba);

                    final double costOfTravel = Utils.costOfTravel(state.getCurrentCity(), action.getDestination(), costPerKm);
                    final double estimatedReward = taskDistribution.reward(state.getCurrentCity(), action.getDestination());
                    if (action.getDestination().equals(state.getDestination())) {
                        // pick up selected
                        qTable.get(state).put(action, estimatedReward - costOfTravel + discountFactor * sum);
                    } else {
                        // moving on
                        qTable.get(state).put(action, -1 * costOfTravel + discountFactor * sum);
                    }
                }

                if (!qTable.containsKey(state)) {
                    System.out.println("We don't have such a state in our qTable:" + state);
                    throw new IllegalStateException("something is off");
                }

                var theChosenOne = Collections.max(qTable.get(state).entrySet(),
                        Map.Entry.comparingByValue());
                valueTable.put(state, theChosenOne.getValue());
                best.put(state, theChosenOne.getKey());
            }

        } while (!convergenceReached(valueTable, prevValueTable, Config.VALUE_ITERATION_THRESHOLD));

        return best;
    }

    private boolean convergenceReached(HashMap<State, Double> vTableA, HashMap<State, Double> vTableB, double epsilon) {
        // this would be much easier in a real programming language, e.g. Python:
        // `diff = sum(a - b for a, b in zip(vTableA.values(), vTableB.values())`
        final var valsA = vTableA.values().toArray();
        final var valsB = vTableB.values().toArray();

        if (valsA.length != valsB.length) {
            System.out.println("huh? not same size?");
            return false;
        }

        double diff = 0.0;
        for (int i = 0; i < valsA.length; i++) {
            diff += (double) valsA[i] - (double) valsB[i];
        }

        return diff <= epsilon;
    }
}

