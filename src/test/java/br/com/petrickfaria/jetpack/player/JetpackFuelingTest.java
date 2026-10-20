package br.com.petrickfaria.jetpack.player;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JetpackFuelingTest {
    @Test
    void coalBurnProducesExactlyTenPercentOfCapacity() {
        assertEquals(600.0D, JetpackFueling.energyForBurnTicks(1600), 0.000001D);
    }

    @Test
    void otherFuelsScaleWithTheirBurnDuration() {
        assertEquals(300.0D, JetpackFueling.energyForBurnTicks(800), 0.000001D);
        assertEquals(37.5D, JetpackFueling.energyForBurnTicks(100), 0.000001D);
    }

    @Test
    void invalidBurnTimesCannotAddEnergy() {
        assertEquals(0.0D, JetpackFueling.energyForBurnTicks(0), 0.000001D);
        assertEquals(0.0D, JetpackFueling.energyForBurnTicks(-20), 0.000001D);
    }
}
