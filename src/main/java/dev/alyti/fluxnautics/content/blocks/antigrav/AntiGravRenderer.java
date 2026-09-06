package dev.alyti.fluxnautics.content.blocks.antigrav;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.api.render.IMachineFX;
import dev.alyti.fluxnautics.client.render.MachineFXRenderer;
import dev.alyti.fluxnautics.client.render.SFRenderTypes;
import dev.alyti.fluxnautics.client.util.RenderBufferHelper;
import dev.alyti.fluxnautics.content.blocks.spatial_anchor.SpatialAnchorBlockEntity;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.client.render.shader.uniform.ShaderUniform;
import foundry.veil.api.client.render.shader.uniform.ShaderUniformAccess;
import mekanism.common.lib.Color;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaterniond;
import org.joml.Vector4f;

import static net.minecraft.client.renderer.LightTexture.FULL_BLOCK;
import static net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;
import static net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;

public class AntiGravRenderer extends MachineFXRenderer<AntiGravBlockEntity, AntiGravRenderer.AntiGravFX> {

    private static final float R = 0.55F;
    private static final float G = 0.96F;
    private static final float B = 1.00F;

    private static final int RING_COUNT = 8;
    private static final float RING_Y_MIN = 0.30F;
    private static final float RING_Y_MAX = 0.70F;
    private static final float RING_RISE_SECONDS = 1.2F;
    private static final float RING_BASE_RADIUS = 0.15F;
    private static final float RING_RADIUS_STEP = 0.01F;
    private static final float RING_TUBE = 0.012F;
    private static final int RING_SIDES = 4;
    private static final int RING_SEGMENTS = 8;

    public record AntiGravFX(float time) implements IWorldOriented {
        @Override
        public boolean shouldReorient() {
            return true;
        }
    }

    public AntiGravRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    public static final IMachineFX<AntiGravBlockEntity, AntiGravFX> FX = new IMachineFX<>() {

        @Override
        public AntiGravFX capture(AntiGravBlockEntity blockEntity, float partialTick) {
            return new AntiGravFX(machineTime(blockEntity, partialTick));
        }

        @Override
        public AntiGravFX captureForItem(ItemStack stack, ItemDisplayContext context, double time) {
            return new AntiGravFX(machineSeconds(time));
        }

        @Override
        public void render(AntiGravFX p, PoseStack matrix, MultiBufferSource buffers, int light, int overlay) {
            final float time = p.time();

            RenderType renderType = SFRenderTypes.endPortalEffect();
            VertexConsumer buffer = buffers.getBuffer(renderType);
            RenderStateShard.ShaderStateShard shard = VeilRenderType.getShards(renderType).shaderState();
            shard.setupRenderState();
            ShaderProgram shader = VeilRenderSystem.getShader();
            if (shader == null) {
                return;
            }
            ShaderUniformAccess uTime = shader.getUniformSafe("time");
            uTime.setFloat(p.time);
            ShaderUniform scale = shader.getUniform("vColorPost");
            if (scale != null) {
                Vector4f v = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
                v.mul(0.0004f);
                scale.setVector(v);
            }
            final float shellAlpha = 0.0055F + 0.0055F * pulse(time, RING_RISE_SECONDS);
            RenderBufferHelper.renderParamCube(buffer, matrix.last(), argb(shellAlpha, R, G, B),
                0, NO_OVERLAY, 0.40F, 0.40F, 0.40F, 0.60F, 0.60F, 0.60F);

            final RenderType renderTypeMain = VeilRenderType.get(Fluxnautics.id("antigrav_field"));
            if (renderTypeMain == null) {
                return;
            }

            buffer = buffers.getBuffer(renderTypeMain);
            for (int i = 0; i < RING_COUNT; i++) {
                final float phase = (time / RING_RISE_SECONDS + i / (float) RING_COUNT) % 1.0F;
                final float y = Mth.lerp(phase, RING_Y_MIN, RING_Y_MAX);
                final float fade = Mth.sin(phase * Mth.PI);
                final float radius = Mth.lerp(phase, RING_BASE_RADIUS, RING_BASE_RADIUS + RING_COUNT * RING_RADIUS_STEP);
                RenderBufferHelper.renderRing(matrix, buffer, 0.5F, y, 0.5F, radius, RING_TUBE,
                    RING_SIDES, RING_SEGMENTS, ringColor(0.55F * fade * fade, R, G, B),
                    Direction.Axis.Y, FULL_BRIGHT, NO_OVERLAY);
            }
        }
    };

    @Override
    protected IMachineFX<AntiGravBlockEntity, AntiGravFX> fx() {
        return FX;
    }

    protected static Color ringColor(float r, float g, float b, float a) {
        return Color.rgbad(r, g, b, Mth.clamp(a, 0.0F, 1.0F));
    }
}