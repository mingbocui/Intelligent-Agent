import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.gui.DisplaySurface;

import java.util.ArrayList;

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
        numOfRabbits.display();
        amountOfGrass.display();
        sumEnergyOfRabbits.display();
    }

    public String[] getInitParam() {
        // TODO Auto-generated method stub
        // Parameters to be set by users via the Repast UI slider bar
        // Do "not" modify the parameters names provided in the skeleton code, you can add more if you want
        String[] params = {"GridSize", "NumInitRabbits", "NumInitGrass", "GrassGrowthRate", "BirthThreshold"};
        return params;
    }

    public Schedule getSchedule() {
        // TODO Auto-generated method stub
        return schedule;
    }

    @Override
    public String getName() {
        return "simple rabbit model";
    }

    public void setup() {
        // TODO Auto-generated method stub

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
        // TODO
        return;
    }

    private void buildDisplay(){
        //TODO
        return;
    }

    private void addNewRabbit(){
        RabbitsGrassSimulationAgent rabbit = new RabbitsGrassSimulationAgent(initRabbitEnergy);
        rabbitsList.add(rabbit);
        space.addRabbit(rabbit);
    }


}
