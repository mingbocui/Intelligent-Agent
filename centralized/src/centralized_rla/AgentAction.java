package centralized_rla;

import logist.task.Task;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.Objects;


public class AgentAction {

    private Task task;
    private ActionType actionType = ActionType.MOVE;

    // two possible actions for each task
    public enum ActionType {
        MOVE, // for initialization
        DELIVERY, // move???
        PICKUP
    }

    public AgentAction(ActionType actionType, Task task){
        this.task = task;
        this.actionType = actionType;
    }

    public boolean isPickup(){
        if(this.actionType == ActionType.PICKUP) return true;
        else return false;
    }


    public City getTaskCity(){
        if(this.isPickup()) return task.pickupCity;
        else return task.deliveryCity;
    }

//    public City getDeliveryCity(){
//        if(!this.isPickup()) return task.deliveryCity;
//        else return null;
//    }


    public Task getTask() {
        return task;
    }

    public ActionType getActionType() {
        return actionType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgentAction that = (AgentAction) o;
        return Objects.equals(task, that.task) &&
                actionType == that.actionType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(task, actionType);
    }

}
