package centralized_rla;

import java.util.ArrayList;
import java.util.List;

public class Utils {
    public static List<SolutionSpace> minimalElements(List<SolutionSpace> sols) {
        double currentMinCost = Double.MAX_VALUE;
        List<SolutionSpace> minElements = new ArrayList<>();
        
        for (final var sol : sols) {
            if (sol.cost() < currentMinCost) {
                minElements = new ArrayList<>(List.of(sol));
                currentMinCost = sol.cost();
            } else if (sol.cost() == currentMinCost) {
                minElements.add(sol);
            }
        }
        
        return minElements;
    }
}
