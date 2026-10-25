package br.com.petrickfaria.jetpack.client;

import br.com.petrickfaria.jetpack.registry.ModSounds;
import br.com.petrickfaria.jetpack.JetpackMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/** Client playback for server-authoritative jetpack thrust state. */
@Mod.EventBusSubscriber(modid = JetpackMod.MOD_ID, value = Dist.CLIENT)
public final class JetpackClientSounds {
    private static final Map<Integer, TurbineSound> ACTIVE = new HashMap<>();
    private static final boolean DEBUG = Boolean.getBoolean("jetpack.soundDebug");
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final Map<Integer, Boolean> DESIRED = new HashMap<>();
    private static final Map<Integer, Boolean> PENDING = new HashMap<>();

    public static void setThrust(int entityId, boolean playing) {
        Minecraft mc = Minecraft.getInstance();
        boolean changed = DESIRED.containsKey(entityId) != playing;
        if (playing) DESIRED.put(entityId, true); else DESIRED.remove(entityId);
        if (DEBUG && changed) LOGGER.info("Jetpack sound desired entity={} thrust={}", entityId, playing);
        TurbineSound current = ACTIVE.get(entityId);
        PlaybackAction action = playbackAction(playing, current != null,
                current == null || current.isStopped());
        if (!playing) {
            PENDING.remove(entityId);
            if (action == PlaybackAction.FADE_OUT) current.fadeOut();
            return;
        }
        Entity entity = mc.level == null ? null : mc.level.getEntity(entityId);
        if (entity == null) {
            PENDING.put(entityId, true);
            return;
        }
        PENDING.remove(entityId);
        if (action == PlaybackAction.RESUME) {
            current.resume();
            return;
        }
        TurbineSound sound = new TurbineSound(entity);
        ACTIVE.put(entityId, sound);
        mc.getSoundManager().play(sound);
        if (DEBUG) LOGGER.info("Jetpack sound submitted entity={} category=PLAYERS volume={}", entityId, sound.volumeForDiagnostic());
    }

    static PlaybackAction playbackAction(boolean playing, boolean hasCurrent, boolean currentStopped) {
        if (!playing) return hasCurrent ? PlaybackAction.FADE_OUT : PlaybackAction.NO_OP;
        return hasCurrent && !currentStopped ? PlaybackAction.RESUME : PlaybackAction.START;
    }

    enum PlaybackAction { NO_OP, START, RESUME, FADE_OUT }

    public static void stopAll() {
        for (TurbineSound sound : ACTIVE.values()) sound.fadeOut();
        ACTIVE.clear();
        PENDING.clear();
        DESIRED.clear();
    }

    @SubscribeEvent
    public static void retryPendingSounds(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { stopAll(); return; }
        if (mc.isPaused()) return;
        // Flight snapshots are already server-authoritative. Reconcile the local
        // engine as well as the dedicated edge-triggered sound packet.
        if (mc.player != null) {
            int id = mc.player.getId();
            var pack = JetpackClientState.getEquipment(id);
            boolean equipped = pack.getItem() instanceof br.com.petrickfaria.jetpack.item.JetpackItem;
            boolean energy = equipped && br.com.petrickfaria.jetpack.item.JetpackItem.energy(pack) > 0;
            boolean thrust = !mc.player.isCreative() && !mc.player.isSpectator()
                    && br.com.petrickfaria.jetpack.player.JetpackFlight.shouldPlayThrustSound(
                    JetpackClientState.getFlight(id).active(), energy,
                    mc.options.keyShift.isDown(), mc.player.isOnGround());
            setThrust(id, thrust);
        }
        for (int id : java.util.List.copyOf(DESIRED.keySet())) {
            TurbineSound sound = ACTIVE.get(id);
            if (sound != null) sound.age++;
            if (sound != null && shouldRestartLoop(sound.age, mc.getSoundManager().isActive(sound))) {
                if (DEBUG) LOGGER.info("Jetpack sound missing from engine entity={}, retrying", id);
                mc.getSoundManager().stop(sound);
                ACTIVE.remove(id);
            }
            setThrust(id, true);
        }
    }

    static boolean shouldRestartLoop(int age, boolean engineActive) {
        return age >= 40 && !engineActive;
    }

    private static final class TurbineSound extends AbstractTickableSoundInstance {
        private static final float TARGET_VOLUME = 1.0F; // Engine gain cap; OGG has 1.9x gain: 1.9 / 0.38 = 5x.
        private static final float FADE_STEP = TARGET_VOLUME / 6.0F;
        private final Entity entity;
        private boolean fadingOut;
        private int age;
        private float volumeForDiagnostic() { return volume; }

        private TurbineSound(Entity entity) {
            super(ModSounds.JETPACK_TURBINE.get(), SoundSource.PLAYERS, RandomSource.create());
            this.entity = entity;
            this.looping = true;
            this.delay = 0;
            this.volume = FADE_STEP;
            this.pitch = 1.0F;
            this.attenuation = Attenuation.LINEAR;
            updatePosition();
        }

        @Override public boolean canStartSilent() { return true; }

        private void fadeOut() { fadingOut = true; }
        private void resume() { fadingOut = false; }

        @Override
        public void tick() {
            if (DEBUG && age == 20) LOGGER.info("Jetpack sound ticked entity={} volume={} fading={}", entity.getId(), volume, fadingOut);
            if (entity.isRemoved()) {
                ACTIVE.values().remove(this);
                stop();
                return;
            }
            updatePosition();
            if (fadingOut) {
                volume = Math.max(0.0F, volume - FADE_STEP);
                if (volume <= 0.0F) {
                    ACTIVE.values().remove(this);
                    stop();
                }
            } else {
                volume = Math.min(TARGET_VOLUME, volume + FADE_STEP);
            }
        }

        private void updatePosition() {
            x = entity.getX();
            y = entity.getY() + entity.getBbHeight() * 0.55D;
            z = entity.getZ();
        }
    }

    private JetpackClientSounds() {}
}
