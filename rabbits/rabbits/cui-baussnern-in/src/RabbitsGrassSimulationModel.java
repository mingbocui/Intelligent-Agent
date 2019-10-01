import uchicago.src.sim.analysis.DataSource;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;
import uchicago.src.sim.engine.*;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.util.SimUtilities;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Class that implements the simulation model for the rabbits grass
 * simulation.  This is the first class which needs to be setup in
 * order to run Repast simulation. It manages the entire RePast
 * environment and the simulation.
 *
 * @author
 */

public class RabbitsGrassSimulationModel extends SimModelImpl {
    private int gridSize = Config.GRID_SIZE;
    private int numInitRabbits = Config.NUM_INIT_RABBITS;
    private int numInitGrass = Config.NUM_INIT_GRASS;
    private int birthThreshold = Config.BIRTH_THRESHOLD;
    private int initRabbitEnergy = Config.INIT_RABBIT_ENERGY;
    private int grassGrowthRate = Config.GRASS_GROWTH_RATE;

    private Schedule schedule;
    private DisplaySurface displaySurface;
    private RabbitsGrassSimulationSpace space;

    private ArrayList<RabbitsGrassSimulationAgent> agents;

    private OpenSequenceGraph reportingGraph;

    private Random rnd;
    private Object2DDisplay displayAgents;

    class AliveAgents implements DataSource, Sequence {
        public Object execute() {
            return Double.valueOf(getSValue());
        }

        public double getSValue() {
            return countLivingAgents();
        }
    }

    class EnergyAgents implements DataSource, Sequence {
        public Object execute() {
            return Double.valueOf(getSValue());
        }

        public double getSValue() {
            return (double)agents.stream().mapToInt(a -> a.getEnergy()).sum();
        }
    }

    class GrassAmount implements DataSource, Sequence {
        public Object execute() {
            return Double.valueOf(getSValue());
        }

        public double getSValue() {
            return space.getTotalGrassAmount();
        }
    }

    public static void main(String[] args) {
        System.out.println("Rabbit skeleton");

        SimInit init = new SimInit();
        RabbitsGrassSimulationModel model = new RabbitsGrassSimulationModel();
        // Do "not" modify the following lines of parsing arguments
        if (args.length == 0) // by default, you don't use parameter file nor batch mode
            init.loadModel(model, "", false);
        else
            init.loadModel(model, args[0], Boolean.parseBoolean(args[1]));

    }

    // let's not do this... we don't know what else might be going on
    // public RabbitsGrassSimulationModel() {
    //     rnd = new Random(RANDOM_SEED);
    // }

    public void begin() {
        this.rnd = new Random(Config.RANDOM_SEED);

        buildModel();
        buildSchedule();
        buildDisplay();

        displaySurface.display();

        reportingGraph.display();
    }

