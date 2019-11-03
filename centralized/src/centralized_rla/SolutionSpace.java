package centralized_rla;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SolutionSpace {
    private List<VehiclePlan> vehiclePlans;
    private List<Vehicle> vehicles;
    private TaskSet tasks;
    
    public SolutionSpace(List<Vehicle> vehicles, TaskSet tasks) {
        this.vehicles = vehicles;
        this.tasks = tasks;
        this.vehiclePlans = new ArrayList<>();
    }
    
    public SolutionSpace(SolutionSpace solutionSpace) {
        this.vehicles = solutionSpace.vehicles;
        this.tasks = solutionSpace.tasks;
        this.vehiclePlans = solutionSpace.vehiclePlans.stream()
                .map(t -> new VehiclePlan(t.vehicle, new ArrayList<ActionTask>(t.actionTasks)))
                .collect(Collectors.toList());
    }
    
    public SolutionSpace naiveSolution(List<Vehicle> vehicles, TaskSet tasks) {
        var sol = new SolutionSpace(vehicles, tasks);
        
        List<ActionTask> as = new ArrayList<>();
        for (final var t : tasks) {
            as.add(ActionTask.pickup(t));
            as.add(ActionTask.delivery(t));
        }
        
        sol.vehiclePlans.add(new VehiclePlan(vehicles.get(0), as));
        for (int i = 1; i < vehicles.size(); i++) {
            sol.vehiclePlans.add(new VehiclePlan(vehicles.get(i), new ArrayList<>()));
        }
        
        return sol;
    }
    
    public SolutionSpace randomSolution(List<Vehicle> vehicles, TaskSet tasks) {
        // TODO implement this
        return naiveSolution(vehicles, tasks);
    }
    
    public double totalMinSpanTreeLength() {
        // TODO use this for the local selection
        // TODO implement this
        return 0.0;
    }
    
    public double cost() {
        return vehiclePlans.stream().mapToDouble(VehiclePlan::cost).sum();
    }
    
    public boolean passesConstraints() {
        boolean allCool = true;
        
        allCool = allCool && vehiclePlans.stream().allMatch(VehiclePlan::passesConstraints);
        allCool = allCool && vehiclePlans.stream().mapToInt(vp -> vp.getTasks().size()).sum() == this.tasks.size();

        return allCool;
    }
    
    public List<SolutionSpace> permuteActions() {
        List<SolutionSpace> newSolutions = new ArrayList<>();
    
        for (int i = 0; i < vehiclePlans.size(); i++) {
            final var vehiclePlan = vehiclePlans.get(i);
            if (vehiclePlan.getTasks().size() < 2) continue;
    
            for (int pos = 0; pos < vehiclePlan.actionTasks.size(); pos++) {
                // Generation of new solutions
                for (int j = 0; j < vehiclePlan.actionTasks.size(); j++) {
                    SolutionSpace sol = new SolutionSpace(this);
    
                    Collections.swap(sol.vehiclePlans.get(i).actionTasks, pos, j);
                    
                    newSolutions.add(sol);
                }
            }
        }
        
        return newSolutions;
    }
    
    
    public List<SolutionSpace> changeVehicle() {
        List<SolutionSpace> newSolutions = new ArrayList<>();
    
        for (int i = 0; i < vehiclePlans.size(); i++) {
            final var vp = vehiclePlans.get(i);
            for (final var task : vp.getTasks()) {
                
                // adding it a each other vehicle at the beginning,
                // randomisation of the delivery and pickup order will be done at a later step
                for (int j = 0; j < vehiclePlans.size(); j++) {
                    // order of vehicles is preserved
                    if (i == j) continue;
    
                    SolutionSpace sol = new SolutionSpace(this);
                    // removing of this task
                    sol.vehiclePlans.get(i).actionTasks = sol.vehiclePlans.get(i).actionTasks.stream()
                            .filter(Predicate.not(t -> t.getTask().equals(task)))
                            .collect(Collectors.toList());
                    
                    var currVp = sol.vehiclePlans.get(j);
                    currVp.actionTasks.add(ActionTask.pickup(task));
                    currVp.actionTasks.add(ActionTask.delivery(task));
                    
                    newSolutions.add(sol);
                }
            }
        }
        
        return newSolutions;
        
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
        return vehiclePlans.stream().map(VehiclePlan::getPlan).collect(Collectors.toList());
    }
    
    private class VehiclePlan {
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
    
    }
}
