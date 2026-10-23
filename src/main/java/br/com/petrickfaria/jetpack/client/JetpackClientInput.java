package br.com.petrickfaria.jetpack.client;

import br.com.petrickfaria.jetpack.JetpackMod;
import br.com.petrickfaria.jetpack.network.JetpackNetwork;
import br.com.petrickfaria.jetpack.player.JetpackFlight;
import br.com.petrickfaria.jetpack.item.JetpackItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JetpackMod.MOD_ID, value = Dist.CLIENT)
public final class JetpackClientInput {
    private static boolean previousJump, lastUp, lastDown, previousFlightActive;
    private static float lastStrafe, lastForward;
    private static long previousJumpAt;
    private static double lastFlightY = Double.NaN;
    private static double measuredAscentSpeed;

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || event.getEntity() != mc.player) return;
        var flight = JetpackClientState.getFlight(mc.player.getId());
        if (!flight.active() || !JetpackFlight.shouldSuppressAscent(
                event.getInput().jumping, mc.player.getY(), flight.ceiling())) return;

        // Stop the vanilla jump input before travel applies it. If this is the last
        // sub-step to the cap, align once to the exact boundary (at most 0.2 blocks).
        if (mc.player.getY() < flight.ceiling()) {
            mc.player.setPos(mc.player.getX(), flight.ceiling(), mc.player.getZ());
            Vec3 motion = mc.player.getDeltaMovement();
            if (motion.y > 0.0D) mc.player.setDeltaMovement(motion.x, 0.0D, motion.z);
        }
        event.getInput().jumping = false;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            previousJump = false; lastUp = false; lastDown = false; lastStrafe = 0.0F; lastForward = 0.0F;
            previousFlightActive = false; lastFlightY = Double.NaN; measuredAscentSpeed = 0.0D;
            return;
        }
        if (mc.screen != null) {
            if (lastUp || lastDown || lastStrafe != 0.0F || lastForward != 0.0F)
                JetpackNetwork.CHANNEL.sendToServer(new JetpackNetwork.FlightInput(false, false, false, 0.0F, 0.0F));
            previousJump = false; lastUp = false; lastDown = false; lastStrafe = 0.0F; lastForward = 0.0F;
            lastFlightY = mc.player.getY();
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
            double y = mc.player.getY();
            if (previousFlightActive && Double.isFinite(lastFlightY) && up && !down
                    && !mc.player.isOnGround()
                    && lastFlightY < flight.ceiling() - JetpackFlight.CEILING_POSITION_TOLERANCE
                    && y > lastFlightY + 1.0E-4D
                    && y < flight.ceiling() - JetpackFlight.CEILING_POSITION_TOLERANCE) {
                double measured = y - lastFlightY;
                double nativeSteadyAscent = JetpackFlight.creativeSteadyAscentSpeed(
                        mc.player.getAbilities().getFlyingSpeed());
                measuredAscentSpeed = Math.max(measuredAscentSpeed, Math.min(measured, nativeSteadyAscent));
            }
            double ascentSpeed = measuredAscentSpeed > 0.0D ? measuredAscentSpeed
                    : JetpackFlight.creativeSteadyAscentSpeed(mc.player.getAbilities().getFlyingSpeed());
            Vec3 motion = mc.player.getDeltaMovement();
            Vec3 constrained = JetpackFlight.constrainFlightMotion(motion, down, mc.player.isOnGround(),
                    y, flight.ceiling(), ascentSpeed);
            if (!constrained.equals(motion)) mc.player.setDeltaMovement(constrained);
            JetpackFlight.enforceCeilingPosition(mc.player, flight.ceiling());
            previousFlightActive = true;
            lastFlightY = mc.player.getY();
        } else {
            previousFlightActive = false;
            lastFlightY = Double.NaN;
            measuredAscentSpeed = 0.0D;
        }
        if (!flight.active() && down && !mc.player.isCreative() && !mc.player.isSpectator()) {
            var pack = JetpackClientState.getEquipment(mc.player.getId());
            if (pack.getItem() instanceof JetpackItem && JetpackItem.energy(pack) > 0 && !mc.player.isOnGround()) {
                Vec3 motion = mc.player.getDeltaMovement();
                Vec3 adjusted = JetpackFlight.controlledDescentMotion(motion, true, false);
                if (!adjusted.equals(motion)) mc.player.setDeltaMovement(adjusted);
            }
        }
        previousJump = jump; lastUp = up; lastDown = down; lastStrafe = strafe; lastForward = forward;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        previousJump = false; lastUp = false; lastDown = false; lastStrafe = 0.0F; lastForward = 0.0F; previousJumpAt = 0L;
        previousFlightActive = false; lastFlightY = Double.NaN; measuredAscentSpeed = 0.0D;
        JetpackClientState.clear();
    }
    private JetpackClientInput() {}
}
