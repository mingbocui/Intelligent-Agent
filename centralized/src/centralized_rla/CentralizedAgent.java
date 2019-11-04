package centralized_rla;

import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.CentralizedBehavior;
import logist.config.Parsers;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 */
public class CentralizedAgent implements CentralizedBehavior {
    private double keepSolutionProbability;
    private long timeoutPlan;
    private long maxIterations;
    
    @Override
    public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config/settings_default.xml");
        } catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }
        
        this.timeoutPlan = ls.get(LogistSettings.TimeoutKey.PLAN);
        
        // As suggested from the slides, 0.3 to 0.5 would be a good choice for p.
        this.keepSolutionProbability = agent.readProperty("keep-solution-probability", Double.class, 0.35);
        this.maxIterations = agent.readProperty("max-iterations", Integer.class, 300);
    }
    
    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long startTime = System.currentTimeMillis();
        SolutionSpace initSpace = new SolutionSpace(vehicles, tasks).largestVehicleSolution(vehicles, tasks);
        SolutionSpace prev = new SolutionSpace(initSpace);
        int nIterations = 0;
        
        while (!outOfTime(startTime, nIterations) && !convergenceReached(nIterations, prev, initSpace)) {
            System.out.println(">>> in iteration " + nIterations);
            if (Math.random() > 1 - this.keepSolutionProbability) {
                System.out.println("\tselecting original plan");
            } else {
                prev = initSpace;
                List<SolutionSpace> newSolutions = new ArrayList<>();
                // now new solutions do not contain initial solution
                // TODO I still think it should be added
                initSpace.changeVehicle().forEach(s -> newSolutions.addAll(s.permuteActions()));
                
                newSolutions.removeIf(Predicate.not(SolutionSpace::passesConstraints));
                System.out.println("\twe have " + newSolutions.size() + " new sols");
                
                // this is the localChoice
                initSpace = newSolutions.stream().min(Comparator.comparingDouble(SolutionSpace::cost)).get();
                System.out.println("\tselecting new plan");
            }
            
            System.out.println("\tcurrent cost " + initSpace.cost());
            
            nIterations += 1;
        }
        
        System.out.println("*** found sol after " + nIterations + " iters, cost " + initSpace.cost());
        
        return initSpace.getPlans();
    }
    
    private boolean convergenceReached(int nIterations, SolutionSpace prev, SolutionSpace current) {
        // TODO if prev == current && nIterations > 3 we have a problem...
        if (nIterations > this.maxIterations) return true;
        else return nIterations > 3 && Math.abs(current.cost() - prev.cost()) < 0.5;
    }
    
    private boolean outOfTime(long startTime, int nIterations) {
        if (nIterations == 0) return false;
        
        long executedTime = System.currentTimeMillis() - startTime;
        double runTimeEstimateEachRound = (double) executedTime / nIterations;
        
        // just to be sure that we don't compute for too long
        return (executedTime + runTimeEstimateEachRound * 1.3) > this.timeoutPlan;
    }
}
