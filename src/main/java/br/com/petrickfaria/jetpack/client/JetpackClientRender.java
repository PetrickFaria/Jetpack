package br.com.petrickfaria.jetpack.client;

import br.com.petrickfaria.jetpack.JetpackMod;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JetpackMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class JetpackClientRender {
    @SubscribeEvent
    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(JetpackBackModel.LAYER, JetpackBackModel::createLayer);
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) renderer.addLayer(new JetpackBackLayer(renderer, event.getEntityModels()));
        }
    }

    @Mod.EventBusSubscriber(modid = JetpackMod.MOD_ID, value = Dist.CLIENT)
    public static final class ClientEvents {
        @SubscribeEvent public static void onEntityLeave(EntityLeaveLevelEvent event) {
            JetpackClientState.remove(event.getEntity().getId());
        }
    }
    private JetpackClientRender() {}
}
