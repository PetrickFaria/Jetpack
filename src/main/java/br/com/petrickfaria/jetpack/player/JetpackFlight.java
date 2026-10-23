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
    public static final int POWERED_ENERGY_PER_TICK = 1;
    public static final double CEILING_POSITION_TOLERANCE = 0.2D;
    private static final double SLOW_DESCENT_SPEED = 0.025D;
    // Player.travel applies the 0.3 crouch input multiplier before airborne travel.
    // Shift remains held during controlled descent, so compensate for that effective
    // air input rather than the uncrouched 0.02 value.
    private static final double CROUCHED_AIRBORNE_INPUT_SPEED = (double) (0.02F * 0.3F);
    private static final double FLIGHT_HORIZONTAL_DRAG = (double) 0.91F;
    // Vanilla LocalPlayer adds 3 * flyingSpeed to vertical motion while jump is held.
    // Creative Player.travel retains 60% of that motion after moving, so sustained
    // ascent displacement converges to input / (1 - 0.6).
    private static final double CREATIVE_VERTICAL_INPUT_MULTIPLIER = 3.0D;
    private static final double CREATIVE_VERTICAL_RETAINED_SPEED = 0.6D;
    private static final float CREATIVE_FLY_SPEED = 0.05F;
    private static final DustParticleOptions RED_THRUST =
            new DustParticleOptions(new Vector3f(0.95F, 0.025F, 0.015F), 0.8F);

    public static void handleInput(ServerPlayer player, boolean toggle, boolean up, boolean down, float strafe, float forward) {
        var state = state(player);
        state.putBoolean("up", up);
        state.putBoolean("down", down);
        state.putFloat("strafe", strafe);
        state.putFloat("forward", forward);
        // The client sends one toggle only after its double-tap detector fires.
        // Do not independently time the same taps on the server: packet timing can
        // differ enough to miss a valid retoggle during a fall.
        if (toggle) {
            if (state.getBoolean("active")) stop(player, state);
            else start(player, state);
        }
    }

    private static void start(ServerPlayer player, net.minecraft.nbt.CompoundTag state) {
        if (player.isCreative() || player.isSpectator()) return;
        ItemStackView pack = equippedPack(player);
        if (pack.item == null || pack.energy <= 0) return;

        double surface = findSurfaceBelow(player);
        if (!Double.isFinite(surface)) return;

        var abilities = player.getAbilities();
        state.putDouble("surfaceY", surface);
        state.putDouble("ceiling", altitudeCeiling(surface, pack.item.maximumHeight()));
        state.putDouble("syncedCeiling", altitudeCeiling(surface, pack.item.maximumHeight()));
        state.putDouble("lastFlightY", player.getY());
        state.putDouble("ascentSpeed", 0.0D);
        state.putBoolean("terrainCorrection", player.getY() > altitudeCeiling(surface, pack.item.maximumHeight()));
        state.putBoolean("active", true);
        state.putBoolean("previousMayfly", abilities.mayfly);
        state.putBoolean("previousFlying", abilities.flying);
        state.putFloat("previousFlySpeed", abilities.getFlyingSpeed());
        abilities.mayfly = true;
        abilities.flying = true;
        abilities.setFlyingSpeed(CREATIVE_FLY_SPEED);
        Vec3 motion = player.getDeltaMovement();
        if (motion.y < 0.0D) player.setDeltaMovement(motion.x, 0.0D, motion.z);
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
            syncThrustSound(player, state, false);
            return;
        }
        if (state.getBoolean("active")) tickPoweredFlight(player, state);
        else tickControlledDescent(player, state);
        boolean hasInput = state.getBoolean("up") || state.getBoolean("down")
                || state.getFloat("strafe") != 0.0F || state.getFloat("forward") != 0.0F;
        boolean active = state.getBoolean("active");
        boolean thrusting = active && (hasInput || state.getBoolean("terrainCorrection"))
                || !active && state.getBoolean("down") && !player.isOnGround();
        thrusting &= equippedPack(player).energy > 0;
        syncThrustSound(player, state, thrusting);
    }

    private static void tickPoweredFlight(Player player, net.minecraft.nbt.CompoundTag state) {
        if (shouldStopOnLanding(state.getBoolean("down"), player.isOnGround())) {
            stop(player, state);
            return;
        }
        ItemStackView equipped = equippedPack(player);
        if (equipped.item == null || equipped.energy <= 0) {
            stop(player, state);
            return;
        }

        double previousY = state.getDouble("lastFlightY");
        boolean hasPreviousY = state.contains("lastFlightY");
        double previousCeiling = state.getDouble("ceiling");
        boolean hadCeiling = state.contains("ceiling");

        // Measure the actual ascent displacement after vanilla has moved the player.
        // This includes the jump impulse, retained vertical momentum, tick ordering,
        // and any server-side movement processing instead of assuming flySpeed is distance.
        double currentY = player.getY();
        boolean newlyLoweredTerrain = state.getBoolean("terrainCorrection");
        if (hadCeiling && state.contains("surfaceY") && Double.isFinite(currentY)
                && state.getBoolean("up") && !state.getBoolean("down") && !player.isOnGround()
                && hasPreviousY && !newlyLoweredTerrain
                && previousY < previousCeiling - CEILING_POSITION_TOLERANCE
                && currentY > previousY + 1.0E-4D
                && currentY < previousCeiling - CEILING_POSITION_TOLERANCE) {
            double measuredAscent = currentY - previousY;
            double nativeSteadyAscent = creativeSteadyAscentSpeed(player.getAbilities().getFlyingSpeed());
            state.putDouble("ascentSpeed", Math.max(state.getDouble("ascentSpeed"),
                    Math.min(measuredAscent, nativeSteadyAscent)));
        }

        // Sample the support directly below the player. It follows terrain rather than the takeoff Y.
        double sampledSurface = findSurfaceBelow(player);
        if (Double.isFinite(sampledSurface)) state.putDouble("surfaceY", sampledSurface);
        double surface = state.contains("surfaceY") ? state.getDouble("surfaceY") : findSurfaceBelow(player);
        if (!Double.isFinite(surface)) {
            // Safety fallback: retain the last validated surface; if none exists, disable flight.
            stop(player, state);
            return;
        }
        double ceiling = altitudeCeiling(surface, equipped.item.maximumHeight());
        if (hadCeiling && ceiling < previousCeiling - 1.0E-4D && player.getY() > ceiling)
            state.putBoolean("terrainCorrection", true);
        if (player.getY() <= ceiling) state.putBoolean("terrainCorrection", false);
        state.putDouble("ceiling", ceiling);
        double synced = state.getDouble("syncedCeiling");
        if (Math.abs(ceiling - synced) >= 1.0E-3D && player instanceof ServerPlayer serverPlayer) {
            state.putDouble("syncedCeiling", ceiling);
            JetpackNetwork.syncFlightState(serverPlayer, true, ceiling);
        }

        Vec3 motion = player.getDeltaMovement();
        double ascentSpeed = state.getDouble("ascentSpeed");
        if (ascentSpeed <= 0.0D)
            ascentSpeed = creativeSteadyAscentSpeed(player.getAbilities().getFlyingSpeed());
        Vec3 constrained = constrainFlightMotion(motion, state.getBoolean("down"), player.isOnGround(),
                player.getY(), ceiling, ascentSpeed);
        if (!constrained.equals(motion)) player.setDeltaMovement(constrained);
        enforceCeilingPosition(player, ceiling);
        if (player instanceof ServerPlayer serverPlayer) {
            enforceServerCeilingPosition(serverPlayer, state, ceiling);
            state.putDouble("lastFlightY", serverPlayer.getY());
        }

        var pack = JetpackEquipment.get(player);
        if (JetpackItem.consume(pack, POWERED_ENERGY_PER_TICK, false) != POWERED_ENERGY_PER_TICK) {
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
        Vec3 adjusted = controlledDescentMotion(motion, true, false,
                state.getFloat("strafe"), state.getFloat("forward"), player.getYRot());
        player.setDeltaMovement(adjusted);
        if (JetpackItem.consume(pack, 1, false) == 1) {
            JetpackEquipment.save(player, pack);
            player.fallDistance = 0;
        }
    }

    public static double altitudeCeiling(double surfaceY, int maximumHeight) {
        return surfaceY + maximumHeight;
    }

    public static double creativeSteadyAscentSpeed(double flyingSpeed) {
        double verticalInput = flyingSpeed * CREATIVE_VERTICAL_INPUT_MULTIPLIER;
        return verticalInput / (1.0D - CREATIVE_VERTICAL_RETAINED_SPEED);
    }

    public static Vec3 constrainFlightMotion(Vec3 motion, boolean down, boolean onGround,
                                               double y, double ceiling) {
        return constrainFlightMotion(motion, down, onGround, y, ceiling,
                creativeSteadyAscentSpeed(CREATIVE_FLY_SPEED));
    }

    public static Vec3 constrainFlightMotion(Vec3 motion, boolean down, boolean onGround,
                                               double y, double ceiling, double ascentSpeed) {
        if (onGround) return motion;
        // Do not replace Creative's native manual descent with the separate slow-descent
        // assist used after powered flight is switched off.
        if (y > ceiling) {
            // ascentSpeed is a measured per-tick displacement, not flyingSpeed. The
            // next travel step moves by this vertical component before retaining momentum.
            double remainingHeight = y - ceiling;
            double correctionSpeed = Math.min(Math.max(0.0D, ascentSpeed), remainingHeight);
            return new Vec3(motion.x, -correctionSpeed, motion.z);
        }
        if (y >= ceiling - 1.0E-5D && motion.y > 0.0D) return new Vec3(motion.x, 0.0D, motion.z);
        return motion;
    }

    /** Suppress native upward input at the boundary without changing horizontal or down movement. */
    public static boolean shouldSuppressAscent(boolean jumping, double y, double ceiling) {
        return jumping && y >= ceiling - CEILING_POSITION_TOLERANCE;
    }

    public static boolean shouldStopOnLanding(boolean shiftDown, boolean onGround) {
        return shiftDown && onGround;
    }

    /** Correct a one-step overshoot; larger gaps are terrain changes and descend gradually. */
    public static double correctedCeilingY(double y, double ceiling) {
        return y > ceiling && y - ceiling <= CEILING_POSITION_TOLERANCE ? ceiling : y;
    }

    public static double authoritativeCeilingY(double y, double ceiling, boolean gradualTerrainCorrection) {
        if (y <= ceiling) return y;
        return gradualTerrainCorrection && y - ceiling > CEILING_POSITION_TOLERANCE ? y : ceiling;
    }

    public static void enforceCeilingPosition(Player player, double ceiling) {
        double y = player.getY();
        double correctedY = correctedCeilingY(y, ceiling);
        if (correctedY == y) return;
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, motion.y > 0.0D ? 0.0D : motion.y, motion.z);
        if (player instanceof ServerPlayer serverPlayer) {
            // Authoritative correction is limited to a single normal flight step.
            serverPlayer.teleportTo(player.getX(), correctedY, player.getZ());
        } else {
            player.setPos(player.getX(), correctedY, player.getZ());
        }
    }

    private static void enforceServerCeilingPosition(ServerPlayer player,
                                                       net.minecraft.nbt.CompoundTag state,
                                                       double ceiling) {
        double y = player.getY();
        boolean gradualTerrainCorrection = state.getBoolean("terrainCorrection");
        if (gradualTerrainCorrection && y - ceiling > CEILING_POSITION_TOLERANCE) {
            double previousY = state.getDouble("lastFlightY");
            if (state.contains("lastFlightY") && y > previousY + 1.0E-4D) {
                Vec3 motion = player.getDeltaMovement();
                player.setDeltaMovement(motion.x, 0.0D, motion.z);
                player.teleportTo(player.getX(), previousY, player.getZ());
            }
            return;
        }
        double correctedY = authoritativeCeilingY(y, ceiling, false);
        if (correctedY == y) return;
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, motion.y > 0.0D ? 0.0D : motion.y, motion.z);
        // Position packets above an unchanged limit are rejected immediately. A newly
        // lowered terrain ceiling is the sole case corrected with the gentle descent.
        player.teleportTo(player.getX(), correctedY, player.getZ());
    }

    public static Vec3 controlledDescentMotion(Vec3 motion, boolean down, boolean onGround) {
        return down && !onGround ? new Vec3(motion.x, -SLOW_DESCENT_SPEED, motion.z) : motion;
    }

    /**
     * During Shift descent, airborne travel adds 0.02 * 0.3 horizontal input before
     * applying 0.91 drag. Creative flight adds the player's 0.05 flying input with
     * the same drag. Add only the missing post-travel delta and preserve controlled Y.
     */
    public static Vec3 controlledDescentMotion(Vec3 motion, boolean down, boolean onGround,
                                                float strafe, float forward, float yawDegrees) {
        if (!down || onGround) return motion;
        Vec3 input = normalizeMovementInput(strafe, forward).yRot((float) Math.toRadians(-yawDegrees));
        double missingAcceleration = (CREATIVE_FLY_SPEED - CROUCHED_AIRBORNE_INPUT_SPEED)
                * FLIGHT_HORIZONTAL_DRAG;
        return new Vec3(motion.x + input.x * missingAcceleration,
                -SLOW_DESCENT_SPEED,
                motion.z + input.z * missingAcceleration);
    }

    public static Vec3 normalizeMovementInput(float strafe, float forward) {
        Vec3 input = new Vec3(strafe, 0.0D, forward);
        double lengthSqr = input.lengthSqr();
        return lengthSqr < 1.0E-7D ? Vec3.ZERO : input.scale(1.0D / Math.max(1.0D, Math.sqrt(lengthSqr)));
    }

    private static void syncThrustSound(Player player, net.minecraft.nbt.CompoundTag state, boolean playing) {
        if (!(player instanceof ServerPlayer serverPlayer) || state.getBoolean("thrustSound") == playing) return;
        state.putBoolean("thrustSound", playing);
        JetpackNetwork.syncThrustSound(serverPlayer, playing);
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
        var state = state(event.getEntity());
        stop(event.getEntity(), state);
        syncThrustSound(event.getEntity(), state, false);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        stop(event.getEntity(), state(event.getEntity()));
        var state = state(event.getEntity());
        state.putBoolean("jumpPressed", false);
        state.putBoolean("up", false);
        state.putBoolean("down", false);
        state.putBoolean("thrustSound", false);
        state.putFloat("strafe", 0.0F);
        state.putFloat("forward", 0.0F);
    }

    private record ItemStackView(JetpackItem item, int energy) {}
    private JetpackFlight() {}
}
