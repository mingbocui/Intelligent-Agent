package centralized_rla;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    
    public double combinedCost() {
        return cost() * spanningTreeLength();
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
    
    public double profit() {
        return this.getTasks().stream().mapToDouble(t -> t.reward).sum() - this.cost();
    }
    
    public double spanningTreeLength() {
        // using kruskal
        Set<Topology.City> citiesToVisit = new HashSet<>(Set.of(vehicle.homeCity()));
        getTasks().forEach(t -> citiesToVisit.addAll(List.of(t.pickupCity, t.deliveryCity)));
        
        List<Edge> edges = new ArrayList<>();
        for (final var cityA : citiesToVisit) {
            for (final var cityB : citiesToVisit) {
                if (cityA != cityB) {
                    edges.add(new Edge(cityA, cityB));
                }
            }
        }
        
        List<Edge> chosenEdges = new ArrayList<>();
        for (final var edge : edges) {
            Set<Topology.City> exploredCities = chosenEdges.stream().flatMap(t -> t.getCities().stream()).collect(Collectors.toSet());
            
            if (exploredCities.containsAll(citiesToVisit)) break;
            
            if (!exploredCities.containsAll(edge.getCities())) {
                chosenEdges.add(edge);
            }
        }
        
        return chosenEdges.stream().mapToDouble(Edge::getDist).sum();
    }
    
    private class Edge {
        public Topology.City start;
        public Topology.City end;
        public double dist;
        
        public Edge(Topology.City start, Topology.City end) {
            this.start = start;
            this.end = end;
            this.dist = start.distanceTo(end);
        }
        
        public double getDist() {
            return dist;
        }
        
        public List<Topology.City> getCities() {
            return new ArrayList<>(List.of(start, end));
        }
    }
}
