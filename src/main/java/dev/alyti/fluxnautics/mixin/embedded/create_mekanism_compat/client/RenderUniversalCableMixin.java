package dev.alyti.fluxnautics.mixin.embedded.create_mekanism_compat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.alyti.fluxnautics.mixin_helper.client.CmcClientSubLevelHelper;
import mekanism.client.render.transmitter.RenderUniversalCable;
import mekanism.common.tile.transmitter.TileEntityUniversalCable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderUniversalCable.class, remap = false)
public abstract class RenderUniversalCableMixin {
    @Inject(
            method = "render(Lmekanism/common/tile/transmitter/TileEntityUniversalCable;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD")
    )
    private void cmc$pushMountedUniversalCableRender(TileEntityUniversalCable tile, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int light, int overlayLight, ProfilerFiller profiler, CallbackInfo callback) {
        CmcClientSubLevelHelper.pushMountedRender(tile);
    }

    @Inject(
            method = "render(Lmekanism/common/tile/transmitter/TileEntityUniversalCable;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("RETURN")
    )
    private void cmc$popMountedUniversalCableRender(TileEntityUniversalCable tile, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int light, int overlayLight, ProfilerFiller profiler, CallbackInfo callback) {
        CmcClientSubLevelHelper.popMountedRender();
    }

    @Redirect(
            method = "render(Lmekanism/common/tile/transmitter/TileEntityUniversalCable;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Sheets;translucentCullBlockSheet()Lnet/minecraft/client/renderer/RenderType;")
    )
    private RenderType cmc$useMountedUniversalCableRenderType() {
        return CmcClientSubLevelHelper.mountedTranslucentBlockSheet();
    }
}