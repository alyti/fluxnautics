package dev.alyti.fluxnautics.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.FluxnauticsClient;
import dev.alyti.fluxnautics.client.render.model.MekaRobit;
import dev.alyti.fluxnautics.content.entities.gear.EntityMekaRobit;
import dev.alyti.fluxnautics.content.items.drone.MekaRobitClientHandler;
import dev.alyti.fluxnautics.content.items.drone.MekaRobitItem;
import dev.alyti.fluxnautics.math.LerpedFloat;
import dev.alyti.fluxnautics.registry.FNDataComponents;
import foundry.veil.api.client.render.MatrixStack;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import mekanism.client.render.item.MekanismISTER;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;

import java.util.Objects;

public class MekaRobitItemRenderer extends MekanismISTER {

    public static final MekaRobitItemRenderer RENDERER = new MekaRobitItemRenderer();

    public static final ResourceLocation TEXTURE = Fluxnautics.id("textures/entity/meka_robit.png");
    public static final ResourceLocation EMISSIVE = Fluxnautics.id("textures/entity/meka_robit_emissive.png");

    public MekaRobit model;
    private LerpedFloat zRot = LerpedFloat.angular();

    @Override
    public void onResourceManagerReload(@NotNull ResourceManager resourceManager) {
        model = new MekaRobit(Minecraft.getInstance().getEntityModels().bakeLayer(MekaRobit.LAYER_LOCATION));
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (model == null) {
            return;
        }

        BakedModel bakedModel = Minecraft.getInstance().getItemRenderer().getModel(stack, null, null, 0);
        ClientHooks.handleCameraTransforms(poseStack, bakedModel, transformType, transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND);

        boolean isSpawned = stack.has(FNDataComponents.MEKA_ROBIT_BOUND_ENTITY.get());
        float tick = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.tickCount : 0;

        model.root().getAllParts().forEach(ModelPart::resetPose);

        model.core.visible = !isSpawned;
        model.shell.visible = !isSpawned;
        model.wings_h.visible = true;
        model.wings_v.visible = true;

        MekaRobitClientHandler handler = FluxnauticsClient.MEKA_ROBIT_CLIENT_HANDLER;

        double zRot = tick * 0.001 * (Math.max(0.5f, handler.extension));

        poseStack.translate(0.5, -0.6, 0.0F);

        if (!isSpawned) {
            model.core.zRot = tick * 0.05F;
        } else {
            model.wings_h.zRot += zRot;
            model.wings_v.zRot += zRot;
        }

        if (transformType == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            float flip = transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? -1.0F : 1.0F;
            poseStack.translate(0.0F, 0.5F, 0.4F);
            poseStack.scale(0.6F, 0.6F, 0.6F);
            poseStack.mulPose(Axis.YN.rotationDegrees(flip * 10.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(10.0F));
            if (!isSpawned) {
                poseStack.translate(0, 0.5, 0);
            }
        } else if (transformType == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || transformType == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            poseStack.translate(0.4F, 0F, -0.1F);
            poseStack.scale(1.5F, 1.5F, 1.5F);
            if (!isSpawned) {
                poseStack.translate(0, 1, 0);
            }
        }

        VertexConsumer vertexconsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        model.renderToBuffer(poseStack, vertexconsumer, packedLight, packedOverlay);

        if (transformType != ItemDisplayContext.GUI) {
            VertexConsumer emissiveConsumer = buffer.getBuffer(SFRenderTypes.eyes(EMISSIVE));
            model.renderToBuffer(poseStack, emissiveConsumer, packedLight, packedOverlay);
        }
    }

    @EventBusSubscriber(value = Dist.CLIENT, modid = Fluxnautics.MODID)
    public static class MekaRobitHandRenderer {

        @SubscribeEvent
        public static void onRenderPlayerHand(RenderHandEvent event) {
            ItemStack heldItem = event.getItemStack();
            if (!(heldItem.getItem() instanceof MekaRobitItem)) return;

            boolean isSpawned = heldItem.has(FNDataComponents.MEKA_ROBIT_BOUND_ENTITY.get());
            if (!isSpawned) return;

            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null) return;

            InteractionHand hand = event.getHand();
            boolean rightHand = hand == InteractionHand.MAIN_HAND ^ player.getMainArm() == HumanoidArm.LEFT;
            boolean isMainHand = hand == InteractionHand.MAIN_HAND;
            float flip = rightHand ? 1.0F : -1.0F;

            MekaRobitClientHandler handler = FluxnauticsClient.MEKA_ROBIT_CLIENT_HANDLER;
            if (!handler.isInFPV()) {
                PoseStack poseStack = event.getPoseStack();
                MultiBufferSource bufferSource = event.getMultiBufferSource();
                int packedLight = event.getPackedLight();

                event.setCanceled(true);

                float equipProgress = event.getEquipProgress();
                float swingProgress = event.getSwingProgress();

                poseStack.pushPose();
                poseStack.translate(flip * (0.64000005F - .1f), -0.4F + equipProgress * -0.6F, -0.71999997F + .3f);

                poseStack.pushPose();
                poseStack.mulPose(Axis.YP.rotationDegrees(flip * 75.0F));
                poseStack.translate(flip * -1.0F, 3.6F, 3.5F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(flip * 120.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(200.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(flip * -135.0F));
                poseStack.translate(flip * 5.6F, 0.0F, 0.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(flip * 40.0F));
                poseStack.translate(flip * 0.05f, -0.1f, -0.3f);

                PlayerRenderer playerrenderer = (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(player);
                if (rightHand) {
                    playerrenderer.renderRightHand(poseStack, bufferSource, packedLight, player);
                } else {
                    playerrenderer.renderLeftHand(poseStack, bufferSource, packedLight, player);
                }
                poseStack.popPose();

                poseStack.pushPose();
                poseStack.translate(flip * -0.1f, 0, -0.3f);

                ItemInHandRenderer firstPersonRenderer = mc.getEntityRenderDispatcher().getItemInHandRenderer();
                ItemDisplayContext transform = rightHand ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
                firstPersonRenderer.renderItem(player, heldItem, transform, !rightHand, poseStack, bufferSource, packedLight);

                poseStack.popPose();
                poseStack.popPose();
            } else {
                if (event.getHand() != InteractionHand.MAIN_HAND) {
                    event.setCanceled(true);
                    return;
                }

                EntityMekaRobit drone = handler.getFPVDrone();
                if (drone == null) return;
                event.setCanceled(true);
            }

        }
        @SubscribeEvent
        public static void onRenderGui(RenderGuiLayerEvent.Pre event) {
            MekaRobitClientHandler handler = FluxnauticsClient.MEKA_ROBIT_CLIENT_HANDLER;
            if (!handler.isInFPV()) return;

            ResourceLocation layerId = event.getName();

            if (VanillaGuiLayers.HOTBAR.equals(layerId) ||
                VanillaGuiLayers.EXPERIENCE_LEVEL.equals(layerId) ||
                VanillaGuiLayers.EXPERIENCE_BAR.equals(layerId) ||
                VanillaGuiLayers.PLAYER_HEALTH.equals(layerId) ||
                VanillaGuiLayers.ARMOR_LEVEL.equals(layerId) ||
                VanillaGuiLayers.FOOD_LEVEL.equals(layerId) ||
                VanillaGuiLayers.AIR_LEVEL.equals(layerId) ||
                VanillaGuiLayers.JUMP_METER.equals(layerId)) {
                event.setCanceled(true);
            }
        }

        public static void onRenderLevel(final VeilRenderLevelStageEvent.Stage stage, final LevelRenderer levelRenderer, final MultiBufferSource.BufferSource bufferSource, final MatrixStack matrixStack, final Matrix4fc frustumMatrix, final Matrix4fc projectionMatrix, final int renderTick, final DeltaTracker deltaTracker, final Camera camera, final Frustum frustum) {
            if (stage != VeilRenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) return;

            MekaRobitClientHandler handler = FluxnauticsClient.MEKA_ROBIT_CLIENT_HANDLER;
            if (!handler.isInFPV()) return;

            EntityMekaRobit drone = handler.getFPVDrone();
            if (drone == null) return;

            Minecraft mc = Minecraft.getInstance();

            PoseStack poseStack =  matrixStack.toPoseStack();

            float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(false);
            int blendedColor = MekaRobitRenderer.getBlendedColor(drone, partialTicks);
            int tintedEmissiveColor = (blendedColor & 0x00FFFFFF) | 0xFF000000;
            renderFpvShader(tintedEmissiveColor);

            poseStack.pushPose();

            MekaRobit fpvModel = MekaRobitItemRenderer.RENDERER.model;
            poseStack.translate(0.0F, -1.1375F, 0.0F);
            poseStack.translate(0.0F, 0.57F, -0.1F);
            poseStack.scale(0.5F, 0.5F, 0.5F);

            float ageInTicks = drone.tickCount + partialTicks;
            fpvModel.setupAnim(drone, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

            fpvModel.core.visible = false;
            fpvModel.shell.visible = false;
            fpvModel.thruster.visible = false;
            fpvModel.wings_h.visible = true;
            fpvModel.wings_v.visible = true;


            int packedLight = 15728880;
            VertexConsumer vertexconsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(MekaRobitItemRenderer.TEXTURE));
            fpvModel.renderToBuffer(poseStack, vertexconsumer, packedLight, OverlayTexture.NO_OVERLAY);

            VertexConsumer emissiveConsumer = bufferSource.getBuffer(SFRenderTypes.endPortalEyes(MekaRobitItemRenderer.EMISSIVE));
            fpvModel.renderToBuffer(poseStack, emissiveConsumer, packedLight, OverlayTexture.NO_OVERLAY, tintedEmissiveColor);
            bufferSource.endBatch();

            poseStack.popPose();
        }

        private static void renderFpvShader(int color) {
            ShaderProgram shader = VeilRenderSystem.setShader(Fluxnautics.id("meka_fpv"));
            if (shader == null) return;

            Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
            modelViewStack.pushMatrix();
            modelViewStack.identity();

            RenderSystem.backupProjectionMatrix();
            Matrix4f orthoMatrix = new Matrix4f().ortho(-1.0F, 1.0F, -1.0F, 1.0F, -1.0F, 1.0F);
            RenderSystem.setProjectionMatrix(orthoMatrix, com.mojang.blaze3d.vertex.VertexSorting.ORTHOGRAPHIC_Z);

            shader.bind();

            RenderSystem.disableDepthTest();
            RenderSystem.disableBlend();

            BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            bufferBuilder.addVertex(-1.0F, -1.0F, 0.0F).setColor(color);
            bufferBuilder.addVertex(1.0F, -1.0F, 0.0F).setColor(color);
            bufferBuilder.addVertex(1.0F, 1.0F, 0.0F).setColor(color);
            bufferBuilder.addVertex(-1.0F, 1.0F, 0.0F).setColor(color);

            BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());

            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            ShaderProgram.unbind();

            modelViewStack.popMatrix();
            RenderSystem.restoreProjectionMatrix();
        }
    }
}