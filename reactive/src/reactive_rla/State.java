package reactive_rla;

import logist.task.TaskDistribution;
import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class State {
    private City currentCity;
    private City destination; // a bit wrong... but a Task requires some information which we don't have
                              // during the value iteration computation, so this is just a placeholder
    private List<AgentAction> actions;

    public State(City city, City destination) {
        this.currentCity = city;
        this.destination = destination;
        this.actions = new ArrayList<>();
    }

    public State(State other) {
        this.currentCity = other.currentCity;
        this.destination = other.destination;
        this.actions = other.actions;
    }

    public City getCurrentCity() {
        return currentCity;
    }

    public void setCurrentCity(City currentCity) {
        this.currentCity = currentCity;
    }

    public City getDestination() {
        return destination;
    }

    public void setDestination(City destination) {
        this.destination = destination;
    }

    public List<AgentAction> getActions() {
        return actions;
    }

    /**
     * This is outside of the constructor, because at runtime we don't need this.
     * We only need this during the setup phase.
     * @return
     */
    public State createActions(TaskDistribution taskDistribution, double costPerKm) {
        this.actions = new ArrayList<>();

        // 1. moving to the next neighbors, this is always possible
        this.currentCity.neighbors()
            .stream()
            .map(c -> AgentAction.createMoveAction(currentCity, c, costPerKm))
            .forEach(this.actions::add);

        // 2. delivering a package, only if we have a target city
        // TODO not sure if we should add the expected reward here, but it makes sense...
        if (this.destination != null) {
            this.actions.add(AgentAction.createPickupAction(currentCity,
                    destination,
                    taskDistribution.reward(currentCity, destination),
                    costPerKm));
        }

        return this;
    }

    @Override
    public boolean equals(Object o) {
        // this should NOT take the actions into account, they are mostly there to make the programming easier
        if (this == o) return true;
        if (!(o instanceof State)) return false;
        State state = (State) o;
        return getCurrentCity().equals(state.getCurrentCity()) &&
                Objects.equals(getDestination(), state.getDestination());
    }

    @Override
    public int hashCode() {
        // this should NOT take the actions into account, they are mostly there to make the programming easier
        return String.format("%s-%s", getCurrentCity(), getDestination()).hashCode();
        //return Objects.hash(getCurrentCity(), getDestination());
    }

    @Override
    public String toString() {
        return "State{" +
                "currentCity=" + currentCity +
                ", destination=" + destination +
                ", nActions=" + actions.size() +
                '}';
    }
}
