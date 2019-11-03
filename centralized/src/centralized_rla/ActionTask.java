package centralized_rla;

import logist.task.Task;


public class ActionTask {
    private Task task;
    private ActionType actionType;
    
    public ActionTask(Task task, ActionType actionType) {
        this.task = task;
        this.actionType = actionType;
    }
    
    public static ActionTask pickup(Task task) {
        return new ActionTask(task, ActionType.PICKUP);
    }
    
    public static ActionTask delivery(Task task) {
        return new ActionTask(task, ActionType.DELIVERY);
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
