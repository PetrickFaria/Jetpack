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
    private static final Map<Integer, Boolean> PENDING = new HashMap<>();

    public static void setThrust(int entityId, boolean playing) {
        Minecraft mc = Minecraft.getInstance();
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
    }

    @SubscribeEvent
    public static void retryPendingSounds(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            PENDING.clear();
            return;
        }
        for (int entityId : java.util.List.copyOf(PENDING.keySet())) {
            if (mc.level.getEntity(entityId) != null) setThrust(entityId, true);
        }
    }

    private static final class TurbineSound extends AbstractTickableSoundInstance {
        private static final float TARGET_VOLUME = 0.38F;
        private static final float FADE_STEP = TARGET_VOLUME / 6.0F;
        private final Entity entity;
        private boolean fadingOut;

        private TurbineSound(Entity entity) {
            super(ModSounds.JETPACK_TURBINE.get(), SoundSource.PLAYERS, RandomSource.create());
            this.entity = entity;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.0F;
            this.pitch = 1.0F;
            this.attenuation = Attenuation.LINEAR;
            updatePosition();
        }

        private void fadeOut() { fadingOut = true; }
        private void resume() { fadingOut = false; }

        @Override
        public void tick() {
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
