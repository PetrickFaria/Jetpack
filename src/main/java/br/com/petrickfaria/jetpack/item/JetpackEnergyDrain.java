package br.com.petrickfaria.jetpack.item;

/** Tick-level integer accounting for the Jetpack's fractional Forge Energy flight cost. */
public final class JetpackEnergyDrain {
    // Sixth-FE units represent both the previous 1/3-FE/tick balance and the new 1/2-FE/tick cost exactly.
    public static final int UNITS_PER_FE = 6;
    public static final int UNITS_PER_POWERED_TICK = 3;

    public static Step nextStep(int currentRemainder) {
        int next = Math.floorMod(currentRemainder, UNITS_PER_FE) + UNITS_PER_POWERED_TICK;
        return next >= UNITS_PER_FE
                ? new Step(1, next - UNITS_PER_FE)
                : new Step(0, next);
    }

    /** Converts the prior remainder format (one third of an FE per unit) to sixths. */
    public static int fromLegacyRemainder(int previousRemainder) {
        return Math.floorMod(previousRemainder, 3) * 2;
    }

    public record Step(int energyToConsume, int remainder) {}

    private JetpackEnergyDrain() {}
}
