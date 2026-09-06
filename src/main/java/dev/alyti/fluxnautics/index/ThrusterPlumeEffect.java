package dev.alyti.fluxnautics.index;

import dev.alyti.fluxnautics.Fluxnautics;
import io.netty.buffer.ByteBuf;
import mekanism.api.text.IHasTextComponent;
import mekanism.api.text.TextComponentUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.function.IntFunction;

public enum ThrusterPlumeEffect implements StringRepresentable, IHasTextComponent {
    CLASSIC("classic", Fluxnautics.id("laser_old")),
    FBM("fbm", Fluxnautics.id("laser_fbm")),
    ;

    public static final IntFunction<ThrusterPlumeEffect> BY_ID = ByIdMap.continuous(ThrusterPlumeEffect::ordinal, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
    public static final StreamCodec<ByteBuf, ThrusterPlumeEffect> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, ThrusterPlumeEffect::ordinal);

    final String label;

    public ResourceLocation getShader() {
        return shader;
    }

    final ResourceLocation shader;

    ThrusterPlumeEffect(String label, ResourceLocation shader) {
        this.label = label;
        this.shader = shader;
    }

    @NotNull
    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public Component getTextComponent() {
        return TextComponentUtil.translate("gui.fluxnautics.group.effect.plume.%s".formatted(getSerializedName()));
    }
}
