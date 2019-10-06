package reactive_rla;

import logist.task.TaskDistribution;
import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class State {
    private City currentCity;
    private City destination; // a bit wrong... but a Task requires some information which we don't have
                              // during the value iteration computation, so this is just a placeholder
    private List<AgentAction> actions;

    public State(City city, City destination) {
        currentCity = city;
        this.destination = destination;
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

    /**
     * This is outside of the constructor, because at runtime we don't need this.
     * We only need this during the setup phase.
     */
    public void createActions(TaskDistribution taskDistribution) {
        this.actions = new ArrayList<>();

        // 1. moving to the next neighbors
        this.actions.addAll(this.currentCity.neighbors()
                                .stream()
                                .map(c -> AgentAction.createMoveAction(currentCity, c))
                                .collect(Collectors.toList()));

        // 2. delivering a package
        // TODO not sure if we should add the expected reward here, but it makes sense...
        this.actions.addAll(Utils.getReachableCities(this.currentCity)
             .stream()
             .map(c -> AgentAction.createPickupAction(currentCity, c, taskDistribution.reward(currentCity, c)))
             .collect(Collectors.toList()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof State)) return false;
        State state = (State) o;
        return getCurrentCity().equals(state.getCurrentCity()) &&
                Objects.equals(getDestination(), state.getDestination());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCurrentCity(), getDestination());
    }
}
