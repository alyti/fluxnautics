package dev.alyti.fluxnautics.mixin.mekanism;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.alyti.fluxnautics.index.ExtraFilterTypes;
import dev.alyti.fluxnautics.index.HarmonicResonanceItemStackFilter;
import mekanism.common.content.filter.FilterType;
import mekanism.common.content.filter.IFilter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.IntFunction;

@Mixin(value = FilterType.class, remap = false)
public class FilterTypeMixin {
    @Shadow(remap = false)
    @Final
    @Mutable
    private static FilterType[] $VALUES;

    @Shadow(remap = false)
    @Final
    @Mutable
    public static IntFunction<FilterType> BY_ID;

    @Shadow(remap = false)
    @Final
    @Mutable
    public static StreamCodec<io.netty.buffer.ByteBuf, FilterType> STREAM_CODEC;

    @Shadow(remap = false)
    @Final
    @Mutable
    public static Codec<FilterType> CODEC;

    public FilterTypeMixin() {}

    @SuppressWarnings("InvokerTarget")
    @Invoker("<init>")
    public static FilterType filterType$init(String internalName, int order, MapCodec<? extends IFilter<?>> codec, StreamCodec<? super RegistryFriendlyByteBuf, ? extends IFilter<?>> streamCodec) {
        throw new AssertionError();
    }

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void warningTypeClinit(CallbackInfo ci) {
        ExtraFilterTypes.HARMONIC_RESONANCE_ITEM_STACK_FILTER = fluxnautics$addVariant("HARMONIC_RESONANCE_ITEM_STACK_FILTER", HarmonicResonanceItemStackFilter.CODEC, HarmonicResonanceItemStackFilter.STREAM_CODEC);

        BY_ID = ByIdMap.continuous(
                FilterType::ordinal,
                FilterType.values(),
                ByIdMap.OutOfBoundsStrategy.WRAP
        );
        STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, FilterType::ordinal);

        CODEC = StringRepresentable.fromEnum(FilterType::values);
    }

    @Unique
    private static FilterType fluxnautics$addVariant(String name, MapCodec<? extends IFilter<?>> codec, StreamCodec<? super RegistryFriendlyByteBuf, ? extends IFilter<?>> streamCodec) {
        ArrayList<FilterType> variants = new ArrayList<>(Arrays.asList($VALUES));
        FilterType newFilter = filterType$init(name, variants.size(), codec, streamCodec);
        variants.add(newFilter);
        FilterTypeMixin.$VALUES = variants.toArray(new FilterType[0]);
        return newFilter;
    }
}
