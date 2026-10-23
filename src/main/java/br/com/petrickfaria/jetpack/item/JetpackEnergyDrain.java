package br.com.petrickfaria.jetpack.item;

/** Tick-level integer accounting for the Jetpack's fractional Forge Energy flight cost. */
public final class JetpackEnergyDrain {
    public static final int TICKS_PER_FE = 3;

    public static Step nextStep(int currentRemainder) {
        int normalized = Math.floorMod(currentRemainder, TICKS_PER_FE);
        int next = normalized + 1;
        return next == TICKS_PER_FE ? new Step(1, 0) : new Step(0, next);
    }

    public record Step(int energyToConsume, int remainder) {}

    private JetpackEnergyDrain() {}
}
