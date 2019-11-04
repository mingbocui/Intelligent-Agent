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
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

/**
 * Current best solution (without selecting random solutions): 178 iters, cost 21060.5, profit: 1782682.5
 */
public class CentralizedAgent implements CentralizedBehavior {
    private double randomSolutionSelection;
    private long timeoutPlan;
    private long maxIterations;
    private int nRetainedSolutions;
    private Random rnd;
    
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
        this.randomSolutionSelection = agent.readProperty("random-solution-selection", Double.class, 0.35);
        this.maxIterations = agent.readProperty("max-iterations", Integer.class, 300);
        this.nRetainedSolutions = agent.readProperty("nb-retained-solutions", Integer.class, 10);
        this.rnd = new Random();
    }
    
    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long startTime = System.currentTimeMillis();
        SolutionSpace initSpace = new SolutionSpace(vehicles, tasks).largestVehicleSolution(vehicles, tasks);
        List<SolutionSpace> candidateSolutions = new ArrayList<>(List.of(initSpace));
        int nIterations = 0;
        
        SolutionSpace currentBest = new SolutionSpace(initSpace);
        
        while (true) {
            System.out.println(">>> in iteration " + nIterations);
            List<SolutionSpace> newSolutions = new ArrayList<>(List.of(initSpace));
            initSpace.changeVehicle().forEach(s -> newSolutions.addAll(s.permuteActions()));
            
            newSolutions.removeIf(Predicate.not(SolutionSpace::passesConstraints));
            System.out.println("\twe have " + newSolutions.size() + " new sols");
            
            var minSols = Utils.minimalElements(newSolutions);
            System.out.println("\tfound " + minSols.size() + " new min solutions");
            var newBest = minSols.get(rnd.nextInt(minSols.size()));
            
            if (newBest.cost() < currentBest.cost()) {
                currentBest = newBest;
            }
            
            // TODO not sure if we should activate this, if I set it to false it sometimes get's as low as 21386.0
            // we select the randomly best value above... but our approach could be too greedily ...
            // this returns a cost of 21334 and a profit of 1782509
            boolean stuck = candidateSolutions.size() >= this.nRetainedSolutions
                    && candidateSolutions.stream().mapToDouble(SolutionSpace::cost).distinct().limit(2).count() <= 1
                    && minSols.size() == 1;
            if (stuck && rnd.nextDouble() < this.randomSolutionSelection) {
                System.out.println("\tselecting random sol");
                initSpace = newSolutions.get(rnd.nextInt(newSolutions.size()));
            } else {
                System.out.println("\tselecting best sol");
                initSpace = newBest;
            }
            
            System.out.println("\tcurrent cost " + initSpace.cost() + ", lowest so far " + currentBest.cost());
            
            nIterations += 1;
            
            if (nIterations > this.maxIterations || outOfTime(startTime, nIterations)) {
                System.out.println("\tmax iterations reached or out of time");
                break;
            }
            if (candidateSolutions.size() > this.nRetainedSolutions) candidateSolutions.remove(0);
            // java... what is this shit? we need to the copy otherwise the compiler complains
            SolutionSpace finalInitSpace = initSpace;
            if (candidateSolutions.size() >= this.nRetainedSolutions && candidateSolutions.stream().allMatch(s -> s.cost() < finalInitSpace.cost())) {
                System.out.print("\t*** restarting search using old best solution, proposed sol has cost of: " + finalInitSpace.cost() + ", saved costs: ");
                candidateSolutions.forEach(c -> System.out.print(c.cost() + ", "));
                System.out.println();
                initSpace = currentBest;
                candidateSolutions.clear();
            }
            /*
            // in case we're somehow stuck, but since we select a random best solution (out of the minimal ones),
            // we can in theory break out of the loop, somehow -> best to leave it and hope
            if (candidateSolutions.size() == this.nRetainedSolutions && candidateSolutions.stream().mapToDouble(SolutionSpace::cost).distinct().limit(2).count() <= 1) {
                System.out.println("\tstuck in a loop");
                break;
            }
             */
            candidateSolutions.add(initSpace);
        }
        
        System.out.println("*** found sol after " + nIterations + " iters, cost " + currentBest.cost() + " profit: " + currentBest.profit());
        
        return currentBest.getPlans();
    }
    
    private boolean outOfTime(long startTime, int nIterations) {
        if (nIterations == 0) return false;
        
        long executedTime = System.currentTimeMillis() - startTime;
        double runTimeEstimateEachRound = (double) executedTime / nIterations;
        
        // just to be sure that we don't compute for too long
        return (executedTime + runTimeEstimateEachRound * 1.3) > this.timeoutPlan;
    }
}
