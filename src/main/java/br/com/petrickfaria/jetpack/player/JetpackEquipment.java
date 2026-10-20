package br.com.petrickfaria.jetpack.player;

import br.com.petrickfaria.jetpack.item.JetpackItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Persistent accessory storage independent of the vanilla chest armor slot. */
public final class JetpackEquipment {
    private static final String EQUIPMENT_KEY = "jetpack_equipment";

    public static ItemStack get(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(EQUIPMENT_KEY, CompoundTag.TAG_COMPOUND)) return ItemStack.EMPTY;
        ItemStack stack = ItemStack.of(data.getCompound(EQUIPMENT_KEY));
        return stack.getItem() instanceof JetpackItem ? stack : ItemStack.EMPTY;
    }

    public static void set(Player player, ItemStack stack) {
        CompoundTag data = player.getPersistentData();
        if (stack.isEmpty() || !(stack.getItem() instanceof JetpackItem)) data.remove(EQUIPMENT_KEY);
        else {
            ItemStack one = stack.copy();
            one.setCount(1);
            data.put(EQUIPMENT_KEY, one.save(new CompoundTag()));
        }
    }

    public static void save(Player player, ItemStack stack) {
        if (stack.getItem() instanceof JetpackItem) set(player, stack);
    }

    private JetpackEquipment() {}
}
