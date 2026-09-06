package dev.alyti.fluxnautics.content.blocks.inertia_dampener;

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
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.Map;
import java.util.WeakHashMap;

import static net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;
import static net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;

public class InertiaDampenerRenderer extends MachineFXRenderer<InertiaDampenerBlockEntity, InertiaDampenerRenderer.DampenerFX> {

    public static float VELOCITY_SCALE_REF = 0.8F;

    public static float MAX_TRAVEL = 0.24F;

    private static final float TRACK_LENGTH = 0.30F;
    private static final float TRACK_HALF_SIZE = 0.009F;
    private static final float SPIKE_HALF_SIZE = 0.017F;
    private static final float TIP_HALF_SIZE = 0.026F;

    private static final float[][] AXIS_TINTS = {
        {1.00F, 0.42F, 0.85F},
        {0.72F, 0.35F, 1.00F},
        {0.45F, 0.65F, 1.00F},
    };

    private static final Map<InertiaDampenerBlockEntity, Vector3f> DISPLAY_OFFSETS = new WeakHashMap<>();


    public InertiaDampenerRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    public record DampenerFX(float time, Vector3f offsets, float intensity) {}

    public static final IMachineFX<InertiaDampenerBlockEntity, DampenerFX> FX = new IMachineFX<>() {

        @Override
        public DampenerFX capture(InertiaDampenerBlockEntity blockEntity, float partialTick) {
            final Vector3d velocity = blockEntity.getCachedLinearVelocity(); // world-frame [m/s]
            final Vector3f state = DISPLAY_OFFSETS.computeIfAbsent(blockEntity, be -> new Vector3f());

            float maxSpeed = 0.0F;
            for (int axis = 0; axis < 3; axis++) {
                final float v = (float) velocity.get(axis);
                maxSpeed = Math.max(maxSpeed, Math.abs(v));
                final float target = Mth.clamp(v / VELOCITY_SCALE_REF, -1.0F, 1.0F) * MAX_TRAVEL;
                state.setComponent(axis, Mth.lerp(0.30F, state.get(axis), target));
            }
            final float intensity = Math.min(1.0F, 0.25F + 0.75F * (maxSpeed / VELOCITY_SCALE_REF));
            return new DampenerFX(machineTime(blockEntity, partialTick), new Vector3f(state), intensity);
        }

        @Override
        public DampenerFX captureForItem(ItemStack stack, ItemDisplayContext context, double time) {
            return new DampenerFX(machineSeconds(time), new Vector3f(), 0.25F);
        }

        @Override
        public void render(DampenerFX p, PoseStack matrix, MultiBufferSource buffers, int light, int overlay) {
            final RenderType renderType = VeilRenderType.get(Fluxnautics.id("dampener_linear"));
            if (renderType == null) {
                return;
            }
            final VertexConsumer buffer = buffers.getBuffer(renderType);

            final float cx = 0.5F;
            final float cy = 0.5F;
            final float cz = 0.5F;

            final float coreAlpha = 0.26F + 0.36F * p.intensity + 0.05F * pulse(p.time, 3.0F);
            RenderBufferHelper.renderParamCube(buffer, matrix.last(), argb(coreAlpha, 0.80F, 0.62F, 1.00F),
                FULL_BRIGHT, NO_OVERLAY,
                cx - 0.12F, cy - 0.12F, cz - 0.12F, cx + 0.12F, cy + 0.12F, cz + 0.12F);

            for (int axis = 0; axis < 3; axis++) {
                final float[] tint = AXIS_TINTS[axis];
                final float offset = p.offsets.get(axis);
                final float travel = Math.abs(offset);

                final int trackColor = argb(0.10F + 0.08F * p.intensity, tint[0], tint[1], tint[2]);
                RenderBufferHelper.renderParamRod(buffer, matrix.last(), trackColor, FULL_BRIGHT, NO_OVERLAY,
                    axis, 1, cx, cy, cz, TRACK_LENGTH, TRACK_HALF_SIZE);
                RenderBufferHelper.renderParamRod(buffer, matrix.last(), trackColor, FULL_BRIGHT, NO_OVERLAY,
                    axis, -1, cx, cy, cz, TRACK_LENGTH, TRACK_HALF_SIZE);

                if (travel > 0.004F) {
                    final int dir = offset >= 0.0F ? 1 : -1;
                    final int spikeColor = argb(0.30F + 0.45F * p.intensity, tint[0], tint[1], tint[2]);
                    RenderBufferHelper.renderParamRod(buffer, matrix.last(), spikeColor, FULL_BRIGHT, NO_OVERLAY,
                        axis, dir, cx, cy, cz, travel, SPIKE_HALF_SIZE);

                    final float tx = axis == 0 ? cx + dir * travel : cx;
                    final float ty = axis == 1 ? cy + dir * travel : cy;
                    final float tz = axis == 2 ? cz + dir * travel : cz;
                    final int tipColor = argb(0.55F + 0.40F * p.intensity, 0.92F, 0.88F, 1.00F);
                    RenderBufferHelper.renderParamCube(buffer, matrix.last(), tipColor, FULL_BRIGHT, NO_OVERLAY,
                        tx - TIP_HALF_SIZE, ty - TIP_HALF_SIZE, tz - TIP_HALF_SIZE,
                        tx + TIP_HALF_SIZE, ty + TIP_HALF_SIZE, tz + TIP_HALF_SIZE);
                }
            }
        }
    };

    @Override
    protected IMachineFX<InertiaDampenerBlockEntity, DampenerFX> fx() {
        return FX;
    }
}