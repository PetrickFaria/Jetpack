package br.com.petrickfaria.jetpack.client;

import br.com.petrickfaria.jetpack.JetpackMod;
import br.com.petrickfaria.jetpack.registry.ModMenus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JetpackMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class JetpackMenuScreens {
    @SubscribeEvent
    public static void register(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ModMenus.FUEL.get(), JetpackFuelScreen::new));
    }
    private JetpackMenuScreens() {}
}
