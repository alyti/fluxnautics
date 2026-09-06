package dev.alyti.fluxnautics.content.blocks.field;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.alyti.fluxnautics.client.render.SFRenderTypes;
import dev.alyti.fluxnautics.client.util.RenderBufferHelper;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.client.render.shader.uniform.ShaderUniformAccess;
import mekanism.client.render.tileentity.MekanismTileEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.client.textures.UnitTextureAtlasSprite;
import org.joml.Vector4f;

public class FieldDriveRenderer  extends MekanismTileEntityRenderer<FieldDriveBlockEntity> {
    public FieldDriveRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void render(FieldDriveBlockEntity blockEntity, float partialTick, PoseStack matrix, MultiBufferSource renderer, int light, int overlayLight, ProfilerFiller profiler) {
        Direction frontFacing = blockEntity.facingSupplier.get();
        renderCuboid(matrix, renderer, light);
    }

    private void renderCuboid(PoseStack stack, MultiBufferSource buffer, int combinedLight) {


        float minX = 0.24f;
        float minY = 0.2f;
        float minZ = 0.24f;

        float maxX = 0.76f;
        float maxY = 1.8f;
        float maxZ = 0.76f;

        VertexConsumer consumer;
        RenderType renderType = SFRenderTypes.endPortalEffect();
        if (renderType == null) {
            return;
        }

        consumer = buffer.getBuffer(renderType);
        RenderStateShard.ShaderStateShard shard = VeilRenderType.getShards(renderType).shaderState();
        shard.setupRenderState();

        ShaderProgram shader = VeilRenderSystem.getShader();
        if (shader != null) {
            ShaderUniformAccess scale = shader.getUniformSafe("vColorPost");
            if (scale != null) {
                Vector4f v = new Vector4f(-0.1f, -0.1f, -0.1f,  -0.1f);
                v.set(0f);
    //            v.sub(-2f, -2f, -2f, -2f);//.mul(-100f);
                scale.setVector(v);
            }
        }

        stack.pushPose();
        PoseStack.Pose pose = stack.last();
        RenderBufferHelper.renderCube(consumer, pose, 0xFFAA00FF,
                combinedLight, UnitTextureAtlasSprite.INSTANCE,
                minX, minY, minZ, maxX, maxY, maxZ);
        stack.popPose();
    }

    @Override
    protected String getProfilerSection() {
        return "";
    }
}
