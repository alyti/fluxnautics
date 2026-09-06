package dev.alyti.fluxnautics.config.group;

import mekanism.api.text.IHasTranslationKey;
import net.minecraft.nbt.CompoundTag;

public class GroupBooleanConfig extends GroupConfig<Boolean, GroupBooleanConfig> {

    public GroupBooleanConfig(String name, IHasTranslationKey description, boolean defaultValue) {
        super(name, description, defaultValue);
    }

    @Override
    public void read(CompoundTag tag) {
        if (tag.contains(getName())) {
            set(tag.getBoolean(getName()));
        }
    }

    @Override
    public void write(CompoundTag tag) {
        tag.putBoolean(getName(), get());
    }
}
