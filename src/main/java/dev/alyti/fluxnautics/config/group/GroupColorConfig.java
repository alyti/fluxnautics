package dev.alyti.fluxnautics.config.group;

import mekanism.api.text.IHasTranslationKey;
import net.minecraft.nbt.CompoundTag;

public class GroupColorConfig extends GroupConfig<Integer, GroupColorConfig> {

    public GroupColorConfig(String name, IHasTranslationKey description, int defaultValue) {
        super(name, description, defaultValue);
    }

    @Override
    public void read(CompoundTag tag) {
        if (tag.contains(getName())) {
            set(tag.getInt(getName()));
        }
    }

    @Override
    public void write(CompoundTag tag) {
        tag.putInt(getName(), get());
    }
}
