import uchicago.src.sim.space.Object2DGrid;

/**
 * Class that implements the simulation space of the rabbits grass simulation.
 *
 * @author
 */

public class RabbitsGrassSimulationSpace {
    private int grassEnergy;
    private final Object2DGrid grassSpace;
    private final Object2DFrid rabbitSpace;

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

    public void getGrassSpace() {
        return grassSpace;
    }

    public void getRabbitSpace() {
        return rabbitSpace;
    }

}
