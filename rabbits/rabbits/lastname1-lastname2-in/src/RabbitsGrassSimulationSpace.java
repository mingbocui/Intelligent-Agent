import uchicago.src.sim.space.Object2DGrid;

/**
 * Class that implements the simulation space of the rabbits grass simulation.
 *
 * @author
 */

public class RabbitsGrassSimulationSpace {
    private int grassEnergy;
    private Object2DGrid grassSpace;
    private Object2DGrid rabbitSpace;

    public Object2DGrid getGrassSpace() {
        return grassSpace;
    }

    public void setGrassSpace(Object2DGrid grassSpace) {
        this.grassSpace = grassSpace;
    }

    public Object2DGrid getRabbitSpace() {
        return rabbitSpace;
    }

    public void setRabbitSpace(Object2DGrid rabbitSpace) {
        this.rabbitSpace = rabbitSpace;
    }

    public void RabbitsGrassSimulationSpace(int fieldWidth, int fieldHeight, int grassEnergy) {
        grassSpace = new Object2DGrid(fieldWidth, fieldHeight);
        rabbitSpace = new Object2DGrid(fieldWidth, fieldHeight);
        this.grassEnergy = grassEnergy;
        for (int i = 0; i < fieldWidth; i++) {
            for (int j = 0; j < fieldHeight; j++) {
                grassSpace.putObjectAt(i, j, new Integer(0));
            }
        }
    }

    public boolean moveAgentAt(int x, int y, int newX, int newY) {
        // TODO fill this
        return true;
    }

    public Object2DGrid getCurrentSpace() {
        return this.rabbitSpace;
    }
}
