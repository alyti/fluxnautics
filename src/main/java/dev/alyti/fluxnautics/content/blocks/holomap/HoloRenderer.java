package dev.alyti.fluxnautics.content.blocks.holomap;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.client.render.SFRenderTypes;
import dev.alyti.fluxnautics.client.util.SimpleHeightmapRenderer;
import dev.alyti.fluxnautics.registry.FNBlocks;
import dev.ryanhcode.sable.Sable;
import foundry.veil.api.client.render.CullFrustum;
import foundry.veil.api.client.render.VeilLevelPerspectiveRenderer;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.FramebufferManager;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.client.render.shader.uniform.ShaderUniform;
import foundry.veil.api.client.render.vertex.VertexArray;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import mekanism.client.render.tileentity.MekanismTileEntityRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.*;
import org.jspecify.annotations.NonNull;

import java.util.Map;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL30C.glGenerateMipmap;

public class HoloRenderer extends MekanismTileEntityRenderer<HoloBlockEntity> {
    private static final ResourceLocation MAP_FBO = Fluxnautics.id("map");
    private static final float RENDER_DISTANCE = 256.0F;

    private static final Matrix4f RENDER_MODELVIEW = new Matrix4f();
    private static final Matrix4f RENDER_PROJECTION = new Matrix4f();
    private static final Vector4f OBLIQUE_PLANE = new Vector4f();
    private static final Quaternionf VIEW = new Quaternionf();

    private static final ObjectSet<BlockPos> RENDER_POSITIONS = new ObjectArraySet<>();
    private static final Long2ObjectMap<MapTexture> TEXTURES = new Long2ObjectArrayMap<>();

    private static final boolean useTessellation = true;
    private static final boolean tessellationWireframe = false;
    private static final boolean tessellateEntities = true;
    private static final int[] baseResolution = new int[]{16};
    private static boolean regenerateMesh = false;


    private static long getKey(BlockPos pos, Direction face) {
        Vec3i normal = face.getNormal();
        return (long) face.getAxis().ordinal() << 60 | ((long) pos.getX() * normal.getX() + (long) pos.getY() * normal.getY() + (long) pos.getZ() * normal.getZ());
    }

    private final VertexArray vao;

    public HoloRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
        this.vao = VertexArray.create();
        this.vao.upload(render(16), VertexArray.DrawUsage.STATIC);
        VertexArray.unbind();
    }

    @Override
    public int getViewDistance() {
        return 16;
    }

    @Override
    protected void render(@NonNull HoloBlockEntity blockEntity, float partialTick, @NonNull PoseStack poseStack, @NonNull MultiBufferSource renderer, int light, int overlayLight, @NonNull ProfilerFiller profiler) {

        BlockPos pos = blockEntity.getBlockPos();
//        Direction facing = blockEntity.getBlockState().getValue(MirrorBlock.FACING);

        RENDER_POSITIONS.add(pos.immutable());
        MapTexture texture = TEXTURES.get(getKey(pos, Direction.UP));
        if (texture == null) {
            return;
        }

//        LogUtils.getLogger().debug("FUCK");
        RenderType renderType = SFRenderTypes.heightmap(true);
        if (renderType == null) {
            return;
        }

        RenderStateShard.ShaderStateShard shard = VeilRenderType.getShards(renderType).shaderState();
        shard.setupRenderState();

        ShaderProgram shader = VeilRenderSystem.getShader();
        if (shader == null) {
            shard.clearRenderState();
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YN.rotationDegrees(Direction.EAST.toYRot() + blockEntity.getYRotation()));
        poseStack.translate(-0.5, -0.5, -0.5);

        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.mul(poseStack.last().pose());
        modelViewStack.translate(-0.8f, 1.3f, -0.8f);
        modelViewStack.scale(2.6f, 1, 2.6f);
//        final Quaternionf orientation = new Quaternionf().rotateY(Mth.DEG_TO_RAD * 90);
//        modelViewStack.rotate(orientation);

        RenderSystem.applyModelViewMatrix();
        RenderSystem.setShaderTexture(0, texture.getId());
        this.vao.bind();
//        if (MapInspector.isRegenerateMesh()) {
//            this.vao.upload(render(MapInspector.getBaseResolution()), VertexArray.DrawUsage.STATIC);
            //setRegenerateMesh(false);
//        }

        ShaderUniform scale = shader.getUniform("Scale");
        if (scale != null) {
//            scale.setVector(getScale());
            scale.setVector(1.0f, 1.0f, 1.0f);
        }
        if (useTessellation) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        }

        this.vao.drawWithRenderType(renderType);

        if (useTessellation) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        }

        VertexBuffer.unbind();

        modelViewStack.popMatrix();
        RenderSystem.applyModelViewMatrix();
        poseStack.popPose();
    }

    @Override
    protected @NonNull String getProfilerSection() {
        return "holo";
    }

    private static MeshData render(int resolution) {
        Tesselator tesselator = RenderSystem.renderThreadTesselator();
        BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        for (int z = 0; z < resolution; z++) {
            for (int x = 0; x < resolution; x++) {
                builder.addVertex(x / (float) resolution, 0, z / (float) resolution)
                        .setUv(x / (float) resolution, z / (float) resolution);

                builder.addVertex((x + 1) / (float) resolution, 0, z / (float) resolution)
                        .setUv((x + 1) / (float) resolution, z / (float) resolution);

                builder.addVertex((x + 1) / (float) resolution, 0, (z + 1) / (float) resolution)
                        .setUv((x + 1) / (float) resolution, (z + 1) / (float) resolution);

                builder.addVertex(x / (float) resolution, 0, (z + 1) / (float) resolution)
                        .setUv(x / (float) resolution, (z + 1) / (float) resolution);
            }
        }

        return builder.buildOrThrow();
    }


    private static float dot(BlockPos pos, Vec3i normal, double x, double y, double z) {
        return (float) ((pos.getX() + 0.5 - normal.getX() * 0.5 - x) * normal.getX() + (pos.getY() + 0.5 - normal.getY() * 0.5 - y) * normal.getY() + (pos.getZ() + 0.5 - normal.getZ() * 0.5 - z) * normal.getZ());
    }

    public static void renderLevel(ClientLevel level, Matrix4fc projection, DeltaTracker deltaTracker, CullFrustum frustum, Camera camera) {
        if (VeilLevelPerspectiveRenderer.isRenderingPerspective() || !projection.isFinite() || Minecraft.getInstance().isPaused()) {
            return;
        }

        FramebufferManager framebufferManager = VeilRenderSystem.renderer().getFramebufferManager();
        AdvancedFbo fbo = framebufferManager.getFramebuffer(MAP_FBO);
        if (fbo == null) {
            return;
        }

        for (BlockPos pos : RENDER_POSITIONS) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(FNBlocks.HOLO)) {
                continue;
            }

            Direction facing = Direction.UP; //state.getValue(MirrorBlock.FACING);
            MapTexture map = TEXTURES.computeIfAbsent(getKey(pos, facing), unused -> new MapTexture());
            map.positions.add(pos);
            if (map.hasRendered()) {
                continue;
            }

            final int volume = 16 * 16;
            if (Sable.HELPER.isInPlotGrid(level, pos)) {
                final Vec3 ppos = Sable.HELPER.projectOutOfSubLevel(level, pos.getCenter());
                final BlockPos a = new BlockPos((int) ppos.x, (int) ppos.y, (int) ppos.z);
                final BlockPos aa = a.immutable().offset(-volume, 0, -volume);
                final BlockPos bb = a.immutable().offset(volume, 0, volume);
                SimpleHeightmapRenderer.generateHeightmapTexture(fbo, aa, bb);
            } else {
                final BlockPos aa = pos.immutable().offset(-volume, 0, -volume);
                final BlockPos bb = pos.immutable().offset(volume, 0, volume);
                SimpleHeightmapRenderer.generateHeightmapTexture(fbo, aa, bb);

            }
