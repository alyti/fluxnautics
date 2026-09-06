package dev.alyti.fluxnautics.mixin.mekanism;

import dev.alyti.fluxnautics.index.ExtraFilterTypes;
import dev.alyti.fluxnautics.index.HarmonicResonanceItemStackFilter;
import mekanism.common.content.filter.BaseFilter;
import mekanism.common.content.filter.FilterType;
import mekanism.common.content.filter.IFilter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BaseFilter.class, remap = false)
public class BaseFilterMixin {

    @Inject(method = "fromType", at = @At("HEAD"), cancellable = true)
    private static void fromType(FilterType filterType, CallbackInfoReturnable<IFilter<?>> cir) {
        if (filterType == ExtraFilterTypes.HARMONIC_RESONANCE_ITEM_STACK_FILTER) {
            cir.setReturnValue(new HarmonicResonanceItemStackFilter());
        }
    }
}