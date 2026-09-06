package dev.alyti.fluxnautics.content.blocks.assembly_terminal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class BlueprintConfig {

    public static final Codec<BlueprintConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.list(ResourceLocation.CODEC).fieldOf("enabled_force_groups").forGetter(BlueprintConfig::enabledForceGroups),
            Codec.BOOL.fieldOf("display_center_of_mass").forGetter(BlueprintConfig::displayCenterOfMass),
            Codec.BOOL.fieldOf("display_bounding_box").forGetter(BlueprintConfig::displayBoundingBox),
            Codec.BOOL.fieldOf("merge_forces").forGetter(BlueprintConfig::mergeForces),
            Codec.DOUBLE.fieldOf("yaw").forGetter(BlueprintConfig::yaw),
            Codec.DOUBLE.fieldOf("pitch").forGetter(BlueprintConfig::pitch)
    ).apply(instance, BlueprintConfig::new));

    public static final StreamCodec<ByteBuf, BlueprintConfig> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), BlueprintConfig::enabledForceGroups,
            ByteBufCodecs.BOOL, BlueprintConfig::displayCenterOfMass,
            ByteBufCodecs.BOOL, BlueprintConfig::displayBoundingBox,
            ByteBufCodecs.BOOL, BlueprintConfig::mergeForces,
            ByteBufCodecs.DOUBLE, BlueprintConfig::yaw,
            ByteBufCodecs.DOUBLE, BlueprintConfig::pitch,
            BlueprintConfig::new);

    private List<ResourceLocation> enabledForceGroups;
    private boolean displayCenterOfMass;
    private boolean displayBoundingBox;
    private boolean mergeForces;
    private double yaw;
    private double pitch;


    public static BlueprintConfig makeDefault(final AssemblyTerminalBlockEntity be) {
        final ObjectList<ResourceLocation> enabledForceGroups = new ObjectArrayList<>();

        for (final ResourceLocation groupId : ForceGroups.REGISTRY.keySet()) {
            if (ForceGroups.REGISTRY.get(groupId).defaultDisplayed())
                enabledForceGroups.add(groupId);
        }

        return new BlueprintConfig(enabledForceGroups, false, false, false, 0.0f, 0.0f);
    }

    public BlueprintConfig(final List<ResourceLocation> enabledForceGroups,
                         final boolean displayCenterOfMass,
                         final boolean displayBoundingBox,
                         final boolean mergeForces,
                         final double yaw,
                         final double pitch) {
        this.enabledForceGroups = new ObjectArrayList<>(enabledForceGroups);;
        this.displayCenterOfMass = displayCenterOfMass;
        this.displayBoundingBox = displayBoundingBox;
        this.mergeForces = mergeForces;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        BlueprintConfig that = (BlueprintConfig) o;
        return displayCenterOfMass == that.displayCenterOfMass && displayBoundingBox == that.displayBoundingBox && mergeForces == that.mergeForces && Double.compare(yaw, that.yaw) == 0 && Double.compare(pitch, that.pitch) == 0 && enabledForceGroups.equals(that.enabledForceGroups);
    }

    @Override
    public int hashCode() {
        int result = enabledForceGroups.hashCode();
        result = 31 * result + Boolean.hashCode(displayCenterOfMass);
        result = 31 * result + Boolean.hashCode(displayBoundingBox);
        result = 31 * result + Boolean.hashCode(mergeForces);
        result = 31 * result + Double.hashCode(yaw);
        result = 31 * result + Double.hashCode(pitch);
        return result;
    }

    public List<ResourceLocation> enabledForceGroups() {
        return this.enabledForceGroups;
    }

    public boolean displayCenterOfMass() {
        return this.displayCenterOfMass;
    }

    public boolean displayBoundingBox() {
        return this.displayBoundingBox;
    }

    public boolean mergeForces() {
        return this.mergeForces;
    }

    public double yaw() {
        return this.yaw;
    }

    public double pitch() {
        return this.pitch;
    }

    public void setDisplayCenterOfMass(final boolean displayCenterOfMass) {
        this.displayCenterOfMass = displayCenterOfMass;
    }

    public void setDisplayBoundingBox(final boolean displayBoundingBox) {
        this.displayBoundingBox = displayBoundingBox;
    }

    public void setMergeForces(final boolean mergeForces) {
        this.mergeForces = mergeForces;
    }

    public void setYaw(final double yaw) {
        this.yaw = yaw;
    }

    public void setPitch(final double pitch) {
        this.pitch = pitch;
    }
}
