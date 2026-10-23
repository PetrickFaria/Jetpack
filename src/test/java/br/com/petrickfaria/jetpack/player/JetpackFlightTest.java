package br.com.petrickfaria.jetpack.player;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JetpackFlightTest {
    @Test
    void ceilingTracksTheCurrentTerrainSurfaceForEveryTier() {
        assertEquals(69.0D, JetpackFlight.altitudeCeiling(64.0D, 5), 0.0D);
        assertEquals(74.0D, JetpackFlight.altitudeCeiling(64.0D, 10), 0.0D);
        assertEquals(79.0D, JetpackFlight.altitudeCeiling(64.0D, 15), 0.0D);
        assertEquals(84.0D, JetpackFlight.altitudeCeiling(64.0D, 20), 0.0D);
        assertEquals(89.0D, JetpackFlight.altitudeCeiling(64.0D, 25), 0.0D);
        assertEquals(114.0D, JetpackFlight.altitudeCeiling(64.0D, 50), 0.0D);
        assertEquals(90.5D, JetpackFlight.altitudeCeiling(80.5D, 10), 0.0D);
    }

    @Test
    void poweredShiftDescentUsesVanillaMotionWithoutChangingHorizontalControl() {
        Vec3 adjusted = JetpackFlight.constrainFlightMotion(new Vec3(0.12D, 0.0D, -0.08D), true, false, 30.0D, 50.0D);
        assertEquals(new Vec3(0.12D, 0.0D, -0.08D), adjusted);
    }

    @Test
    void poweredShiftDescentDoesNotOverrideNativeVerticalSpeed() {
        Vec3 adjusted = JetpackFlight.constrainFlightMotion(new Vec3(0.0D, -0.3D, 0.0D), true, false, 30.0D, 50.0D);
        assertEquals(-0.3D, adjusted.y, 1.0E-9D);
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
    void lowerTerrainAppliesAGentleDescentWithoutTeleportingMotion() {
        Vec3 adjusted = JetpackFlight.constrainFlightMotion(new Vec3(0.1D, 0.25D, -0.1D), false, false, 75.0D, 70.0D);
        assertEquals(0.1D, adjusted.x, 0.0D);
        assertEquals(-0.025D, adjusted.y, 0.0D);
        assertEquals(-0.1D, adjusted.z, 0.0D);
    }

    @Test
    void lowerTerrainCapsAnExistingFastFallToASafeRate() {
        Vec3 adjusted = JetpackFlight.constrainFlightMotion(new Vec3(0.1D, -0.8D, -0.1D), false, false, 75.0D, 70.0D);
        assertEquals(-0.08D, adjusted.y, 0.0D);
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

    @Test
    void upwardInputIsSuppressedAtTheBoundaryWithoutChangingOtherInputs() {
        assertFalse(JetpackFlight.shouldSuppressAscent(false, 5.0D, 5.0D));
        assertFalse(JetpackFlight.shouldSuppressAscent(true, 4.79D, 5.0D));
        assertTrue(JetpackFlight.shouldSuppressAscent(true, 4.8D, 5.0D));
        assertTrue(JetpackFlight.shouldSuppressAscent(true, 5.0D, 5.0D));
        assertTrue(JetpackFlight.shouldSuppressAscent(true, 20.0D, 5.0D));
    }

    @Test
    void oneStepOvershootIsCorrectedToTheCeiling() {
        assertEquals(5.0D, JetpackFlight.correctedCeilingY(5.15D, 5.0D), 0.0D);
        assertEquals(5.0D, JetpackFlight.authoritativeCeilingY(5.15D, 5.0D, false), 0.0D);
    }

    @Test
    void largeUnauthorizedOvershootIsCorrectedButTerrainLoweringRemainsGradual() {
        assertEquals(5.0D, JetpackFlight.authoritativeCeilingY(6.0D, 5.0D, false), 0.0D);
        assertEquals(6.0D, JetpackFlight.authoritativeCeilingY(6.0D, 5.0D, true), 0.0D);
    }

    @Test
    void jetpackStopsOnlyAfterShiftDescentHasActuallyLanded() {
        assertFalse(JetpackFlight.shouldStopOnLanding(true, false));
        assertFalse(JetpackFlight.shouldStopOnLanding(false, true));
        assertTrue(JetpackFlight.shouldStopOnLanding(true, true));
    }
}
