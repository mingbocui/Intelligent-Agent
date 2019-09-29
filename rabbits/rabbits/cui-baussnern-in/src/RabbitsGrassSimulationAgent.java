import epfl.lia.logist.tools.Pair;
import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Random;


public class RabbitsGrassSimulationAgent implements Drawable {
    private final int RANDOM_SEED = 42;

    // position
    private int x = 0;
    private int y = 0;

    private int energy = 0;

    private int id = 0;
    private static int RABBIT_INIT_COUNTER = 0;

    private RabbitsGrassSimulationSpace space;
    private Random rnd;

    public RabbitsGrassSimulationAgent() {
        rnd = new Random(RANDOM_SEED);
        id = ++RABBIT_INIT_COUNTER;
    }

    public RabbitsGrassSimulationAgent(int initEnergy) {
        this();
        energy = initEnergy;
    }

    public RabbitsGrassSimulationAgent(int initEnergy, RabbitsGrassSimulationSpace space) {
        this();
        this.energy = initEnergy;
        this.space = space;
    }

    public RabbitsGrassSimulationAgent(int initEnergy, int randomSeed) {
        this();
        energy = initEnergy;
        rnd = new Random(randomSeed);
    }

    public void setXY(int newX, int newY) {
        x = newX;
        y = newY;
    }

    public void setSpace(RabbitsGrassSimulationSpace space) {
        this.space = space;
    }

    public void draw(SimGraphics G) {
        // TODO color rabbit with amount of energy?
        G.drawFastRoundRect(Color.green);
    } // colored rabbit agent with green cell;

    public int getX() { return x; }
    public int getY() { return y; }

    public int getId() { return id; }
    public int getEnergy() { return energy; }

    public boolean isAlive() { return energy > 1; }

    public void report() {
        System.out.println(String.format("%dth rabbit, located at: (%d, %d), remaining energy: %d", id, x, y, energy));
    }

    public void step() {
        Object2DGrid grid = space.getCurrentSpace();

        var possiblePositions = new ArrayList<Pair<Integer>>();
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                // the `+ _gs` deals with the negative case. -1 + 0 % 20 == -1 (in Java), but we want op(-1 + 0) == 19
                // (assuming the grid size is 20). in order to achieve this we "exploit" the modulus operator
                // x + grid-size % grid-size =(if x is lower bounded by 0)= x
                final var _gs = space.getGridSize();
                var _x = (i + x + _gs) % _gs;
                var _y = (i + y + _gs) % _gs;
                possiblePositions.add(new Pair<>(new Integer(_x), new Integer(_y)));
            }
        }

        possiblePositions.removeIf(dir -> space.isOccupied(dir.getFirst(), dir.getSecond()));

        if (possiblePositions.size() > 0) {
            setXY(rnd.nextInt(possiblePositions.size()), rnd.nextInt(possiblePositions.size()));
            this.energy -= 1;
        } else {
            System.out.println(String.format("Rabbit %d could not be moved", id));
        }
    }

    public void consume(int energy) {
        this.energy += energy;
    }

    public void setEnergy(int i) {
        this.energy = i;
    }
}
