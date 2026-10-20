package br.com.petrickfaria.jetpack.player;

import br.com.petrickfaria.jetpack.JetpackMod;
import br.com.petrickfaria.jetpack.item.JetpackItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Server-owned furnace-style ER charging state, saved on the jetpack item itself. */
@Mod.EventBusSubscriber(modid = JetpackMod.MOD_ID)
public final class JetpackFueling {
    private static final String FUEL_TAG = "JetpackFuel";
    private static final String BURN_LEFT = "JetpackBurnLeft";
    private static final String BURN_TOTAL = "JetpackBurnTotal";
    private static final String BURN_VALUE = "JetpackBurnEnergy";
    private static final String BURN_FRACTION = "JetpackBurnFraction";
    private static final double FE_PER_BURN_TICK = JetpackItem.CAPACITY / 1600.0D;

    public static double energyForBurnTicks(int burnTicks) {
        return Math.max(0, burnTicks) * FE_PER_BURN_TICK;
    }

    public static boolean isFuel(ItemStack stack) {
        return !stack.isEmpty() && !stack.getItem().hasCraftingRemainingItem()
                && ForgeHooks.getBurnTime(stack, RecipeType.SMELTING) > 0;
    }

    public static ItemStack getFuel(ItemStack jetpack) {
        CompoundTag tag = jetpack.getTag();
        return tag != null && tag.contains(FUEL_TAG, CompoundTag.TAG_COMPOUND)
                ? ItemStack.of(tag.getCompound(FUEL_TAG)) : ItemStack.EMPTY;
    }

    public static void setFuel(Player player, UUID id, ItemStack fuel) {
        if (!fuel.isEmpty() && !isFuel(fuel)) return;
        ItemStack host = findHost(player, id);
        if (host.isEmpty()) return;
        CompoundTag tag = host.getOrCreateTag();
        if (fuel.isEmpty()) tag.remove(FUEL_TAG);
        else {
            ItemStack one = fuel.copy();
            one.setCount(Math.min(one.getCount(), one.getMaxStackSize()));
            tag.put(FUEL_TAG, one.save(new CompoundTag()));
        }
        saveHost(player, id, host);
    }

    public static ItemStack findHost(Player player, UUID id) {
        ItemStack equipped = JetpackEquipment.get(player);
        if (matches(equipped, id)) return equipped;
        for (ItemStack stack : player.getInventory().items) if (matches(stack, id)) return stack;
        for (ItemStack stack : player.getInventory().offhand) if (matches(stack, id)) return stack;
        return ItemStack.EMPTY;
    }

    public static void saveHost(Player player, UUID id, ItemStack host) {
        ItemStack equipped = JetpackEquipment.get(player);
        if (matches(equipped, id)) {
            JetpackEquipment.save(player, host);
            return;
        }
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            if (matches(player.getInventory().items.get(i), id)) {
                player.getInventory().setItem(i, host);
                return;
            }
        }
        for (int i = 0; i < player.getInventory().offhand.size(); i++) {
            if (matches(player.getInventory().offhand.get(i), id)) {
                player.getInventory().offhand.set(i, host);
                player.getInventory().setChanged();
                return;
            }
        }
    }

    private static boolean matches(ItemStack stack, UUID id) {
        return !stack.isEmpty() && stack.getItem() instanceof JetpackItem
                && stack.hasTag() && stack.getTag().hasUUID(JetpackItem.INSTANCE_ID)
                && id.equals(stack.getTag().getUUID(JetpackItem.INSTANCE_ID));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || event.side.isClient() || !(event.player instanceof ServerPlayer)) return;
        Player player = event.player;
        Set<UUID> seen = new HashSet<>();
        ItemStack equipped = JetpackEquipment.get(player);
        if (!equipped.isEmpty() && hasFuelState(equipped)) tickHost(player, equipped, seen);
        for (ItemStack stack : player.getInventory().items) if (hasFuelState(stack)) tickHost(player, stack, seen);
        for (ItemStack stack : player.getInventory().offhand) if (hasFuelState(stack)) tickHost(player, stack, seen);
    }

    private static boolean hasFuelState(ItemStack stack) {
        return stack.getItem() instanceof JetpackItem && stack.hasTag()
                && (stack.getTag().contains(FUEL_TAG) || stack.getTag().contains(BURN_LEFT));
    }

    private static void tickHost(Player player, ItemStack host, Set<UUID> seen) {
        CompoundTag tag = host.getOrCreateTag();
        UUID id = tag.hasUUID(JetpackItem.INSTANCE_ID) ? tag.getUUID(JetpackItem.INSTANCE_ID) : null;
        if (id != null && !seen.add(id)) return;
        if (JetpackItem.energy(host) >= JetpackItem.CAPACITY) return;

        int left = tag.getInt(BURN_LEFT);
        int total = tag.getInt(BURN_TOTAL);
        double budget = tag.getDouble(BURN_VALUE);
        if (left <= 0) {
            ItemStack fuel = getFuel(host);
            if (!isFuel(fuel)) return;
            int burnTicks = ForgeHooks.getBurnTime(fuel, RecipeType.SMELTING);
            fuel.shrink(1);
            if (fuel.isEmpty()) tag.remove(FUEL_TAG);
            else tag.put(FUEL_TAG, fuel.save(new CompoundTag()));
            left = total = burnTicks;
            budget = Math.min(JetpackItem.CAPACITY - JetpackItem.energy(host), energyForBurnTicks(burnTicks));
        }

        double fraction = tag.getDouble(BURN_FRACTION) + budget / Math.max(1, total);
        int whole = (int) fraction;
        if (whole > 0) {
            int received = JetpackItem.receive(host, whole, false);
            fraction -= received;
        }
        left--;
        tag.putInt(BURN_LEFT, left);
        tag.putInt(BURN_TOTAL, total);
        tag.putDouble(BURN_VALUE, budget);
        tag.putDouble(BURN_FRACTION, fraction);

        if (id != null) saveHost(player, id, host);
        else if (JetpackEquipment.get(player).getItem() instanceof JetpackItem && host == equippedHost(player))
            JetpackEquipment.save(player, host);
        else player.getInventory().setChanged();
    }

    private static ItemStack equippedHost(Player player) { return JetpackEquipment.get(player); }

    private JetpackFueling() {}
}
