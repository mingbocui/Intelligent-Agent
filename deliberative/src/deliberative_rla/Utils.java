package deliberative_rla;

import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology;

import java.util.*;

public class Utils {
    public static HashMap<Topology.City, ArrayList<Task>> taskPerCity(TaskSet tasks) {
        var map = new HashMap<Topology.City, ArrayList<Task>>();
        
        for (final var task : tasks) {
            if (map.containsKey(task.pickupCity)) {
                map.get(task.pickupCity).add(task);
            } else {
                map.put(task.pickupCity, new ArrayList<>(Arrays.asList(task)));
            }
        }
        
        return map;
    }
}
