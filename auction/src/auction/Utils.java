package auction;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

public class Utils {
    public static Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
        City current = vehicle.getCurrentCity();
        Plan plan = new Plan(current);
        
        for (Task task : tasks) {
            // move: current city => pickup location
            for (City city : current.pathTo(task.pickupCity))
                plan.appendMove(city);
            
            plan.appendPickup(task);
            
            // move: pickup location => delivery location
            for (City city : task.path())
                plan.appendMove(city);
            
            plan.appendDelivery(task);
            
            // set current city
            current = task.deliveryCity;
        }
        return plan;
    }
}
