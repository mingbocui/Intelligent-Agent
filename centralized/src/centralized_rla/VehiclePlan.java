package centralized_rla;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class VehiclePlan {
    public Vehicle vehicle;
    public List<ActionTask> actionTasks;
    
    public VehiclePlan(Vehicle vehicle, List<ActionTask> actionTasks) {
        this.vehicle = vehicle;
        this.actionTasks = actionTasks;
    }
    
    public List<Task> getTasks() {
        return actionTasks.stream().filter(ActionTask::isDelivery).map(ActionTask::getTask).collect(Collectors.toList());
    }
    
    private boolean allPickUpsBeforeDeliveries() {
        for (int i = 0; i < actionTasks.size(); i++) {
            if (actionTasks.get(i).isDelivery()) {
                final var at = actionTasks.get(i);
                final int pickUpIdx = IntStream.range(0, i).filter(idx -> actionTasks.get(idx).isPickup() && actionTasks.get(idx).getTask().equals(at.getTask())).findFirst().orElse(i);
                
                if (pickUpIdx >= i) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    private boolean loadCapacityViolated() {
        int currentLoad = 0;
        
        for (final var t : actionTasks) {
            if (t.isPickup()) {
                currentLoad += t.getTask().weight;
                
                if (currentLoad > vehicle.capacity()) {
                    return true;
                }
            } else {
                currentLoad -= t.getTask().weight;
            }
        }
        
        return false;
    }
    
    public boolean passesConstraints() {
        return allPickUpsBeforeDeliveries() && !loadCapacityViolated();
    }
    
    public double cost() {
        return getPlan().totalDistance() * vehicle.costPerKm();
    }
    
    public Plan getPlan() {
        if (actionTasks.isEmpty()) return Plan.EMPTY;
        
        Plan plan = new Plan(vehicle.homeCity());
        
        for (int i = 0, prev = -1; i < actionTasks.size(); i++, prev++) {
            shortestPath(actionTasks, vehicle, prev, i).forEach(plan::appendMove);
            if (actionTasks.get(i).isPickup()) {
                plan.appendPickup(actionTasks.get(i).getTask());
            } else {
                plan.appendDelivery(actionTasks.get(i).getTask());
            }
        }
        
        return plan;
    }
    
    private List<Topology.City> shortestPath(List<ActionTask> actionTasks, Vehicle vehicle, int prevIdx, int currentIdx) {
        Topology.City current;
        
        if (prevIdx < 0) {
            current = vehicle.homeCity();
        } else {
            final var prevAction = actionTasks.get(prevIdx);
            if (prevAction.isPickup()) {
                current = prevAction.getTask().pickupCity;
            } else {
                current = prevAction.getTask().deliveryCity;
            }
        }
        
        final var currAction = actionTasks.get(currentIdx);
        
        Topology.City target;
        if (currAction.isPickup()) {
            target = currAction.getTask().pickupCity;
        } else {
            target = currAction.getTask().deliveryCity;
        }
        
        if (current.equals(target)) {
            return new ArrayList<>();
        } else {
            return current.pathTo(target);
        }
    }
}
