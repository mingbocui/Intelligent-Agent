package deliberative_rla;

import jdk.jshell.spi.ExecutionControl;
import logist.plan.Plan;
import logist.task.TaskSet;
import logist.topology.Topology.City;

public interface IAlgorithm {
    /**
     * The `carryingTasks` and `newTasks` are split, probably not necessary as we can just "change" the origin of these
     * tasks to be the current city.
     *
     * @param startingCity  Starting city to branch to look for a new solution.
     * @param carryingTasks The tasks that the agent is currently holding, initially an empty set.
     * @param newTasks      The new tasks that should be added to the plan. Can be empty later (if no new task is available but we have to change our path) (if no new task is available but we have to change our path).
     * @return
     */
    Plan optimalPlan(City startingCity, TaskSet carryingTasks, TaskSet newTasks);
}
