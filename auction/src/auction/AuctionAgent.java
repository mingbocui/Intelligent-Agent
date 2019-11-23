package auction;

import auction.centralized_rla.CentralizedAgent;
import logist.LogistSettings;
import logist.Measures;
import logist.agent.Agent;
import logist.behavior.AuctionBehavior;
import logist.config.Parsers;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.*;

public class AuctionAgent implements AuctionBehavior {

    private long timeoutSetup;
    private long timeoutPlan;
    private long timeoutBid;
    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private Random random;
    private Vehicle vehicle;
    private City currentCity;
    private TaskSet previousTasks;
    private TaskSet currentTasks;
    private CentralizedAgent centralizedPlanner;
    private List<Plan> solutionWithoutBid;
    private List<Plan> solutionWithBid;
    
    @Override
    public void setup(Topology topology, TaskDistribution distribution,
                      Agent agent) {

        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config/settings_auction.xml");
        } catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }

        this.timeoutSetup = ls.get(LogistSettings.TimeoutKey.SETUP);
        this.timeoutPlan = ls.get(LogistSettings.TimeoutKey.PLAN);
        this.timeoutBid = ls.get(LogistSettings.TimeoutKey.BID);
        
        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;
        this.vehicle = agent.vehicles().get(0);
        this.currentCity = vehicle.homeCity();
        
        long seed = -9019554669489983951L * currentCity.hashCode() * agent.id();
        this.random = new Random(seed);

        this.centralizedPlanner = new CentralizedAgent(timeoutPlan);
        this.previousTasks = TaskSet.create(new Task[0]);
    }
    
    /**
     * This signal informs the agent about the outcome of an auction. lastWinner
     * is the id of the agent that won the task. The actual bids of all agents is given
     * as an array lastOffers indexed by agent id. A null offer indicates that the
     * agent did not participate in the auction.
     *
     * @param previous
     * @param winner
     * @param bids
     */
    @Override
    public void auctionResult(Task previous, int winner, Long[] bids) {
        System.out.println("auction result, viewing from agent " + agent.id());
        for (int i = 0; i < bids.length; i++) {
            var s = String.format(">>> agent: %d bids: %d", i, bids[i]);
            if (i == winner) {
                s += " winner";
            }
            System.out.println(s);
        }
        
        if (winner == agent.id()) {
            currentCity = previous.deliveryCity;
        }
    }
    
    
    /**
     * This signal asks the agent to offer a price for a task and it is sent for each
     * task that is auctioned. The agent should return the amount of money it
     * would like to receive for delivering that task. If the agent wins the auction,
     * it is assigned the task, and it must deliver it in the final plan. The reward
     * of the task will be set to the agent’s price. It is possible to return null to
     * reject the task unconditionally.
     *
     * IDEAS:
     *  * just get the min cost + some random number (same as template agent)
     *  * observe behaviour of over next agent
     *
     * @param task
     * @return
     */
    @Override
    public Long askPrice(Task task) {
//        if (vehicle.capacity() < task.weight)
//            return null;
//
//        long distanceTask = task.pickupCity.distanceUnitsTo(task.deliveryCity);
//        long distanceSum = distanceTask
//                + currentCity.distanceUnitsTo(task.pickupCity);
//        double marginalCost = Measures.unitsToKM(distanceSum
//                * vehicle.costPerKm());
//
//        double ratio = 1.0 + (random.nextDouble() * 0.05 * task.id);
//        double bid = ratio * marginalCost;
//
//        return Math.round(bid);
//        previousTasks.a
//        CentralizedAgent planner = new CentralizedAgent(timeoutPlan);
//        currentTasks = previousTasks.add(task);
//        currentTasks = previousTasks.add(previousTasks.size(), task);
//        TaskSet a;
//        a.add
//        previousTasks.stream().
//        TaskSet tempSet = TaskSet.create(task);
//        currentTasks = TaskSet.intersect(previousTasks, task);
//        currentTasks = previousTasks.addAll(task);
        Double marginalCost_double = this.centralizedPlanner.solutionSpace(agent.vehicles(), currentTasks).combinedCost() - this.centralizedPlanner.solutionSpace(agent.vehicles(), previousTasks).combinedCost();

        long marginalCost = marginalCost_double.longValue();

        return marginalCost;
    }
    
    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {



//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);
        
//        Plan planVehicle1 = Utils.naivePlan(vehicle, tasks);
//
//        List<Plan> plans = new ArrayList<Plan>();
//        plans.add(planVehicle1);
//        while (plans.size() < vehicles.size())
//            plans.add(Plan.EMPTY);

//        CentralizedAgent planner = new CentralizedAgent(timeoutPlan);

        List<Plan> plans = this.centralizedPlanner.plan(vehicles, tasks);
        
        return plans;
    }
}
