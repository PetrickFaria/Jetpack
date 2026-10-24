package br.com.petrickfaria.jetpack.player;

import org.junit.jupiter.api.Test;
import net.minecraft.resources.ResourceLocation;
import br.com.petrickfaria.jetpack.item.JetpackItem;
import br.com.petrickfaria.jetpack.item.JetpackEnergyDrain;
import br.com.petrickfaria.jetpack.menu.JetpackFuelMenu;

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
    void coalBurnProducesExactlyFivePercentOfCapacity() {
        assertEquals(300.0D, JetpackFueling.energyForBurnTicks(1600), 0.000001D);
        assertEquals(300.0D, (double) JetpackItem.COAL_ENERGY, 0.000001D);
        assertEquals(5.0D, JetpackItem.COAL_ENERGY * 100.0D / JetpackItem.CAPACITY, 0.0D);
    }

    @Test
    void oneCoalAddsFivePercentToAnExistingFiftyPercentCharge() {
        double fraction = 0.0D;
        int charge = 3000;
        int totalBurnTicks = 1600;
        int burnLeft = totalBurnTicks;
        while (burnLeft > 0) {
            fraction = JetpackFueling.accumulatedCreditForGameTick(totalBurnTicks, burnLeft, fraction);
            int whole = (int) fraction;
            charge = Math.min(JetpackItem.CAPACITY, charge + whole);
            fraction -= whole;
            burnLeft -= Math.min(JetpackFueling.BURN_TICKS_PER_GAME_TICK, burnLeft);
        }
        assertEquals(3300, charge);
        assertEquals(55.0D, charge * 100.0D / JetpackItem.CAPACITY, 0.0D);
    }

    @Test
    void staleOversizedFuelNbtCannotMakeOneCoalFillTheBattery() {
        double fraction = JetpackFueling.accumulatedCreditForGameTick(1600, 1600, 6000.0D);
        assertEquals(30.0D, fraction, 0.000001D);
        assertTrue(fraction < JetpackItem.CAPACITY);
    }

    @Test
    void queuedCoalOnlyStartsWhenChargeHasFallenToNinetyFivePercent() {
        assertTrue(JetpackFueling.mayConsumeNextFuel(5700));
        assertTrue(JetpackFueling.mayConsumeNextFuel(5699));
        assertFalse(JetpackFueling.mayConsumeNextFuel(5701));
        assertFalse(JetpackFueling.mayConsumeNextFuel(JetpackItem.CAPACITY));
    }

    @Test
    void aStackOfSixtyFourConsumesTwentyToFillAndLeavesFortyFourForTheNextRefill() {
        int charge = 0;
        int fuelInSlot = 64;
        while (charge < JetpackItem.CAPACITY) {
            assertTrue(JetpackFueling.mayConsumeNextFuel(charge));
            fuelInSlot--;
            charge = Math.min(JetpackItem.CAPACITY, charge + JetpackItem.COAL_ENERGY);
        }
        assertEquals(6000, charge);
        assertEquals(44, fuelInSlot);
        assertFalse(JetpackFueling.mayConsumeNextFuel(charge));

        charge -= JetpackItem.COAL_ENERGY;
        assertTrue(JetpackFueling.mayConsumeNextFuel(charge));
        fuelInSlot--;
        charge = Math.min(JetpackItem.CAPACITY, charge + JetpackItem.COAL_ENERGY);
        assertEquals(6000, charge);
        assertEquals(43, fuelInSlot);
    }

    @Test
    void displayedSlotFramesAndFunctionalSlotsShareTheSameLayout() {
        assertEquals(80, JetpackFuelMenu.FUEL_SLOT_X);
        assertEquals(76, JetpackFuelMenu.FUEL_SLOT_Y);
        assertEquals(112, JetpackFuelMenu.INVENTORY_SLOT_START_Y);
        assertEquals(166, JetpackFuelMenu.HOTBAR_SLOT_Y);
    }

    @Test
    void otherFuelsScaleWithTheirBurnDuration() {
        assertEquals(150.0D, JetpackFueling.energyForBurnTicks(800), 0.000001D);
        assertEquals(18.75D, JetpackFueling.energyForBurnTicks(100), 0.000001D);
    }

    @Test
    void twentyCoalSupplyAFullJetpackInExactlyTwoHundredTicks() {
        double charged = 0.0D;
        int ticks = 0;
        for (int coal = 0; coal < 20; coal++) {
            int left = 1600;
            while (left > 0) {
                charged += JetpackFueling.energyForGameTick(300.0D, 1600, left);
                left -= Math.min(JetpackFueling.BURN_TICKS_PER_GAME_TICK, left);
                ticks++;
            }
        }
        assertEquals(200, ticks);
        assertEquals(6000.0D, charged, 0.000001D);
    }

    @Test
    void everyTierProvidesExactlyTenMinutesAtTwentyTicksPerSecond() {
        String[] tiers = {"wood", "iron", "gold", "emerald", "diamond", "netherite"};
        for (String tier : tiers) {
            int remaining = JetpackItem.CAPACITY;
            int remainder = 0;
            int activeTicks = 0;
            while (remaining > 0) {
                JetpackEnergyDrain.Step step = JetpackEnergyDrain.nextStep(remainder);
                remaining -= step.energyToConsume();
                remainder = step.remainder();
                activeTicks++;
            }
            assertEquals(12_000, activeTicks, tier);
            assertEquals(600, activeTicks / 20, tier);
        }
    }

    @Test
    void fractionalFlightDrainCarriesAcrossSeparatePoweredPeriods() {
        JetpackEnergyDrain.Step first = JetpackEnergyDrain.nextStep(0);
        JetpackEnergyDrain.Step second = JetpackEnergyDrain.nextStep(first.remainder());
        JetpackEnergyDrain.Step third = JetpackEnergyDrain.nextStep(second.remainder());
        assertEquals(0, first.energyToConsume());
        assertEquals(1, second.energyToConsume());
        assertEquals(0, second.remainder());
        assertEquals(0, third.energyToConsume());
    }

    @Test
    void previousFractionalRemaindersConvertExactlyToSixthFeUnits() {
        assertEquals(0, JetpackEnergyDrain.fromLegacyRemainder(0));
        assertEquals(2, JetpackEnergyDrain.fromLegacyRemainder(1));
        assertEquals(4, JetpackEnergyDrain.fromLegacyRemainder(2));
    }

    @Test
    void twentyFivePercentChargeTakesFiftyTicksWithFiveCoal() {
        double charged = 0.0D;
        int ticks = 0;
        for (int coal = 0; coal < 5; coal++) {
            int left = 1600;
            while (left > 0) {
                charged += JetpackFueling.energyForGameTick(300.0D, 1600, left);
                left -= Math.min(JetpackFueling.BURN_TICKS_PER_GAME_TICK, left);
                ticks++;
            }
        }
        assertEquals(50, ticks);
        assertEquals(1500.0D, charged, 0.000001D);
    }

    @Test
    void partialFuelOnlyDeliversItsProportionalEnergy() {
        double delivered = 0.0D;
        int left = 1600;
        while (left > 0) {
            delivered += JetpackFueling.energyForGameTick(300.0D, 1600, left);
            left -= Math.min(JetpackFueling.BURN_TICKS_PER_GAME_TICK, left);
        }
        assertEquals(300.0D, delivered, 0.000001D);
    }

    @Test
    void invalidBurnTimesCannotAddEnergy() {
        assertEquals(0.0D, JetpackFueling.energyForBurnTicks(0), 0.000001D);
        assertEquals(0.0D, JetpackFueling.energyForBurnTicks(-20), 0.000001D);
    }
}
