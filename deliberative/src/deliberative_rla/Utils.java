package deliberative_rla;

import logist.plan.Action;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology;

import java.util.*;

public class Utils {
    public static HashMap<Topology.City, Set<Set<Task>>> taskPerCity(TaskSet tasks) {
        var map = new HashMap<Topology.City, ArrayList<Task>>();
        
        for (final var task : tasks) {
            if (map.containsKey(task.pickupCity)) {
                map.get(task.pickupCity).add(task);
            } else {
                map.put(task.pickupCity, new ArrayList<>(Arrays.asList(task)));
            }
        }
    
        var powerMap = new HashMap<Topology.City, Set<Set<Task>>>();
        
        for (final var entry : map.entrySet()){
            powerMap.put(entry.getKey(), Utils.powerSet(new LinkedHashSet<>(entry.getValue())));
        }
        
        return powerMap;
    }
    
    /**
     * source: https://stackoverflow.com/a/1670871
     * @param originalSet
     * @param <T>
     * @return
     */
    public static <T> Set<Set<T>> powerSet(Set<T> originalSet) {
        Set<Set<T>> sets = new HashSet<Set<T>>();
        if (originalSet.isEmpty()) {
            sets.add(new HashSet<T>());
            return sets;
        }
        List<T> list = new ArrayList<T>(originalSet);
        T head = list.get(0);
        Set<T> rest = new HashSet<T>(list.subList(1, list.size()));
        for (Set<T> set : powerSet(rest)) {
            Set<T> newSet = new HashSet<T>();
            newSet.add(head);
            newSet.addAll(set);
            sets.add(newSet);
            sets.add(set);
        }
        return sets;
    }
    
    public static ArrayList<Action> planAsList(Plan plan) {
        var planAsList = new ArrayList<Action>();
        plan.forEach(planAsList::add);
        return planAsList;
    }
    
    public static String getCityString(Action.Move moveAction) {
        // it's always `Move (" + destination + ")`
        var s = moveAction.toString();
        
        return s.substring(6, s.length() - 1);
    }
}
