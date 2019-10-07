package reactive_rla;

import logist.task.Task;
import logist.topology.Topology;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class Utils {
    public static List<Topology.City> getReachableCities(final Topology.City origin) {
        var reachableCities = new HashSet<Topology.City>();
        var seenCities = new HashSet<Topology.City>();
        seenCities.add(origin);
        var citiesToProcess = new HashSet<Topology.City>(origin.neighbors());
        reachableCities.addAll(citiesToProcess);

        // simple breadth-first algorithm to get all reachable cities
        while (citiesToProcess.size() > 0) {
            var newCitiesToProcess = new HashSet<Topology.City>();
            for (final var city : citiesToProcess) {
                var neighbors = city.neighbors();

                newCitiesToProcess.addAll(neighbors);
                reachableCities.addAll(neighbors);
            }

            seenCities.addAll(citiesToProcess);
            citiesToProcess = newCitiesToProcess;
            citiesToProcess.removeIf(seenCities::contains);
        }

        reachableCities.remove(origin);

        return new ArrayList<>(reachableCities);
    }

    public static double benefit(Task task, double costPerKm) {
        return task.reward - task.pickupCity.distanceTo(task.deliveryCity) * costPerKm;
    }

    public static double costOfTravel(Topology.City origin, Topology.City destination, double costPerKm) {
        return origin.distanceTo(destination) * costPerKm;
    }
}
