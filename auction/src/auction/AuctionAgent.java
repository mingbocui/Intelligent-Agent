package auction;

import auction.centralized_rla.CentralizedAgent;
import auction.centralized_rla.SolutionSpace;
import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.AuctionBehavior;
import logist.config.Parsers;
import logist.plan.Action;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.*;
import java.util.stream.Collectors;

public class AuctionAgent implements AuctionBehavior {

    private long timeoutSetup;
    private long timeoutPlan;
    private long timeoutBid;
    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private Random random;
    private List<Task> wonTasks;
    private CentralizedAgent centralizedPlanner;
    private int nAuctionRounds;
    private SolutionSpace currentSolution;
    private SolutionSpace solutionIfAuctionWon;
    
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
        //this.vehicle = agent.vehicles().get(0);
        //this.currentCity = vehicle.homeCity();
        
        //long seed = -9019554669489983951L * currentCity.hashCode() * agent.id();
        this.random = new Random(42);

        this.centralizedPlanner = new CentralizedAgent(topology, distribution, agent);
        this.wonTasks = new ArrayList<>();
        this.nAuctionRounds = 0;
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
        System.out.println("auction result, viewing from agent " + agent.id() + " n round " + nAuctionRounds);
        for (int i = 0; i < bids.length; i++) {
            var s = String.format(">>> agent: %d bids: %d", i, bids[i]);
            if (i == winner) {
                s += " winner";
            }
            if (i == agent.id()) {
                s += " (me)";
            }
            System.out.println(s);
        }
        
        if (winner == agent.id()) {
            this.wonTasks.add(previous);
            this.currentSolution = this.solutionIfAuctionWon;
        }
        this.nAuctionRounds++;
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
        if (task.reward == 0) {
            return null;
        }
        
        // TODO try to get the first with a I-want-this weight of 1, 0.8, 0.5, 0.25, 0.1
        List oldAndNew = new ArrayList<Task>(this.wonTasks);
        oldAndNew.add(task);
        System.out.println("in round " + this.nAuctionRounds + " with task " + task.toString() + " we have " + this.wonTasks.size() + " bids won");
        
        this.solutionIfAuctionWon = this.centralizedPlanner.solution(agent.vehicles(), oldAndNew, this.timeoutBid);
        
        double price;
        
        if (this.currentSolution == null) {
            price = this.solutionIfAuctionWon.cost();
        } else {
            price = this.solutionIfAuctionWon.cost() - this.currentSolution.cost();
        }
        System.out.println("we're asking for " + price + " task is worth: " + task.reward);
        
        // if we don't make any money with it, we would rather discard it
        if (task.reward < price && this.wonTasks.size() > 0) {
            return null;
        }
        
        price *= 1 + this.random.nextDouble() * 0.3;
        
        return (long)Math.ceil(price);
    }
    
    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        if (tasks.size() == 0) {
            return SolutionSpace.emptyPlan(this.agent.vehicles());
        }
        
        return centralizedPlanner.solution(vehicles, tasks.stream().collect(Collectors.toList()), this.timeoutPlan).getPlans();
    }
}
