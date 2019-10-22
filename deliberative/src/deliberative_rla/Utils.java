package deliberative_rla;

import logist.plan.Action;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.time.Duration;
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

        for (final var entry : map.entrySet()) {
            powerMap.put(entry.getKey(), Utils.powerSet(new LinkedHashSet<>(entry.getValue())));
        }

        return powerMap;
    }

    /**
     * source: https://stackoverflow.com/a/1670871
     *
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

    public static String getCityString(Action moveAction) {
        if (moveAction instanceof Action.Move) {
            // it's always `Move (" + destination + ")`
            var s = moveAction.toString();

            return s.substring(6, s.length() - 1);
        }
        throw new IllegalArgumentException("can't call this on a not move-action");
    }

    private static boolean stupidCircle(ArrayList<Action> plan, int begin) {
        return plan.subList(begin, plan.size())
                .stream()
                .allMatch(a -> a instanceof Action.Move);
    }

    public static boolean hasUselessCircle(State ns) {
        return hasUselessCircle(ns.initialCity, ns.city, ns.plan);
    }

    public static boolean hasUselessCircle(City initialCity, City currentCity, ArrayList<Action> actions) {
        if (actions.size() < 2) {
            return false;
        }

        String visiting = initialCity.toString();

        if (visiting.equals(currentCity.toString()) && stupidCircle(actions, 0)) return true;

        for (int i = 0; i < actions.size(); i++) {
            if (actions.get(i) instanceof Action.Move) {
                visiting = Utils.getCityString(actions.get(i));
            }
            // check if rest is only moves, if current city is equal to old
            if (visiting.equals(currentCity.toString())
                    && i + 1 < actions.size()
                    && stupidCircle(actions, i + 1)) {
                return true;
            }
        }

        return false;
    }

    public static String humanReadableFormat(Duration duration) {
        return duration.toString()
                .substring(2)
                .replaceAll("(\\d[HMS])(?!$)", "$1 ")
                .toLowerCase();
    }
}
