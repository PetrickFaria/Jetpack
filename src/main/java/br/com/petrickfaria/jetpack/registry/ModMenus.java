package br.com.petrickfaria.jetpack.registry;

import br.com.petrickfaria.jetpack.JetpackMod;
import br.com.petrickfaria.jetpack.menu.JetpackFuelMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, JetpackMod.MOD_ID);
    public static final RegistryObject<MenuType<JetpackFuelMenu>> FUEL = MENUS.register("fuel", () -> IForgeMenuType.create(JetpackFuelMenu::new));
    private ModMenus() {}
}
