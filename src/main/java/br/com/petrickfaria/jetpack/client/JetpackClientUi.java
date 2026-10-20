package br.com.petrickfaria.jetpack.client;

import br.com.petrickfaria.jetpack.JetpackMod;
import br.com.petrickfaria.jetpack.item.JetpackItem;
import br.com.petrickfaria.jetpack.network.JetpackNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
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
    private static final int SLOT_SIZE = 18;

    @SubscribeEvent
    public static void renderInventorySlot(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen)) return;
        Minecraft mc = Minecraft.getInstance();
        int left = (mc.getWindow().getGuiScaledWidth() - 176) / 2;
        int top = (mc.getWindow().getGuiScaledHeight() - 166) / 2;
        int x = left + 26;
        int y = top + 26;
        var pose = event.getPoseStack();
        // A vanilla-like recessed slot beside the chestplate slot.
        GuiComponent.fill(pose, x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF373737);
        GuiComponent.fill(pose, x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xFF8B8B8B);
        GuiComponent.fill(pose, x + 2, y + 2, x + SLOT_SIZE - 2, y + SLOT_SIZE - 2, 0xFF3F3F3F);
        ItemStack pack = JetpackClientState.getEquipment();
        if (!pack.isEmpty()) {
            mc.getItemRenderer().renderAndDecorateItem(pack, x + 1, y + 1);
            mc.getItemRenderer().renderGuiItemDecorations(mc.font, pack, x + 1, y + 1);
        }
    }

    @SubscribeEvent
    public static void clickInventorySlot(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen) || event.getButton() != 0) return;
        Minecraft mc = Minecraft.getInstance();
        int left = (mc.getWindow().getGuiScaledWidth() - 176) / 2;
        int top = (mc.getWindow().getGuiScaledHeight() - 166) / 2;
        int x = left + 26;
        int y = top + 26;
        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        if (mouseX < x || mouseX >= x + SLOT_SIZE || mouseY < y || mouseY >= y + SLOT_SIZE) return;
        event.setCanceled(true);
        if (mc.player != null)
            JetpackNetwork.CHANNEL.sendToServer(new JetpackNetwork.EquipmentClick());
    }

    @SubscribeEvent
    public static void renderHud(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        ItemStack pack = JetpackClientState.getEquipment();
        if (!(pack.getItem() instanceof JetpackItem)) return;
        int energy = JetpackItem.energy(pack);
        float fraction = Math.max(0.0F, Math.min(1.0F, energy / (float) JetpackItem.CAPACITY));
        int x = event.getWindow().getGuiScaledWidth() - 126;
        int y = 10;
        var pose = event.getPoseStack();
        mc.font.draw(pose, "Jetpack:", x, y, 0xFF000000);
        int barY = y + 12;
        GuiComponent.fill(pose, x, barY, x + 96, barY + 10, 0xFF202020);
        GuiComponent.fill(pose, x + 1, barY + 1, x + 95, barY + 9, 0xFF7A1010);
        int fill = Math.round(92 * fraction);
        if (fill > 0) GuiComponent.fill(pose, x + 2, barY + 2, x + 2 + fill, barY + 8, 0xFFE53935);
        mc.font.draw(pose, Math.round(fraction * 100) + "%", x + 101, barY + 1, 0xFF000000);
    }

    @SubscribeEvent
    public static void onLogout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        JetpackClientState.clear();
    }

    private JetpackClientUi() {}
}
