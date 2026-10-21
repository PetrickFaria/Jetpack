package br.com.petrickfaria.jetpack.player;

import br.com.petrickfaria.jetpack.item.JetpackItem;
import br.com.petrickfaria.jetpack.network.JetpackNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.mojang.math.Vector3f;

@Mod.EventBusSubscriber(modid = "jetpack")
public final class JetpackFlight {
    private static final String FLIGHT_KEY = "jetpack_flight";
    private static final int SURFACE_SEARCH_DEPTH = 128;
    private static final double SLOW_DESCENT_SPEED = 0.025D;
    private static final double TERRAIN_CORRECTION_SPEED = 0.08D;
    private static final float CREATIVE_FLY_SPEED = 0.05F;
    private static final DustParticleOptions RED_THRUST =
            new DustParticleOptions(new Vector3f(0.95F, 0.025F, 0.015F), 0.8F);

    public static void handleInput(ServerPlayer player, boolean toggle, boolean up, boolean down, float strafe, float forward) {
        var state = state(player);
        boolean jumpEdge = up && !state.getBoolean("jumpPressed");
        state.putBoolean("jumpPressed", up);
        state.putBoolean("down", down);
        if (jumpEdge) {
            long now = System.currentTimeMillis();
            long last = state.getLong("lastJumpAt");
            boolean doubleTap = last > 0L && now - last <= 300L;
            state.putLong("lastJumpAt", doubleTap ? 0L : now);
            if (doubleTap) {
                if (state.getBoolean("active")) stop(player, state);
                else start(player, state);
            }
        }
    }

    private static void start(ServerPlayer player, net.minecraft.nbt.CompoundTag state) {
        if (player.isCreative() || player.isSpectator()) return;
        ItemStackView pack = equippedPack(player);
        if (pack.item == null || pack.energy <= 0) return;

        double surface = findSurfaceBelow(player);
        if (!Double.isFinite(surface) || player.getY() > altitudeCeiling(surface, pack.item.maximumHeight())) return;

        var abilities = player.getAbilities();
        state.putDouble("surfaceY", surface);
        state.putDouble("ceiling", altitudeCeiling(surface, pack.item.maximumHeight()));
        state.putDouble("syncedCeiling", altitudeCeiling(surface, pack.item.maximumHeight()));
        state.putBoolean("active", true);
        state.putBoolean("previousMayfly", abilities.mayfly);
        state.putBoolean("previousFlying", abilities.flying);
        state.putFloat("previousFlySpeed", abilities.getFlyingSpeed());
        state.putBoolean("previousSprinting", player.isSprinting());
        abilities.mayfly = true;
        abilities.flying = true;
        abilities.setFlyingSpeed(CREATIVE_FLY_SPEED);
        player.setSprinting(false);
        player.onUpdateAbilities();
        player.fallDistance = 0;
        JetpackNetwork.syncFlightState(player, true, altitudeCeiling(surface, pack.item.maximumHeight()));
    }

    private static ItemStackView equippedPack(Player player) {
        var stack = JetpackEquipment.get(player);
        return stack.getItem() instanceof JetpackItem item
                ? new ItemStackView(item, JetpackItem.energy(stack)) : new ItemStackView(null, 0);
    }

    private static void stop(Player player, net.minecraft.nbt.CompoundTag state) {
        if (!state.getBoolean("active")) return;
        state.putBoolean("active", false);
        state.putBoolean("down", false);
        // A game-mode change owns its abilities; do not overwrite Creative/Spectator permissions.
        if (!player.isCreative() && !player.isSpectator()) {
            var abilities = player.getAbilities();
            abilities.mayfly = state.getBoolean("previousMayfly");
            abilities.flying = state.getBoolean("previousFlying") && abilities.mayfly;
            abilities.setFlyingSpeed(state.getFloat("previousFlySpeed"));
            player.setSprinting(state.getBoolean("previousSprinting"));
        }
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
        if (state.getBoolean("active")) tickPoweredFlight(player, state);
        else tickControlledDescent(player, state);
    }

