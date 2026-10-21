package br.com.petrickfaria.jetpack.player;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JetpackFlightTest {
    @Test
    void shiftDescentIsSlowAndPreservesHorizontalControl() {
        Vec3 adjusted = JetpackFlight.constrainFlightMotion(new Vec3(0.12D, 0.0D, -0.08D), true, false, 30.0D, 50.0D);
        assertEquals(0.12D, adjusted.x, 1.0E-9D);
        assertEquals(-0.025D, adjusted.y, 1.0E-9D);
        assertEquals(-0.08D, adjusted.z, 1.0E-9D);
    }

    @Test
    void releasingShiftStopsCommandedDescent() {
        Vec3 motion = new Vec3(0.0D, 0.0D, 0.0D);
        assertEquals(motion, JetpackFlight.constrainFlightMotion(motion, false, false, 30.0D, 50.0D));
    }

    @Test
    void shiftDoesNotOverrideNormalGroundMovement() {
        Vec3 motion = new Vec3(0.03D, -0.08D, 0.02D);
        assertEquals(motion, JetpackFlight.constrainFlightMotion(motion, true, true, 10.0D, 50.0D));
    }

    @Test
    void ceilingStopsOnlyUpwardMotion() {
        Vec3 rising = new Vec3(0.1D, 0.2D, -0.1D);
        Vec3 capped = JetpackFlight.constrainFlightMotion(rising, false, false, 55.0D, 55.0D);
        assertEquals(0.0D, capped.y, 0.0D);
        assertEquals(rising.x, capped.x, 0.0D);
        assertEquals(rising.z, capped.z, 0.0D);
        Vec3 falling = new Vec3(0.1D, -0.1D, -0.1D);
        assertEquals(falling, JetpackFlight.constrainFlightMotion(falling, false, false, 55.0D, 55.0D));
    }
}
