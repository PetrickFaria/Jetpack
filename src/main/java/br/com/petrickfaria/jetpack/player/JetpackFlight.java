package br.com.petrickfaria.jetpack.player;

import br.com.petrickfaria.jetpack.item.JetpackItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "jetpack")
public final class JetpackFlight {
    private static final String FLIGHT_KEY = "jetpack_flight";
    // Vanilla creative flight accelerates at 0.05 blocks/tick and applies 0.91 air drag.
    private static final double CREATIVE_FLY_ACCELERATION = 0.05D;
    private static final double CREATIVE_MAX_SPEED = CREATIVE_FLY_ACCELERATION / (1.0D - 0.91D);
    private static final double BRAKE_DISTANCE = 3.0D;
    private static final double BRAKE_FALL_SPEED = 0.16D;

    public static void handleInput(ServerPlayer player, boolean toggle, boolean up, boolean down, float strafe, float forward) {
        CompoundTag state = state(player);
        boolean jumpEdge = up && !state.getBoolean("jumpPressed");
        state.putBoolean("jumpPressed", up);
        state.putBoolean("up", up);
        state.putBoolean("down", down);
        state.putFloat("strafe", Mth.clamp(strafe, -1.0F, 1.0F));
        state.putFloat("forward", Mth.clamp(forward, -1.0F, 1.0F));
        boolean doubleTap = false;
        if (jumpEdge) {
            long now = System.currentTimeMillis();
            long last = state.getLong("lastJumpAt");
            doubleTap = last > 0L && now - last <= 300L;
            state.putLong("lastJumpAt", doubleTap ? 0L : now);
        }
        // The server derives the double tap from rising key edges; it does not trust a client toggle flag.
        if (doubleTap) {
            if (state.getBoolean("active")) stop(player, state, true);
            else start(player, state);
        }
    }

    private static void start(ServerPlayer player, CompoundTag state) {
        if (player.isCreative() || player.isSpectator()) return;
        var pack = JetpackEquipment.get(player);
        if (!(pack.getItem() instanceof JetpackItem item) || JetpackItem.energy(pack) <= 0) return;

        double groundY;
        if (state.contains("lastGroundY")) groundY = state.getDouble("lastGroundY");
        else groundY = findSurface(player, 256);
        if (!Double.isFinite(groundY) || player.getY() > groundY + item.maximumHeight()) return;

        state.putDouble("baseY", groundY);
        state.putBoolean("active", true);
        state.putBoolean("braking", false);
        state.putBoolean("previousNoGravity", player.isNoGravity());
        player.setNoGravity(true);
        player.fallDistance = 0;
    }