    private static void tickPoweredFlight(Player player, net.minecraft.nbt.CompoundTag state) {
        ItemStackView equipped = equippedPack(player);
        if (equipped.item == null || equipped.energy <= 0) {
            stop(player, state);
            return;
        }

        // Sample the support directly below the player. It follows terrain rather than the takeoff Y.
        if (player.tickCount % 3 == 0) {
            double surface = findSurfaceBelow(player);
            if (Double.isFinite(surface)) state.putDouble("surfaceY", surface);
        }
        double surface = state.contains("surfaceY") ? state.getDouble("surfaceY") : findSurfaceBelow(player);
        if (!Double.isFinite(surface)) {
            // Safety fallback: retain the last validated surface; if none exists, disable flight.
            stop(player, state);
            return;
        }
        double ceiling = altitudeCeiling(surface, equipped.item.maximumHeight());
        state.putDouble("ceiling", ceiling);
        double synced = state.getDouble("syncedCeiling");
        if (Math.abs(ceiling - synced) >= 0.25D && player instanceof ServerPlayer serverPlayer) {
            state.putDouble("syncedCeiling", ceiling);
            JetpackNetwork.syncFlightState(serverPlayer, true, ceiling);
        }

        Vec3 motion = player.getDeltaMovement();
        Vec3 constrained = constrainFlightMotion(motion, state.getBoolean("down"), player.isOnGround(), player.getY(), ceiling);
        if (!constrained.equals(motion)) player.setDeltaMovement(constrained);

        var pack = JetpackEquipment.get(player);
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

    /** Slow, energy-backed descent while the main flight toggle is off. */
    private static void tickControlledDescent(Player player, net.minecraft.nbt.CompoundTag state) {
        if (!state.getBoolean("down") || player.isOnGround()) return;
        var pack = JetpackEquipment.get(player);
        if (!(pack.getItem() instanceof JetpackItem) || JetpackItem.energy(pack) <= 0) return;
        Vec3 motion = player.getDeltaMovement();
        Vec3 adjusted = controlledDescentMotion(motion, true, false);
        player.setDeltaMovement(adjusted);
        if (JetpackItem.consume(pack, 1, false) == 1) {
            JetpackEquipment.save(player, pack);
            player.fallDistance = 0;
        }
    }

    public static double altitudeCeiling(double surfaceY, int maximumHeight) {
        return surfaceY + maximumHeight;
    }

    public static Vec3 constrainFlightMotion(Vec3 motion, boolean down, boolean onGround, double y, double ceiling) {
        if (onGround) return motion;
        if (down) return controlledDescentMotion(motion, true, false);
        if (y > ceiling) {
            // A newly lower terrain surface lowers the cap gradually, without teleporting the player.
            return new Vec3(motion.x, Mth.clamp(motion.y, -TERRAIN_CORRECTION_SPEED, -SLOW_DESCENT_SPEED), motion.z);
        }
        if (y >= ceiling && motion.y > 0.0D) return new Vec3(motion.x, 0.0D, motion.z);
        return motion;
    }

    public static Vec3 controlledDescentMotion(Vec3 motion, boolean down, boolean onGround) {
        return down && !onGround ? new Vec3(motion.x, -SLOW_DESCENT_SPEED, motion.z) : motion;
    }

    private static void emitThrusterParticles(ServerPlayer player) {
        if (!(player.level instanceof ServerLevel level)) return;
        double yaw = Math.toRadians(player.getYRot());
        // In the torso model coordinates each nozzle exits at x=+/-3.55, y=10.7, z=4.5 pixels.
        double localX = 3.55D / 16.0D;
        double back = 4.5D / 16.0D;
        double sideX = Math.cos(yaw) * localX;
        double sideZ = Math.sin(yaw) * localX;
        double backX = Math.sin(yaw) * back;
        double backZ = -Math.cos(yaw) * back;
        double outletY = player.getY() + 1.5D - 10.7D / 16.0D;
        for (int side : new int[] {-1, 1}) {
            double x = player.getX() + backX + side * sideX;
            double z = player.getZ() + backZ + side * sideZ;
            level.sendParticles(RED_THRUST, x, outletY, z, 1, 0.012D, -0.045D, 0.012D, 0.008D);
            level.sendParticles(ParticleTypes.SMOKE, x, outletY, z, 1, 0.02D, -0.02D, 0.02D, 0.006D);
        }
    }

    /**
     * Highest physical support surface directly beneath the player's horizontal center.
     * Full blocks, slabs, stairs and waterlogged solids qualify; fluid-only blocks and leaves do not.
     */
    public static double findSurfaceBelow(Player player) {
        int x = Mth.floor(player.getX());
        int z = Mth.floor(player.getZ());
        int startY = Math.min(Mth.floor(player.getY() + 1.0E-4D), player.level.getMaxBuildHeight() - 1);
        int minY = Math.max(player.level.getMinBuildHeight(), startY - SURFACE_SEARCH_DEPTH);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, startY, z);
        if (!player.level.hasChunkAt(pos)) return Double.NaN;
        double localX = player.getX() - x;
        double localZ = player.getZ() - z;
        double feetY = player.getY() + 0.02D;
        for (int y = startY; y >= minY; y--) {
            pos.set(x, y, z);
            BlockState block = player.level.getBlockState(pos);
            if (block.isAir() || block.is(BlockTags.LEAVES)) continue;
            VoxelShape shape = block.getCollisionShape(player.level, pos);
            if (shape.isEmpty()) continue;
            double highestTopInBlock = Double.NEGATIVE_INFINITY;
            for (AABB box : shape.toAabbs()) {
                if (localX + 1.0E-5D < box.minX || localX - 1.0E-5D > box.maxX
                        || localZ + 1.0E-5D < box.minZ || localZ - 1.0E-5D > box.maxZ) continue;
                double top = y + box.maxY;
                if (top <= feetY) highestTopInBlock = Math.max(highestTopInBlock, top);
            }
            if (Double.isFinite(highestTopInBlock)) return highestTopInBlock;
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

    private record ItemStackView(JetpackItem item, int energy) {}
    private JetpackFlight() {}
}
