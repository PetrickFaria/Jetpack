package br.com.petrickfaria.jetpack.registry;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/** Creative inventory tab for the six jetpacks. */
public final class ModCreativeTabs {
    public static final CreativeModeTab JETPACK = new CreativeModeTab("jetpack.jetpack") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(ModItems.WOOD.get());
        }
    };

    private ModCreativeTabs() {}
}
