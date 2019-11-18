package auction.centralized_rla;

import logist.task.Task;


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
    
    public boolean isDelivery() {
        return this.actionType == ActionType.DELIVERY;
    }
    
    public Task getTask() {
        return task;
    }
    
    public ActionType getActionType() {
        return actionType;
    }
    
    public enum ActionType {
        DELIVERY,
        PICKUP
    }
}
