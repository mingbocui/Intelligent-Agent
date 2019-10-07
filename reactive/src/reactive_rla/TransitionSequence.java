package reactive_rla;

import java.util.Objects;

public class TransitionSequence {

    // transition state tuple T(s, a, s')
    private State startState; // start state
    private AgentAction agentAction; // action
    private State endState; // end state

    public TransitionSequence(State startState, AgentAction agentAction, State endState) {
        this.startState = startState;
        this.agentAction = agentAction;
        this.endState = endState;
    }

    public State getStartState() {
        return startState;
    }

    public State getEndState() {
        return endState;
    }

    public AgentAction getAgentAction() {
        return agentAction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransitionSequence that = (TransitionSequence) o;
        return Objects.equals(startState, that.startState) &&
                Objects.equals(endState, that.endState) &&
                Objects.equals(agentAction, that.agentAction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startState, endState, agentAction);
    }
}
