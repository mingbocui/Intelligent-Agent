import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;

import java.awt.Color;
import java.util.Random;


/**
 * Class that implements the simulation agent for the rabbits grass simulation.
 *
 * @author
 */

public class RabbitsGrassSimulationAgent implements Drawable {

    // position
    private int x;
    private int y;
    // random move
    private int vX;
    private int vY;
    //
    private int energy = 10; //we initialize the energy with 10

    private int idOfRabbit;
    private int numOfRabbit = 0;

    private RabbitsGrassSimulationSpace space;
    private Random rnd;

    // smart ideas from tutorial
    private void setVxVy() {
        vX = 0;
        vY = 0;
        // rabbit could saty and does not move
        vX = (int) Math.floor(rnd.nextDouble() * 3) - 1;
        vY = (int) Math.floor(rnd.nextDouble() * 3) - 1;

    }

    public void setXY(int newX, int newY) {
        x = newX;
        y = newY;
    }

    public void setSpace(RabbitsGrassSimulationSpace space) {
        this.space = space;
    }

    public void draw(SimGraphics G) {
        G.drawFastRoundRect(Color.green); // not sure
    }

    public RabbitsGrassSimulationAgent(int initEnergy) {
        x = 0;
        y = 0;
        energy = initEnergy;
        numOfRabbit++;
        idOfRabbit = numOfRabbit;
        this.rnd = new Random(44);
    }

    public int getX() { return x; }
    public int getY() { return y; }

    public int getIdOfRabbit() {
        return idOfRabbit;
    }
    public int getEnergy() {
        return energy;
    }
    public boolean isAlive() { return energy > 1; }

    public void report() {
        System.out.println(String.format("%dth rabbit, located at: (%d, %d), remaining energy: %d", idOfRabbit, x, y, energy));
    }

    public void step() { // to update status
        // TODO check if the field is occupied, if that is the case try another direction
        // TODO pick up grass if present
        // TODO remove penalty for moving etc...
        setVxVy();
        int newX = x + vX;
        int newY = y + vY;

        Object2DGrid grid = space.getCurrentSpace();
    }

    private boolean tryMove(int newX, int newY) {
        return space.moveRabbit(x, y, newX, newY);
    }
}
