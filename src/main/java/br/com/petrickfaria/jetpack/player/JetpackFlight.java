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
    private static final double CLIMB_SPEED = 0.38D;
    private static final double DESCENT_SPEED = 0.38D;
    private static final double BRAKE_DISTANCE = 3.0D;
    private static final double BRAKE_FALL_SPEED = 0.16D;

    public static void handleInput(ServerPlayer player, boolean toggle, boolean up, boolean down) {
        CompoundTag state = state(player);
        boolean jumpEdge = up && !state.getBoolean("jumpPressed");
        state.putBoolean("jumpPressed", up);
        state.putBoolean("up", up);
        state.putBoolean("down", down);
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
        state.putBoolean("braking", userRequested && !player.onGround() && JetpackItem.energy(remaining) > 0);
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
        if (player.onGround() && !state.getBoolean("active")) {
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
        double y = motion.y;
        double ceiling = state.getDouble("baseY") + item.maximumHeight();
        if (state.getBoolean("up")) y = player.getY() >= ceiling ? 0.0D : Math.min(CLIMB_SPEED, ceiling - player.getY());
        else if (state.getBoolean("down")) y = -DESCENT_SPEED;
        else y = 0.0D;

        if (JetpackItem.consume(pack, 1, false) != 1) {
            JetpackEquipment.save(player, pack);
            stop(player, state, false);
            return;
        }
        JetpackEquipment.save(player, pack);
        player.setDeltaMovement(motion.x, y, motion.z);
        player.fallDistance = 0;
        player.hurtMarked = true;
    }

    private static void tickBrake(Player player, CompoundTag state) {
        if (player.onGround()) { state.putBoolean("braking", false); return; }
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
