import org.apache.commons.math.util.MathUtils;
import uchicago.src.sim.space.Object2DGrid;

import java.util.Random;

/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * the grassSpace objects are Integer
 *
 * @author
 */

public class RabbitsGrassSimulationSpace {
    public int getTotalGrassAmount() {
        var count = 0;
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                count += ((Grass)grassSpace.getObjectAt(i, j)).value;
            }
        }
        return count;
    }

    private class Grass extends Number {
        public int value = 0;
        public void grow(int amount) {
            value += amount;
        }

        public void grow() {
            this.grow(1);
        }

        public int harvest() {
            var t = value;
            value = 0;
            return t;
        }

        @Override
        public int intValue() { return value; }

        @Override
        public long longValue() { return value; }

        @Override
        public float floatValue() { return value; }

        @Override
        public double doubleValue() { return value; }
    }

    private int gridSize;
    private Object2DGrid grassSpace;
    private Object2DGrid rabbitSpace;
    private Random rnd;

    public RabbitsGrassSimulationSpace(int gridSize) {
        this.gridSize = gridSize;
        this.rabbitSpace = new Object2DGrid(gridSize, gridSize);
        this.rnd = new Random(42);

        this.grassSpace = new Object2DGrid(gridSize, gridSize);
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                this.grassSpace.putObjectAt(i, j, new Grass());
            }
        }
    }

    public Object2DGrid getGrassSpace() {
        return grassSpace;
    }
    public void setGrassSpace(Object2DGrid grassSpace) {
        this.grassSpace = grassSpace;
    }
    public void setRabbitSpace(Object2DGrid rabbitSpace) { this.rabbitSpace = rabbitSpace; }
    public Object2DGrid getAgentSpace() { return this.rabbitSpace; }

    public void spreadGrass(int grassAmount){
        for (int i = 0; i < grassAmount; i++) {
            var x = rnd.nextInt(gridSize);
            var y = rnd.nextInt(gridSize);
            ((Grass)grassSpace.getObjectAt(x, y)).grow();
        }
    }

    public boolean isOccupied(int x, int y){
        return rabbitSpace.getObjectAt(x, y) != null;
    }

    public void addRabbit(RabbitsGrassSimulationAgent rabbit){
        rabbitSpace.putObjectAt(rabbit.getX(), rabbit.getY(), rabbit);
    }

    public void removeRabbitFromSpace(int x, int y){
        rabbitSpace.putObjectAt(x,y,null);
    }

    public int harvestGrass(int x, int y) {
        return ((Grass)grassSpace.getObjectAt(x,y)).harvest();
    }

    public int getGrassAmountAt(int x, int y){
        return ((Grass)grassSpace.getObjectAt(x,y)).value;
    }

    public int getGridSize() {
        return gridSize;
    }

    public void setGridSize(int gridSize) {
        this.gridSize = gridSize;
    }
}
