package br.com.petrickfaria.jetpack.player;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class JetpackWaterSurfaceTest {
    @Test void waterSurfaceUsesActualFluidLevelForEveryTier() {
        for (int limit : new int[] {5,10,15,20,25,50}) {
            for (double level : new double[] {1.0D, 8.0D/9.0D, 4.0D/9.0D, 1.0D/9.0D}) {
                double surface = JetpackFlight.waterSurfaceBelowFeet(63, level, 80);
                assertEquals(63 + level, surface, 1.0E-9D);
                assertEquals(63 + level + limit, JetpackFlight.altitudeCeiling(surface, limit), 1.0E-9D);
                assertTrue(surface > 34); // Deep ocean floor is not the reference.
            }
        }
    }
    @Test void emptyFluidAndSurfaceAboveFeetAreRejected() {
        assertFalse(Double.isFinite(JetpackFlight.waterSurfaceBelowFeet(63, 0, 80)));
        assertFalse(Double.isFinite(JetpackFlight.waterSurfaceBelowFeet(63, 1, 63.5D)));
    }
}
