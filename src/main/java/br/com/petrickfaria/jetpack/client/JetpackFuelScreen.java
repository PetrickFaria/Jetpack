package br.com.petrickfaria.jetpack.client;

import br.com.petrickfaria.jetpack.item.JetpackItem;
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
        // Compact steel-and-redstone housing with a recessed work area.
        fill(pose, x, y, x + imageWidth, y + imageHeight, 0xFF15191D);
        fill(pose, x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF8A9297);
        fill(pose, x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFF292E33);
        fill(pose, x + 4, y + 4, x + imageWidth - 4, y + imageHeight - 4, 0xFF393D40);
        fill(pose, x + 5, y + 5, x + imageWidth - 5, y + 18, 0xFF24292E);
        fill(pose, x + 5, y + 18, x + imageWidth - 5, y + 19, 0xFF9B302B);
        fill(pose, x + 8, y + 21, x + imageWidth - 8, y + 67, 0xFF454747);
        fill(pose, x + 8, y + 21, x + imageWidth - 8, y + 22, 0xFF737A7D);
        fill(pose, x + 8, y + 67, x + imageWidth - 8, y + 68, 0xFF262A2D);
        fill(pose, x + 5, y + 71, x + imageWidth - 5, y + 72, 0xFF777F83);
        fill(pose, x + 5, y + 74, x + imageWidth - 5, y + imageHeight - 5, 0xFF303438);
        fill(pose, x + 8, y + 77, x + imageWidth - 8, y + imageHeight - 6, 0xFF484744);

        // Small corner fasteners and side rails keep the pixel-art framing crisp.
        for (int[] corner : new int[][] {{7, 7}, {imageWidth - 10, 7}, {7, imageHeight - 10}, {imageWidth - 10, imageHeight - 10}}) {
            fill(pose, x + corner[0], y + corner[1], x + corner[0] + 2, y + corner[1] + 2, 0xFF92999B);
            fill(pose, x + corner[0], y + corner[1] + 1, x + corner[0] + 2, y + corner[1] + 2, 0xFF50575B);
        }
        fill(pose, x + 25, y + 28, x + 27, y + 61, 0xFF353A3E);
        fill(pose, x + imageWidth - 27, y + 28, x + imageWidth - 25, y + 61, 0xFF353A3E);

        // Recessed ER meter with a raised metal lip and fine red tick marks.
        int bx = x + 46;
        int by = y + 27;
        fill(pose, bx - 3, by - 3, bx + 87, by + 9, 0xFF1A1D20);
        fill(pose, bx - 2, by - 2, bx + 86, by + 8, 0xFF9AA1A3);
        fill(pose, bx - 1, by - 1, bx + 85, by + 7, 0xFF25292D);
        fill(pose, bx, by, bx + 84, by + 6, 0xFF5A1516);
        int energy = Math.max(0, Math.min(JetpackItem.CAPACITY, menu.energy()));
        int width = Math.round(82.0F * energy / JetpackItem.CAPACITY);
        if (width > 0) {
            fill(pose, bx + 1, by + 1, bx + 1 + width, by + 5, 0xFFCF2929);
            fill(pose, bx + 1, by + 1, bx + 1 + width, by + 2, 0xFFFF7A70);
            fill(pose, bx + 1, by + 4, bx + 1 + width, by + 5, 0xFF9E1C21);
        }
        for (int tick = 1; tick < 10; tick++) {
            int tickX = bx + 1 + tick * 8;
            if (tickX < bx + 1 + width) fill(pose, tickX, by + 2, tickX + 1, by + 5, 0xFF762020);
        }

        // A small guarded burner sits between the ER meter and the fuel slot.
        int flameX = x + 83;
        int flameY = y + 41;
        fill(pose, x + 77, y + 39, x + 99, y + 55, 0xFF24282B);
        fill(pose, x + 78, y + 40, x + 98, y + 41, 0xFF848C8F);
        fill(pose, x + 80, y + 53, x + 96, y + 55, 0xFF171A1D);
        if (menu.burnLeft() > 0) {
            int flicker = (int) (System.currentTimeMillis() / 140L % 3L);
            fill(pose, flameX + 4, flameY + 7 + flicker, flameX + 10, flameY + 15, 0xFFFF4A00);
            fill(pose, flameX + 5, flameY + 3, flameX + 9, flameY + 12 + flicker, 0xFFFFA000);
            fill(pose, flameX + 6, flameY + 7, flameX + 8, flameY + 13, 0xFFFFE36A);
        } else {
            fill(pose, flameX + 3, flameY + 11, flameX + 11, flameY + 13, 0xFF8A6A4B);
        }
        fill(pose, x + 79, y + 55, x + 97, y + 56, 0xFFA0A4A4);
    }

    @Override
    protected void renderLabels(PoseStack pose, int mouseX, int mouseY) {
        this.font.draw(pose, "Energia Redstônica", 8, 7, 0xFFE5E6E4);
        this.font.draw(pose, "ER", 29, 28, 0xFFC5C9C9);
        this.font.draw(pose, Math.round(menu.energy() * 100.0F / JetpackItem.CAPACITY) + "%", 137, 25, 0xFFF0E8DF);
        this.font.draw(pose, "Inventário", this.inventoryLabelX, this.inventoryLabelY, 0xFFE0DDD4);
    }
}
