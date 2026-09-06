package dev.alyti.fluxnautics.content.entities.gear;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.energy.IEnergyContainer;
import mekanism.common.util.StorageUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class ItemStackEnergyProxy implements IEnergyContainer {

    private final Supplier<ItemStack> stackSupplier;
    private final long fallbackMaxEnergy;
    @Nullable private final IContentsListener listener;

    public ItemStackEnergyProxy(Supplier<ItemStack> stackSupplier, long fallbackMaxEnergy,
                                @Nullable IContentsListener listener) {
        this.stackSupplier = stackSupplier;
        this.fallbackMaxEnergy = fallbackMaxEnergy;
        this.listener = listener;
    }

    @Nullable
    private IEnergyContainer delegate() {
        ItemStack stack = stackSupplier.get();
        if (stack.isEmpty()) return null;
        return StorageUtils.getEnergyContainer(stack, 0);
    }

    @Override public long getEnergy() {
        IEnergyContainer d = delegate();
        return d == null ? 0L : d.getEnergy();
    }

    @Override public void setEnergy(long energy) {
        IEnergyContainer d = delegate();
        if (d != null) d.setEnergy(Math.max(0L, energy));
        onContentsChanged();
    }

    @Override public long getMaxEnergy() {
        IEnergyContainer d = delegate();
        long max = d == null ? 0L : d.getMaxEnergy();
        return max > 0 ? max : fallbackMaxEnergy;
    }

    @Override public boolean isEmpty() {
        ItemStack stack = stackSupplier.get();
        if (stack.isEmpty()) return false;
        IEnergyContainer d = delegate();
        return d == null || d.isEmpty();
    }

    @Override public long insert(long amount, Action action, AutomationType automationType) {
        IEnergyContainer d = delegate();
        if (d == null)
            return amount;
        long remainder = d.insert(amount, action, automationType);
        if (action.execute() && remainder != amount)
            onContentsChanged();
        return remainder;
    }

    @Override public long extract(long amount, Action action, AutomationType automationType) {
        IEnergyContainer d = delegate();
        if (d == null)
            return 0L;
        long extracted = d.extract(amount, action, AutomationType.MANUAL);
        if (action.execute() && extracted > 0)
            onContentsChanged();
        return extracted;
    }

    @Override public void onContentsChanged() {
        if (listener != null) listener.onContentsChanged();
    }

    @Override public CompoundTag serializeNBT(HolderLookup.Provider provider) { return new CompoundTag(); }
    @Override public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) { }
}