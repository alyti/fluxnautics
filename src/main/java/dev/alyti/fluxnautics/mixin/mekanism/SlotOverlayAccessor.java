package dev.alyti.fluxnautics.mixin.mekanism;

import mekanism.common.inventory.container.slot.SlotOverlay;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SlotOverlay.class)
public interface SlotOverlayAccessor {
    @Accessor("texture")
    void fluxnautics$setTexture(ResourceLocation texture);
}