//            fbo.bindDraw(false);

//            AdvancedFbo.unbind();
//            AdvancedFbo.unbindDraw();

//            final Quaternionf orientation = new Quaternionf().rotateX(Mth.DEG_TO_RAD * 90);
//            RENDER_PROJECTION.identity().ortho(-volume, volume, -volume, volume, 0.5f, 256);
//            final float chunk = 16;
//            for (int x = -4; x < 4; x++) {
//                for (int z = -4; z < 4; z++) {
//                    VeilLevelPerspectiveRenderer.render(fbo, new Matrix4f().translate(chunk * x, 0f, chunk * z).scale(2f), RENDER_PROJECTION,  new Vector3d(pos.getX() + 0.5 + (chunk * x), 100, pos.getZ() + 0.5 + (chunk * z)), orientation, 128f, deltaTracker, false);
//                }
//            }

            map.copy(fbo);
            map.setRendered(true);
        }

        ObjectIterator<Long2ObjectMap.Entry<MapTexture>> iterator = TEXTURES.long2ObjectEntrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, MapTexture> entry = iterator.next();
            MapTexture map = entry.getValue();
            map.setRendered(false);
            map.positions.removeIf(p -> !RENDER_POSITIONS.contains(p));
            if (map.positions.isEmpty()) {
                iterator.remove();
                map.close();
            }
        }

        RENDER_POSITIONS.clear();
    }

    private static class MapTexture extends AbstractTexture {

        private final ObjectSet<BlockPos> positions;
        private boolean rendered;

        private int width;
        private int height;

        private MapTexture() {
            this.positions = new ObjectArraySet<>();
            this.setFilter(false, true);
            this.width = -1;
            this.height = -1;
        }

        @Override
        public void load(ResourceManager resourceManager) {
        }

        public void copy(AdvancedFbo fbo) {
            int id = this.getId();
            int width = fbo.getWidth();
            int height = fbo.getHeight();
            if (this.width != width || this.height != height) {
                this.width = width;
                this.height = height;
                TextureUtil.prepareImage(NativeImage.InternalGlFormat.RGBA, id, 4, width, height);
            }

            RenderSystem.bindTexture(id);
            fbo.bindRead();
            glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
            AdvancedFbo.unbind();
            glGenerateMipmap(GL_TEXTURE_2D);
        }

        public boolean hasRendered() {
            return this.rendered;
        }

        public void setRendered(boolean rendered) {
            this.rendered = rendered;
        }
    }
}