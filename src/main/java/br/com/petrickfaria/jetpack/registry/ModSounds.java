package br.com.petrickfaria.jetpack.registry;

import br.com.petrickfaria.jetpack.JetpackMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, JetpackMod.MOD_ID);
    public static final RegistryObject<SoundEvent> JETPACK_TURBINE = SOUNDS.register("jetpack_turbine",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(JetpackMod.MOD_ID, "jetpack_turbine")));

    private ModSounds() {}
}
