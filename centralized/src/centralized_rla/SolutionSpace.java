package centralized_rla;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.TaskSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    
    /**
     * as suggested in the slide, "Give all the tasks to the biggest vehicle"
     *
     * @param vehicles
     * @param tasks
     * @return
     */
    public SolutionSpace largestVehicleSolution(List<Vehicle> vehicles, TaskSet tasks) {
        var sol = new SolutionSpace(vehicles, tasks);
        
        List<ActionTask> as = new ArrayList<>();
        for (final var t : tasks) {
            as.add(ActionTask.pickup(t));
            as.add(ActionTask.delivery(t));
        }
        
        // find the biggest vehicle, should not behave different from naiveSolution
        int biggestVehicleId = IntStream.range(0, vehicles.size())
                .reduce((a, b) -> vehicles.get(a).capacity() > vehicles.get(b).capacity() ? a : b)
                .orElse(-1);
        
        System.out.println("selected largest vehicle with id: " + biggestVehicleId + " and capacity " + vehicles.get(biggestVehicleId).capacity());
        
        for (int i = 0; i < vehicles.size(); i++) {
            if (i == biggestVehicleId) sol.vehiclePlans.add(new VehiclePlan(vehicles.get(i), as));
            else sol.vehiclePlans.add(new VehiclePlan(vehicles.get(i), new ArrayList<>()));
        }
        
        return sol;
    }
    
    public double totalMinSpanTreeLength() {
        // TODO use this for the local selection
        // TODO implement this
        return 0.0;
    }
    
    public double cost() {
        return vehiclePlans.stream().mapToDouble(VehiclePlan::cost).sum();
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
        List<SolutionSpace> newSolutions = new ArrayList<>();
        
        // sadly we need the indexes
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
