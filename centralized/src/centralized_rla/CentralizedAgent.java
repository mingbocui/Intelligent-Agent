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
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CentralizedAgent implements CentralizedBehavior {
    private double randomSolutionSelection;
    private long timeoutPlan;
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
        this.nRetainedSolutions = agent.readProperty("nb-retained-solutions", Integer.class, 10);
        this.useSpanningTreeForCost = agent.readProperty("use-spanning-tree-for-cost", Boolean.class, true);
        this.useRandomInitSolution = agent.readProperty("use-random-init-solution", Boolean.class, false);
        this.useClosestPickUpSolution = agent.readProperty("use-closest-pickup-solution", Boolean.class, true);
        this.useClosestPickUpSolutionByPickup = agent.readProperty("use-closest-pickup-solution-by-pickup", Boolean.class, true);
        this.rnd = new Random(agent.readProperty("random-seed", Integer.class, 1337));
    }
    
    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long startTime = System.currentTimeMillis();
        SolutionSpace initSpace;
        if (this.useRandomInitSolution) {
            System.out.println("Using random init solution");
            initSpace = SolutionSpace.randomSolution(vehicles, tasks, this.useSpanningTreeForCost, this.rnd);
        } else if (this.useClosestPickUpSolution) {
            if (this.useClosestPickUpSolutionByPickup) {
                System.out.println("Using closest pickup solution");
                initSpace = SolutionSpace.assignClosestTasksByPickup(vehicles, tasks, this.useSpanningTreeForCost, this.rnd);
            } else {
                System.out.println("Using closest delivery solution");
                initSpace = SolutionSpace.assignClosestTasksByDelivery(vehicles, tasks, this.useSpanningTreeForCost, this.rnd);
            }
        } else {
            System.out.println("Using largest vehicle init solution");
            initSpace = SolutionSpace.largestVehicleSolution(vehicles, tasks, this.useSpanningTreeForCost);
        }
        
        List<SolutionSpace> candidateSolutions = new ArrayList<>(List.of(initSpace));
        List<SolutionSpace> currentMinSolutions = new ArrayList<>();
        int nIterations = 0;
        
        SolutionSpace currentBest = new SolutionSpace(initSpace);
        
        while (true) {
            System.out.println(">>> in iteration " + nIterations);
            List<SolutionSpace> newSolutions = initSpace.changeVehicle().parallelStream()
                    .flatMap(s -> s.permuteActions().stream()).collect(Collectors.toList());
            newSolutions.add(initSpace);
            
            newSolutions.removeIf(Predicate.not(SolutionSpace::passesConstraints));
            System.out.println("\twe have " + newSolutions.size() + " new sols");
            
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
            //
            //  *** found solution after 90 iterations, combinedCost (cost): 1.81207077E7 (18732.5), lowest so far: 1785010.5
            boolean stuck = candidateSolutions.size() >= this.nRetainedSolutions
                    && candidateSolutions.stream().mapToDouble(SolutionSpace::combinedCost).distinct().limit(2).count() <= 1;
            boolean chooseRandom = false;
            if (stuck || rnd.nextDouble() < this.randomSolutionSelection) {
                if (!currentMinSolutions.isEmpty()) {
                    System.out.println("\tselecting sol from queue, " + currentMinSolutions.size() + " left to explore");
                    initSpace = currentMinSolutions.get(rnd.nextInt(currentMinSolutions.size()));
                    currentMinSolutions.remove(initSpace);
                } else {
                    System.out.println("\tselecting random sol");
                    initSpace = newSolutions.get(rnd.nextInt(newSolutions.size()));
                }
                chooseRandom = true;
            } else {
                var minSols = Utils.minimalElements(newSolutions);
                System.out.println("\tfound " + minSols.size() + " new min solutions");
                var newBest = minSols.get(rnd.nextInt(minSols.size()));
                System.out.println("\tselecting best sol");
                initSpace = newBest;
                
                if (minSols.get(0).combinedCost() < currentBest.combinedCost()) {
                    for (int i = 0; i < Math.min(10, minSols.size()); i++) {
                        currentMinSolutions.add(minSols.get(rnd.nextInt(minSols.size())));
                    }
                }
            }
            
            if (initSpace.combinedCost() < currentBest.combinedCost()) {
                currentBest = initSpace;
            }
            
            System.out.println(String.format("\tcurrent combinedCost (cost): %s (%s), lowest so far: %s",
                    initSpace.combinedCost(), initSpace.cost(), currentBest.combinedCost()));
            
            nIterations += 1;
            
            if (outOfTime(startTime, nIterations)) {
                System.out.println("\tout of time!");
                break;
            }
            if (candidateSolutions.size() > this.nRetainedSolutions) candidateSolutions.remove(0);
            // java... what is this shit? we need to do the copy otherwise the compiler complains
            SolutionSpace finalInitSpace = initSpace;
            if (candidateSolutions.size() >= this.nRetainedSolutions
                    && !chooseRandom // done to prevent overwriting the selection above from being stuck
                    && candidateSolutions.stream().allMatch(s -> s.combinedCost() < finalInitSpace.combinedCost())) {
                System.out.print("\t*** restarting search using old best solution, proposed sol has combinedCost of: " + finalInitSpace.combinedCost() + ", saved combinedCosts: ");
                candidateSolutions.forEach(c -> System.out.print(c.combinedCost() + ", "));
                System.out.println();
                initSpace = currentBest;
                candidateSolutions.clear();
            }
            
            candidateSolutions.add(initSpace);
        }
        
        System.out.println(String.format("*** found solution after %d iterations, combinedCost (cost): %s (%s), profit: %s",
                nIterations, currentBest.combinedCost(), currentBest.cost(), currentBest.profit()));
        
        return currentBest.getPlans();
    }
    
    private boolean outOfTime(long startTime, int nIterations) {
        if (nIterations == 0) return false;
        
        long executedTime = System.currentTimeMillis() - startTime;
        double runTimeEstimateEachRound = (double) executedTime / nIterations;
        
        // just to be sure that we don't compute for too long
        return (executedTime + runTimeEstimateEachRound * 2.0) > this.timeoutPlan;
    }
}
