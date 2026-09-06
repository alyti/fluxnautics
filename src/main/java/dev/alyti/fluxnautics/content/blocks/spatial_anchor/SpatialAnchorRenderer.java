package dev.alyti.fluxnautics.content.blocks.spatial_anchor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.api.render.IMachineFX;
import dev.alyti.fluxnautics.client.render.MachineFXRenderer;
import dev.alyti.fluxnautics.client.util.RenderBufferHelper;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import static net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;
import static net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;

public class SpatialAnchorRenderer extends MachineFXRenderer<SpatialAnchorBlockEntity, SpatialAnchorRenderer.AnchorFX> {

    private static final float R = 1.00F;
    private static final float G = 0.78F;
    private static final float B = 0.35F;

    private static final float PULSE_PERIOD = 2.4F;

    private static final float PULSE_REACH_MIN = 0.14F;
    private static final float PULSE_REACH_MAX = 0.40F;

    private static final float AURA_MIN = 0.38F;
    private static final float AURA_MAX = 0.62F;

    public record AnchorFX(float time) {}

    public static final IMachineFX<SpatialAnchorBlockEntity, AnchorFX> FX = new IMachineFX<>() {

        @Override
        public AnchorFX capture(SpatialAnchorBlockEntity blockEntity, float partialTick) {
            return new AnchorFX(machineTime(blockEntity, partialTick));
        }

        @Override
        public AnchorFX captureForItem(ItemStack stack, ItemDisplayContext context, double time) {
            return new AnchorFX(machineSeconds(time));
        }

        @Override
        public void render(AnchorFX p, PoseStack matrix, MultiBufferSource buffers, int light, int overlay) {
            final RenderType renderType = VeilRenderType.get(Fluxnautics.id("anchor_stasis"));
            if (renderType == null) {
                return;
            }
            final VertexConsumer buffer = buffers.getBuffer(renderType);

            RenderBufferHelper.renderParamCube(buffer, matrix.last(), argb(0.95F, R, G, B),
                FULL_BRIGHT, NO_OVERLAY, 0.44F, 0.44F, 0.44F, 0.56F, 0.56F, 0.56F);

            final float auraAlpha = 0.06F + 0.03F * pulse(p.time, 4.8F);
            RenderBufferHelper.renderParamCube(buffer, matrix.last(), argb(auraAlpha, R, G, B),
                FULL_BRIGHT, NO_OVERLAY, AURA_MIN, AURA_MIN, AURA_MIN, AURA_MAX, AURA_MAX, AURA_MAX);

            final float pulse = (p.time % PULSE_PERIOD) / PULSE_PERIOD;
            final float reach = PULSE_REACH_MIN + (PULSE_REACH_MAX - PULSE_REACH_MIN) * easeOut(pulse);
            final float alpha = 0.30F * (1.0F - pulse) * (1.0F - pulse);         // fade out as it expands
            RenderBufferHelper.renderParamCube(buffer, matrix.last(), argb(alpha, 1.00F, 0.87F, 0.55F),
                FULL_BRIGHT, NO_OVERLAY,
                0.5F - reach, 0.5F - reach, 0.5F - reach,
                0.5F + reach, 0.5F + reach, 0.5F + reach);
        }
    };


    @Override
    protected IMachineFX<SpatialAnchorBlockEntity, AnchorFX> fx() {
        return FX;
    }

    public SpatialAnchorRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }
}