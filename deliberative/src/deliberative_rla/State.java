package deliberative_rla;

import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class State {

    private State prevState; //last state where the current state transfer from
    private City currentCity; // the city where the Vehicle locates now
    private double totalCost; // the total cost till this state
    private double carriedWeight; // the weight of all tasks, which could not exceed the capacity of the vehicle
    private TaskSet taskSet; // all tasks carried by the vehicle

}
