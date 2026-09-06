package dev.alyti.fluxnautics.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.alyti.fluxnautics.Fluxnautics;
import foundry.veil.api.client.render.MatrixStack;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL31;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlackHoleRenderer {
    private static final ResourceLocation SHADER = Fluxnautics.id("wormhole/wormhole");

    public static RenderTarget miscTranslucentTexture = null;
    private static final int uAABBSize = 14;
    private static final List<Vector3f> blackHoles = new ArrayList<>();
    private static final Matrix4f projectionMatrix = new Matrix4f();
    private static final Matrix4f viewMatrix = new Matrix4f();


    private static RenderTarget depthTextureForTranslucency;
    private static int cachedSlot = -1;

    private static RenderTarget worldTexture = null;

    private static Vec3 lastCameraPos = new Vec3(0f, 0f, 0f);

    private static int findFreeTextureSlot() {
        int maxUnits = GL31.glGetInteger(GL31.GL_MAX_TEXTURE_IMAGE_UNITS) - 1;
        int freeSlot = -1;

        int originalActiveUnit = GL31.glGetInteger(GL31.GL_ACTIVE_TEXTURE);

        for (int i = maxUnits - 1; i >= 0; i--) {
            GL31.glActiveTexture(GL31.GL_TEXTURE0 + i);
            int boundTexture = GL31.glGetInteger(GL31.GL_TEXTURE_BINDING_2D);

            if (boundTexture == 0) {
                freeSlot = i;
                break;
            }
        }

        GL31.glActiveTexture(originalActiveUnit);

        if (freeSlot == -1) {
            throw new RuntimeException("Failed to find free texture slot.");
        }

        return freeSlot;
    }

    private static void drawBlackHoleToDepthBuffer(PoseStack poseStack, Vector3f bhPos, Camera camera) {
        // Draws the black hole to a separate depth buffer for future minecraft transparent passes filtering
        Vec3 camPos = camera.getPosition();
        poseStack.pushPose();

        var viewSpaceSpherePos = poseStack.last().pose().transform(
                new Vector4f(bhPos.x - (float) camPos.x, bhPos.y - (float) camPos.y, bhPos.z - (float) camPos.z, 1.0f));

        Tesselator tessellator = Tesselator.getInstance();
//        BufferBuilder builder = tessellator.begin();

//        ShaderInstance shader = MoniShaders.WORMHOLE_SHADER;
//        RenderSystem.setShader(() -> shader);

        final ShaderProgram shader = VeilRenderSystem.setShader(SHADER);


        shader.getUniformSafe("SpherePos").setVector(viewSpaceSpherePos.x, viewSpaceSpherePos.y,
                viewSpaceSpherePos.z);
        shader.getUniformSafe("uWriteOnlyDepth").setDouble(1);
        shader.getUniformSafe("uMass").setFloat(9.05f);
        shader.getUniformSafe("uDistModifier").setFloat(0.2f);
        shader.getUniformSafe("uSchwarzschildRadius").setFloat(2.55f);
        shader.getUniformSafe("uSphereRadius").setFloat(5.2f);

        AABB box = BlackHoleRendererHelpers.createAABBAt(bhPos, uAABBSize);
        BufferBuilder b = tessellator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        BlackHoleRendererHelpers.addBoxTriangles(poseStack, b,
                (float) (box.minX - camPos.x), (float) (box.minY - camPos.y), (float) (box.minZ - camPos.z),
                (float) (box.maxX - camPos.x), (float) (box.maxY - camPos.y), (float) (box.maxZ - camPos.z),
                1, 1, 0, 1);

        b.build();

        poseStack.popPose();
    }

    public static void updateTextures() {
        cachedSlot = -1;
        Window w = Minecraft.getInstance().getWindow();
        int mcWidth = w.getWidth();
        int mcHeight = w.getHeight();
        if (mcWidth == 0 || mcHeight == 0 ||
                (worldTexture != null && mcWidth == worldTexture.width && mcHeight == worldTexture.height)) {
            return;
        }

        if (depthTextureForTranslucency != null) {
            depthTextureForTranslucency.resize(mcWidth, mcHeight, false);
        } else {
            depthTextureForTranslucency = new TextureTarget(mcWidth, mcHeight, true, Minecraft.ON_OSX);
        }

        if (worldTexture != null) {
            worldTexture.resize(mcWidth, mcHeight, false);
        } else {
            worldTexture = new TextureTarget(mcWidth, mcHeight, true, Minecraft.ON_OSX);
        }

        if (miscTranslucentTexture != null) {
            miscTranslucentTexture.resize(mcWidth, mcHeight, false);
        } else {
            miscTranslucentTexture = new TextureTarget(mcWidth, mcHeight, true, Minecraft.ON_OSX);
        }
    }

    public static boolean hasBlackHoles() {
        return !blackHoles.isEmpty();
    }

    public static void render(Vector3f position) {
        blackHoles.add(position);
    }

    public static void handleTranslucentPassBegin(int programHandle) {
        if (!BlackHoleRendererHelpers.isRenderingMinecraftTranslucentLayer /* || Iris.getCurrentPack().isPresent() */ ||
                !hasBlackHoles()) {
            return;
        }

        int activeUnit = GL31.glGetInteger(GL31.GL_ACTIVE_TEXTURE);
        int uDepthLocation = GL31.glGetUniformLocation(programHandle, "u_BlackHoleDepthTexture");

        if (uDepthLocation != -1) {
            if (cachedSlot == -1) {
                cachedSlot = findFreeTextureSlot();
            }
            int targetUnit = cachedSlot;

            GL31.glUniform1i(uDepthLocation, targetUnit);

            GL31.glActiveTexture(GL31.GL_TEXTURE0 + targetUnit);
            GL31.glBindTexture(GL31.GL_TEXTURE_2D, depthTextureForTranslucency.getDepthTextureId());

            GL31.glActiveTexture(activeUnit);
        }
    }

    public static void preTranslucentPass(PoseStack poseStack, Camera camera) {
        miscTranslucentTexture.copyDepthFrom(Minecraft.getInstance().getMainRenderTarget());
        miscTranslucentTexture.bindWrite(true);
        RenderSystem.clear(GL31.GL_COLOR_BUFFER_BIT, false);

        int currentFBO = GL31.glGetInteger(GL31.GL_FRAMEBUFFER_BINDING);

        depthTextureForTranslucency.bindWrite(true);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(GL31.GL_ALWAYS);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.clearDepth(1);
        RenderSystem.clear(GL31.GL_DEPTH_BUFFER_BIT, false);
        GL31.glCullFace(GL31.GL_FRONT); // To render only back faces

        for (Vector3f bh : blackHoles) {
            drawBlackHoleToDepthBuffer(poseStack, bh, camera);
        }

        GL31.glCullFace(GL31.GL_BACK);
        RenderSystem.depthFunc(GL31.GL_LEQUAL);
        GL31.glBindFramebuffer(GL31.GL_FRAMEBUFFER, currentFBO);

        BlackHoleRendererHelpers.isRenderingMinecraftTranslucentLayer = true;
        BlackHoleRendererHelpers.isRenderingMinecraftTranslucentLayer = false;
    }

    private static void renderCore(PoseStack poseStack, Vec3 cameraPos, Matrix4f projectionMatrix, MultiBufferSource.BufferSource bufferSource) {
//        Window w = Minecraft.getInstance().getWindow();
//        if (worldTexture.width != w.getWidth() ||
//                worldTexture.height != w.getHeight()) {
//            updateTextures();
//        }

        poseStack.pushPose();
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(projectionMatrix, null);
        RenderSystem.enableDepthTest();
//        GL31.glCullFace(GL31.GL_FRONT);

//        var mcWorldTexture = Minecraft.getInstance().getMainRenderTarget();

//        GL31.glBindFramebuffer(GL31.GL_READ_FRAMEBUFFER, mcWorldTexture.frameBufferId);
//        GL31.glBindFramebuffer(GL31.GL_DRAW_FRAMEBUFFER, worldTexture.frameBufferId);
//        GlStateManager._glBlitFrameBuffer(0, 0, w.getWidth(), w.getHeight(), 0, 0, w.getWidth(), w.getHeight(),
//                GL31.GL_COLOR_BUFFER_BIT, GL31.GL_NEAREST);
//        GlStateManager._glBlitFrameBuffer(0, 0, w.getWidth(), w.getHeight(), 0, 0, w.getWidth(), w.getHeight(),
//                GL31.GL_DEPTH_BUFFER_BIT, GL31.GL_NEAREST);

//        mcWorldTexture.bindWrite(true);

        final ShaderProgram shader = VeilRenderSystem.setShader(SHADER);
        shader.bind();

        shader.getUniformSafe("uWriteOnlyDepth").setInt(0);
        shader.getUniformSafe("uMass").setFloat(9.05f);
        shader.getUniformSafe("uDistModifier").setFloat(0.2f);
        shader.getUniformSafe("uSchwarzschildRadius").setFloat(2.55f);
        shader.getUniformSafe("uSphereRadius").setFloat(5.2f);

//        shader.setTexture("WorldColor", worldTexture.) .setSampler("WorldColor", worldTexture);
//        RenderSystem.setShaderTexture(0, worldTexture.getColorTextureId());
//        shader.getUniformSafe("uWriteOnlyDepth").setDouble(0);

        Tesselator tesselator = Tesselator.getInstance();

        for (Vector3f blackHolePos : blackHoles) {
            BufferBuilder builder = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
            var viewSpaceSpherePos = poseStack.last().pose().transform(
                    new Vector4f(blackHolePos.x - (float) cameraPos.x, blackHolePos.y - (float) cameraPos.y,
                            blackHolePos.z - (float) cameraPos.z, 1.0f));
            shader.getUniformSafe("SpherePos").setVector(viewSpaceSpherePos.x, viewSpaceSpherePos.y,
                    viewSpaceSpherePos.z);
            AABB box = BlackHoleRendererHelpers.createAABBAt(blackHolePos, uAABBSize);
            BlackHoleRendererHelpers.addBoxTriangles(poseStack, builder,
                    (float) (box.minX - cameraPos.x), (float) (box.minY - cameraPos.y),
                    (float) (box.minZ - cameraPos.z),
                    (float) (box.maxX - cameraPos.x), (float) (box.maxY - cameraPos.y),
                    (float) (box.maxZ - cameraPos.z),
                    1, 1, 0, 1);
//            builder.build();

            BufferUploader.drawWithShader(builder.buildOrThrow());
        }

        GL31.glCullFace(GL31.GL_BACK);
        poseStack.popPose();

//        if (Iris.getCurrentPack().isEmpty()) {
            RenderSystem.enableBlend();
//            miscTranslucentTexture.blitToScreen(miscTranslucentTexture.width, miscTranslucentTexture.height, false);
            RenderSystem.enableDepthTest();
//        }

        blackHoles.clear();
        ShaderProgram.unbind();
        RenderSystem.restoreProjectionMatrix();
    }

    public static void renderStage(final VeilRenderLevelStageEvent.Stage stage, final LevelRenderer levelRenderer, final MultiBufferSource.BufferSource bufferSource, final MatrixStack matrixStack, final Matrix4fc frustumMatrix, final Matrix4fc projectionMatrix, final int renderTick, final DeltaTracker deltaTracker, final Camera camera, final Frustum frustum) {

        if (!hasBlackHoles() || stage != VeilRenderLevelStageEvent.Stage.AFTER_WEATHER )
            return;
//        if (Iris.getCurrentPack().isPresent()) {
//            lastCameraPos = event.getCamera().getPosition();
//            // copy because viewMatrix = event.getPoseStack().last().pose() doesnt work :(
//            viewMatrix.identity().mul(event.getPoseStack().last().pose());
//            projectionMatrix.identity().mul(event.getProjectionMatrix());
//            return;
//        }
        renderCore(matrixStack.toPoseStack(), camera.getPosition(), (Matrix4f) projectionMatrix, bufferSource);
    }
//
//    public static void renderWithShadersOn() {
//        if (!hasBlackHoles()) {
//            return;
//        }
//        Window w = Minecraft.getInstance().getWindow();
//        if (worldTexture.width != w.getWidth() ||
//                worldTexture.height != w.getHeight()) {
//            updateTextures();
//        }
//        PoseStack poseStack = new PoseStack();
//        poseStack.setIdentity();
//        poseStack.mulPoseMatrix(viewMatrix);
//        renderCore(poseStack, lastCameraPos, projectionMatrix);
//    }
//
//    @SubscribeEvent
//    public static void onResize(ScreenEvent.Init.Post event) {
//        updateTextures();
//    }
}