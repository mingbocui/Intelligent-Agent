package reactive_rla;

import logist.agent.Agent;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import org.apache.commons.math.util.MathUtils;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class ReactiveWorld {
    private Topology topology;
    private TaskDistribution taskDistribution;
    private Agent agent;
    private double discountFactor;
    private double costPerKm;
    private HashMap<State, AgentAction> stateActionTable;
    //private HashMap<StateActionPair, Double> valueTable;
    private List<State> states;
    private HashMap<State, Double> valueTable;


    public ReactiveWorld(Topology topology, TaskDistribution taskDistribution, Agent agent, double discountFactor, double costPerKm) {
        this.topology = topology;
        this.taskDistribution = taskDistribution;
        this.agent = agent;
        this.discountFactor = discountFactor;
        this.costPerKm = costPerKm;

        this.states = initStates();
    }

    public Topology.City getBestNextCity(State state) {
        // TODO implement this
        Topology.City bestCity = null;
        double maxReward = -Double.MAX_VALUE;

        var currentCity = state.getCurrentCity();
        for (final var neighborCity : Utils.getReachableCities(currentCity)) {
            double tempReward = taskDistribution.reward(currentCity, neighborCity);
            if (tempReward > maxReward) {
                bestCity = neighborCity;
                maxReward = tempReward;
            }
        }
        return bestCity;
    }

    private List<State> initStates() {
        var states = new ArrayList<State>();
        for (final var origin : topology.cities()) {
            for (final var destination : Utils.getReachableCities(origin)) { // this is a bit of an overkill,
                // but this will reflect the topology
                var state = new State(origin, destination);
                state.createActions(this.taskDistribution, this.costPerKm);

                states.add(state);
            }

            // the "pick up" state
            states.add(new State(origin, null));
        }
        return states;
    }

    /**
     * Uses value iteration to find the optimal strategy.
     * <p>
     * Algorithm:
     * <p>
     * repeat
     * for s ∈ S do
     * for a ∈ A do
     * Q(s, a) ← R(s, a) + γ * Σ(s'∈S) T (s, a, s') * V(s)
     * end for
     * V (S) ← max(a) Q(s, a)
     * end for
     * until good enough
     * <p>
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
        var prevValueTable = new HashMap<State, Double>(); // bitchy Java complains otherwise
        var best = new HashMap<State, AgentAction>();
        var qTable = new HashMap<State, HashMap<AgentAction, Double>>(); // TODO not sure if AgentAction should be replaced with City

        var rnd = new Random();

        // Init of tables
        for (var state : states) {
            valueTable.put(state, 0.0);
            best.put(state, new AgentAction());

            var t = new HashMap<AgentAction, Double>();
            // TODO null pointer here?
            t.putAll(state.getActions().stream().collect(Collectors.toMap(a -> a, a -> 0.0)));
            qTable.put(state, t);
        }


        do {
            prevValueTable = new HashMap<>(valueTable);
            for (final var state : states) {
                if (Config.TESTING) {
                    System.out.println("considering state " + state.getCurrentCity() + " having " + state.getActions().size() + " states");
                }
                for (final var action : state.getActions()) {
                    if (Config.TESTING) {
                        System.out.println("considering state " + state.getCurrentCity() + " action: " + action.getDestination());
                    }

                    double sum = 0.0;
                    double sumProba = 0.0;

                    // pick up selected
                    for (final var possibleDestination : Utils.getReachableCities(state.getCurrentCity())) {
                        final var v = valueTable.get(new State(state.getCurrentCity(), possibleDestination));
                        final var p = taskDistribution.probability(state.getCurrentCity(), possibleDestination);
                        sum += v * p;
                        sumProba += p;
                    }

                    // moving on
                    sum += valueTable.get(new State(state.getCurrentCity(), null)) * (1 - sumProba);

                    final double costOfTravel = state.getCurrentCity().distanceTo(action.getDestination()) * costPerKm;
                    final double estimatedReward = taskDistribution.reward(state.getCurrentCity(), action.getDestination());
                    if (action.getDestination().equals(state.getDestination())) {
                        // pick up selected
                        qTable.get(state).put(action, estimatedReward - costOfTravel + discountFactor * sum);
                    } else {
                        // moving on
                        qTable.get(state).put(action, -1 * costOfTravel + discountFactor * sum);
                    }
                }

                // a bit confusing, but not that we get the entry from qTable given the current state
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
            diff += (double)valsA[i] - (double)valsB[i];
        }

        return diff <= epsilon;
    }
}