    private static void stop(Player player, CompoundTag state, boolean userRequested) {
        if (!state.getBoolean("active")) return;
        state.putBoolean("active", false);
        state.putBoolean("up", false);
        state.putBoolean("down", false);
        player.setNoGravity(state.getBoolean("previousNoGravity"));
        var remaining = JetpackEquipment.get(player);
        state.putBoolean("braking", userRequested && !player.isOnGround() && JetpackItem.energy(remaining) > 0);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || event.side.isClient()) return;
        Player player = event.player;
        CompoundTag state = state(player);
        if (player.isCreative() || player.isSpectator()) {
            stop(player, state, false);
            return;
        }
        if (player.isOnGround() && !state.getBoolean("active")) {
            state.putDouble("lastGroundY", player.getY());
            state.putBoolean("braking", false);
        }
        if (state.getBoolean("active")) tickPoweredFlight(player, state);
        else if (state.getBoolean("braking")) tickBrake(player, state);
    }

    private static void tickPoweredFlight(Player player, CompoundTag state) {
        var pack = JetpackEquipment.get(player);
        if (!(pack.getItem() instanceof JetpackItem item) || JetpackItem.energy(pack) <= 0) {
            stop(player, state, false);
            return;
        }

        Vec3 motion = player.getDeltaMovement();
        double lift = state.getBoolean("up") == state.getBoolean("down") ? 0.0D
                : state.getBoolean("up") ? 1.0D : -1.0D;
        Vec3 startingMotion = lift == 0.0D ? new Vec3(motion.x, 0.0D, motion.z) : motion;
        Vec3 velocity = creativeFlightVelocity(startingMotion, state.getFloat("strafe"), state.getFloat("forward"),
                lift, player.getYRot());
        double ceiling = state.getDouble("baseY") + item.maximumHeight();
        if (player.getY() >= ceiling && velocity.y > 0.0D) velocity = new Vec3(velocity.x, 0.0D, velocity.z);
        else if (velocity.y > 0.0D && player.getY() + velocity.y > ceiling)
            velocity = new Vec3(velocity.x, ceiling - player.getY(), velocity.z);
        // Sprint does not increase jetpack speed; vanilla state is otherwise untouched.
        player.setSprinting(false);

        if (JetpackItem.consume(pack, 1, false) != 1) {
            JetpackEquipment.save(player, pack);
            stop(player, state, false);
            return;
        }
        JetpackEquipment.save(player, pack);
        player.setDeltaMovement(velocity);
        player.fallDistance = 0;
        player.hurtMarked = true;
    }

    private static void tickBrake(Player player, CompoundTag state) {
        if (player.isOnGround()) { state.putBoolean("braking", false); return; }
        Vec3 motion = player.getDeltaMovement();
        if (motion.y >= 0.0D) return;
        double surface = findSurface(player, 4);
        double distance = player.getY() - surface;
        if (!Double.isFinite(surface) || distance < 0.0D || distance > BRAKE_DISTANCE) return;
        var pack = JetpackEquipment.get(player);
        if (JetpackItem.energy(pack) <= 0 || JetpackItem.consume(pack, 1, false) != 1) {
            JetpackEquipment.save(player, pack);
            state.putBoolean("braking", false);
            return;
        }
        JetpackEquipment.save(player, pack);
        player.setDeltaMovement(motion.x, Math.max(motion.y, -BRAKE_FALL_SPEED), motion.z);
        player.fallDistance = 0;
        player.hurtMarked = true;
    }

    static Vec3 creativeFlightVelocity(Vec3 current, float strafe, float forward, double lift, float yawDegrees) {
        double x = Mth.clamp(strafe, -1.0F, 1.0F);
        double z = Mth.clamp(forward, -1.0F, 1.0F);
        double y = Mth.clamp(lift, -1.0D, 1.0D);
        double inputLength = Math.sqrt(x * x + y * y + z * z);
        if (inputLength > 1.0D) { x /= inputLength; y /= inputLength; z /= inputLength; }
        double yaw = yawDegrees * (Math.PI / 180.0D);
        double worldX = x * Math.cos(yaw) - z * Math.sin(yaw);
        double worldZ = z * Math.cos(yaw) + x * Math.sin(yaw);
        Vec3 next = current.add(worldX * CREATIVE_FLY_ACCELERATION,
                y * CREATIVE_FLY_ACCELERATION, worldZ * CREATIVE_FLY_ACCELERATION);
        double length = next.length();
        return length > CREATIVE_MAX_SPEED ? next.scale(CREATIVE_MAX_SPEED / length) : next;
    }

    private static double findSurface(Player player, int depth) {
        int x = Mth.floor(player.getX());
        int z = Mth.floor(player.getZ());
        int fromY = Math.min(player.blockPosition().getY() - 1, player.level.getMaxBuildHeight() - 1);
        int minY = Math.max(player.level.getMinBuildHeight(), fromY - depth);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, fromY, z);
        for (int y = fromY; y >= minY; y--) {
            pos.set(x, y, z);
            if (player.level.getBlockState(pos).isFaceSturdy(player.level, pos, Direction.UP)) return y + 1.0D;
        }
        return Double.NaN;
    }

    private static CompoundTag state(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(FLIGHT_KEY, CompoundTag.TAG_COMPOUND)) data.put(FLIGHT_KEY, new CompoundTag());
        return data.getCompound(FLIGHT_KEY);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        CompoundTag original = event.getOriginal().getPersistentData();
        CompoundTag target = event.getEntity().getPersistentData();
        if (original.contains("jetpack_equipment", CompoundTag.TAG_COMPOUND))
            target.put("jetpack_equipment", original.getCompound("jetpack_equipment").copy());
        target.put(FLIGHT_KEY, new CompoundTag());
    }

    @SubscribeEvent
    public static void onLogout(PlayerLoggedOutEvent event) {
        CompoundTag state = state(event.getEntity());
        stop(event.getEntity(), state, false);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        CompoundTag state = state(event.getEntity());
        if (state.getBoolean("active")) event.getEntity().setNoGravity(state.getBoolean("previousNoGravity"));
        state.putBoolean("active", false);
        state.putBoolean("braking", false);
        state.putBoolean("jumpPressed", false);
        state.putBoolean("up", false);
        state.putBoolean("down", false);
    }

    private JetpackFlight() {}
}
