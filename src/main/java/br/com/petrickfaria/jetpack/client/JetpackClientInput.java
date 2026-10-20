package br.com.petrickfaria.jetpack.client;

import br.com.petrickfaria.jetpack.JetpackMod;
import br.com.petrickfaria.jetpack.network.JetpackNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JetpackMod.MOD_ID, value = Dist.CLIENT)
public final class JetpackClientInput {
    private static boolean previousJump;
    private static boolean lastUp;
    private static boolean lastDown;
    private static long previousJumpAt;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            previousJump = false; lastUp = false; lastDown = false;
            return;
        }
        if (mc.screen != null) {
            if (lastUp || lastDown)
                JetpackNetwork.CHANNEL.sendToServer(new JetpackNetwork.FlightInput(false, false, false));
            previousJump = false; lastUp = false; lastDown = false;
            return;
        }
        boolean jump = mc.options.keyJump.isDown();
        boolean up = jump;
        boolean down = mc.options.keyShift.isDown();
        boolean toggle = false;
        if (jump && !previousJump) {
            long now = System.currentTimeMillis();
            if (now - previousJumpAt <= 300L) { toggle = true; previousJumpAt = 0L; }
            else previousJumpAt = now;
        }
        if (toggle || up != lastUp || down != lastDown)
            JetpackNetwork.CHANNEL.sendToServer(new JetpackNetwork.FlightInput(toggle, up, down));
        previousJump = jump; lastUp = up; lastDown = down;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        previousJump = false; lastUp = false; lastDown = false; previousJumpAt = 0L;
    }

    private JetpackClientInput() {}
}
