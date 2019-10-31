package centralized_rla;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.*;

public class Solution {
    //every Vehicle has a list of actions, two action type in AgentAction wrt each task
    Map<Vehicle, List<AgentAction>> vehicleMapToActions;
    List<Vehicle> allVehicles;

    public Solution(List<Vehicle> allVehicles){
        this.allVehicles = allVehicles;
        //initial the map
        allVehicles.stream().forEach(vehicle -> {vehicleMapToActions.put(vehicle, new ArrayList<AgentAction>());});
    }

    // TODO assign the task to the closest vehicle
    public Solution initialSolution(List<Vehicle> allVehicles, TaskSet allTasks){
        Map<Vehicle, List<AgentAction>> vehicleMapToActions = new HashMap<>();
        allVehicles.stream().forEach(vehicle -> {vehicleMapToActions.put(vehicle, new ArrayList<AgentAction>());});
        return null;
    }



    public Plan singlePlan(Vehicle vehicle){
        // the home city of vehicle
        City currentCity = vehicle.homeCity();
        // plan for a single vehicle
        Plan vehiclePlan = new Plan(currentCity);
        for(AgentAction agentAction : vehicleMapToActions.get(vehicle)){
            //taskCIty could be the delivery or pickup city of the task, depend on the corresponding action we play on this task
            Task task = agentAction.getTask();
            City taskCity = agentAction.getTaskCity();
            List<City> path = currentCity.pathTo(taskCity);
            path.stream().forEach(city -> {vehiclePlan.appendMove(city);});
            if(agentAction.getActionType() == AgentAction.ActionType.PICKUP) vehiclePlan.appendPickup(task);
            else vehiclePlan.appendDelivery(task);

        }
        return vehiclePlan;
    }

    public List<Plan> construcPlan(){
        List<Plan> plans = new ArrayList<>();
        this.allVehicles.stream().forEach(vehicle -> {plans.add(this.singlePlan(vehicle));});

        return plans;
    }

    //total cost of all vehicles, simple sum
    // assume vehicles' costPerKm are the same
    public long cost(long costPerkm){

        long cost = 0;
        for(Vehicle vehicle : this.allVehicles){
            City vehicleHomeCity = vehicle.homeCity();
            for(AgentAction agentAction : vehicleMapToActions.get(vehicle)){
                cost += vehicleHomeCity.distanceTo(agentAction.getTaskCity())*costPerkm;
            }
        }
        return cost;
    }

}
