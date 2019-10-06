package reactive_rla;

import logist.agent.Agent;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class AgentManager {

    private Agent agent; // only used to get the costPerKm
    private TaskDistribution td;

    public HashMap<StateActionPair, Double> rewardTable;
    public HashMap<TransitionSequence, Double> transitionProbTable;

    public AgentManager(Agent agent, TaskDistribution td, Topology topology){
        this.agent = agent;
        this.td = td;
        this.rewardTable = this.initRewardTable(topology);
        this.transitionProbTable = this.initTransitionProbTable(topology);
    }

    public List<State> initStates(Topology topology){
        // initialize all states between every
        var states = new LinkedList<State>();
        for (final var cityA : topology.cities()) {
            for (final var cityB : topology.cities()) {
                if (cityA != cityB) {
                    states.add(new State(cityA, cityB, true)); // there is task
                    states.add(new State(cityA, cityB, false));// there is no task
                }
            }
        }
        return states;
    }

    public List<AgentAction> initActions(Topology topology){
        List<AgentAction> actions = new LinkedList<AgentAction>();
        // TODO this should be using routes and valid paths only
        // TODO for the `moving` type: just add the neighbors
        // TODO for the `pickup` type: check valid paths
        for(City cityA : topology.cities()){
            for(City cityB : topology.cities()){
                if(cityA != cityB){
                    actions.add(new AgentAction(cityA, cityB, true)); // choose to pick up
                    actions.add(new AgentAction(cityA, cityB, false));// choose to move
                }
            }
        }
        return actions;

    }

    public double getReward(State state, AgentAction action) {
        // TODO can the agent have multiple vehicles?
        double res = res = state.getFromCity().distanceTo(state.getToCity()) * (agent.vehicles().get(0).costPerKm());
//        agent.readProperty("cost-per-km")

        if(!action.isHasPickup()) // choose to move
            return res;
        else // choose to take the task
            return td.reward(state.getFromCity(), state.getToCity()) - res; // nett reward = reward - cost
    }

    public HashMap<StateActionPair, Double> initRewardTable(Topology topology){

        HashMap<StateActionPair, Double> rewardTable = new HashMap<StateActionPair, Double>();
        List<State> states = initStates(topology);
        List<AgentAction> actions = initActions(topology);

        // TODO check whether all (state, action) pair is legal or not

        for(State state : states){
            // TODO use only valid actions for this state
            for(AgentAction action : actions){
                double reward = getReward(state, action);
                StateActionPair stateActionPair = new StateActionPair(state, action);
                rewardTable.put(stateActionPair, reward);
            }
        }
        return rewardTable;
    }


    // T(s, a, s'): probability to arrive in state s' given that you are in state s and that you take action a
    public HashMap<TransitionSequence, Double> initTransitionProbTable(Topology topology){
        HashMap<TransitionSequence, Double> transitionProbTable = new HashMap<TransitionSequence, Double>();
        List<State> states = initStates(topology);
        List<AgentAction> actions = initActions(topology);
        for(State statePrev : states){
            // TODO use only valid actions for this state
            for(AgentAction action : actions){
                for(State stateNext : states){
                    double prob = 0;
                    // if pick up task
                    // TODO think about setup of transition prob table
                    // TODO check the correctness here, a little bit of confused about the transition probability
                    if(action.isHasPickup() && statePrev.getToCity() == stateNext.getFromCity()){
                        prob = td.probability(statePrev.getFromCity(), stateNext.getFromCity());
                    } else if(!action.isHasPickup() && action.getDestCity() == stateNext.getFromCity()){
                        prob = td.probability(statePrev.getFromCity(), stateNext.getFromCity());
                    }
                    transitionProbTable.put(new TransitionSequence(statePrev, action, stateNext), prob);
                }
            }
        }
        return transitionProbTable;
    }
}
