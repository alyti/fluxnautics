package dev.alyti.fluxnautics.config.group;

import mekanism.api.text.IHasTranslationKey;
import net.minecraft.nbt.CompoundTag;

public class GroupStringConfig extends GroupConfig<String, GroupStringConfig> {

    public GroupStringConfig(String name, IHasTranslationKey description, String defaultValue) {
        super(name, description, defaultValue);
    }

    @Override
    public void read(CompoundTag tag) {
        if (tag.contains(getName())) {
            set(tag.getString(getName()));
        }
    }

    @Override
    public void write(CompoundTag tag) {
        tag.putString(getName(), get());
    }
}
