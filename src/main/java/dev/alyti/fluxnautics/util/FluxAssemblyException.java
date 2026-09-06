package dev.alyti.fluxnautics.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.Objects;

public class FluxAssemblyException extends RuntimeException {

    public final Component component;
    private BlockPos position = null;

    public FluxAssemblyException(Component component) {
        super(component.getString());
        this.component = component;
    }

    public Component getComponent() { return this.component; }

    public boolean hasPosition() {
        return position != null;
    }

    public BlockPos getPosition() {
        return position;
    }

    public static void write(CompoundTag compound, HolderLookup.Provider registries, FluxAssemblyException exception) {
        if (exception == null)
            return;

        CompoundTag nbt = new CompoundTag();
        nbt.putString("Component", Component.Serializer.toJson(exception.component, registries));
        if (exception.hasPosition())
            nbt.putLong("Position", exception.getPosition()
                .asLong());

        compound.put("LastException", nbt);
    }

    public static FluxAssemblyException read(CompoundTag compound, HolderLookup.Provider registries) {
        if (!compound.contains("LastException"))
            return null;

        CompoundTag nbt = compound.getCompound("LastException");
        String string = nbt.getString("Component");
        FluxAssemblyException exception = new FluxAssemblyException(Objects.requireNonNull(Component.Serializer.fromJson(string, registries)));
        if (nbt.contains("Position"))
            exception.position = BlockPos.of(nbt.getLong("Position"));

        return exception;
    }

    public static FluxAssemblyException sameSubLevel() {
        return new FluxAssemblyException(Component.translatable("fluxnautics.assembly.same_sublevel"));
    }

    public static FluxAssemblyException noSpace() {
        return new FluxAssemblyException(Component.translatable("fluxnautics.assembly.no_space"));
    }
}