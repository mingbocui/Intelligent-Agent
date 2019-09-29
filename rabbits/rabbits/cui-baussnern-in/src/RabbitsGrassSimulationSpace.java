import uchicago.src.sim.space.Object2DGrid;

import java.util.Random;

/**
 * Class that implements the simulation space of the rabbits grass simulation.
 *
 * @author
 */

public class RabbitsGrassSimulationSpace {
    private int grassEnergy;
    private Object2DGrid grassSpace;
    private Object2DGrid rabbitSpace;

    private Random rnd;

    public RabbitsGrassSimulationSpace(int gridSize, int grassEnergy) {
        this.grassSpace = new Object2DGrid(gridSize, gridSize);
        this.rabbitSpace = new Object2DGrid(gridSize, gridSize);
        this.grassEnergy = grassEnergy;
        this.rnd = new Random(42);
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                this.grassSpace.putObjectAt(i, j, new Integer(0));
            }
        }
    }

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


    // spread certain amount of grass
    public void spreadGrass(int grassAmount){
        if (grassAmount < 0) {
            throw new IllegalArgumentException("grassAmount must be non-negative");
        }
        while (grassAmount > 0) {

            int x = (int)(rnd.nextDouble()*(this.rabbitSpace.getSizeX()));
            int y = (int)(rnd.nextDouble()*(this.rabbitSpace.getSizeY()));
            int originalGrassAmount = getGrassAmountAt(x, y);
            grassSpace.putObjectAt(x, y, originalGrassAmount + 1);
            grassAmount--;
        }
    }

    public Object2DGrid getCurrentSpace() {
        return this.rabbitSpace;
    }

    public boolean isOccupied(int x, int y){
        return rabbitSpace.getObjectAt(x, y) != null;
    }

    // add rabbit to the rabbit space, but just need add one agent for every call of this function?
    public void addRabbit(RabbitsGrassSimulationAgent rabbit){
        boolean flag = false;
        while(!flag){
            // random position
            int x = (int)(rnd.nextDouble()*(rabbitSpace.getSizeX()));
            int y = (int)(rnd.nextDouble()*(rabbitSpace.getSizeY()));
            if(!isOccupied(x, y)){
                rabbitSpace.putObjectAt(x,y,rabbit);
                rabbit.setXY(x,y);
                // rabbit's living space
                rabbit.setSpace(this);
                flag = true;
            }
        }
    }

    public void removeRabbitFromSpace(int x, int y){
        rabbitSpace.putObjectAt(x,y,null);
    }

    public boolean moveRabbit(int xPre, int yPre, int xPost, int yPost){
        if(isOccupied(xPost, yPost)) return false;
        else{
            // TODO maybe check that post-vars are in bound
            RabbitsGrassSimulationAgent rabbit = (RabbitsGrassSimulationAgent) rabbitSpace.getObjectAt(xPre,yPre);
            removeRabbitFromSpace(xPre,yPre);
            rabbit.setXY(xPost,yPost);
            rabbitSpace.putObjectAt(xPost,yPost,rabbit);
            return true;
        }
    }

    // get the amount of grass at specific position
    public int getGrassAmountAt(int x, int y){
        if(grassSpace.getObjectAt(x,y) == null) return 0;
        else return ((Integer)grassSpace.getObjectAt(x,y)).intValue();
    }






}
