package dev.alyti.fluxnautics.mixin.mekanism;

import dev.alyti.fluxnautics.index.HarmonicResonanceFilter;
import mekanism.common.content.filter.IFilter;
import mekanism.common.integration.computer.BaseComputerHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Map;

@Mixin(value = BaseComputerHelper.class, remap = false)
public class BaseComputerHelperMixin {
    @Shadow
    protected Map<String, Object> convertFilterCommon(IFilter<?> result) {
        return null;
    }

    /**
     * Injects a new overload of the convert method specifically for HarmonicResonanceFilter.
     * This resolves the "no suitable method found" error in the Annotation Processor.
     */
    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Unique
    public Object convert(@Nullable HarmonicResonanceFilter<?> filter) {
        if (filter == null) {
            return null;
        }

        return convertFilterCommon(filter);
    }
}