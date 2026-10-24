package br.com.petrickfaria.jetpack.client;

import br.com.petrickfaria.jetpack.item.JetpackItem;
import br.com.petrickfaria.jetpack.menu.JetpackFuelMenu;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.systems.RenderSystem;

public final class JetpackFuelScreen extends AbstractContainerScreen<JetpackFuelMenu> {
    private static final ResourceLocation FURNACE_TEXTURE = new ResourceLocation("textures/gui/container/furnace.png");
    public JetpackFuelScreen(JetpackFuelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 184;
        this.inventoryLabelY = 90;
    }

    @Override
    protected void renderBg(PoseStack pose, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        Palette palette = palette();

        // Balanced panel zones: title, ER meter, furnace burner and player inventory.
        fill(pose, x, y, x + imageWidth, y + imageHeight, palette.outer());
        fill(pose, x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, palette.rim());
        fill(pose, x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, palette.inset());
        fill(pose, x + 4, y + 4, x + imageWidth - 4, y + imageHeight - 4, palette.surface());

        // Centered title plaque.
        fill(pose, x + 5, y + 5, x + imageWidth - 5, y + 30, palette.header());
        fill(pose, x + 29, y + 5, x + 147, y + 30, palette.accentDark());
        fill(pose, x + 30, y + 6, x + 146, y + 29, palette.rim());
        fill(pose, x + 31, y + 7, x + 145, y + 28, palette.header());
        fill(pose, x + 35, y + 26, x + 141, y + 27, palette.highlight());
        fill(pose, x + 5, y + 30, x + imageWidth - 5, y + 31, palette.accent());

        // ER meter sits in its own shallow panel.
        fill(pose, x + 8, y + 32, x + imageWidth - 8, y + 44, palette.panel());
        fill(pose, x + 8, y + 32, x + imageWidth - 8, y + 33, palette.highlight());
        int bx = x + 46;
        int by = y + 35;
        fill(pose, bx - 3, by - 2, bx + 87, by + 8, 0xFF1A1D20);
        fill(pose, bx - 2, by - 1, bx + 86, by + 7, 0xFF9AA1A3);
        fill(pose, bx - 1, by, bx + 85, by + 6, 0xFF25292D);
        fill(pose, bx, by + 1, bx + 84, by + 5, 0xFF5A1516);
        int energy = Math.max(0, Math.min(JetpackItem.CAPACITY, menu.energy()));
        int width = Math.round(82.0F * energy / JetpackItem.CAPACITY);
        if (width > 0) {
            fill(pose, bx + 1, by + 2, bx + 1 + width, by + 5, 0xFFCF2929);
            fill(pose, bx + 1, by + 2, bx + 1 + width, by + 3, 0xFFFF7A70);
            fill(pose, bx + 1, by + 4, bx + 1 + width, by + 5, 0xFF9E1C21);
        }
        for (int tick = 1; tick < 10; tick++) {
            int tickX = bx + 1 + tick * 8;
            if (tickX < bx + 1 + width) fill(pose, tickX, by + 3, tickX + 1, by + 5, 0xFF762020);
        }

        // Reuse the vanilla furnace flame from its GUI atlas with the same upward
        // crop and burn-progress behavior used by the regular furnace screen.
        fill(pose, x + 5, y + 44, x + imageWidth - 5, y + 63, palette.panel());
        fill(pose, x + 5, y + 44, x + imageWidth - 5, y + 45, palette.highlight());
        fill(pose, x + 77, y + 44, x + 99, y + 62, palette.accentDark());
        fill(pose, x + 78, y + 45, x + 98, y + 46, palette.highlight());
        fill(pose, x + 79, y + 60, x + 97, y + 61, palette.outer());
        if (menu.burnLeft() > 0 && menu.burnTotal() > 0) {
            int flameHeight = Math.max(1, Math.min(13,
                    (int) Math.ceil(13.0D * menu.burnLeft() / menu.burnTotal())));
            RenderSystem.setShaderTexture(0, FURNACE_TEXTURE);
            blit(pose, x + 82, y + 59 - flameHeight, 176, 13 - flameHeight, 14, flameHeight);
        }

        fill(pose, x + 5, y + 84, x + imageWidth - 5, y + 85, palette.rim());
        fill(pose, x + 5, y + 88, x + imageWidth - 5, y + imageHeight - 5, palette.inset());
        fill(pose, x + 8, y + 91, x + imageWidth - 8, y + imageHeight - 6, palette.panel());

        // Slot wells align with the functional menu hitboxes.
        for (int[] corner : new int[][] {{7, 7}, {imageWidth - 10, 7}, {7, imageHeight - 10}, {imageWidth - 10, imageHeight - 10}}) {
            fill(pose, x + corner[0], y + corner[1], x + corner[0] + 2, y + corner[1] + 2, palette.highlight());
            fill(pose, x + corner[0], y + corner[1] + 1, x + corner[0] + 2, y + corner[1] + 2, palette.accentDark());
        }
        drawSlotFrame(pose, x + JetpackFuelMenu.FUEL_SLOT_X, y + JetpackFuelMenu.FUEL_SLOT_Y, palette);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                drawSlotFrame(pose, x + 8 + col * 18,
                        y + JetpackFuelMenu.INVENTORY_SLOT_START_Y + row * 18, palette);
        for (int col = 0; col < 9; col++)
            drawSlotFrame(pose, x + 8 + col * 18, y + JetpackFuelMenu.HOTBAR_SLOT_Y, palette);
    }

