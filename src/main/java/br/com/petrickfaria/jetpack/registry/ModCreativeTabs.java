package br.com.petrickfaria.jetpack.registry;

import br.com.petrickfaria.jetpack.JetpackMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/** Registers the dedicated creative inventory tab for the six jetpacks. */
public final class ModCreativeTabs {
    private ModCreativeTabs() {}

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModCreativeTabs::registerCreativeTab);
    }

    private static void registerCreativeTab(CreativeModeTabEvent.Register event) {
        event.registerCreativeModeTab(new ResourceLocation(JetpackMod.MOD_ID, "jetpack"), builder ->
                builder.title(Component.translatable("item_group.jetpack.jetpack"))
                        .icon(() -> new ItemStack(ModItems.WOOD.get()))
                        .displayItems((parameters, output) -> {
                            output.accept(ModItems.WOOD.get());
                            output.accept(ModItems.IRON.get());
                            output.accept(ModItems.GOLD.get());
                            output.accept(ModItems.EMERALD.get());
                            output.accept(ModItems.DIAMOND.get());
                            output.accept(ModItems.NETHERITE.get());
                        }));
    }
}
