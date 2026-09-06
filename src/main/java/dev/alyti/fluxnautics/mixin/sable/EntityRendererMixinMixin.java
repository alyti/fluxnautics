package dev.alyti.fluxnautics.mixin.sable;

import com.bawnorton.mixinsquared.TargetHandler;
import dev.alyti.fluxnautics.client.CloakedSublevelCache;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityRenderer.class, priority = 1500)
public class EntityRendererMixinMixin {

    @TargetHandler(
            mixin = "dev.ryanhcode.sable.mixin.entity.entity_rendering.EntityRendererMixin",
            name = "sable$shouldRender"
    )
    @Inject(
            method = "@MixinSquared:Handler",
            at = @At("TAIL"),
            cancellable = true
    )
    private <E extends Entity> void fluxnautics$skipCloakedEntities(
            E entity, Frustum frustum, double pCamX, double pCamY, double pCamZ, CallbackInfoReturnable<Boolean> cir, CallbackInfo ci
    ) {
        if (!CloakedSublevelCache.isEmpty()) {
            if (CloakedSublevelCache.isEntityCloaked(entity))
                cir.setReturnValue(false);
        }
    }
}