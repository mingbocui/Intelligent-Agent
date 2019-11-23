package auction.centralized_rla;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SolutionSpace {
    private List<VehiclePlan> vehiclePlans;
    private List<Vehicle> vehicles;
    private List<Task> tasks;
    private boolean useSpanningTreeForCost;
    private double _cost = -1;
    
    public SolutionSpace(List<Vehicle> vehicles, List<Task> tasks, boolean useSpanningTreeForCost) {
        this.vehicles = vehicles;
        this.tasks = tasks;
        this.vehiclePlans = new ArrayList<>();
        this.useSpanningTreeForCost = useSpanningTreeForCost;
    }
    
    public static List<Plan> emptyPlan(List<Vehicle> vehicles) {
        return vehicles.stream().map(v -> Plan.EMPTY).collect(Collectors.toList());
    }
    
    public SolutionSpace(SolutionSpace solutionSpace) {
        this.vehicles = solutionSpace.vehicles;
        this.tasks = solutionSpace.tasks;
        this.vehiclePlans = solutionSpace.vehiclePlans.stream()
                .map(t -> new VehiclePlan(t.vehicle, new ArrayList<ActionTask>(t.actionTasks)))
                .collect(Collectors.toList());
        this.useSpanningTreeForCost = solutionSpace.useSpanningTreeForCost;
    }
    
    
    /**
     * as suggested in the slide, "Give all the tasks to the biggest vehicle"
     *
     * @param vehicles
     * @param tasks
     * @return
     */
    public static SolutionSpace largestVehicleSolution(List<Vehicle> vehicles, List<Task> tasks, boolean useSpanningTreeForCost) {
        var sol = new SolutionSpace(vehicles, tasks, useSpanningTreeForCost);
        
        List<ActionTask> as = new ArrayList<>();
        for (final var t : tasks) {
            as.add(ActionTask.pickup(t));
            as.add(ActionTask.delivery(t));
        }
        
        // find the biggest vehicle, should not behave different from naiveSolution
        int biggestVehicleId = IntStream.range(0, vehicles.size())
                .reduce((a, b) -> vehicles.get(a).capacity() > vehicles.get(b).capacity() ? a : b)
                .orElse(-1);
        
        System.out.println("selected largest vehicle with id: " + biggestVehicleId + " and capacity " + sol.vehicles.get(biggestVehicleId).capacity());
        
        for (int i = 0; i < vehicles.size(); i++) {
            if (i == biggestVehicleId) sol.vehiclePlans.add(new VehiclePlan(vehicles.get(i), as));
            else sol.vehiclePlans.add(new VehiclePlan(vehicles.get(i), new ArrayList<>()));
        }
        
        return sol;
    }
    
    public static SolutionSpace randomSolution(List<Vehicle> vehicles, List<Task> tasks, boolean useSpanningTreeForCost, Random rnd) {
        var sol = new SolutionSpace(vehicles, tasks, useSpanningTreeForCost);
        List<List<ActionTask>> as = new ArrayList<>();
        List<Integer> weightSoFar = new ArrayList<>();
        vehicles.forEach(v -> {
            as.add(new ArrayList<>());
            weightSoFar.add(0);
        });
        var taskList = new ArrayList<>(tasks);
        Collections.shuffle(taskList, rnd);
        
        int currVehicleId = 0;
        for (final Task t : taskList) {
            while (weightSoFar.get(currVehicleId) + t.weight > vehicles.get(currVehicleId).capacity()) {
                currVehicleId = (currVehicleId + 1) % vehicles.size();
            }
            
            weightSoFar.set(currVehicleId, weightSoFar.get(currVehicleId) + t.weight);
            as.get(currVehicleId).add(ActionTask.pickup(t));
            as.get(currVehicleId).add(ActionTask.delivery(t));
            
            currVehicleId = (currVehicleId + 1) % (vehicles.size());
        }
        
        for (int i = 0; i < vehicles.size(); i++) {
            sol.vehiclePlans.add(new VehiclePlan(vehicles.get(i), as.get(i)));
        }
        
        return sol;
    }
    
    public static SolutionSpace assignClosestTasksByPickup(List<Vehicle> vehicles, List<Task> tasks, boolean useSpanningTreeForCost, Random rnd) {
        return assignClosestTasks(vehicles, tasks, useSpanningTreeForCost, false, rnd);
    }
    
    public static SolutionSpace assignClosestTasksByDelivery(List<Vehicle> vehicles, List<Task> tasks, boolean useSpanningTreeForCost, Random rnd) {
        return assignClosestTasks(vehicles, tasks, useSpanningTreeForCost, false, rnd);
    }
    
    /**
     * assigns the tasks to the vehicle which is closest to the pickup
     *
     * @param vehicles
     * @param tasks
     * @param useSpanningTreeForCost
     * @return
     */
    public static SolutionSpace assignClosestTasks(List<Vehicle> vehicles, List<Task> tasks, boolean useSpanningTreeForCost, boolean usePickUp, Random rnd) {
        var sol = new SolutionSpace(vehicles, tasks, useSpanningTreeForCost);
        List<List<ActionTask>> as = new ArrayList<>();
        List<Integer> weightSoFar = new ArrayList<>();
        vehicles.forEach(v -> {
            as.add(new ArrayList<>());
            weightSoFar.add(0);
        });
        var taskList = new ArrayList<>(tasks);
        Collections.shuffle(taskList, rnd);
        
        for (final Task t : taskList) {
            var vehicleStream = IntStream.range(0, vehicles.size()).boxed();
            
            if (usePickUp) {
                vehicleStream = vehicleStream.sorted(Comparator.comparingDouble(i -> vehicles.get(i).homeCity().distanceTo(t.pickupCity)));
            } else {
                vehicleStream = vehicleStream.sorted(Comparator.comparingDouble(i -> vehicles.get(i).homeCity().distanceTo(t.deliveryCity)));
            }
    
            List<Integer> idxCitiesByDist = vehicleStream.collect(Collectors.toList());
            
            int idxIdx = 0;
            int currVehicleId = idxCitiesByDist.get(idxIdx);
            
            while (weightSoFar.get(currVehicleId) + t.weight > vehicles.get(currVehicleId).capacity()) {
                idxIdx += 1;
                currVehicleId = idxCitiesByDist.get(idxIdx);
            }
            
            weightSoFar.set(currVehicleId, weightSoFar.get(currVehicleId) + t.weight);
            as.get(currVehicleId).add(ActionTask.pickup(t));
            as.get(currVehicleId).add(ActionTask.delivery(t));
        }
        
        for (int i = 0; i < vehicles.size(); i++) {
            sol.vehiclePlans.add(new VehiclePlan(vehicles.get(i), as.get(i)));
        }
        
        return sol;
    }
    
    public double combinedCost() {
        if (this.useSpanningTreeForCost) {
            return vehiclePlans.stream().mapToDouble(VehiclePlan::combinedCost).sum();
        } else {
            return this.cost();
        }
    }
    
    public double spanningTreeLength() {
        return vehiclePlans.stream().mapToDouble(VehiclePlan::spanningTreeLength).sum();
    }
    
    public double cost() {
        if (_cost == -1) {
            this._cost = vehiclePlans.stream().mapToDouble(VehiclePlan::cost).sum();
        }
        return this._cost;
    }
    
    public double profit() {
        return vehiclePlans.stream().mapToDouble(VehiclePlan::profit).sum();
    }
    
    public boolean passesConstraints() {
        boolean allCool = true;
        
        // each individual plan passes all constraints
        allCool = allCool && vehiclePlans.stream().allMatch(VehiclePlan::passesConstraints);
        // all tasks are done
        allCool = allCool && vehiclePlans.stream().mapToInt(vp -> vp.getTasks().size()).sum() == this.tasks.size();
        
        return allCool;
    }
    
    public List<SolutionSpace> permuteActions() {
        List<SolutionSpace> newSolutions = new ArrayList<>(List.of(new SolutionSpace(this)));
        
        // sadly we need the indexes
        for (int i = 0; i < vehiclePlans.size(); i++) {
            final var vehiclePlan = vehiclePlans.get(i);
            if (vehiclePlan.getTasks().size() < 2) continue;
            
            for (int pos = 0; pos < vehiclePlan.actionTasks.size(); pos++) {
                for (int j = 0; j < vehiclePlan.actionTasks.size(); j++) {
                    if (pos != j) {
                        int j_after_removal = pos < j ? j - 1 : j;
                        SolutionSpace sol = new SolutionSpace(this);
                        final var el = sol.vehiclePlans.get(i).actionTasks.remove(pos);
                        sol.vehiclePlans.get(i).actionTasks.add(j_after_removal, el);
                        if (sol.passesConstraints()) {
                            newSolutions.add(sol);
                        }
                    }
                }
            }
        }
        
        return newSolutions;
    }
    
    public List<SolutionSpace> changeVehicle() {
        List<SolutionSpace> newSolutions = new ArrayList<>();
        
        // sadly we need the indexes
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
    
    public List<Plan> getPlans() {
        return vehiclePlans.stream().map(VehiclePlan::getPlan).collect(Collectors.toList());
    }
}
