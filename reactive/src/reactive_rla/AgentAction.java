package reactive_rla;

import logist.topology.Topology.City;

import java.util.Objects;

public class AgentAction {
    private City startCity;
    private City destCity; // destination city of certain action
    private boolean hasPickup; // two possibilities, pick up the task or move to another city;

    public AgentAction(City startCity, City destCity, boolean hasPickup) {
        this.startCity = startCity;
        this.destCity = destCity;
        this.hasPickup = hasPickup;
    }

    public City getStartCity() {
        return startCity;
    }

    public City getDestCity() {
        return destCity;
    }

    public boolean isHasPickup() {
        return hasPickup;
    }

    // TODO I used the auto-genarated hashCode() and eqauls() function here, logic correctness checking needed
    @Override
    public boolean equals(Object o) {
        // TODO sam fix this
        if (this == o) return true;
        if (!(o instanceof AgentAction)) return false;
        AgentAction agentAction = (AgentAction) o;
        return isHasPickup() == agentAction.isHasPickup() &&
                getStartCity().equals(agentAction.getStartCity()) &&
                getDestCity().equals(agentAction.getDestCity());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getStartCity(), getDestCity(), isHasPickup());
    }
}