    public String[] getInitParam() {
        // TODO Auto-generated method stub
        // Parameters to be set by users via the Repast UI slider bar
        // Do "not" modify the parameters names provided in the skeleton code, you can add more if you want
        String[] params = {"GridSize", "NumInitRabbits", "NumInitGrass",
                "GrassGrowthRate", "BirthThreshold", "InitRabbitEnergy"};
        return params;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public int getGridSize() {
        return gridSize;
    }
    public void setGridSize(int gridSize) {
        this.gridSize = gridSize;
    }

    public int getGrassGrowthRate() {
        return grassGrowthRate;
    }
    public void setGrassGrowthRate(int grassGrowthRate) {
        this.grassGrowthRate = grassGrowthRate;
    }

    public int getNumInitRabbits() {
        return numInitRabbits;
    }
    public void setNumInitRabbits(int numInitRabbits) {
        this.numInitRabbits = numInitRabbits;
    }

    public int getNumInitGrass() {
        return numInitGrass;
    }
    public void setNumInitGrass(int numInitGrass) {
        this.numInitGrass = numInitGrass;
    }

    public int getBirthThreshold() {
        return birthThreshold;
    }

    public void setBirthThreshold(int birthThreshold) {
        this.birthThreshold = birthThreshold;
    }

    public void setInitRabbitEnergy(int initRabbitEnergy) { this.initRabbitEnergy = initRabbitEnergy; }
    public int getInitRabbitEnergy() { return this.initRabbitEnergy; }

    @Override
    public String getName() {
        return "simple rabbit model";
    }

    public void setup() {
        space = null;
        agents = new ArrayList<RabbitsGrassSimulationAgent>();
        schedule = new Schedule(1);

        if (displaySurface != null) {
            displaySurface.dispose();
        }

        displaySurface = new DisplaySurface(this, "window 1");
        registerDisplaySurface("window 1", displaySurface);

        if (reportingGraph != null) {
            reportingGraph.dispose();
        }
        reportingGraph = new OpenSequenceGraph("Reporting Graph", this);

        this.registerMediaProducer("Plot", reportingGraph);
    }

    private void buildModel(){
        System.out.println("Model Building Process");

        space = new RabbitsGrassSimulationSpace(this.gridSize);
        space.spreadGrass(numInitGrass);
        agents = new ArrayList<RabbitsGrassSimulationAgent>();
        for (int i = 0; i < numInitRabbits; i++) {
            addNewAgent();
        }

        // report the status of rabbits
        for (final var agent: agents) {
            agent.report();
        }
    }

    private void buildSchedule(){
        class Step extends BasicAction {
            public void execute() {
                SimUtilities.shuffle(agents);

                // order of actions is constant
                triggerAgentMove();
                triggerAgentEating();
                triggerAgentReproduction();
                triggerAgentDeaths();
                triggerGrassGrowth();

                displayAgents.setObjectList(agents); // somehow they get copied or what???

                displaySurface.updateDisplay();
            }
        }

        class Reporting extends BasicAction {
            public void execute() {
                reportingGraph.step();

                System.out.println(String.format("We have %d alive agents (rabbits)", countLivingAgents()));
            }
        }

        schedule.scheduleActionBeginning(0, new Step());
        schedule.scheduleActionBeginning(0, new Reporting()); // should be done after the `Step` action, not just randomly
    }

    private void buildDisplay(){
        var grassColorMap = new ColorMap();

        grassColorMap.mapColor(0, Color.white);
        for (int i = 1; i < 16; i++) {
            grassColorMap.mapColor(i, new Color(0, i * 8 + 127, 0));
        }

        var displayGrass = new Value2DDisplay(space.getGrassSpace(), grassColorMap);
        displayAgents = new Object2DDisplay(space.getAgentSpace());

        displayAgents.setObjectList(agents);
        displaySurface.addDisplayable(displayGrass, "Grass");
        displaySurface.addDisplayable(displayAgents, "Agents");

        reportingGraph.addSequence("num of rabbits", new AliveAgents());
        reportingGraph.addSequence("total rabbit energy", new EnergyAgents());
        reportingGraph.addSequence("amount of grass", new GrassAmount());
    }

    private void triggerGrassGrowth() {
        space.spreadGrass(this.grassGrowthRate);
    }

    private void triggerAgentDeaths() {
        var aliveAgents = new ArrayList<RabbitsGrassSimulationAgent>();
        for (var agent: agents) {
            if (agent.isAlive()) {
                aliveAgents.add(agent);
            } else {
                space.removeRabbitFromSpace(agent.getX(), agent.getY());
            }
        }
        this.agents = aliveAgents;
    }

    private void triggerAgentReproduction() {
        var numberOfNewAgents = 0;
        for (var agent: agents) {
            if (agent.getEnergy() > this.birthThreshold) {
                agent.setEnergy(agent.getEnergy() - this.birthThreshold);
                numberOfNewAgents++;
            }
        }

        if ((agents.size() == gridSize * gridSize) && (numberOfNewAgents > 0)) {
            System.out.println("Warning: the world is fully occupied");
        } else {
            System.out.println(String.format("We will add %d new agents, %d places are still not occupied",
                    numberOfNewAgents, gridSize * gridSize - agents.size()));
        }


        for (int i = 0; i < numberOfNewAgents; i++) {
            this.addNewAgent();
        }
    }

    private void triggerAgentMove() {
        for (var agent: agents) {
            agent.step();
        }
    }
    private void triggerAgentEating() {
        for (var agent: agents) {
            agent.consume(space.harvestGrass(agent.getX(), agent.getY())); // harvestGrass can return 0
        }
    }

    private int countLivingAgents() {
        var count = 0;
        for (final var agent: agents) {
            if (agent.isAlive()) {
                count++;
            }
        }
        return count;
    }

    private void addNewAgent(){
        var rabbit = new RabbitsGrassSimulationAgent(initRabbitEnergy, this.space);
        var possiblePositions = new ArrayList<Point>();
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                if (!space.isOccupied(i, j)) {
                    possiblePositions.add(new Point(i, j));
                }
            }
        }

        if (possiblePositions.size() > 0) {
            var pos = possiblePositions.get(rnd.nextInt(possiblePositions.size()));
            rabbit.setXY(pos.x, pos.y);
            agents.add(rabbit);
            space.addRabbit(rabbit);
        } else {
            System.out.println("WARING: Could not place new agent.");
        }
    }
}
