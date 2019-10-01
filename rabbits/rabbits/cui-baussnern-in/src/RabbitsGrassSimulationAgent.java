import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;

import java.awt.*;
import java.util.ArrayList;
import java.util.Random;

import static java.lang.Math.abs;


public class RabbitsGrassSimulationAgent implements Drawable {
    private static int RABBIT_INIT_COUNTER = 0; // the worst way to keep an id, and we don't even need it that much

    private int id = 0;
    private Point position;
    private int energy = 0;

    private RabbitsGrassSimulationSpace space;
    private Random rnd;

    public RabbitsGrassSimulationAgent() {
        rnd = new Random(Config.RANDOM_SEED);
        id = ++RABBIT_INIT_COUNTER;
        position = new Point();
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
        position.x = newX;
        position.y = newY;
    }

    public void setSpace(RabbitsGrassSimulationSpace space) {
        this.space = space;
    }

    public void draw(SimGraphics G) {
        G.drawCircle(Config.AGENT_BASE_COLOR);
        //G.drawFastRoundRect(Color.red);
    }

    public int getX() { return position.x; }
    public int getY() { return position.y; }

    public void setPosition(Point position) {
        this.position = position;
    }

    public int getId() { return id; }
    public int getEnergy() { return energy; }

    public boolean isAlive() { return energy > 1; }

    public void report() {
        System.out.println(String.format("%dth rabbit, located at: (%d, %d), remaining energy: %d",
                getId(), getX(), getX(), getEnergy()));
    }

    public void step() {
        // This would be much smoother, if I would know how to get the positions of the null-elements...
        // var neighborhood = space.getAgentSpace().getVonNeumannNeighbors(x, y, True);
        // neighborhood.removeIf(a -> a != null);
        // var el = neighborhood.get(rnd.nextInt(neighborhood.size()));
        // ...

        // 1. first get a list of possible positions
        var possiblePositions = new ArrayList<Point>();
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                // 1.1 check for legality of the move (the direction)
                // the `+ _gs` deals with the negative case. -1 + 0 % 20 == -1 (in Java), but we want op(-1 + 0) == 19
                // (assuming the grid size is 20). in order to achieve this we "exploit" the modulus operator
                // x + grid-size % grid-size =(if x is lower bounded by 0)= x
                // if you're unsure about this, use a pen and paper to draw the movement-grid
                final var _gs = space.getGridSize();
                if ((abs(i) + abs(j)) == 1) { // since we can only move
                    var _x = (i + this.getX() + _gs) % _gs;
                    var _y = (j + this.getY() + _gs) % _gs;
                    possiblePositions.add(new Point(_x, _y));
                }
            }
        }

        // 2. remove occupied positions
        possiblePositions.removeIf(dir -> space.isOccupied(dir.x, dir.y));

        // 3. select a random position out of the valid ones
        if (possiblePositions.size() > 0) {
            this.setPosition(possiblePositions.get(rnd.nextInt(possiblePositions.size())));
            this.energy -= Config.AGENT_MOVEMENT_COST;
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
