final class RngMath {
    private static final long MASK = 0xFFFF_FFFFL;
    private static final long MULTIPLIER = 1103515245L;
    private static final long INCREMENT = 24691L;
    private static final long MULTIPLIER_INVERSE = 0xEEB9EB65L;

    private RngMath() {}

    static long previousState(long desiredState) {
        return (MULTIPLIER_INVERSE * ((desiredState - INCREMENT) & MASK)) & MASK;
    }

    static long nextState(long state) {
        return (MULTIPLIER * state + INCREMENT) & MASK;
    }
}
