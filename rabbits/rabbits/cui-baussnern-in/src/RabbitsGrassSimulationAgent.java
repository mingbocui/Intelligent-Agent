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
    private int energy;

    private int idOfRabbit;
    private int numOfRabbit = 0;

    private RabbitsGrassSimulationSpace space;
    private Random rnd;

    // smart ideas from tutorial
    private void setVxVy() {
        vX = 0;
        vY = 0;
        // rabbit could stay and does not move
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

    public void draw(SimGraphics G) { G.drawFastRoundRect(Color.green); } // colored rabbit agent with green cell;

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

    public int getIdOfRabbit() { return idOfRabbit; }

    public int getEnergy() { return energy; }

    public boolean isAlive() { return energy > 1; }

    public void report() {
        System.out.println(String.format("%dth rabbit, located at: (%d, %d), remaining energy: %d", idOfRabbit, x, y, energy));
    }

    public void step() { // to update status

        Object2DGrid grid = space.getCurrentSpace();

        setVxVy();
        if(this.vX != 0 || this.vY != 0){

            int newX = (x + vX) % grid.getSizeX();
            int newY = (y + vY) % grid.getSizeY();

            if(!tryMove(newX, newY)){
                System.out.println("This rabbit is blocked and could not make any move");
            }

            else{
                // every step move cost 1 energy
                this.energy -= 1;

                // update new position
                setXY(newX,newY);

                // eat the grass on the grid,
                eat(newX,newY);

                // remove rabbit if its energy is zero
                if(this.energy == 0){
                    space.removeRabbitFromSpace(newX, newY);
                }

                //TODO Sam, implement the reproduce function
            }

        }


    }

    // to check whether the next cell is occupied
    private boolean tryMove(int newX, int newY) {
        if(!space.moveRabbit(x, y, newX, newY)){
            // Done: check if the field is occupied, if that is the case try another direction
            for(int i = 0; i < 4; i++){
                if(reDirectIfOccupied(i)){
                    //TODO Sam, find a way to update the newX and newY
                    return true;
                }
            }
//            System.out.println("This rabbit is blocked and could not make any move");
            return false;
        }
        else
            return space.moveRabbit(x, y, newX, newY);
    }

    // helper function
    private boolean reDirectIfOccupied(int i){
        boolean flag = false;
        switch(i){
            case 0 : flag = tryMove(x,y+1); break;
            case 1 : flag = tryMove(x, y-1); break;
            case 2 : flag = tryMove(x-1,y); break;
            case 3 : flag = tryMove(x+1, y); break;
        }
        return flag;
    }

    public void eat(int x, int y){
        Object2DGrid grassSpace = space.getCurrentSpace();
        int amountOfGrass = (int) grassSpace.getObjectAt(x, y);
        grassSpace.putObjectAt(x, y, new Integer(0)); // consumed all grass of the grid
        this.energy += amountOfGrass; // rabbit will get energy from the grass;
    }

}
