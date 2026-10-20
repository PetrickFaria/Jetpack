package br.com.petrickfaria.jetpack.player;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JetpackFlightTest {
    @Test
    void normalCreativeAccelerationIsAppliedWithoutSprintBoost() {
        Vec3 velocity = JetpackFlight.creativeFlightVelocity(Vec3.ZERO, 0.0F, 1.0F, 0.0D, 0.0F);
        assertEquals(0.05D, velocity.z, 0.000001D);
        assertEquals(0.0D, velocity.x, 0.000001D);
    }

    @Test
    void diagonalInputIsNormalizedAndVelocityCannotAccumulatePastCreativeSpeed() {
        Vec3 velocity = Vec3.ZERO;
        for (int tick = 0; tick < 100; tick++)
            velocity = JetpackFlight.creativeFlightVelocity(velocity, 1.0F, 1.0F, 1.0D, 0.0F);
        assertTrue(velocity.length() <= 0.05D / 0.09D + 1.0E-9D);
    }

    @Test
    void yawRotatesForwardInputIntoWorldDirection() {
        Vec3 velocity = JetpackFlight.creativeFlightVelocity(Vec3.ZERO, 0.0F, 1.0F, 0.0D, 90.0F);
        assertEquals(-0.05D, velocity.x, 0.000001D);
        assertEquals(0.0D, velocity.z, 0.000001D);
    }
}
