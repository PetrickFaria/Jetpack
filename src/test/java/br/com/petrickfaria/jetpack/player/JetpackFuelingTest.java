package br.com.petrickfaria.jetpack.player;

import org.junit.jupiter.api.Test;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JetpackFuelingTest {
    @Test
    void onlyCoalAndCharcoalAreAccepted() {
        assertTrue(JetpackFueling.isFuel(new ItemStack(Items.COAL)));
        assertTrue(JetpackFueling.isFuel(new ItemStack(Items.CHARCOAL)));
        assertFalse(JetpackFueling.isFuel(new ItemStack(Items.COAL_BLOCK)));
        assertFalse(JetpackFueling.isFuel(new ItemStack(Items.STICK)));
        assertFalse(JetpackFueling.isFuel(new ItemStack(Items.OAK_PLANKS)));
        assertFalse(JetpackFueling.isFuel(new ItemStack(Items.OAK_LOG)));
        assertFalse(JetpackFueling.isFuel(new ItemStack(Items.LAVA_BUCKET)));
        assertFalse(JetpackFueling.isFuel(new ItemStack(Items.DRIED_KELP_BLOCK)));
        assertFalse(JetpackFueling.isFuel(new ItemStack(Items.BLAZE_ROD)));
        assertFalse(JetpackFueling.isFuel(ItemStack.EMPTY));
    }

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
    void coalSuppliesAFullJetpackInExactlyTwoHundredTicks() {
        double charged = 0.0D;
        int ticks = 0;
        for (int coal = 0; coal < 10; coal++) {
            int left = 1600;
            while (left > 0) {
                charged += JetpackFueling.energyForGameTick(600.0D, 1600, left);
                left -= Math.min(80, left);
                ticks++;
            }
        }
        assertEquals(200, ticks);
        assertEquals(6000.0D, charged, 0.000001D);
    }

    @Test
    void halfChargeTakesOneHundredTicksWithFiveCoal() {
        double charged = 0.0D;
        int ticks = 0;
        for (int coal = 0; coal < 5; coal++) {
            int left = 1600;
            while (left > 0) {
                charged += JetpackFueling.energyForGameTick(600.0D, 1600, left);
                left -= Math.min(80, left);
                ticks++;
            }
        }
        assertEquals(100, ticks);
        assertEquals(3000.0D, charged, 0.000001D);
    }

    @Test
    void partialFuelOnlyDeliversItsProportionalEnergy() {
        double delivered = 0.0D;
        int left = 800;
        while (left > 0) {
            delivered += JetpackFueling.energyForGameTick(300.0D, 800, left);
            left -= Math.min(80, left);
        }
        assertEquals(300.0D, delivered, 0.000001D);
    }

    @Test
    void invalidBurnTimesCannotAddEnergy() {
        assertEquals(0.0D, JetpackFueling.energyForBurnTicks(0), 0.000001D);
        assertEquals(0.0D, JetpackFueling.energyForBurnTicks(-20), 0.000001D);
    }
}
