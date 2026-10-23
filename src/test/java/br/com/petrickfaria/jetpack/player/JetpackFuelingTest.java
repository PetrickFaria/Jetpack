package br.com.petrickfaria.jetpack.player;

import org.junit.jupiter.api.Test;
import net.minecraft.resources.ResourceLocation;
import br.com.petrickfaria.jetpack.item.JetpackItem;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JetpackFuelingTest {
    @Test
    void onlyCoalAndCharcoalAreAccepted() {
        assertTrue(JetpackFueling.isAllowedFuelId(new ResourceLocation("minecraft", "coal")));
        assertTrue(JetpackFueling.isAllowedFuelId(new ResourceLocation("minecraft", "charcoal")));
        assertFalse(JetpackFueling.isAllowedFuelId(new ResourceLocation("minecraft", "coal_block")));
        assertFalse(JetpackFueling.isAllowedFuelId(new ResourceLocation("minecraft", "stick")));
        assertFalse(JetpackFueling.isAllowedFuelId(new ResourceLocation("minecraft", "oak_planks")));
        assertFalse(JetpackFueling.isAllowedFuelId(new ResourceLocation("minecraft", "oak_log")));
        assertFalse(JetpackFueling.isAllowedFuelId(new ResourceLocation("minecraft", "lava_bucket")));
        assertFalse(JetpackFueling.isAllowedFuelId(new ResourceLocation("minecraft", "dried_kelp_block")));
        assertFalse(JetpackFueling.isAllowedFuelId(new ResourceLocation("minecraft", "blaze_rod")));
        assertFalse(JetpackFueling.isAllowedFuelId(null));
    }

    @Test
    void coalBurnProducesExactlyFourPercentOfCapacity() {
        assertEquals(240.0D, JetpackFueling.energyForBurnTicks(1600), 0.000001D);
        assertEquals(240.0D, (double) JetpackItem.COAL_ENERGY, 0.000001D);
        assertEquals(4.0D, JetpackItem.COAL_ENERGY * 100.0D / JetpackItem.CAPACITY, 0.0D);
    }

    @Test
    void otherFuelsScaleWithTheirBurnDuration() {
        assertEquals(120.0D, JetpackFueling.energyForBurnTicks(800), 0.000001D);
        assertEquals(15.0D, JetpackFueling.energyForBurnTicks(100), 0.000001D);
    }

    @Test
    void twentyFiveCoalSupplyAFullJetpackInExactlyTwoHundredTicks() {
        double charged = 0.0D;
        int ticks = 0;
        for (int coal = 0; coal < 25; coal++) {
            int left = 1600;
            while (left > 0) {
                charged += JetpackFueling.energyForGameTick(240.0D, 1600, left);
                left -= Math.min(JetpackFueling.BURN_TICKS_PER_GAME_TICK, left);
                ticks++;
            }
        }
        assertEquals(200, ticks);
        assertEquals(6000.0D, charged, 0.000001D);
    }

    @Test
    void everyTierProvidesExactlyFifteenMinutesAtTwentyTicksPerSecond() {
        String[] tiers = {"wood", "iron", "gold", "emerald", "diamond", "netherite"};
        for (String tier : tiers) {
            int remaining = JetpackItem.CAPACITY;
            int remainder = 0;
            int activeTicks = 0;
            while (remaining > 0) {
                JetpackItem.FlightDrainStep step = JetpackItem.nextFlightDrainStep(remainder);
                remaining -= step.energyToConsume();
                remainder = step.remainder();
                activeTicks++;
            }
            assertEquals(18_000, activeTicks, tier);
            assertEquals(900, activeTicks / 20, tier);
        }
    }

    @Test
    void fractionalFlightDrainCarriesAcrossSeparatePoweredPeriods() {
        JetpackItem.FlightDrainStep first = JetpackItem.nextFlightDrainStep(0);
        JetpackItem.FlightDrainStep second = JetpackItem.nextFlightDrainStep(first.remainder());
        JetpackItem.FlightDrainStep third = JetpackItem.nextFlightDrainStep(second.remainder());
        assertEquals(0, first.energyToConsume());
        assertEquals(0, second.energyToConsume());
        assertEquals(1, third.energyToConsume());
        assertEquals(0, third.remainder());
    }

    @Test
    void twentyPercentChargeTakesFortyTicksWithFiveCoal() {
        double charged = 0.0D;
        int ticks = 0;
        for (int coal = 0; coal < 5; coal++) {
            int left = 1600;
            while (left > 0) {
                charged += JetpackFueling.energyForGameTick(240.0D, 1600, left);
                left -= Math.min(JetpackFueling.BURN_TICKS_PER_GAME_TICK, left);
                ticks++;
            }
        }
        assertEquals(40, ticks);
        assertEquals(1200.0D, charged, 0.000001D);
    }

    @Test
    void partialFuelOnlyDeliversItsProportionalEnergy() {
        double delivered = 0.0D;
        int left = 1600;
        while (left > 0) {
            delivered += JetpackFueling.energyForGameTick(240.0D, 1600, left);
            left -= Math.min(JetpackFueling.BURN_TICKS_PER_GAME_TICK, left);
        }
        assertEquals(240.0D, delivered, 0.000001D);
    }

    @Test
    void invalidBurnTimesCannotAddEnergy() {
        assertEquals(0.0D, JetpackFueling.energyForBurnTicks(0), 0.000001D);
        assertEquals(0.0D, JetpackFueling.energyForBurnTicks(-20), 0.000001D);
    }
}

