package dev.alyti.fluxnautics.index;

import io.netty.buffer.ByteBuf;
import mekanism.api.text.IHasTextComponent;
import mekanism.api.text.TextComponentUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.function.IntFunction;

public enum ThrusterEffect implements StringRepresentable, IHasTextComponent {
    LIGHTNING("lightning"),
    END_PORTAL("end_portal"),
    ;

    public static final IntFunction<ThrusterEffect> BY_ID = ByIdMap.continuous(ThrusterEffect::ordinal, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
    public static final StreamCodec<ByteBuf, ThrusterEffect> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, ThrusterEffect::ordinal);

    final String label;

    ThrusterEffect(String label) {
        this.label = label;
    }

    @NotNull
    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public Component getTextComponent() {
        return TextComponentUtil.translate("gui.fluxnautics.group.effect.%s".formatted(getSerializedName()));
    }
}
