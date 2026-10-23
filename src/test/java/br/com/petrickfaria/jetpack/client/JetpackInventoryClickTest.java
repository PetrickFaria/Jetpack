package br.com.petrickfaria.jetpack.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void equipmentSlotHitboxMatchesMarkedPositionAtDifferentGuiScales() {
        for (int[] size : new int[][]{{320, 240}, {480, 270}, {854, 480}}) {
            int x = JetpackInventorySlots.equipmentSlotLeft(size[0]);
            int y = JetpackInventorySlots.equipmentSlotTop(size[1]);
            assertTrue(JetpackInventorySlots.isEquipmentSlotAt(size[0], size[1], x + 9, y + 9));
            assertFalse(JetpackInventorySlots.isEquipmentSlotAt(size[0], size[1], x - 1, y + 9));
            assertFalse(JetpackInventorySlots.isEquipmentSlotAt(size[0], size[1], x + 18, y + 9));
            assertFalse(JetpackInventorySlots.isEquipmentSlotAt(size[0], size[1], x + 9, y + 18));
            int panelLeft = (size[0] - 176) / 2;
            int oldSlotLeft = panelLeft - 10;
            int panelTop = (size[1] - 166) / 2;
            assertFalse(JetpackInventorySlots.isEquipmentSlotAt(size[0], size[1], oldSlotLeft + 9, panelTop + 26 + 9));
        }
    }
}
