package reactive_rla;

import logist.topology.Topology.City;

import java.util.Objects;

public class AgentAction {
    private City origin;
    private City destination;
    private ActionType actionType;
    private double cost;
    private double estimatedReward;

    public enum ActionType {
        MOVE,
        PICKUP
    }

    /**
     * Dummy empty constructor
     */
    public AgentAction() {}

    /**
     *
     * @param origin
     * @param destination
     * @param actionType
     * @param estimatedReward this can be 0.0 if the
     */
    public AgentAction(City origin, City destination, ActionType actionType, double estimatedReward, double costPerKm) {
        // TODO pass costPerKm somehow down
        this.origin = origin;
        this.destination = destination;
        this.actionType = actionType;

        this.cost = computeCost(costPerKm);

        // this will throw some errors with the current implementation
        //if (actionType.equals(ActionType.PICKUP) && estimatedReward == 0.0)  {
        //    throw new IllegalArgumentException("if the action type is PICKUP and the estimated reward is 0 something is off");
        //}
        this.estimatedReward = estimatedReward;
    }

    public static AgentAction createMoveAction(City origin, City destination, double costPerKm) {
        return new AgentAction(origin, destination, ActionType.MOVE, 0.0, costPerKm);
    }

    public static AgentAction createPickupAction(City origin, City destination, double estimatedReward, double costPerKm) {
        return new AgentAction(origin, destination, ActionType.PICKUP, estimatedReward, costPerKm);
    }

    private double computeCost(double costPerKm) {
        return origin.distanceTo(destination) * costPerKm;
    }

    public double getBenefit() {
        return estimatedReward - cost;
    }

    public City getOrigin() {
        return origin;
    }

    public void setOrigin(City origin) {
        this.origin = origin;
    }

    public City getDestination() {
        return destination;
    }

    public void setDestination(City destination) {
        this.destination = destination;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public double getEstimatedReward() {
        return estimatedReward;
    }

    public void setEstimatedReward(double estimatedReward) {
        this.estimatedReward = estimatedReward;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AgentAction)) return false;
        AgentAction that = (AgentAction) o;
        return Double.compare(that.getCost(), getCost()) == 0 &&
                Double.compare(that.getEstimatedReward(), getEstimatedReward()) == 0 &&
                getOrigin().equals(that.getOrigin()) &&
                getDestination().equals(that.getDestination()) &&
                getActionType() == that.getActionType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOrigin(), getDestination(), getActionType(), getCost(), getEstimatedReward());
    }
}
