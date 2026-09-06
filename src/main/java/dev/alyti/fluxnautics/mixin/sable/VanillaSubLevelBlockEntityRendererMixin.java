package dev.alyti.fluxnautics.mixin.sable;

import dev.alyti.fluxnautics.client.CloakedSublevelCache;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.mixinhelpers.sublevel_render.vanilla.VanillaSubLevelBlockEntityRenderer;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VanillaSubLevelBlockEntityRenderer.class)
public abstract class VanillaSubLevelBlockEntityRendererMixin {

    @Inject(method = "renderSingleBE", at = @At("HEAD"), cancellable = true)
    private void fluxnautics$skipCloakedBlockEntity(BlockEntity blockEntity, com.mojang.blaze3d.vertex.PoseStack poseStack, float partialTick, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
        if (!CloakedSublevelCache.isEmpty()) {
            ClientSubLevel containingSubLevel = Sable.HELPER.getContainingClient(blockEntity);
            if (containingSubLevel != null && CloakedSublevelCache.isCloaked(containingSubLevel)) {
                ci.cancel();
            }
        }
    }
}