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

public class CentralizedAgent implements CentralizedBehavior {
    private double randomSolutionSelection;
    private long timeoutPlan;
    private long maxIterations;
    private int nRetainedSolutions;
    private boolean useSpanningTreeForCost;
    private boolean useRandomInitSolution;
    private boolean useClosestPickUpSolution;
    private boolean useClosestPickUpSolutionByPickup;
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
        this.useSpanningTreeForCost = agent.readProperty("use-spanning-tree-for-cost", Boolean.class, true);
        this.useRandomInitSolution = agent.readProperty("use-random-init-solution", Boolean.class, false);
        this.useClosestPickUpSolution = agent.readProperty("use-closest-pickup-solution", Boolean.class, true);
        this.useClosestPickUpSolutionByPickup = agent.readProperty("use-closest-pickup-solution-by-pickup", Boolean.class, true);
        this.rnd = new Random(agent.readProperty("random-seed", Integer.class, 42));
    }
    
    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long startTime = System.currentTimeMillis();
        SolutionSpace initSpace;
        if (this.useRandomInitSolution) {
            System.out.println("Using random init solution");
            initSpace = SolutionSpace.randomSolution(vehicles, tasks, this.useSpanningTreeForCost);
        } else if (this.useClosestPickUpSolution) {
            if (this.useClosestPickUpSolutionByPickup) {
                System.out.println("Using closest pickup solution");
                initSpace = SolutionSpace.assignClosestTasksByPickup(vehicles, tasks, this.useSpanningTreeForCost);
            } else {
                System.out.println("Using closest delivery solution");
                initSpace = SolutionSpace.assignClosestTasksByDelivery(vehicles, tasks, this.useSpanningTreeForCost);
            }
        } else {
            System.out.println("Using largest vehicle init solution");
            initSpace = SolutionSpace.largestVehicleSolution(vehicles, tasks, this.useSpanningTreeForCost);
        }
        
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
            
            // TODO not sure if we should activate this, if I set it to false it sometimes get's as low as 21386.0
            // we select the randomly best value above... but our approach could be too greedily ...
            // this returns a combinedCost of 21334 and a profit of 1782509
            // with this off it finds this:
            //  *** found sol after 180 iters, cost 21386.0 profit: 1782357.0
            //  *** found sol after 202 iters, cost 21794.0 profit: 1781949.0
            // with this on:
            //      cost 19618
            //  *** found sol after 197 iters, cost 21760.0 profit: 1781983.0
            //  *** found sol after 183 iters, cost 19111.0 profit: 1784632.0
            boolean stuck = candidateSolutions.size() >= this.nRetainedSolutions
                    && candidateSolutions.stream().mapToDouble(SolutionSpace::combinedCost).distinct().limit(2).count() <= 1
                    && minSols.size() == 1;
            boolean chooseRandom = false;
            if (stuck /*&& rnd.nextDouble() < this.randomSolutionSelection */) {
                System.out.println("\tselecting random sol");
                initSpace = newSolutions.get(rnd.nextInt(newSolutions.size()));
                chooseRandom = true;
            } else {
                System.out.println("\tselecting best sol");
                initSpace = newBest;
            }
            
            if (initSpace.combinedCost() < currentBest.combinedCost()) {
                currentBest = initSpace;
            }
            
            System.out.println(String.format("\tcurrent combinedCost (cost): %s (%s), lowest so far: %s",
                    initSpace.combinedCost(), initSpace.cost(), currentBest.combinedCost()));
            
            nIterations += 1;
            
            if (nIterations > this.maxIterations || outOfTime(startTime, nIterations)) {
                System.out.println("\tmax iterations reached or out of time");
                break;
            }
            if (candidateSolutions.size() > this.nRetainedSolutions) candidateSolutions.remove(0);
            // java... what is this shit? we need to do the copy otherwise the compiler complains
            SolutionSpace finalInitSpace = initSpace;
            if (candidateSolutions.size() >= this.nRetainedSolutions
                    && !chooseRandom
                    && candidateSolutions.stream().allMatch(s -> s.combinedCost() < finalInitSpace.combinedCost())) {
                System.out.print("\t*** restarting search using old best solution, proposed sol has combinedCost of: " + finalInitSpace.combinedCost() + ", saved combinedCosts: ");
                candidateSolutions.forEach(c -> System.out.print(c.combinedCost() + ", "));
                System.out.println();
                initSpace = currentBest;
                candidateSolutions.clear();
            }
            /*
            // in case we're somehow stuck, but since we select a random best solution (out of the minimal ones),
            // we can in theory break out of the loop, somehow -> best to leave it and hope
            if (candidateSolutions.size() == this.nRetainedSolutions && candidateSolutions.stream().mapToDouble(SolutionSpace::combinedCost).distinct().limit(2).count() <= 1) {
                System.out.println("\tstuck in a loop");
                break;
            }
             */
            candidateSolutions.add(initSpace);
        }
        
        System.out.println(String.format("*** found solution after %d iterations, combinedCost (cost): %s (%s), lowest so far: %s",
                nIterations, currentBest.combinedCost(), currentBest.cost(), currentBest.profit()));
        
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
