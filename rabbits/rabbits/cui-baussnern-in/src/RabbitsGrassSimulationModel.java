import epfl.lia.logist.tools.Pair;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.engine.*;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.util.SimUtilities;

import java.awt.*;
import java.util.ArrayList;
import java.util.Random;

/**
 * Class that implements the simulation model for the rabbits grass
 * simulation.  This is the first class which needs to be setup in
 * order to run Repast simulation. It manages the entire RePast
 * environment and the simulation.
 *
 * @author
 */

public class RabbitsGrassSimulationModel extends SimModelImpl {
    private static final int GRIDSIZE = 20;
    private static final int NUMINITRABBITS = 10;
    private static final int NUMINITGRASS = 100;
    private static final int AMOUNTGRASSENERGY = 100;
    private static final int GRASSGROWTHRATE = 20;
    private static final int BIRTHTHRESHOLD = 10;
    private static final int INITRABBITENERGY = 10;
    private static final int RANDOM_SEED = 42;

    private int gridSize = GRIDSIZE;
    private int numInitRabbits = NUMINITRABBITS;
    private int numInitGrass = NUMINITGRASS;
    private int amountGrassEnergy = AMOUNTGRASSENERGY;
    private int grassGrowthRate = GRASSGROWTHRATE;
    private int birthThreshold = BIRTHTHRESHOLD;
    private int initRabbitEnergy = INITRABBITENERGY;

    private Schedule schedule;
    private DisplaySurface displaySurface;
    private RabbitsGrassSimulationSpace space;

    private ArrayList<RabbitsGrassSimulationAgent> agents;

    // creating graph to show Energy, Rabbits, Grass, etc
    // TODO rename this... somewhat confusingly named
    private OpenSequenceGraph sumEnergyOfRabbits; //sumEnergyOfRabbits
    private OpenSequenceGraph numOfRabbits; //numOfRabbits
    private OpenSequenceGraph amountOfGrass; //amountOfGrass

    private Random rnd;
    private Object2DDisplay displayAgents;

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
        this.rnd = new Random(RANDOM_SEED);

        buildModel();
        buildSchedule();
        buildDisplay();
        displaySurface.display();
        // TODO register these the same way as the displaySurface, check `this.setup()`
        // numOfRabbits.display();
        // amountOfGrass.display();
        // sumEnergyOfRabbits.display();
    }

    public String[] getInitParam() {
        // TODO Auto-generated method stub
        // Parameters to be set by users via the Repast UI slider bar
        // Do "not" modify the parameters names provided in the skeleton code, you can add more if you want
        String[] params = {"GridSize", "NumInitRabbits", "NumInitGrass", "GrassGrowthRate", "BirthThreshold"};
        return params;
    }

    public Schedule getSchedule() {
        return schedule;
    }

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

        class CountLivingAgents extends BasicAction {
            public void execute() {
                System.out.println(String.format("We have %d alive agents (rabbits)", countLivingAgents()));
            }
        }


        schedule.scheduleActionBeginning(0, new Step());
        schedule.scheduleActionAtInterval(10, new CountLivingAgents());
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

    private void buildDisplay(){
        var colorMap = new ColorMap();

        colorMap.mapColor(0, Color.white);
        for (int i = 1; i < 16; i++) {
            colorMap.mapColor(i, new Color((int)(i * 8 + 127), 0, 0));
        }

        var displayGrass = new Value2DDisplay(space.getGrassSpace(), colorMap);
        displayAgents = new Object2DDisplay(space.getAgentSpace());

        displayAgents.setObjectList(agents);
        displaySurface.addDisplayable(displayGrass, "Grass");
        displaySurface.addDisplayable(displayAgents, "Agents");
    }

    private int countLivingAgents() {
        var count = 0;
        // TODO replace this with a map and sum... uag fucking java
        for(final var agent: agents) {
            if (agent.isAlive()) {
                count++;
            }
        }
        return count;
    }

    private void addNewAgent(){
        var rabbit = new RabbitsGrassSimulationAgent(initRabbitEnergy, this.space);
        // TODO get a random place
        var possiblePositions = new ArrayList<Point>();
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                if (!space.isOccupied(i, j)) {
                    possiblePositions.add(new Point(i, j));
                }
            }
        }

        var pos = possiblePositions.get(rnd.nextInt(possiblePositions.size()));
        rabbit.setXY(pos.x, pos.y);

        agents.add(rabbit);
        space.addRabbit(rabbit);
    }
}
