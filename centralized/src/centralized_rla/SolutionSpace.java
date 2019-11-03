package centralized_rla;

import logist.plan.Action;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.TaskSet;
import logist.topology.Topology;

import java.util.ArrayList;
import java.util.List;

public class SolutionSpace {
    private List<VehiclePlan> vehicleActions;
    
    public SolutionSpace() {
        vehicleActions = new ArrayList<>();
    }
    
    public SolutionSpace naiveSolution(List<Vehicle> vehicles, TaskSet tasks) {
        var sol = new SolutionSpace();
        
        List<ActionTask> as = new ArrayList<>();
        for (final var t : tasks) {
            as.add(ActionTask.pickup(t));
            as.add(ActionTask.delivery(t));
        }
        
        sol.vehicleActions.add(new VehiclePlan(vehicles.get(0), as));
        for (int i = 1; i < vehicles.size(); i++) {
            sol.vehicleActions.add(new VehiclePlan(vehicles.get(i), new ArrayList<>()));
        }
        
        return sol;
    }
    
    public double cost() {
        return 0.0;
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
    
    public List<Plan> getPlans() {
        List<Plan> plans = new ArrayList<Plan>();
    
        for (final var vehiclePlan : this.vehicleActions) {
            if (vehiclePlan.actionTasks.isEmpty()) {
                plans.add(Plan.EMPTY);
            } else {
                Plan plan = new Plan(vehiclePlan.vehicle.homeCity());
                
                final var actionTasks = vehiclePlan.actionTasks;
                for (int i = 0, prev = -1; i < actionTasks.size(); i++, prev++) {
                    shortestPath(actionTasks, vehiclePlan.vehicle, prev, i)
                            .forEach(plan::appendMove);
                    if (actionTasks.get(i).isPickup()) {
                        plan.appendPickup(actionTasks.get(i).getTask());
                    } else {
                        plan.appendDelivery(actionTasks.get(i).getTask());
                    }
                }
                
                plans.add(plan);
            }
        }
        
        return plans;
    }
    
    private enum ActionType {
        DELIVERY,
        PICKUP
    }
    
    private class VehiclePlan {
        public Vehicle vehicle;
        public List<ActionTask> actionTasks;
    
        public VehiclePlan(Vehicle vehicle, List<ActionTask> actionTasks) {
            this.vehicle = vehicle;
            this.actionTasks = actionTasks;
        }
    }
}
