package dev.alyti.fluxnautics.api.datamaps;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.alyti.fluxnautics.Fluxnautics;
import net.minecraft.resources.ResourceLocation;

public record ForcefieldAbsorption(double absorption, boolean percentage) {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Fluxnautics.MODID, "forcefield_absorption");

    private static final Codec<Double> ABSORPTION_CODEC = Codec.doubleRange(0, Double.MAX_VALUE);

    public static final Codec<ForcefieldAbsorption> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ABSORPTION_CODEC.optionalFieldOf("absorption", 0.0).forGetter(ForcefieldAbsorption::absorption),
        Codec.BOOL.optionalFieldOf("percentage", false).forGetter(ForcefieldAbsorption::percentage)
    ).apply(instance, ForcefieldAbsorption::new));
}
