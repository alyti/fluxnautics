package dev.alyti.fluxnautics.util.placement.ghost;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import dev.alyti.fluxnautics.util.placement.PlacementClient;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.util.SublevelRenderOffsetHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public abstract class GhostBlockRenderer {

    private static final GhostBlockRenderer STANDARD = new DefaultGhostBlockRenderer();
    private static final GhostBlockRenderer TRANSPARENT = new TransparentGhostBlockRenderer();

    public static GhostBlockRenderer standard() {
        return STANDARD;
    }

    public static GhostBlockRenderer transparent() {
        return TRANSPARENT;
    }

    public abstract void render(PoseStack ms, Vec3 camera, GhostBlockParams params);

    private static class DefaultGhostBlockRenderer extends GhostBlockRenderer {

        @Override
        public void render(PoseStack ms, Vec3 camera, GhostBlockParams params) {
            Minecraft mc = Minecraft.getInstance();
            ms.pushPose();
            BlockPos pos = params.getPos();
            SublevelRenderOffsetHelper.posePlotToProjected(Sable.HELPER.getContainingClient(pos), ms);
            Vec3 rotationPoint = SublevelRenderOffsetHelper.translation(new Vec3(pos.getX(), pos.getY(), pos.getZ()));
            ms.translate(pos.getX() - camera.x - rotationPoint.x, pos.getY() - camera.y - rotationPoint.y,
                pos.getZ() - camera.z - rotationPoint.z);
            mc.getBlockRenderer().renderSingleBlock(params.getState(), ms, mc.renderBuffers().bufferSource(),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            ms.popPose();
        }
    }

    private static class TransparentGhostBlockRenderer extends GhostBlockRenderer {

        @Override
        public void render(PoseStack ms, Vec3 camera, GhostBlockParams params) {
            Minecraft mc = Minecraft.getInstance();
            float alpha = params.getAlphaSupplier().get() * .75f * PlacementClient.getCurrentAlpha();

            MultiBufferSource shared = mc.renderBuffers().bufferSource();
            MultiBufferSource tinted = params.isTinted()
                ? new TintedBufferSource(shared, params.getTintRed(), params.getTintGreen(), params.getTintBlue(), alpha)
                : new TintedBufferSource(shared, 1, 1, 1, alpha);

            ms.pushPose();
            BlockPos pos = params.getPos();
            SublevelRenderOffsetHelper.posePlotToProjected(Sable.HELPER.getContainingClient(pos), ms);
            Vec3 rotationPoint = SublevelRenderOffsetHelper.translation(new Vec3(pos.getX(), pos.getY(), pos.getZ()));
            ms.translate(pos.getX() - camera.x - rotationPoint.x, pos.getY() - camera.y - rotationPoint.y,
                pos.getZ() - camera.z - rotationPoint.z);

            ms.translate(.5, .5, .5);
            ms.scale(.85f, .85f, .85f);
            ms.translate(-.5, -.5, -.5);

            mc.getBlockRenderer().renderSingleBlock(params.getState(), ms, tinted,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);

            ms.popPose();
        }
    }

    @MethodsReturnNonnullByDefault
    private record TintedBufferSource(MultiBufferSource delegate, float red, float green, float blue, float alpha)
        implements MultiBufferSource {

        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            return new TintingVertexConsumer(delegate.getBuffer(renderType), red, green, blue, alpha);
        }
    }

    @MethodsReturnNonnullByDefault
    private record TintingVertexConsumer(VertexConsumer delegate, float red, float green, float blue, float alpha)
        implements VertexConsumer {

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            delegate.setColor((int) (r * red), (int) (g * green), (int) (b * blue), (int) (a * alpha));
            return this;
        }

        @Override
        public VertexConsumer setColor(float r, float g, float b, float a) {
            delegate.setColor(r * red, g * green, b * blue, a * alpha);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            delegate.setNormal(x, y, z);
            return this;
        }
    }
}
