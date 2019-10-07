package reactive_rla;

final public class Config {
    public static final double VALUE_ITERATION_THRESHOLD = 1E-18; // might be a bit too low
    public static final boolean TESTING = true; // setting this to false will stop all logging
    public static final int VERBOSITY_LEVEL = 10; // 10 is info, 20 is debug
    public static final double ACTION_ACCEPTANCE_PERCENTAGE = 0.8;
}
