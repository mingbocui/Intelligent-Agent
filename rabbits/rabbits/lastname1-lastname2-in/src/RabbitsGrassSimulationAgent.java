import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;


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

    // smart ideas from tutorial
    private void setVxVy() {
        vX = 0;
        vY = 0;
        while ((vX == 0) && (vY == 0)) {
            vX = (int) Math.floor(Math.random() * 3) - 1;
            vY = (int) Math.floor(Math.random() * 3) - 1;
        }
    }

    public void setXY(int newX, int newY) {
        x = newX;
        y = newY;
    }

    public void setSpace(RabbitsGrassSimulationSpace space) {
        this.space = space;
    }

    public void draw(SimGraphics G) {
        // TODO Auto-generated method stub
        G.drawFastRoundRect(Color.green); // not sure
    }

    public RabbitsGrassSimulationAgent(int iniEnery) {
        x = 0;
        y = 0;
        energy = iniEnery;
        numOfRabbit++;
        idOfRabbit = numOfRabbit;
    }

    public int getX() {
        // TODO Auto-generated method stub
        return X;
    }

    public int getY() {
        // TODO Auto-generated method stub
        return Y;
    }

    public int getIdOfRabbit() {
        return idOfRabbit;
    }

    public int getEnergy() {
        return energy;
    }

    public void report() {
        System.out.print(getIdOfRabbit() + "th rabbit" +
                "located at" + "(" + x"," + y"), still left energy of" + getEnergy());
    }

    public void step() { // to update status
        setVxVy();
        int newX = x + vX;
        int newY = y + vY;

        Object2DGrid grid = space.getCurrentSpace();

    }

    private boolean tryMove(int newX, int newY) {
        return cdSpace.moveAgentAt(x, y, newX, newY);
    }


}
