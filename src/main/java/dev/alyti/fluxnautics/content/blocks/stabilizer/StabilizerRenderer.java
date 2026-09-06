package dev.alyti.fluxnautics.content.blocks.stabilizer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.alyti.fluxnautics.api.render.IMachineFX;
import dev.alyti.fluxnautics.client.render.MachineFXRenderer;
import dev.alyti.fluxnautics.client.util.RenderBufferHelper;
import mekanism.common.lib.Color;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

public class StabilizerRenderer extends MachineFXRenderer<StabilizerBlockEntity, StabilizerRenderer.GimbalFX> {
    private static final float MAX_RADIUS = 0.390F;
    private static final float MIN_RADIUS = 0.180F;

    public StabilizerRenderer(BlockEntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    protected IMachineFX<StabilizerBlockEntity, GimbalFX> fx() {
        return FX;
    }

    public record GimbalFX(Quaternionf orientation, float rawX, float rawY, float rawZ,
                           float rX, float rY, float rZ,
                           boolean pitch, boolean yaw, boolean roll) {}

    public static final IMachineFX<StabilizerBlockEntity, GimbalFX> FX = new IMachineFX<>() {

        @Override
        public GimbalFX capture(StabilizerBlockEntity be, float partialTick) {
            float rawX = be.getDisplayAngleX(partialTick);
            float rawY = be.getDisplayAngleY(partialTick);
            float rawZ = be.getDisplayAngleZ(partialTick);

            float absX = Math.abs((float) be.angularVelocityTarget.x);
            float absY = Math.abs((float) be.angularVelocityTarget.y);
            float absZ = Math.abs((float) be.angularVelocityTarget.z);
            float maxAni = Math.max(absX, Math.max(absY, absZ));
            float radiusRange = MAX_RADIUS - MIN_RADIUS;

            float rX = maxAni > 0 ? MIN_RADIUS + (absX / maxAni) * radiusRange : MIN_RADIUS;
            float rY = maxAni > 0 ? MIN_RADIUS + (absY / maxAni) * radiusRange : MIN_RADIUS;
            float rZ = maxAni > 0 ? MIN_RADIUS + (absZ / maxAni) * radiusRange : MIN_RADIUS;

            return new GimbalFX(orientationFor(be.getBlockState().getValue(StabilizerBlock.FACING)),
                rawX, rawY, rawZ, rX, rY, rZ,
                be.config.stabilizePitch(), be.config.stabilizeYaw(), be.config.stabilizeRoll());
        }

        @Override
        public GimbalFX captureForItem(ItemStack stack, ItemDisplayContext context, double time) {
            float t = (float) (time / 20.0);
            return new GimbalFX(new Quaternionf(),
                t * 24.0F, -t * 31.0F, t * 47.0F,
                MIN_RADIUS, MIN_RADIUS, MIN_RADIUS,
                true, true, true);
        }

        @Override
        public void render(GimbalFX p, PoseStack pose, MultiBufferSource buffer, int packedLight, int packedOverlay) {
            pose.pushPose();
            pose.translate(0.5, 0.5, 0.5);
            pose.mulPose(p.orientation());
            VertexConsumer consumer = buffer.getBuffer(RenderType.debugQuads());

            if (p.pitch()) {
                pose.pushPose();
                pose.mulPose(Axis.XP.rotationDegrees(p.rawX()));
                RenderBufferHelper.renderRing(pose, consumer, 0f, 0f, 0f, p.rX(), 0.02f, 8, 8,
                    Color.rgb(0xFF0000).alpha(0.75), Direction.Axis.X, packedLight, packedOverlay);
                pose.popPose();
            }
            if (p.yaw()) {
                pose.pushPose();
                pose.mulPose(Axis.YP.rotationDegrees(p.rawY()));
                RenderBufferHelper.renderRing(pose, consumer, 0f, 0f, 0f, p.rY(), 0.02f, 8, 8,
                    Color.rgb(0x00FF00).alpha(0.75), Direction.Axis.Y, packedLight, packedOverlay);
                pose.popPose();
            }
            if (p.roll()) {
                pose.pushPose();
                pose.mulPose(Axis.ZP.rotationDegrees(p.rawZ()));
                RenderBufferHelper.renderRing(pose, consumer, 0f, 0f, 0f, p.rZ(), 0.02f, 8, 8,
                    Color.rgb(0x0000FF).alpha(0.75), Direction.Axis.Z, packedLight, packedOverlay);
                pose.popPose();
            }
            pose.popPose();
        }
    };

    private static Quaternionf orientationFor(Direction down) {
        return switch (down) {
            case DOWN  -> new Quaternionf();
            case UP    -> new Quaternionf().rotationX((float) Math.PI);
            case NORTH -> new Quaternionf().rotationX((float) (Math.PI / 2));
            case SOUTH -> new Quaternionf().rotationX((float) (-Math.PI / 2));
            case EAST  -> new Quaternionf().rotationZ((float) (Math.PI / 2));
            case WEST  -> new Quaternionf().rotationZ((float) (-Math.PI / 2));
        };
    }

    @Override
    public boolean shouldRenderOffScreen(StabilizerBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}