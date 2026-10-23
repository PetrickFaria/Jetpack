package br.com.petrickfaria.jetpack.client;

import br.com.petrickfaria.jetpack.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

/** Client playback for server-authoritative jetpack thrust state. */
public final class JetpackClientSounds {
    private static final Map<Integer, TurbineSound> ACTIVE = new HashMap<>();

    public static void setThrust(int entityId, boolean playing) {
        Minecraft mc = Minecraft.getInstance();
        TurbineSound current = ACTIVE.get(entityId);
        if (!playing) {
            if (current != null) current.fadeOut();
            return;
        }
        Entity entity = mc.level == null ? null : mc.level.getEntity(entityId);
        if (entity == null) return;
        if (current != null && !current.isStopped()) return;
        TurbineSound sound = new TurbineSound(entity);
        ACTIVE.put(entityId, sound);
        mc.getSoundManager().play(sound);
    }

    public static void stopAll() {
        for (TurbineSound sound : ACTIVE.values()) sound.fadeOut();
        ACTIVE.clear();
    }

    private static final class TurbineSound extends AbstractTickableSoundInstance {
        private static final float TARGET_VOLUME = 0.22F;
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
