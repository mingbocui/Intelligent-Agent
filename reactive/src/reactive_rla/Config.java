package reactive_rla;

final public class Config {
    public static final int INIT_VALUE = -Integer.MIN_VALUE; // init for what?
    public static final double VALUE_ITERATION_THRESHOLD = 1E-18; // might be a bit too low
    public static final boolean TESTING = true;
    public static final int DEBUG_LEVEL = 20; // 10 is debug, debug, 20 is info
    public static final double ACTION_ACCEPTANCE_PERCENTAGE = 0.8;
}
