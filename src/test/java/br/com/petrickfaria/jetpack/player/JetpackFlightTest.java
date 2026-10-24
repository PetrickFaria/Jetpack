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
    void creativeAscentSpeedMatchesTheSteadyDisplacementProducedByVanillaJumpInput() {
        double flyingSpeed = (double) 0.05F;
        double verticalVelocity = 0.0D;
        double ascentY = 0.0D;
        double previousAscentY = 0.0D;
        double observedAscentDisplacement = 0.0D;
        for (int tick = 0; tick < 100; tick++) {
            // LocalPlayer adds 3 * flyingSpeed while Space is held. Player.travel
            // moves using that velocity, then retains 60% for the next tick.
            double movementThisTick = verticalVelocity + flyingSpeed * 3.0D;
            ascentY += movementThisTick;
            observedAscentDisplacement = ascentY - previousAscentY;
            previousAscentY = ascentY;
            verticalVelocity = movementThisTick * 0.6D;
        }
        assertEquals(observedAscentDisplacement,
                JetpackFlight.creativeSteadyAscentSpeed(flyingSpeed), 1.0E-12D);
    }

    @Test
    void automaticDescentMatchesObservedCreativeAscentDisplacementForEveryTier() {
        double flyingSpeed = (double) 0.05F;
        double verticalVelocity = 0.0D;
        double observedAscentDisplacement = 0.0D;
        for (int tick = 0; tick < 100; tick++) {
            observedAscentDisplacement = verticalVelocity + flyingSpeed * 3.0D;
            verticalVelocity = observedAscentDisplacement * 0.6D;
        }

        for (int maximumHeight : new int[] {5, 10, 15, 20, 25, 50}) {
            double ceiling = JetpackFlight.altitudeCeiling(64.0D, maximumHeight);
            double y = ceiling + 2.0D;
            Vec3 adjusted = JetpackFlight.constrainFlightMotion(
                    new Vec3(0.1D, 0.0D, -0.1D), false, false, y, ceiling,
                    observedAscentDisplacement);
            // Simulate the next entity move and compare actual Y deltas, rather than
            // comparing a speed constant or only the velocity passed to the helper.
            double nextY = y + adjusted.y;
            double descentDisplacement = nextY - y;
            assertEquals(-observedAscentDisplacement, descentDisplacement, 1.0E-12D,
                    "tier " + maximumHeight);
            assertEquals(0.1D, adjusted.x, 0.0D);
            assertEquals(-0.1D, adjusted.z, 0.0D);
        }
    }

    @Test
    void terrainCorrectionEasesIntoCreativeSpeedAndStopsAtTheCeilingForEveryTier() {
        double creativeSpeed = JetpackFlight.creativeSteadyAscentSpeed((double) 0.05F);
        for (int maximumHeight : new int[] {5, 10, 15, 20, 25, 50}) {
            double ceiling = JetpackFlight.altitudeCeiling(64.0D, maximumHeight);
            double y = ceiling + 3.0D;
            double velocity = 0.0D;
            int ticks = 0;
            boolean reachedCruiseSpeed = false;
            while (y > ceiling && ticks < 100) {
                double remaining = y - ceiling;
                velocity = JetpackFlight.smoothTerrainCorrectionVelocity(
                        velocity, creativeSpeed, remaining, 0.06D);
                assertTrue(velocity <= 0.0D, "correction never rises, tier " + maximumHeight);
                if (remaining > creativeSpeed && Math.abs(velocity + creativeSpeed) < 1.0E-12D)
                    reachedCruiseSpeed = true;
                double step = Math.max(velocity, ceiling - y);
                y += step;
                ticks++;
            }
            assertTrue(ticks >= 6, "entry is eased over several ticks, tier " + maximumHeight);
            assertTrue(reachedCruiseSpeed, "correction reaches the original Creative target speed, tier " + maximumHeight);
            assertEquals(ceiling, y, 1.0E-12D, "no height overshoot, tier " + maximumHeight);
            assertEquals(0.0D, JetpackFlight.smoothTerrainCorrectionVelocity(
                    velocity, creativeSpeed, 0.0D, 0.06D), 0.0D);
        }
    }

    @Test
    void correctionUsesMeasuredAscentRatherThanTheConfiguredFlySpeed() {
        Vec3 adjusted = JetpackFlight.constrainFlightMotion(
                new Vec3(0.0D, 0.0D, 0.0D), false, false, 75.0D, 70.0D, 0.36D);
        assertEquals(-0.36D, adjusted.y, 0.0D);
    }

    @Test
    void finalTerrainCorrectionStepDoesNotOvershootTheCeiling() {
        Vec3 adjusted = JetpackFlight.constrainFlightMotion(
                new Vec3(0.1D, 0.0D, -0.1D), false, false, 70.02D, 70.0D, 0.375D);
        assertEquals(-0.02D, adjusted.y, 1.0E-9D);
    }

    @Test
    void lowerTerrainCorrectionReplacesAnExistingFallWithMeasuredCreativeSpeed() {
        Vec3 adjusted = JetpackFlight.constrainFlightMotion(
                new Vec3(0.1D, -0.8D, -0.1D), false, false, 75.0D, 70.0D, 0.375D);
        assertEquals(-0.375D, adjusted.y, 1.0E-9D);
    }

    @Test
    void unpoweredControlledDescentSpeedRemainsUnchanged() {
        Vec3 adjusted = JetpackFlight.controlledDescentMotion(new Vec3(0.1D, 0.4D, -0.1D), true, false);
        assertEquals(0.1D, adjusted.x, 0.0D);
        assertEquals(-0.025D, adjusted.y, 0.0D);
        assertEquals(-0.1D, adjusted.z, 0.0D);
    }

    @Test
    void thrustSoundFollowsPoweredFlightAndOnlyPoweredAirborneShiftDescent() {
        assertEquals(true, JetpackFlight.shouldPlayThrustSound(true, true, false, false));
        assertEquals(true, JetpackFlight.shouldPlayThrustSound(true, true, false, true));
        assertEquals(false, JetpackFlight.shouldPlayThrustSound(true, false, false, false));
        assertEquals(true, JetpackFlight.shouldPlayThrustSound(false, true, true, false));
        assertEquals(false, JetpackFlight.shouldPlayThrustSound(false, true, true, true));
        assertEquals(false, JetpackFlight.shouldPlayThrustSound(false, false, true, false));
        assertEquals(false, JetpackFlight.shouldPlayThrustSound(false, true, false, false));
    }

    @Test
    void controlledDescentHorizontalDeltaMatchesCreativeFlightForCardinalAndDiagonalInput() {
        double drag = (double) 0.91F;
        double airborneInput = (double) (0.02F * 0.3F);
        double creativeInput = (double) 0.05F;
        double[][] inputs = {{0, 1}, {0, -1}, {-1, 0}, {1, 0}, {1, 1}};
        for (double[] direction : inputs) {
            Vec3 input = JetpackFlight.normalizeMovementInput((float) direction[0], (float) direction[1]);
            Vec3 startingVelocity = new Vec3(0.11D, -0.17D, -0.07D);
            Vec3 afterOrdinaryAirTravel = new Vec3(
                    (startingVelocity.x + input.x * airborneInput) * drag,
                    startingVelocity.y,
                    (startingVelocity.z + input.z * airborneInput) * drag);
            Vec3 controlled = JetpackFlight.controlledDescentMotion(afterOrdinaryAirTravel, true, false,
                    (float) direction[0], (float) direction[1], 0.0F);
            double expectedX = (startingVelocity.x + input.x * creativeInput) * drag;
            double expectedZ = (startingVelocity.z + input.z * creativeInput) * drag;
            assertEquals(expectedX, controlled.x, 1.0E-12D);
            assertEquals(-0.025D, controlled.y, 0.0D);
            assertEquals(expectedZ, controlled.z, 1.0E-12D);
        }
    }

    @Test
    void controlledDescentUsesFacingDirectionAndStopsAddingThrustWhenKeysAreReleased() {
        Vec3 forwardInput = JetpackFlight.normalizeMovementInput(0.0F, 1.0F).yRot((float) Math.toRadians(-90.0D));
        Vec3 afterAirTravel = new Vec3(0.0D, -0.2D, 0.0D);
        Vec3 controlled = JetpackFlight.controlledDescentMotion(afterAirTravel, true, false,
                0.0F, 1.0F, 90.0F);
        assertEquals(forwardInput.x * ((double) 0.05F - (double) (0.02F * 0.3F))
                * (double) 0.91F, controlled.x, 1.0E-12D);
        assertEquals(forwardInput.z * ((double) 0.05F - (double) (0.02F * 0.3F))
                * (double) 0.91F, controlled.z, 1.0E-12D);
        assertEquals(afterAirTravel.x, JetpackFlight.controlledDescentMotion(
                afterAirTravel, true, false, 0.0F, 0.0F, 90.0F).x, 0.0D);
        assertEquals(afterAirTravel.z, JetpackFlight.controlledDescentMotion(
                afterAirTravel, true, false, 0.0F, 0.0F, 90.0F).z, 0.0D);
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
