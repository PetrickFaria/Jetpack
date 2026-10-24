package br.com.petrickfaria.jetpack.menu;

import br.com.petrickfaria.jetpack.player.JetpackFueling;
import br.com.petrickfaria.jetpack.player.JetpackEquipment;
import br.com.petrickfaria.jetpack.item.JetpackItem;
import br.com.petrickfaria.jetpack.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import java.util.UUID;

public final class JetpackFuelMenu extends AbstractContainerMenu {
    public static final int FUEL_SLOT_X = 80;
    public static final int FUEL_SLOT_Y = 76;
    public static final int INVENTORY_SLOT_START_Y = 112;
    public static final int HOTBAR_SLOT_Y = 166;
    private final Inventory playerInventory;
    private final UUID jetpackId;
    private final ItemStack clientSnapshot;
    private int clientEnergy;
    private int clientBurnLeft;
    private int clientBurnTotal;

    public JetpackFuelMenu(int id, Inventory inventory, UUID jetpackId) {
        this(id, inventory, jetpackId, ItemStack.EMPTY);
    }

    public JetpackFuelMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, buf.readUUID(), buf.readItem());
    }

    private JetpackFuelMenu(int id, Inventory inventory, UUID jetpackId, ItemStack snapshot) {
        super(ModMenus.FUEL.get(), id);
        this.playerInventory = inventory;
        this.jetpackId = jetpackId;
        this.clientSnapshot = snapshot.copy();

        this.addSlot(new Slot(new FuelSlotContainer(inventory.player, jetpackId), 0, FUEL_SLOT_X, FUEL_SLOT_Y) {
            @Override public boolean mayPlace(ItemStack stack) { return JetpackFueling.isFuel(stack); }
            @Override public int getMaxStackSize() { return 64; }
        });

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_SLOT_START_Y + row * 18));
        for (int col = 0; col < 9; col++) this.addSlot(new Slot(inventory, col, 8 + col * 18, HOTBAR_SLOT_Y));

        this.addDataSlot(new DataSlot() {
            @Override public int get() { return getHost().isEmpty() ? 0 : JetpackItem.energy(getHost()); }
            @Override public void set(int value) { clientEnergy = value; }
        });
        this.addDataSlot(new DataSlot() {
            @Override public int get() { return getHost().isEmpty() ? 0 : getHost().getOrCreateTag().getInt("JetpackBurnLeft"); }
            @Override public void set(int value) { clientBurnLeft = value; }
        });
        this.addDataSlot(new DataSlot() {
            @Override public int get() { return getHost().isEmpty() ? 0 : getHost().getOrCreateTag().getInt("JetpackBurnTotal"); }
            @Override public void set(int value) { clientBurnTotal = value; }
        });
    }

    private ItemStack getHost() {
        Player player = playerInventory.player;
        ItemStack found = JetpackFueling.findHost(player, jetpackId);
        return found.isEmpty() ? clientSnapshot : found;
    }

    public int energy() { return playerInventory.player.level.isClientSide ? clientEnergy : JetpackItem.energy(getHost()); }
    public int burnLeft() { return playerInventory.player.level.isClientSide ? clientBurnLeft : getHost().getOrCreateTag().getInt("JetpackBurnLeft"); }
    public int burnTotal() { return playerInventory.player.level.isClientSide ? clientBurnTotal : getHost().getOrCreateTag().getInt("JetpackBurnTotal"); }
    public UUID jetpackId() { return jetpackId; }
    public String materialName() {
        ItemStack host = getHost();
        return host.getItem() instanceof JetpackItem item ? item.materialName() : "";
    }

    @Override public boolean stillValid(Player player) { return player.isAlive() && !JetpackFueling.findHost(player, jetpackId).isEmpty(); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return result;
        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index == 0) {
            if (!this.moveItemStackTo(stack, 1, 37, true)) return ItemStack.EMPTY;
        } else if (JetpackFueling.isFuel(stack)) {
            if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (index < 28) {
            if (!this.moveItemStackTo(stack, 28, 37, false)) return ItemStack.EMPTY;
        } else if (!this.moveItemStackTo(stack, 1, 28, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return result;
    }

    private static final class FuelSlotContainer implements Container {
        private final Player player;
        private final UUID id;
        FuelSlotContainer(Player player, UUID id) { this.player = player; this.id = id; }
        private ItemStack host() { return JetpackFueling.findHost(player, id); }
        @Override public int getContainerSize() { return 1; }
        @Override public boolean isEmpty() { return JetpackFueling.getFuel(host()).isEmpty(); }
        @Override public ItemStack getItem(int slot) { return slot == 0 ? JetpackFueling.getFuel(host()) : ItemStack.EMPTY; }
        @Override public ItemStack removeItem(int slot, int amount) {
            ItemStack fuel = getItem(slot);
            if (fuel.isEmpty()) return ItemStack.EMPTY;
            ItemStack removed = fuel.split(amount);
            JetpackFueling.setFuel(player, id, fuel);
            return removed;
        }
        @Override public ItemStack removeItemNoUpdate(int slot) { ItemStack fuel = getItem(slot); JetpackFueling.setFuel(player, id, ItemStack.EMPTY); return fuel; }
        @Override public void setItem(int slot, ItemStack stack) {
            if (slot == 0) JetpackFueling.setFuel(player, id, stack);
        }
        @Override public void setChanged() {
            player.getInventory().setChanged();
            if (player.containerMenu != null) player.containerMenu.broadcastChanges();
        }
        @Override public boolean stillValid(Player player) { return this.player == player && !host().isEmpty(); }
        @Override public void clearContent() { JetpackFueling.setFuel(player, id, ItemStack.EMPTY); }
    }
}
