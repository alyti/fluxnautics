package dev.alyti.fluxnautics.content.blocks.daemon_protocol;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.alyti.fluxnautics.client.render.MekaRobitItemRenderer;
import dev.alyti.fluxnautics.client.render.MekaRobitRenderer;
import dev.alyti.fluxnautics.client.render.SFRenderTypes;
import dev.alyti.fluxnautics.client.render.model.MekaRobit;
import dev.alyti.fluxnautics.content.blocks.forcefield.ForcefieldEmitterBlockEntity;
import mekanism.client.render.tileentity.MekanismTileEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemDisplayContext;

import static dev.alyti.fluxnautics.client.render.MekaRobitItemRenderer.EMISSIVE;
import static dev.alyti.fluxnautics.client.render.MekaRobitItemRenderer.TEXTURE;

public class DaemonProtocolRenderer extends MekanismTileEntityRenderer<DaemonProtocolBlockEntity> implements BlockEntityRenderer<DaemonProtocolBlockEntity> {
    public DaemonProtocolRenderer(BlockEntityRendererProvider.Context context) {
        super(context);

    }

    @Override
    protected void render(DaemonProtocolBlockEntity tile, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light, int packedOverlay, ProfilerFiller profiler) {
        MekaRobit model = MekaRobitItemRenderer.RENDERER.model;

        if (tile.ownedEntity == null || !tile.ownedEntity.isAlive())
            return;

        model.setupAnim(tile.ownedEntity, 0.0F, 0.0F, tile.ownedEntity.tickCount + partialTick, 0.0F, 0.0F);

        model.core.visible = true;
        model.shell.visible = true;
        model.thruster.visible = true;
        model.wings_h.visible = true;
        model.wings_v.visible = true;
        int blendedColor = MekaRobitRenderer.getBlendedColor(tile.ownedEntity, partialTick);
        int tintedEmissiveColor = (blendedColor & 0x00FFFFFF) | 0xFF000000;
        int packedLight = 15728880;

        float smoothYaw = tile.ownedEntity.smoothYaw.getValue(partialTick);
        float smoothPitch = tile.ownedEntity.smoothPitch.getValue(partialTick);
        float smoothRoll = tile.ownedEntity.smoothRoll.getValue(partialTick);

        poseStack.pushPose();

        poseStack.translate(0.5D, 0D, 0.5D);
        poseStack.scale(0.5f, 0.5f, 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - smoothYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-smoothPitch));
        VertexConsumer vertexconsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        model.renderToBuffer(poseStack, vertexconsumer, packedLight, OverlayTexture.NO_OVERLAY);
        VertexConsumer emissiveConsumer = buffer.getBuffer(SFRenderTypes.endPortalEyes(EMISSIVE));
        model.renderToBuffer(poseStack, emissiveConsumer, packedLight, OverlayTexture.NO_OVERLAY, tintedEmissiveColor);
        poseStack.popPose();
    }

    @Override
    protected String getProfilerSection() {
        return "daemon_protocol";
    }
}
