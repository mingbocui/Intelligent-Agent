package auction.centralized_rla;

import java.util.ArrayList;
import java.util.List;

public class Utils {
    public static List<SolutionSpace> minimalElements(List<SolutionSpace> sols) {
        double currentMinCost = Double.MAX_VALUE;
        List<SolutionSpace> minElements = new ArrayList<>();
        
        for (final var sol : sols) {
            if (sol.combinedCost() < currentMinCost) {
                minElements.clear();
                minElements.add(sol);
                currentMinCost = sol.combinedCost();
            } else if (sol.combinedCost() == currentMinCost) {
                minElements.add(sol);
            }
        }
        
        return minElements;
    }
}
