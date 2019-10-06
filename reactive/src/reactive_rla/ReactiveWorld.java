package reactive_rla;

import logist.agent.Agent;
import logist.task.TaskDistribution;
import logist.topology.Topology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
        // 1. get real reward for this given action in Task
        // 2. compare that to a the task of moving to the best neighbour
        // 3. pick the one with a higher reward

        Topology.City bestCity = null;
        double maxReward = -Double.MAX_VALUE;

        var currentCity = state.getCurrentCity();
        for(final var neighborCity : Utils.getReachableCities(currentCity)){
            double tempReward = taskDistribution.reward(currentCity, neighborCity);
            if(tempReward > maxReward){
                bestCity = neighborCity;
                maxReward = tempReward;
            }
        }
        return bestCity;
    }

    private List<State> initStates() {
        var states = new ArrayList<State>();
        for (final var origin: topology.cities()) {
            for (final var destination: Utils.getReachableCities(origin)) { // this is a bit of an overkill,
                                                                            // but this will reflect the topology
                var state = new State(origin, destination);
                state.createActions(this.taskDistribution, this.costPerKm);

                states.add(state);
            }
        }
        return states;
    }

    private void valueIteration (Topology topology, Agent agent, TaskDistribution taskDistribution) {
        valueTable = new HashMap<>();
        var prevValueTable = new HashMap<State, Double>();
        // TODO create Best(state) - table here as well
    }

    /*
    private void valueIteration(Topology topology, Agent agent, TaskDistribution td){
        stateActionTable = new HashMap<State, AgentAction>();
        valueTable = new HashMap<StateActionPair, Double>();

        AgentHelper agentHelper = new AgentHelper(agent, td, topology);
        List<State> states = agentHelper.initStates(topology);
        List<AgentAction> actions = agentHelper.initActions(topology);

        var difference = 100.0;
        while (difference > Config.VALUE_ITERATION_THRESHOLD) {
            for (var statePrev : states) {
                double currValue;
                // TODO create function: `List<AgentAction> getValidActionsForState(State state)`
                for (var action : actions) {
                    var stateActionPair = new StateActionPair(statePrev, action);
                    currValue = agentHelper.rewardTable.get(stateActionPair); // just query from the created table
                    for (var stateNext : states) {
                        var transitionSequence = new TransitionSequence(statePrev, action, stateNext);
                        var prob = agentHelper.transitionProbTable.get(transitionSequence); // query from the second

                        // TODO not sure, implementation of value iteration
                        AgentAction bestAction = stateActionTable.get(stateNext);
                        var futureValue = valueTable.get(new StateActionPair(stateNext, bestAction));
                        // TODO something is null here... super odd...
                        currValue += discountFactor * prob * futureValue;
                    }

                    // check the convergence after update the table
                    // TODO why is this the best action? select max (reward - cost), no?
                    var bestAction = stateActionTable.get(statePrev); // best action of the current state
                    var currBestStateActionPair = new StateActionPair(statePrev,bestAction); // best (s,a) pair of current state
                    // if currentSate is not in the transition table
                    if (valueTable.get(currBestStateActionPair) == null) {
                        valueTable.put(new StateActionPair(statePrev, action), currValue);
                        stateActionTable.put(statePrev, action);
                    } else {
                        var preValue = valueTable.get(currBestStateActionPair);
                        // TODO maybe this is wrong?
                        if (preValue < currValue) {
                            valueTable.remove(currBestStateActionPair);
                            valueTable.put(new StateActionPair(statePrev, action), currValue);
                            stateActionTable.remove(statePrev);
                            stateActionTable.put(statePrev,action);
                            difference = (currValue - preValue) / preValue;
                        }
                    }
                }
            }
        }
    }*/
}
