package dev.alyti.fluxnautics.config.group;

import mekanism.api.text.IHasTranslationKey;
import mekanism.common.inventory.container.sync.dynamic.ContainerSync;
import net.minecraft.nbt.CompoundTag;

public abstract class GroupConfig<V, CONFIG extends GroupConfig<V, CONFIG>> {

    private final String name;
    private final IHasTranslationKey description;
    @ContainerSync
    protected V value;

    protected GroupConfig(String name, IHasTranslationKey description, V defaultValue) {
        this.name = name;
        this.description = description;
        this.value = defaultValue;
    }

    public String getName() {
        return name;
    }

    public IHasTranslationKey getDescription() {
        return description;
    }

    public V get() {
        return value;
    }

    public void set(V value) {
        this.value = value;
    }

    public abstract void read(CompoundTag tag);

    public abstract void write(CompoundTag tag);
}
