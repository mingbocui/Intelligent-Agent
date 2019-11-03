package centralized_rla;

import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.CentralizedBehavior;
import logist.config.Parsers;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 */
public class CentralizedAgent implements CentralizedBehavior {
    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private long timeout_setup;
    private long timeout_plan;
    
    @Override
    public void setup(Topology topology, TaskDistribution distribution,
                      Agent agent) {
        
        // this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config/settings_default.xml");
        } catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }
        
        // the setup method cannot last more than timeout_setup milliseconds
        timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
        // the plan method cannot execute more than timeout_plan milliseconds
        timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
        
        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;
    }
    
    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        SolutionSpace initSpace = new SolutionSpace(vehicles, tasks).randomSolution(vehicles, tasks);
        SolutionSpace prev = new SolutionSpace(initSpace);
        int nIterations = 0;
        
        // TODO it should also return something if we're running out of time
        while(!convergenceReached(nIterations, prev, initSpace)) {
            if (nIterations % 10 == 0) {
                System.out.println("nIters " + nIterations);
            }
            List<SolutionSpace> newSolutions = new ArrayList<>();
            newSolutions.add(initSpace);
            initSpace.changeVehicle().forEach(s -> newSolutions.addAll(s.permuteActions()));
            
            newSolutions.removeIf(Predicate.not(SolutionSpace::passesConstraints));
            System.out.println("we have " + newSolutions.size() + " new sols");
            
            prev = initSpace;
            initSpace = newSolutions.stream().min(Comparator.comparingDouble(SolutionSpace::cost)).get();
            System.out.println("current cost " + initSpace.cost());
            nIterations += 1;
        }
        System.out.println("found sol after " + nIterations + " iters, cost " + initSpace.cost());
        
        return initSpace.getPlans();
    }
    
    private boolean convergenceReached(int nIterations, SolutionSpace prev, SolutionSpace current) {
        if (nIterations > 100) return true;
        else return nIterations > 3 && Math.abs(current.cost() - prev.cost()) < 0.5;
    }
}
