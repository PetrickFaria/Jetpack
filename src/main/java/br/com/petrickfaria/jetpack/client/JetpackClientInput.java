package br.com.petrickfaria.jetpack.client;

import br.com.petrickfaria.jetpack.JetpackMod;
import br.com.petrickfaria.jetpack.network.JetpackNetwork;
import br.com.petrickfaria.jetpack.player.JetpackFlight;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JetpackMod.MOD_ID, value = Dist.CLIENT)
public final class JetpackClientInput {
    private static boolean previousJump, lastUp, lastDown;
    private static float lastStrafe, lastForward;
    private static long previousJumpAt;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            previousJump = false; lastUp = false; lastDown = false; lastStrafe = 0.0F; lastForward = 0.0F;
            return;
        }
        if (mc.screen != null) {
            if (lastUp || lastDown || lastStrafe != 0.0F || lastForward != 0.0F)
                JetpackNetwork.CHANNEL.sendToServer(new JetpackNetwork.FlightInput(false, false, false, 0.0F, 0.0F));
            previousJump = false; lastUp = false; lastDown = false; lastStrafe = 0.0F; lastForward = 0.0F;
            return;
        }
        boolean jump = mc.options.keyJump.isDown();
        boolean up = jump;
        boolean down = mc.options.keyShift.isDown();
        float strafe = (mc.options.keyRight.isDown() ? 1.0F : 0.0F) - (mc.options.keyLeft.isDown() ? 1.0F : 0.0F);
        float forward = (mc.options.keyUp.isDown() ? 1.0F : 0.0F) - (mc.options.keyDown.isDown() ? 1.0F : 0.0F);
        boolean toggle = false;
        if (jump && !previousJump) {
            long now = System.currentTimeMillis();
            if (now - previousJumpAt <= 300L) { toggle = true; previousJumpAt = 0L; }
            else previousJumpAt = now;
        }
        if (toggle || up != lastUp || down != lastDown || strafe != lastStrafe || forward != lastForward)
            JetpackNetwork.CHANNEL.sendToServer(new JetpackNetwork.FlightInput(toggle, up, down, strafe, forward));

        var flight = JetpackClientState.getFlight(mc.player.getId());
        if (flight.active()) {
            Vec3 motion = mc.player.getDeltaMovement();
            Vec3 constrained = JetpackFlight.constrainFlightMotion(motion, down, mc.player.isOnGround(), mc.player.getY(), flight.ceiling());
            if (!constrained.equals(motion)) mc.player.setDeltaMovement(constrained);
        }
        previousJump = jump; lastUp = up; lastDown = down; lastStrafe = strafe; lastForward = forward;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        previousJump = false; lastUp = false; lastDown = false; lastStrafe = 0.0F; lastForward = 0.0F; previousJumpAt = 0L;
        JetpackClientState.clear();
    }
    private JetpackClientInput() {}
}
