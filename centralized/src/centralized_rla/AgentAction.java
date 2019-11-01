package centralized_rla;

import logist.task.Task;
import logist.topology.Topology.City;

import java.util.Objects;


public class AgentAction {
    private Task task;
    private ActionType actionType;
    
    public AgentAction(ActionType actionType, Task task) {
        this.task = task;
        this.actionType = actionType;
    }
    
    public boolean isPickup() {
        return this.actionType == ActionType.PICKUP;
    }
    
    public boolean isMove() {
        return this.actionType == ActionType.MOVE;
    }
    
    
    public City getTaskCity() {
        if (this.isPickup()) {
            return task.pickupCity;
        } else if (this.isMove()) {
            return task.deliveryCity;
        } else {
            // TODO complete this
            return null;
        }
    }
    
    public Task getTask() {
        return task;
    }

//    public City getDeliveryCity(){
//        if(!this.isPickup()) return task.deliveryCity;
//        else return null;
//    }
    
    public ActionType getActionType() {
        return actionType;
    }
    
    public enum ActionType {
        MOVE, // for initialization
        DELIVERY, // move???
        PICKUP
    }
    
}
