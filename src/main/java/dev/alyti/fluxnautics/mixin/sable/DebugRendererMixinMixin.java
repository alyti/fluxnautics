package dev.alyti.fluxnautics.mixin.sable;


import com.bawnorton.mixinsquared.TargetHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.alyti.fluxnautics.client.CloakedSublevelCache;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(value = DebugRenderer.class, priority = 1500)
public class DebugRendererMixinMixin {

    @TargetHandler(
            mixin = "dev.ryanhcode.sable.mixin.debug_render.DebugRendererMixin",
            name = "renderFilledBox"
    )
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true)
    private static void fluxnautics$skipCloakedDebugBox(PoseStack poseStack, MultiBufferSource bufferSource, BlockPos blockPos, float f, float g, float h, float i, float j, CallbackInfo ci) {
        if (!CloakedSublevelCache.isEmpty()) {
            ClientSubLevel subLevel = Sable.HELPER.getContainingClient(blockPos);
            if (subLevel != null && CloakedSublevelCache.isCloaked(subLevel)) {
                ci.cancel();
            }
        }
    }
}