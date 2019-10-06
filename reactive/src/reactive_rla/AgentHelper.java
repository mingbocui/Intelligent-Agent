package reactive_rla;

import logist.agent.Agent;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class AgentHelper {
    private Agent agent; // only used to get the costPerKm
    private TaskDistribution td;

    public HashMap<StateActionPair, Double> rewardTable;
    public HashMap<TransitionSequence, Double> transitionProbTable;

    public AgentHelper(Agent agent, TaskDistribution td, Topology topology){
        this.agent = agent;
        this.td = td;
        this.rewardTable = this.initRewardTable(topology);
        this.transitionProbTable = this.initTransitionProbTable(topology);
    }

    public List<State> initStates(Topology topology){
        // initialize all states between every
        var states = new LinkedList<State>();
        for (final var origin: topology.cities()) {
            for (final var destination: Utils.getReachableCities(origin)) {
                states.add(new State(origin, destination, true)); // there is task
                states.add(new State(origin, destination, false));// there is no task
            }
        }
        return states;
    }

    public List<AgentAction> initActions(Topology topology){
        var actions = new ArrayList<AgentAction>();
        // TODO this should be using routes and valid paths only
        // TODO for the `moving` type: just add the neighbors
        // TODO for the `pickup` type: check valid paths
        for (final var origin: topology.cities()) {
            for (final var dest: Utils.getReachableCities(origin)) {
                actions.add(new AgentAction(origin, dest, true)); // choose to pick up
                actions.add(new AgentAction(origin, dest, false));// choose to move
            }
        }
        return actions;
    }

    public double getReward(State state, AgentAction action) {
        // TODO can the agent have multiple vehicles?
        // TODO read this from the agent?
        double res = res = state.getFromCity().distanceTo(state.getToCity()) * (agent.vehicles().get(0).costPerKm());
//        agent.readProperty("cost-per-km")

        if(!action.isHasPickup()) // choose to move
            return res;
        else // choose to take the task
            return td.reward(state.getFromCity(), state.getToCity()) - res; // nett reward = reward - cost
    }

    public HashMap<StateActionPair, Double> initRewardTable(Topology topology){
        var rewardTable = new HashMap<StateActionPair, Double>();
        List<State> states = initStates(topology);
        List<AgentAction> actions = initActions(topology);

        // TODO check whether all (state, action) pair is legal or not

        for (final var state : states) {
            // TODO use only valid actions for this state
            for (final var action : actions) {
                var reward = getReward(state, action);
                rewardTable.put(new StateActionPair(state, action), reward);
            }
        }
        return rewardTable;
    }


    // T(s, a, s'): probability to arrive in state s' given that you are in state s and that you take action a
    public HashMap<TransitionSequence, Double> initTransitionProbTable(Topology topology){
        HashMap<TransitionSequence, Double> transitionProbTable = new HashMap<TransitionSequence, Double>();
        List<State> states = initStates(topology);
        List<AgentAction> actions = initActions(topology);
        for (var prevState: states) {
            // TODO use only valid actions for this state
            for (var action: actions) {
                for (var nextState: states) {
                    double prob = 0;
                    // if pick up task
                    // TODO think about setup of transition prob table
                    // TODO check the correctness here, a little bit of confused about the transition probability
                    if(action.isHasPickup() && prevState.getToCity() == nextState.getFromCity()){
                        prob = td.probability(prevState.getFromCity(), nextState.getFromCity());
                    } else if(!action.isHasPickup() && action.getDestCity() == nextState.getFromCity()){
                        prob = td.probability(prevState.getFromCity(), nextState.getFromCity());
                    }
                    transitionProbTable.put(new TransitionSequence(prevState, action, nextState), prob);
                }
            }
        }
        return transitionProbTable;
    }
}
