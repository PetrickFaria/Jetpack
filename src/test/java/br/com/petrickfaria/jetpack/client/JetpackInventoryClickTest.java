package br.com.petrickfaria.jetpack.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JetpackInventoryClickTest {
    @Test
    void mapsVanillaMainInventoryAndHotbarSlots() {
        int guiWidth = 320;
        int guiHeight = 240;
        int left = (guiWidth - 176) / 2;
        int top = (guiHeight - 166) / 2;

        assertEquals(9, JetpackInventorySlots.menuSlotAt(guiWidth, guiHeight, left + 9, top + 85));
        assertEquals(35, JetpackInventorySlots.menuSlotAt(guiWidth, guiHeight, left + 8 + 8 * 18 + 1, top + 84 + 2 * 18 + 1));
        assertEquals(36, JetpackInventorySlots.menuSlotAt(guiWidth, guiHeight, left + 9, top + 143));
        assertEquals(44, JetpackInventorySlots.menuSlotAt(guiWidth, guiHeight, left + 8 + 8 * 18 + 1, top + 143));
    }

    @Test
    void ignoresCraftingArmorAndSpaceOutsidePlayerInventoryGrid() {
        int guiWidth = 320;
        int guiHeight = 240;
        int left = (guiWidth - 176) / 2;
        int top = (guiHeight - 166) / 2;

        assertEquals(-1, JetpackInventorySlots.menuSlotAt(guiWidth, guiHeight, left + 8, top + 8));
        assertEquals(-1, JetpackInventorySlots.menuSlotAt(guiWidth, guiHeight, left + 8, top + 60));
        assertEquals(-1, JetpackInventorySlots.menuSlotAt(guiWidth, guiHeight, left + 172, top + 90));
    }
}
