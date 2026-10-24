package br.com.petrickfaria.jetpack.client;

import br.com.petrickfaria.jetpack.JetpackMod;
import br.com.petrickfaria.jetpack.item.JetpackItem;
import br.com.petrickfaria.jetpack.network.JetpackNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JetpackMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class JetpackClientUi {
    private static final int SLOT_SIZE = JetpackInventorySlots.EQUIPMENT_SLOT_SIZE;

    @SubscribeEvent
    public static void renderInventorySlot(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen)) return;
        Minecraft mc = Minecraft.getInstance();
        int x = JetpackInventorySlots.equipmentSlotLeft(event.getScreen().width);
        int y = JetpackInventorySlots.equipmentSlotTop(event.getScreen().height);
        var pose = event.getPoseStack();
        // Keep the approved slot bounds/position while giving its inset a subtle
        // steel bevel and a tiny redstone-burner mark when it is empty.
        GuiComponent.fill(pose, x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF171A1D);
        GuiComponent.fill(pose, x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xFF91999C);
        GuiComponent.fill(pose, x + 1, y + SLOT_SIZE - 2, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xFF292D30);
        GuiComponent.fill(pose, x + 2, y + 2, x + SLOT_SIZE - 2, y + SLOT_SIZE - 2, 0xFF34383B);
        GuiComponent.fill(pose, x + 3, y + 3, x + SLOT_SIZE - 3, y + SLOT_SIZE - 3, 0xFF3F3F3F);
        GuiComponent.fill(pose, x + 3, y + 3, x + SLOT_SIZE - 3, y + 4, 0xFF575B5D);
        GuiComponent.fill(pose, x + 3, y + SLOT_SIZE - 4, x + SLOT_SIZE - 3, y + SLOT_SIZE - 3, 0xFF7A2422);
        ItemStack pack = JetpackClientState.getEquipment(mc.player.getId());
        if (!pack.isEmpty()) {
            mc.getItemRenderer().renderAndDecorateItem(pack, x + 1, y + 1);
            mc.getItemRenderer().renderGuiItemDecorations(mc.font, pack, x + 1, y + 1);
        } else {
            // Low-contrast twin thrusters identify the auxiliary equipment slot
            // without competing with the vanilla armor slots around it.
            GuiComponent.fill(pose, x + 5, y + 6, x + 8, y + 12, 0xFF252A2D);
            GuiComponent.fill(pose, x + 10, y + 6, x + 13, y + 12, 0xFF252A2D);
            GuiComponent.fill(pose, x + 6, y + 6, x + 7, y + 10, 0xFF81898C);
            GuiComponent.fill(pose, x + 11, y + 6, x + 12, y + 10, 0xFF81898C);
            GuiComponent.fill(pose, x + 8, y + 8, x + 10, y + 11, 0xFF782321);
            GuiComponent.fill(pose, x + 8, y + 8, x + 9, y + 9, 0xFFB83A32);
        }
    }

    @SubscribeEvent
    public static void clickInventorySlot(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        boolean overEquipmentSlot = JetpackInventorySlots.isEquipmentSlotAt(
                screen.width, screen.height, event.getMouseX(), event.getMouseY());

        if (overEquipmentSlot && event.getButton() == 0 && Screen.hasShiftDown()) {
            event.setCanceled(true);
            JetpackNetwork.CHANNEL.sendToServer(new JetpackNetwork.ShiftClickUnequip());
            return;
        }

        if (overEquipmentSlot && event.getButton() == 1) {
            event.setCanceled(true);
            JetpackNetwork.CHANNEL.sendToServer(new JetpackNetwork.OpenFuelFromEquipment());
            return;
        }

        if (event.getButton() == 0 && Screen.hasShiftDown()) {
            int menuSlot = inventorySlotAt(screen, event.getMouseX(), event.getMouseY());
            ItemStack source = stackForInventoryMenuSlot(mc.player.getInventory(), menuSlot);
            if (source.getItem() instanceof JetpackItem) {
                ItemStack equipped = JetpackClientState.getEquipment(mc.player.getId());
                if (equipped.isEmpty()) {
                    event.setCanceled(true);
                    JetpackNetwork.CHANNEL.sendToServer(new JetpackNetwork.ShiftClickEquip(menuSlot));
                }
                return;
            }
        }

        if (event.getButton() == 1) {
            int menuSlot = inventorySlotAt(screen, event.getMouseX(), event.getMouseY());
            ItemStack source = stackForInventoryMenuSlot(mc.player.getInventory(), menuSlot);
            if (source.getItem() instanceof JetpackItem) {
                event.setCanceled(true);
                JetpackNetwork.CHANNEL.sendToServer(new JetpackNetwork.OpenFuelFromInventory(menuSlot));
                return;
            }
        }

        if (event.getButton() != 0) return;
        if (!overEquipmentSlot) return;
        event.setCanceled(true);
        JetpackNetwork.CHANNEL.sendToServer(new JetpackNetwork.EquipmentClick());
    }

    /** Maps only the vanilla player inventory grid; other mod container layouts are left untouched. */
    private static int inventorySlotAt(InventoryScreen screen, double mouseX, double mouseY) {
        return JetpackInventorySlots.menuSlotAt(screen.width, screen.height, mouseX, mouseY);
    }

    private static ItemStack stackForInventoryMenuSlot(net.minecraft.world.entity.player.Inventory inventory, int menuSlot) {
        if (menuSlot >= 9 && menuSlot <= 35) return inventory.items.get(menuSlot);
        if (menuSlot >= 36 && menuSlot <= 44) return inventory.items.get(menuSlot - 36);
        return ItemStack.EMPTY;
    }

    @SubscribeEvent
    public static void renderHud(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        ItemStack pack = JetpackClientState.getEquipment(mc.player.getId());
        if (!(pack.getItem() instanceof JetpackItem)) return;
        int energy = JetpackItem.energy(pack);
        float fraction = Math.max(0.0F, Math.min(1.0F, energy / (float) JetpackItem.CAPACITY));
        int x = event.getWindow().getGuiScaledWidth() - 126;
        int y = 10;
        var pose = event.getPoseStack();
        String title = "Jetpack";
        int titleX = x + (96 - mc.font.width(title)) / 2;
        drawOutlinedText(mc, pose, title, titleX, y);
        boolean active = JetpackClientState.getFlight(mc.player.getId()).active();
        int barY = y + 12;
        GuiComponent.fill(pose, x - 1, barY - 1, x + 97, barY + 11, 0xFF15191D);
        GuiComponent.fill(pose, x, barY, x + 96, barY + 10, 0xFF9AA1A3);
        GuiComponent.fill(pose, x + 1, barY + 1, x + 95, barY + 9, 0xFF272C30);
        GuiComponent.fill(pose, x + 2, barY + 2, x + 94, barY + 8, 0xFF681719);
        int fill = Math.round(92 * fraction);
        if (fill > 0) {
            GuiComponent.fill(pose, x + 2, barY + 2, x + 2 + fill, barY + 8,
                    active ? 0xFFF0443F : 0xFFCF2929);
            GuiComponent.fill(pose, x + 2, barY + 2, x + 2 + fill, barY + 3,
                    active ? 0xFFFFB0A3 : 0xFFFF7A70);
            GuiComponent.fill(pose, x + 2, barY + 7, x + 2 + fill, barY + 8,
                    active ? 0xFFC52B32 : 0xFF9E1C21);
        }
        for (int tick = 1; tick < 10; tick++) {
            int tickX = x + 2 + tick * 9;
            if (tickX < x + 2 + fill) GuiComponent.fill(pose, tickX, barY + 3, tickX + 1, barY + 7,
                    active ? 0xFF9E2828 : 0xFF762020);
        }
        drawOutlinedText(mc, pose, Math.round(fraction * 100) + "%", x + 101, barY + 1);
    }

    private static void drawOutlinedText(Minecraft mc, com.mojang.blaze3d.vertex.PoseStack pose,
                                         String text, int x, int y) {
        mc.font.draw(pose, text, x - 1, y, 0xFF000000);
        mc.font.draw(pose, text, x + 1, y, 0xFF000000);
        mc.font.draw(pose, text, x, y - 1, 0xFF000000);
        mc.font.draw(pose, text, x, y + 1, 0xFF000000);
        mc.font.draw(pose, text, x, y, 0xFFFFFFFF);
    }

    @SubscribeEvent
    public static void onLogout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        JetpackClientState.clear();
    }

    private JetpackClientUi() {}
}