    @Override
    protected void renderLabels(PoseStack pose, int mouseX, int mouseY) {
        Palette palette = palette();
        String title = "Jetpack";
        String subtitle = "(" + menu.materialName() + ")";
        this.font.draw(pose, title, (this.imageWidth - this.font.width(title)) / 2, 8, 0xFFF8F6EE);
        this.font.draw(pose, subtitle, (this.imageWidth - this.font.width(subtitle)) / 2, 18, palette.subtitle());
        this.font.draw(pose, "ER", 18, 36, 0xFFF8F6EE);
        String percent = Math.round(menu.energy() * 100.0F / JetpackItem.CAPACITY) + "%";
        this.font.draw(pose, percent, 137, 36, 0xFFF8F6EE);
        this.font.draw(pose, "Inventário", this.inventoryLabelX, this.inventoryLabelY, 0xFFE0DDD4);
    }

    private void drawSlotFrame(PoseStack pose, int x, int y, Palette palette) {
        fill(pose, x, y, x + 18, y + 18, palette.accentDark());
        fill(pose, x + 1, y + 1, x + 17, y + 17, palette.rim());
        fill(pose, x + 2, y + 2, x + 16, y + 16, palette.slotFace());
        fill(pose, x + 2, y + 2, x + 16, y + 3, palette.highlight());
        fill(pose, x + 2, y + 3, x + 3, y + 16, palette.highlight());
        fill(pose, x + 3, y + 15, x + 16, y + 16, palette.accentDark());
        fill(pose, x + 15, y + 3, x + 16, y + 15, palette.accentDark());
    }

    private Palette palette() {
        return switch (menu.materialName()) {
            case "Madeira" -> new Palette(0xFF24180F, 0xFF88613D, 0xFF473326, 0xFF332419,
                    0xFF302116, 0xFF8A4F2B, 0xFFA27649, 0xFFC18B55, 0xFF52351F, 0xFFD8B184, 0xFF493321).lifted();
            case "Ferro" -> new Palette(0xFF202326, 0xFF8F999E, 0xFF444A4E, 0xFF2D3236,
                    0xFF252A2E, 0xFF687984, 0xFFA2ADB2, 0xFFD0D7DA, 0xFF38434A, 0xFFD4DADD, 0xFF43494C).lifted();
            case "Ouro" -> new Palette(0xFF30240B, 0xFFB18C35, 0xFF685522, 0xFF403418,
                    0xFF3A2D0D, 0xFF8C6A20, 0xFFC5A344, 0xFFE3C45C, 0xFF594313, 0xFFEAD47A, 0xFF685522).lifted();
            case "Esmeralda" -> new Palette(0xFF0C261A, 0xFF39856A, 0xFF1D553F, 0xFF14382B,
                    0xFF103223, 0xFF1F694B, 0xFF4A9E7D, 0xFF71C39A, 0xFF164831, 0xFF9AD7B8, 0xFF1D513C).lifted();
            case "Diamante" -> new Palette(0xFF102930, 0xFF4B9DAC, 0xFF215866, 0xFF183C46,
                    0xFF143740, 0xFF267486, 0xFF65B6C4, 0xFF8CD2DD, 0xFF1A5664, 0xFFA9E0E7, 0xFF205563).lifted();
            case "Netherita" -> new Palette(0xFF191718, 0xFF6B6361, 0xFF3B3737, 0xFF292626,
                    0xFF211F20, 0xFF514B4A, 0xFF827A77, 0xFFA59B96, 0xFF343031, 0xFFB8ADA7, 0xFF343031).lifted();
            default -> new Palette(0xFF15191D, 0xFF8A9297, 0xFF393D40, 0xFF292E33,
                    0xFF24292E, 0xFF70787D, 0xFF9AA1A3, 0xFFB7BEC0, 0xFF383D41, 0xFFE0DDD4, 0xFF383D41).lifted();
        };
    }

    private record Palette(int outer, int rim, int surface, int inset, int header,
                           int panel, int accent, int highlight, int accentDark, int subtitle, int slotFace) {
        private Palette lifted() {
            return new Palette(outer, mix(rim, 0.08F), mix(surface, 0.24F), mix(inset, 0.18F),
                    mix(header, 0.12F), mix(panel, 0.22F), mix(accent, 0.08F), highlight,
                    accentDark, subtitle, mix(slotFace, 0.12F));
        }

        private static int mix(int color, float amount) {
            int r = lighten((color >> 16) & 0xFF, amount);
            int g = lighten((color >> 8) & 0xFF, amount);
            int b = lighten(color & 0xFF, amount);
            return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
        }

        private static int lighten(int channel, float amount) {
            return Math.min(255, channel + Math.round((255 - channel) * amount));
        }
    }
}
