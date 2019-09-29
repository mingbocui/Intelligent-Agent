import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.util.SimUtilities;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
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

    private ArrayList<RabbitsGrassSimulationAgent> rabbitsList;

    // creating graph to show Energy, Rabbits, Grass, etc
    // TODO rename this... somewhat confusingly named
    private OpenSequenceGraph sumEnergyOfRabbits;
    private OpenSequenceGraph numOfRabbits;
    private OpenSequenceGraph amountOfGrass;

    private Random rnd;

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

    public void begin() {
        // TODO Auto-generated method stub
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
        rabbitsList = new ArrayList<RabbitsGrassSimulationAgent>();
        schedule = new Schedule(1);

        if (displaySurface != null) {
            displaySurface.dispose();
        }

        displaySurface = null;
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

        space = new RabbitsGrassSimulationSpace(this.gridSize, this.amountGrassEnergy);
        space.spreadGrass(numInitGrass);
        rabbitsList = new ArrayList<RabbitsGrassSimulationAgent>();
        for (int i = 0; i < numInitRabbits; i++) {
            addNewRabbit();
        }

        // report the status of rabbits
        rabbitsList.forEach(rabbit -> rabbit.report());
    }


    private void buildSchedule(){
        class Step extends BasicAction {
            public void execute() {
                SimUtilities.shuffle(rabbitsList); // not sure if necessary
                rabbitsList.forEach(a -> a.step());

                int deadAgents = reapDeadAgents();
                for (int i = 0; i < deadAgents; i++) {
                    addNewRabbit();
                }

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

    private void buildDisplay(){
        var colorMap = new ColorMap();

        colorMap.mapColor(0, Color.white);
        colorMap.mapColor(1, Color.green);

        // TODO add more colors? how many? how are they determined?

        var displayGrass = new Value2DDisplay(space.getGrassSpace(), colorMap);
        var displayAgents = new Object2DDisplay(space.getRabbitSpace());

        displayAgents.setObjectList(rabbitsList);
        displaySurface.addDisplayable(displayGrass, "Grass");
        displaySurface.addDisplayable(displayAgents, "Agents");
    }

    private int countLivingAgents() {
        var count = 0;
        // TODO replace this with a map and sum... uag fucking java
        for(final var agent: rabbitsList) {
            if (agent.isAlive()) {
                count++;
            }
        }
        return count;
    }

    private int reapDeadAgents() {
        int oldCount = rabbitsList.size();
        var newList = new ArrayList<RabbitsGrassSimulationAgent>();
        // TODO maybe use ArrayList.removeIf()
        for (final var agent: rabbitsList) {
            if (!agent.isAlive()) {
                var x = agent.getX();
                var y = agent.getY();
                space.removeRabbitFromSpace(x, y);
                // TODO do something else here with the rabbit?
            } else {
                newList.add(agent);
            }
        };

        this.rabbitsList = newList;
        return oldCount - this.rabbitsList.size();
    }

    private void addNewRabbit(){
        RabbitsGrassSimulationAgent rabbit = new RabbitsGrassSimulationAgent(initRabbitEnergy);
        rabbitsList.add(rabbit);
        space.addRabbit(rabbit);
    }
}
