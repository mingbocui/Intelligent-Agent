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

    // spread certain amount of grass
    public void spreadGrass(int grassAmount){
        if(grassAmount<0){
            throw new IllegalArgumentException("grassAmount must be non-negative");
        }
        while(grassAmount>=0){
            // TODO use a real random engine
            int x = (int)(Math.random()*(rabbitSpace.getSizeX()));
            int y = (int)(Math.random()*(rabbitSpace.getSizeY()));
            int originalGrassAmount = getGrassAmountAt(x, y);
            grassSpace.putObjectAt(x, y, originalGrassAmount+1);
            grassAmount--;
        }

    }

//    public boolean moveAgentAt(int x, int y, int newX, int newY) {
//        // implemented as function moveRabbit below
//        return true;
//    }

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
            int x = (int)(Math.random()*(rabbitSpace.getSizeX()));
            int y = (int)(Math.random()*(rabbitSpace.getSizeY()));
            if(!isOccupied(x, y)){
                rabbitSpace.putObjectAt(x,y,rabbit);
                rabbit.setXY(x,y);
                // rabbit's living space
                rabbit.setSpace(this);
                flag = true;
            }
        }
    }

    public void removeRabbit(int x, int y){
        rabbitSpace.putObjectAt(x,y,null);
    }

    public boolean moveRabbit(int xPre, int yPre, int xPos, int yPos){
        if(isOccupied(xPos, yPos)) return false;
        else{
            RabbitsGrassSimulationAgent rabbit = (RabbitsGrassSimulationAgent) rabbitSpace.getObjectAt(xPre,yPre);
            removeRabbit(xPre,yPre);
            rabbit.setXY(xPos,yPos);
            rabbitSpace.putObjectAt(xPos,yPos,rabbit);
            return true;
        }
    }

    // get the amount of grass at specific position
    public int getGrassAmountAt(int x, int y){
        if(grassSpace.getObjectAt(x,y) == null) return 0;
        else return ((Integer)grassSpace.getObjectAt(x,y)).intValue();
    }






}
