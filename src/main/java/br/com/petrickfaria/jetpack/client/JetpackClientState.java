package br.com.petrickfaria.jetpack.client;

import net.minecraft.world.item.ItemStack;

/** Server-synchronized client snapshot used by the inventory accessory and HUD. */
public final class JetpackClientState {
    private static ItemStack equipment = ItemStack.EMPTY;

    public static void setEquipment(ItemStack stack) { equipment = stack.copy(); }
    public static ItemStack getEquipment() { return equipment.copy(); }
    public static void clear() { equipment = ItemStack.EMPTY; }

    private JetpackClientState() {}
}
