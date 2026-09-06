package dev.alyti.fluxnautics.mixin.mekanism;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.index.ExtraSlotOverlays;
import mekanism.common.inventory.container.slot.SlotOverlay;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;

@Mixin(value = SlotOverlay.class, remap = false)
public class SlotOverlayMixin {
    @Shadow(remap = false)
    @Final
    @Mutable
    private static SlotOverlay[] $VALUES;

    public SlotOverlayMixin() {}


    @SuppressWarnings("InvokerTarget")
    @Invoker("<init>")
    public static SlotOverlay slotOverlay$init(String internalName, int order, String texture, int width, int height) {
        throw new AssertionError();
    }

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void slotOverlayClinit(CallbackInfo ci) {
        ResourceLocation arbitraryTexture = Fluxnautics.id("gui/slot/overlay_meka_robit.png");
        ExtraSlotOverlays.MEKA_ROBIT_OVERLAY = fluxnautics$addVariant("MEKA_ROBIT", arbitraryTexture, 18, 18);
    }

    @Unique
    private static SlotOverlay fluxnautics$addVariant(String name, ResourceLocation texture, int width, int height) {
        ArrayList<SlotOverlay> variants = new ArrayList<>(Arrays.asList($VALUES));
//        int ordinal = variants.get(variants.size() - 1).ordinal() + 1;
        SlotOverlay overlay = slotOverlay$init(name, variants.size(), "dummy.png", width, height);
        try {
            Field textureField = SlotOverlay.class.getDeclaredField("texture");
            textureField.setAccessible(true);
            textureField.set(overlay, texture);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject custom ResourceLocation into SlotOverlay", e);
        }
//        ((SlotOverlayAccessor) (Object) overlay).fluxnautics$setTexture(texture);
        
        variants.add(overlay);
        SlotOverlayMixin.$VALUES = variants.toArray(new SlotOverlay[0]);
        return overlay;
    }
}