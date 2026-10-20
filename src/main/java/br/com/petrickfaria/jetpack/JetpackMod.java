package br.com.petrickfaria.jetpack;

import br.com.petrickfaria.jetpack.registry.ModItems;
import br.com.petrickfaria.jetpack.network.JetpackNetwork;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(JetpackMod.MOD_ID)
public final class JetpackMod {
    public static final String MOD_ID = "jetpack";

    public JetpackMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(bus);
        JetpackNetwork.register();
    }
}
