package dev.alyti.fluxnautics.config.group;

import mekanism.api.text.IHasTranslationKey;
import net.minecraft.nbt.CompoundTag;

import java.util.List;

public class GroupEnumConfig<V extends Enum<V>> extends GroupConfig<V, GroupEnumConfig<V>> {

    private final List<V> enumConstants;

    public GroupEnumConfig(String name, IHasTranslationKey description, V defaultValue) {
        super(name, description, defaultValue);
        this.enumConstants = List.of(value.getDeclaringClass().getEnumConstants());
    }

    @Override
    public void read(CompoundTag tag) {
        if (tag.contains(getName())) {
            String name = tag.getString(getName());
            for (V val : getEnumConstants()) {
                if (val.name().equals(name))
                    set(val);
            }
        }
    }

    @Override
    public void write(CompoundTag tag) {
        tag.putString(getName(), get().name());
    }

    public List<V> getEnumConstants() {
        return enumConstants;
    }
}
