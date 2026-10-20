package br.com.petrickfaria.jetpack.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraft.core.Direction;
import javax.annotation.Nullable;

/** Five minutes at one FE per tick; one coal (1600 burn ticks) supplies exactly 10%. */
public class JetpackItem extends Item {
    public static final int CAPACITY = 6000;
    public static final int COAL_ENERGY = CAPACITY / 10;
    private final int maximumHeight;

    public JetpackItem(int maximumHeight, Properties properties) {
        super(properties);
        this.maximumHeight = maximumHeight;
    }

    public int maximumHeight() { return maximumHeight; }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new EnergyProvider(stack);
    }

    public static int energy(ItemStack stack) {
        return stack.getCapability(CapabilityEnergy.ENERGY).map(IEnergyStorage::getEnergyStored).orElse(0);
    }

    public static int receive(ItemStack stack, int amount, boolean simulate) {
        return stack.getCapability(CapabilityEnergy.ENERGY).map(e -> e.receiveEnergy(amount, simulate)).orElse(0);
    }

    public static int consume(ItemStack stack, int amount, boolean simulate) {
        return stack.getCapability(CapabilityEnergy.ENERGY).map(e -> e.extractEnergy(amount, simulate)).orElse(0);
    }

    private static final class EnergyProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
        private final ItemStack stack;
        private final IEnergyStorage storage = new IEnergyStorage() {
            @Override public int receiveEnergy(int maxReceive, boolean simulate) {
                if (maxReceive <= 0) return 0;
                int accepted = Math.min(maxReceive, CAPACITY - getEnergyStored());
                if (!simulate && accepted > 0) stack.getOrCreateTag().putInt("EnergyRedstonica", getEnergyStored() + accepted);
                return accepted;
            }
            @Override public int extractEnergy(int maxExtract, boolean simulate) {
                if (maxExtract <= 0) return 0;
                int extracted = Math.min(maxExtract, getEnergyStored());
                if (!simulate && extracted > 0) stack.getOrCreateTag().putInt("EnergyRedstonica", getEnergyStored() - extracted);
                return extracted;
            }
            @Override public int getEnergyStored() {
                return Math.max(0, Math.min(CAPACITY, stack.getOrCreateTag().getInt("EnergyRedstonica")));
            }
            @Override public int getMaxEnergyStored() { return CAPACITY; }
            @Override public boolean canExtract() { return true; }
            @Override public boolean canReceive() { return true; }
        };
        private final LazyOptional<IEnergyStorage> energy = LazyOptional.of(() -> storage);
        EnergyProvider(ItemStack stack) { this.stack = stack; }
        @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
            return capability == CapabilityEnergy.ENERGY ? energy.cast() : LazyOptional.empty();
        }
        @Override public CompoundTag serializeNBT() { CompoundTag tag = new CompoundTag(); tag.putInt("EnergyRedstonica", storage.getEnergyStored()); return tag; }
        @Override public void deserializeNBT(CompoundTag tag) { stack.getOrCreateTag().putInt("EnergyRedstonica", tag.getInt("EnergyRedstonica")); }
    }
}
