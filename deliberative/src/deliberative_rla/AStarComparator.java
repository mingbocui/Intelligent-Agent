package deliberative_rla;

import java.util.Comparator;

public class AStarComparator implements Comparator<AStarState> {
    // TODO write tests for this, create scenarios
    @Override
    public int compare(AStarState s1, AStarState s2) {
        return Double.compare(s1.fScore(), s2.fScore());
    }
}
