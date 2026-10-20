package br.com.petrickfaria.jetpack.client;

import br.com.petrickfaria.jetpack.menu.JetpackFuelMenu;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class JetpackFuelScreen extends AbstractContainerScreen<JetpackFuelMenu> {
    public JetpackFuelScreen(JetpackFuelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 73;
    }

    @Override
    protected void renderBg(PoseStack pose, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        fill(pose, x, y, x + imageWidth, y + imageHeight, 0xFFC6B998);
        fill(pose, x + 3, y + 3, x + imageWidth - 3, y + imageHeight - 3, 0xFF8A8067);
        fill(pose, x + 5, y + 5, x + imageWidth - 5, y + imageHeight - 5, 0xFFC6B998);

        // ER bar above the animated fire and fuel slot.
        int bx = x + 45;
        int by = y + 23;
        fill(pose, bx - 2, by - 2, bx + 88, by + 10, 0xFF33251F);
        fill(pose, bx, by, bx + 84, by + 6, 0xFF651010);
        int energy = Math.max(0, Math.min(6000, menu.energy()));
        int width = Math.round(82.0F * energy / 6000.0F);
        if (width > 0) fill(pose, bx + 1, by + 1, bx + 1 + width, by + 5, 0xFFE53935);

        int flameX = x + 83;
        int flameY = y + 42;
        if (menu.burnLeft() > 0) {
            int flicker = (int) (System.currentTimeMillis() / 140L % 3L);
            fill(pose, flameX + 4, flameY + 7 + flicker, flameX + 10, flameY + 15, 0xFFFF4A00);
            fill(pose, flameX + 5, flameY + 3, flameX + 9, flameY + 12 + flicker, 0xFFFFA000);
            fill(pose, flameX + 6, flameY + 7, flameX + 8, flameY + 13, 0xFFFFE36A);
        } else {
            fill(pose, flameX + 3, flameY + 11, flameX + 11, flameY + 13, 0xFF725B45);
        }
    }

    @Override
    protected void renderLabels(PoseStack pose, int mouseX, int mouseY) {
        this.font.draw(pose, "Energia Redstônica", 8, 7, 0xFF202020);
        this.font.draw(pose, Math.round(menu.energy() * 100.0F / 6000.0F) + "%", 136, 23, 0xFF202020);
        this.font.draw(pose, "Inventário", this.inventoryLabelX, this.inventoryLabelY, 0xFF202020);
    }
}
