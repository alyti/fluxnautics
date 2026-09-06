package dev.alyti.fluxnautics.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.client.render.model.MekaRobit;
import dev.alyti.fluxnautics.client.util.RenderBufferHelper;
import dev.alyti.fluxnautics.content.entities.gear.EntityMekaRobit;
import dev.alyti.fluxnautics.util.SFColors;
import dev.ryanhcode.sable.Sable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.awt.Color;

public class MekaRobitRenderer extends MobRenderer<EntityMekaRobit, MekaRobit> {

    private static final ResourceLocation TEXTURE = Fluxnautics.id("textures/entity/meka_robit.png");
    private static final ResourceLocation EMISSIVE = Fluxnautics.id("textures/entity/meka_robit_emissive.png");

    public MekaRobitRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new MekaRobit(ctx.bakeLayer(MekaRobit.LAYER_LOCATION)), 0.3F);

        this.addLayer(new EyesLayer<>(this) {
            @Override
            public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, EntityMekaRobit e, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
                VertexConsumer vertexconsumer = buffer.getBuffer(this.renderType());
                int blendedColor = getBlendedColor(e, partialTicks);
                int tintedColor = (blendedColor & 0x00FFFFFF) | 0x11000000;
                this.getParentModel().renderToBuffer(poseStack, vertexconsumer, packedLight, OverlayTexture.NO_OVERLAY, tintedColor);
            }

            @Override
            public RenderType renderType() {
                return SFRenderTypes.endPortalEffect.apply(EMISSIVE, RenderStateShard.TRANSLUCENT_TRANSPARENCY);
            }
        });

        this.addLayer(new LayerLaserBeams(this));
    }

    @Override
    public ResourceLocation getTextureLocation(EntityMekaRobit e) { return TEXTURE; }

    @Override
    protected boolean isBodyVisible(EntityMekaRobit e) { return true; }

    @Override
    protected RenderType getRenderType(EntityMekaRobit e, boolean bodyVisible, boolean translucent, boolean glowing) {
        return RenderType.entityTranslucent(TEXTURE);
    }

    @Override
    protected void setupRotations(EntityMekaRobit e, PoseStack pose, float bob, float yaw, float partial, float scale) {
        float smoothYaw = e.smoothYaw.getValue(partial);
        float smoothPitch = e.smoothPitch.getValue(partial);
        float smoothRoll = e.smoothRoll.getValue(partial);

        float tick = e.tickCount + partial;

        pose.mulPose(Axis.YP.rotationDegrees(180.0F - smoothYaw));
        pose.mulPose(Axis.XP.rotationDegrees(-smoothPitch));
        pose.mulPose(Axis.ZP.rotationDegrees(smoothRoll));
        pose.translate(0.0F, Mth.sin(tick * 0.1F) * 0.05F, 0.0F);
    }

    public static Vector4f getColorVector(int colorInt, boolean isRainbow, float partialTicks) {
        Color c = new Color(colorInt, true);
        if (isRainbow) {
            final org.joml.Vector3d baseLCh = SFColors.LabToLCh(SFColors.toOklab(c));
            final float t = (float) ((((double) Util.getMillis() / 50) % 100 + partialTicks) * 2 * Math.PI / 100f);
            c = SFColors.LChOklab(0.8f, 0.3f, (float) (t + baseLCh.z()));
        }
        return new Vector4f(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f);
    }

    public static int getBlendedColor(EntityMekaRobit drone, float partialTicks) {
        Vector4f idle = getColorVector(drone.getIdleColor(), drone.isRainbowMode(), partialTicks);
        Vector4f attack = getColorVector(drone.getAttackColor(), drone.isRainbowMode(), partialTicks);

        float t = drone.clientColorTransition;
        float r = Mth.lerp(t, idle.x, attack.x);
        float g = Mth.lerp(t, idle.y, attack.y);
        float b = Mth.lerp(t, idle.z, attack.z);
        float a = Mth.lerp(t, idle.w, attack.w);

        int ri = (int)(r * 255) & 0xFF;
        int gi = (int)(g * 255) & 0xFF;
        int bi = (int)(b * 255) & 0xFF;
        int ai = (int)(a * 255) & 0xFF;

        return (ai << 24) | (ri << 16) | (gi << 8) | bi;
    }
    public static class LayerLaserBeams extends RenderLayer<EntityMekaRobit, MekaRobit> {

        public boolean focusBeam = true;

        public LayerLaserBeams(MekaRobitRenderer renderer) {
            super(renderer);
        }

        public void setFocusBeam(boolean focus) {
            this.focusBeam = focus;
        }

        private Vec3 projectToPose(PoseStack pose, Vec3 worldPos) {
            Vec3 camPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            Vector4f vec = new Vector4f(
                (float)(worldPos.x - camPos.x),
                (float)(worldPos.y - camPos.y),
                (float)(worldPos.z - camPos.z),
                1.0f
            );
            Matrix4f worldToLocal = new Matrix4f(pose.last().pose()).invert();
            worldToLocal.transform(vec);
            return new Vec3(vec.x, vec.y, vec.z);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffer, int packedLight, EntityMekaRobit drone, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if (drone.getCombatState() != EntityMekaRobit.STATE_ATTACK_ONGOING) return;

            Entity target = drone.level().getEntity(drone.getTargetId());
            Vec3 rawTargetPos;

            if (target != null) {
                rawTargetPos = target.getEyePosition(partialTicks).subtract(0, 0.2f, 0);
            } else {
                rawTargetPos = Sable.HELPER.projectOutOfSubLevel(drone.level(), drone.getLaserTarget());
            }

            if (rawTargetPos.equals(Vec3.ZERO))
                return;

            if (!drone.clientTargetValid) {
                drone.clientTargetPos = rawTargetPos;
                drone.clientTargetValid = true;
            } else {
                if (drone.clientTargetPos.distanceToSqr(rawTargetPos) > 16.0D) {
                    drone.clientTargetPos = rawTargetPos;
                } else {
                    drone.clientTargetPos = drone.clientTargetPos.lerp(rawTargetPos, 0.5f);
                }
            }
            Vec3 targetPos = drone.clientTargetPos;

            VertexConsumer consumer = buffer.getBuffer(SFRenderTypes.END_PORTAL_CUSTOM);
            TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ResourceLocation.parse("minecraft:block/white_concrete"));

            Vec3 leaderPos;
            if (drone.getMainDroneId() != -1) {
                EntityMekaRobit main = drone.getMainDrone();
                if (main == null) return;
                leaderPos = main.getEyePosition(partialTicks);
            } else if (drone.isSwarmMinion()) {
                Player owner = drone.getOwner();
                if (owner == null) return;
                leaderPos = owner.getEyePosition(partialTicks);
            } else {
                leaderPos = drone.getEyePosition(partialTicks);
            }

            Vec3 droneEyePos = drone.getEyePosition(partialTicks);

            double droneToTargetDist = droneEyePos.distanceTo(targetPos);
            double smallDist = Math.min(2.0, droneToTargetDist * 0.25);
            Vec3 smallWorldFocus = droneToTargetDist < 0.001 ? droneEyePos : droneEyePos.add(targetPos.subtract(droneEyePos).scale(smallDist / droneToTargetDist));

            double leaderToTargetDist = leaderPos.distanceTo(targetPos);
            double bigDist = Math.min(16.0, leaderToTargetDist * 0.35);
            Vec3 bigWorldFocus = leaderToTargetDist < 0.001 ? leaderPos : leaderPos.add(targetPos.subtract(leaderPos).scale(bigDist / leaderToTargetDist));

            Vec3 end = projectToPose(pose, targetPos);
            Vec3 smallFocus = projectToPose(pose, smallWorldFocus);
            Vec3 bigFocus = drone.isFocusFiring() ? projectToPose(pose, bigWorldFocus) : null;

            float rotZ = getParentModel().wings_h.zRot;

            double centerYLocal = 1.126;
            double r = 0.45;
            double zOffset = -0.3;

            Vec3[] baseTipsLocal = new Vec3[] {
                new Vec3(r, centerYLocal, zOffset),
                new Vec3(0, centerYLocal + r, zOffset),
                new Vec3(-r, centerYLocal, zOffset),
                new Vec3(0, centerYLocal - r, zOffset)
            };

            double cos = Mth.cos(rotZ);
            double sin = Mth.sin(rotZ);

            int blendedColor = getBlendedColor(drone, partialTicks);

            float thicknessMod = drone.smoothBeamThickness.getValue(partialTicks);

            float smallBeam = 0.02F * thicknessMod;
            float smallCrystal = 0.10F * thicknessMod;
            float bigBeam = 0.05F * thicknessMod;
            float bigCrystal = 0.15F * thicknessMod;
            float mainBeam = 0.20F * thicknessMod;
            float singleBeam = 0.10F * thicknessMod;

            for (Vec3 baseTipLocal : baseTipsLocal) {
                double x = baseTipLocal.x;
                double y = baseTipLocal.y - centerYLocal;
                Vec3 tip = new Vec3(x * cos - y * sin, centerYLocal + x * sin + y * cos, baseTipLocal.z);

                drawBeam(consumer, pose, sprite, blendedColor, packedLight, tip, smallFocus, smallBeam, ageInTicks);
            }

            drawFocusCrystal(consumer, pose, sprite, blendedColor, packedLight, smallFocus, smallCrystal, ageInTicks);

            if (drone.isFocusFiring() && bigFocus != null) {
                drawBeam(consumer, pose, sprite, blendedColor, packedLight, smallFocus, bigFocus, bigBeam, ageInTicks);
                drawFocusCrystal(consumer, pose, sprite, blendedColor, packedLight, bigFocus, bigCrystal, ageInTicks);

                if ((!drone.isSwarmMinion()) || (drone.getMainDroneId() == -1 && drone.getFormationIndex() == 0)) {
                    drawBeam(consumer, pose, sprite, blendedColor, packedLight, bigFocus, end, mainBeam, ageInTicks);
                }
            } else {
                drawBeam(consumer, pose, sprite, blendedColor, packedLight, smallFocus, end, singleBeam, ageInTicks);
            }
        }

        private void drawFocusCrystal(VertexConsumer consumer, PoseStack pose, TextureAtlasSprite sprite, int color, int light, Vec3 pos, float size, float tick) {
            pose.pushPose();
            pose.translate(pos.x, pos.y, pos.z);

            pose.mulPose(Axis.XP.rotationDegrees(45));
            pose.mulPose(Axis.YP.rotationDegrees(45));
            pose.mulPose(Axis.ZP.rotationDegrees(45));

            pose.mulPose(Axis.XP.rotationDegrees(tick * 10.0F));
            pose.mulPose(Axis.YP.rotationDegrees(tick * 10.0F));

            float half = size / 2.0F;
            RenderBufferHelper.renderCube(consumer, pose.last(), color, light, sprite, -half, -half, -half, half, half, half);

            pose.popPose();
        }

        private void drawBeam(VertexConsumer consumer, PoseStack pose, TextureAtlasSprite sprite, int color, int light, Vec3 start, Vec3 end, float thickness, float tick) {
            Vec3 dir = end.subtract(start);
            float length = (float) dir.length();
            if (length < 0.001F) return;

            pose.pushPose();
            pose.translate(start.x, start.y, start.z);

            Vector3f norm = new Vector3f((float)dir.x, (float)dir.y, (float)dir.z).normalize();
            Vector3f forward = new Vector3f(0, 0, 1);
            float dot = forward.dot(norm);

            if (dot < -0.9999F) {
                pose.mulPose(Axis.YP.rotationDegrees(180));
            } else if (dot < 0.9999F) {
                Vector3f axis = new Vector3f();
                forward.cross(norm, axis);
                if (axis.lengthSquared() < 1.0E-6F) {
                    axis.set(0, 1, 0);
                }
                axis.normalize();
                float angle = (float) Math.acos(dot);
                Quaternionf q = new Quaternionf().fromAxisAngleRad(axis, angle);
                pose.mulPose(q);
            }

            float half = thickness / 2.0F;
            RenderBufferHelper.renderCube(consumer, pose.last(), color, light, sprite, -half, -half, 0, half, half, length);

            pose.popPose();
        }
    }
}