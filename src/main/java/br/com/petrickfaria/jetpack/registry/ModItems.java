package br.com.petrickfaria.jetpack.registry;

import br.com.petrickfaria.jetpack.JetpackMod;
import br.com.petrickfaria.jetpack.item.JetpackItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, JetpackMod.MOD_ID);
    public static final RegistryObject<JetpackItem> WOOD = register("wood", "Madeira", 5);
    public static final RegistryObject<JetpackItem> IRON = register("iron", "Ferro", 10);
    public static final RegistryObject<JetpackItem> GOLD = register("gold", "Ouro", 15);
    public static final RegistryObject<JetpackItem> EMERALD = register("emerald", "Esmeralda", 20);
    public static final RegistryObject<JetpackItem> DIAMOND = register("diamond", "Diamante", 25);
    public static final RegistryObject<JetpackItem> NETHERITE = register("netherite", "Netherita", 50);

    private static RegistryObject<JetpackItem> register(String name, String materialName, int maxHeight) {
        return ITEMS.register("jetpack_" + name, () -> new JetpackItem(maxHeight, new Item.Properties().stacksTo(1).tab(ModCreativeTabs.JETPACK), materialName));
    }
    private ModItems() {}
}
