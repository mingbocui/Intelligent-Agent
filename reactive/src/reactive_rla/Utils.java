package reactive_rla;

import logist.topology.Topology;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class Utils {
    public static List<Topology.City> getReachableCities(final Topology.City origin) {
        var reachableCities = new HashSet<Topology.City>();
        var citiesToProcess = new HashSet<Topology.City>();
        var seenCities = new HashSet<Topology.City>();
        seenCities.add(origin);
        citiesToProcess.addAll(origin.neighbors());


        // simple breadth-first algorithm to get all reachable cities
        while (citiesToProcess.size() > 0) {
            var newCities = new HashSet<Topology.City>();
            for (final var city: citiesToProcess) {
                var neighbors = city.neighbors();

                newCities.addAll(neighbors);
                reachableCities.addAll(neighbors);
            }

            seenCities.addAll(citiesToProcess);
            citiesToProcess = newCities;
            citiesToProcess.removeIf(c -> seenCities.contains(c));
        }

        if (reachableCities.contains(origin)) {
            reachableCities.remove(origin);
        }

        return reachableCities.stream().collect(Collectors.toList());
    }

    public static double totalCostForAction(AgentAction agentAction) {
        // TODO implement this
        return 0.0;
    }
}
