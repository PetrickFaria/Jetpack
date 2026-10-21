package br.com.petrickfaria.jetpack.player;

import br.com.petrickfaria.jetpack.item.JetpackItem;
import br.com.petrickfaria.jetpack.network.JetpackNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.mojang.math.Vector3f;

@Mod.EventBusSubscriber(modid = "jetpack")
public final class JetpackFlight {
    private static final String FLIGHT_KEY = "jetpack_flight";
    private static final double SLOW_DESCENT_SPEED = 0.025D;
    private static final float CREATIVE_FLY_SPEED = 0.05F;
    private static final DustParticleOptions RED_THRUST = new DustParticleOptions(new Vector3f(0.95F, 0.025F, 0.015F), 0.8F);

    public static void handleInput(ServerPlayer player, boolean toggle, boolean up, boolean down, float strafe, float forward) {
        var state = state(player);
        boolean jumpEdge = up && !state.getBoolean("jumpPressed");
        state.putBoolean("jumpPressed", up);
        state.putBoolean("down", down);
        boolean doubleTap = false;
        if (jumpEdge) {
            long now = System.currentTimeMillis();
            long last = state.getLong("lastJumpAt");
            doubleTap = last > 0L && now - last <= 300L;
            state.putLong("lastJumpAt", doubleTap ? 0L : now);
        }
        // The server derives the toggle from rising jump edges rather than trusting a client toggle flag.
        if (doubleTap) {
            if (state.getBoolean("active")) stop(player, state);
            else start(player, state);
        }
    }

    private static void start(ServerPlayer player, net.minecraft.nbt.CompoundTag state) {
        if (player.isCreative() || player.isSpectator()) return;
        var pack = JetpackEquipment.get(player);
        if (!(pack.getItem() instanceof JetpackItem item) || JetpackItem.energy(pack) <= 0) return;

        double groundY = state.contains("lastGroundY") ? state.getDouble("lastGroundY") : findSurface(player, 256);
        if (!Double.isFinite(groundY) || player.getY() > groundY + item.maximumHeight()) return;

        var abilities = player.getAbilities();
        state.putDouble("baseY", groundY);
        state.putBoolean("active", true);
        state.putBoolean("previousMayfly", abilities.mayfly);
        state.putBoolean("previousFlying", abilities.flying);
        state.putFloat("previousFlySpeed", abilities.getFlyingSpeed());
        state.putBoolean("previousSprinting", player.isSprinting());
        abilities.mayfly = true;
        abilities.flying = true;
        abilities.setFlyingSpeed(CREATIVE_FLY_SPEED);
        player.setSprinting(false); // suppress the separate vanilla sprint-flight speed boost
        player.onUpdateAbilities();
        player.fallDistance = 0;
        JetpackNetwork.syncFlightState(player, true, groundY + item.maximumHeight());
    }

    private static void stop(Player player, net.minecraft.nbt.CompoundTag state) {
        if (!state.getBoolean("active")) return;
        state.putBoolean("active", false);
        state.putBoolean("down", false);
        var abilities = player.getAbilities();
        abilities.mayfly = state.getBoolean("previousMayfly");
        abilities.flying = state.getBoolean("previousFlying") && abilities.mayfly;
        abilities.setFlyingSpeed(state.getFloat("previousFlySpeed"));
        player.setSprinting(state.getBoolean("previousSprinting"));
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.onUpdateAbilities();
            JetpackNetwork.syncFlightState(serverPlayer, false, 0.0D);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) return;
        Player player = event.player;
        var state = state(player);
        if (player.isCreative() || player.isSpectator()) {
            stop(player, state);
            return;
        }
        if (player.isOnGround() && !state.getBoolean("active"))
            state.putDouble("lastGroundY", player.getY());
        if (state.getBoolean("active")) tickPoweredFlight(player, state);
    }

    private static void tickPoweredFlight(Player player, net.minecraft.nbt.CompoundTag state) {
        var pack = JetpackEquipment.get(player);
        if (!(pack.getItem() instanceof JetpackItem item) || JetpackItem.energy(pack) <= 0) {
            stop(player, state);
            return;
        }

        double ceiling = state.getDouble("baseY") + item.maximumHeight();
        Vec3 motion = player.getDeltaMovement();
        if (player.getY() > ceiling) {
            player.setPos(player.getX(), ceiling, player.getZ());
        }
        Vec3 constrained = constrainFlightMotion(motion, state.getBoolean("down"), player.isOnGround(), player.getY(), ceiling);
        if (!constrained.equals(motion)) player.setDeltaMovement(constrained);

        if (JetpackItem.consume(pack, 1, false) != 1) {
            JetpackEquipment.save(player, pack);
            stop(player, state);
            return;
        }
        JetpackEquipment.save(player, pack);
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.tickCount % 2 == 0)
            emitThrusterParticles(serverPlayer);
        player.fallDistance = 0;
    }

    public static Vec3 constrainFlightMotion(Vec3 motion, boolean down, boolean onGround, double y, double ceiling) {
        Vec3 adjusted = down && !onGround
                ? new Vec3(motion.x, -SLOW_DESCENT_SPEED, motion.z) : motion;
        if (y >= ceiling && adjusted.y > 0.0D) return new Vec3(adjusted.x, 0.0D, adjusted.z);
        return adjusted;
    }

    private static void emitThrusterParticles(ServerPlayer player) {
        if (!(player.level instanceof ServerLevel level)) return;
        double yaw = Math.toRadians(player.getYRot());
        double backX = Math.sin(yaw) * 0.28D;
        double backZ = -Math.cos(yaw) * 0.28D;
        double sideX = Math.cos(yaw) * 0.20D;
        double sideZ = Math.sin(yaw) * 0.20D;
        for (int side : new int[] {-1, 1}) {
            double x = player.getX() + backX + side * sideX;
            double y = player.getY() + 0.20D;
            double z = player.getZ() + backZ + side * sideZ;
            level.sendParticles(RED_THRUST, x, y, z, 1, 0.015D, -0.05D, 0.015D, 0.01D);
            level.sendParticles(ParticleTypes.SMOKE, x, y, z, 1, 0.025D, -0.025D, 0.025D, 0.008D);
        }
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

    private static net.minecraft.nbt.CompoundTag state(Player player) {
        var data = player.getPersistentData();
        if (!data.contains(FLIGHT_KEY, net.minecraft.nbt.CompoundTag.TAG_COMPOUND))
            data.put(FLIGHT_KEY, new net.minecraft.nbt.CompoundTag());
        return data.getCompound(FLIGHT_KEY);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        var original = event.getOriginal().getPersistentData();
        var target = event.getEntity().getPersistentData();
        if (original.contains("jetpack_equipment", net.minecraft.nbt.CompoundTag.TAG_COMPOUND))
            target.put("jetpack_equipment", original.getCompound("jetpack_equipment").copy());
        target.put(FLIGHT_KEY, new net.minecraft.nbt.CompoundTag());
    }

    @SubscribeEvent
    public static void onLogout(PlayerLoggedOutEvent event) {
        stop(event.getEntity(), state(event.getEntity()));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        stop(event.getEntity(), state(event.getEntity()));
        var state = state(event.getEntity());
        state.putBoolean("jumpPressed", false);
        state.putBoolean("down", false);
    }

    private JetpackFlight() {}
}
