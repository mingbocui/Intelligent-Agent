package reactive_rla;

import java.util.Objects;

public class StateActionPair {
    // reward table (s, a) pair
    private State state;
    private AgentAction agentAction;

    public StateActionPair(State state, AgentAction agentAction) {
        this.state = state;
        this.agentAction = agentAction;
    }

    public State getState() {
        return state;
    }

    public AgentAction getAgentAction() {
        return agentAction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StateActionPair that = (StateActionPair) o;
        return Objects.equals(state, that.state) &&
                Objects.equals(agentAction, that.agentAction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(state, agentAction);
    }
}
