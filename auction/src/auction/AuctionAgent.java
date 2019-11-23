package auction;

import auction.centralized_rla.CentralizedAgent;
import auction.centralized_rla.SolutionSpace;
import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.AuctionBehavior;
import logist.config.Parsers;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;

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
    private List<Task> myWonTasks;
    private List<Task> oppWonTasks; // opponennt won tasks

    private CentralizedAgent centralizedPlanner;
    private int nAuctionRounds;
    private SolutionSpace myCurrentSolution;
    private SolutionSpace mySolutionIfAuctionWon;
    private SolutionSpace oppCurrentSolution;
    private SolutionSpace oppSolutionIfAuctionWon;


    private double ratioMarginalCost = 0.9;
    private double ratioMarginalCostOpp = 0.8; // ration to bid higher price than our opponents

    
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
        this.myWonTasks = new ArrayList<>();
        this.oppWonTasks = new ArrayList<>();
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
        // TODO get lowest bid -> adapt
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
            this.ratioMarginalCostOpp = this.ratioMarginalCostOpp - 0.01; // if we win, we will reduce ratio and bid a lower price
            this.myWonTasks.add(previous);
            this.myCurrentSolution = this.mySolutionIfAuctionWon;
        }
        else{
            this.ratioMarginalCostOpp = this.ratioMarginalCostOpp + 0.01; // if we lose, we will increase ratio and bid a higher price
            this.oppWonTasks.add(previous);
            this.oppCurrentSolution = this.oppSolutionIfAuctionWon;
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
        List myOldAndNew = new ArrayList<Task>(this.myWonTasks);
        List oppOldAndNew = new ArrayList<Task>(this.oppWonTasks);

        myOldAndNew.add(task);
        oppOldAndNew.add(task);

        System.out.println("in round " + this.nAuctionRounds + " with task " + task.toString() + " we have " + this.myWonTasks.size() + " bids won");
        System.out.println("in round " + this.nAuctionRounds + " with task " + task.toString() + " opponents have " + this.oppWonTasks.size() + " bids won");

        // TODO split the timeoutBID to two parts
        this.mySolutionIfAuctionWon = this.centralizedPlanner.solution(agent.vehicles(), myOldAndNew, this.timeoutBid);
        this.oppSolutionIfAuctionWon = this.centralizedPlanner.solution(agent.vehicles(), oppOldAndNew, this.timeoutBid);
        
        double myPrice;
        double oppPrice;
        
        if (this.myCurrentSolution == null) {
            myPrice = this.mySolutionIfAuctionWon.cost();
        } else {
            myPrice = this.mySolutionIfAuctionWon.cost() - this.myCurrentSolution.cost();
        }

        if (this.oppCurrentSolution == null) {
            oppPrice = this.oppSolutionIfAuctionWon.cost();
        } else {
            oppPrice = this.oppSolutionIfAuctionWon.cost() - this.oppCurrentSolution.cost();
        }

        System.out.println("we're asking for " + myPrice + " task is worth: " + task.reward);
        System.out.println("our opponents may aski for " + oppPrice + " task is worth: " + task.reward);
    
        // TODO optimise this. I think a balance between being too greedy
        // if we don't make any money with it, we would rather discard it
        if (task.reward < myPrice && this.myWonTasks.size() > 0) {
            return null;
        } else {
            myPrice = this.ratioMarginalCost * oppPrice;
//            myPrice *= 1 + this.random.nextDouble() * 0.3;
        }
        
        return (long)Math.ceil(myPrice);
    }
    
    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        if (tasks.size() == 0) {
            return SolutionSpace.emptyPlan(this.agent.vehicles());
        }
        
        // TODO mingbao can you use the already computed plan, but replace the old tasks with the new ones?
        // you'll probably need to use `Action.toString()` and then extract some shit in order to find compare the tasks
        // then just do `newPlan.appendPickup(task)`
        // the reason is that we don't need to recompute the old solution, and we don't want to.
        
        return centralizedPlanner.solution(vehicles, tasks.stream().collect(Collectors.toList()), this.timeoutPlan).getPlans();
    }
}
