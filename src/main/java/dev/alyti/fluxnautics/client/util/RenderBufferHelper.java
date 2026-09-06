package dev.alyti.fluxnautics.client.util;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.alyti.fluxnautics.math.MatrixHelper;
import mekanism.common.lib.Color;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.joml.Vector3fc;

import java.util.EnumSet;
import java.util.Set;

public class RenderBufferHelper {
    public static void renderRing(PoseStack poseStack, VertexConsumer buffer, float x, float y, float z, float r,
                                  float tubeRadius,
                                  int sides, int segments, Color c,
                                  Direction.Axis axis, int combinedLight, int combinedOverlay) {
        PoseStack.Pose pose = poseStack.last();
        float sideDelta = (float) (2.0 * Math.PI / sides);
        float ringDelta = (float) (2.0 * Math.PI / segments);

        int color = c.argb();

        float theta = 0;
        float cosTheta = 1.0F;
        float sinTheta = 0.0F;

        for (int i = 0; i < segments; i++) {
            float theta1 = theta + ringDelta;
            float cosTheta1 = Mth.cos(theta1);
            float sinTheta1 = Mth.sin(theta1);

            float phi = 0;
            float cosPhi = 1.0F;
            float sinPhi = 0.0F;

            for (int j = 0; j < sides; j++) {
                float phi1 = phi + sideDelta;
                float cosPhi1 = Mth.cos(phi1);
                float sinPhi1 = Mth.sin(phi1);

                float dist0 = r + (tubeRadius * cosPhi);
                float dist1 = r + (tubeRadius * cosPhi1);

                switch (axis) {
                    case Y:
                        vertex(buffer, pose, x + sinTheta * dist0, y + tubeRadius * sinPhi, z + cosTheta * dist0, color, 0, 0, combinedOverlay, combinedLight, sinTheta * cosPhi, sinPhi, cosTheta * cosPhi);
                        vertex(buffer, pose, x + sinTheta1 * dist0, y + tubeRadius * sinPhi, z + cosTheta1 * dist0, color, 0, 0, combinedOverlay, combinedLight, sinTheta1 * cosPhi, sinPhi, cosTheta1 * cosPhi);
                        vertex(buffer, pose, x + sinTheta1 * dist1, y + tubeRadius * sinPhi1, z + cosTheta1 * dist1, color, 0, 0, combinedOverlay, combinedLight, sinTheta1 * cosPhi1, sinPhi1, cosTheta1 * cosPhi1);
                        vertex(buffer, pose, x + sinTheta * dist1, y + tubeRadius * sinPhi1, z + cosTheta * dist1, color, 0, 0, combinedOverlay, combinedLight, sinTheta * cosPhi1, sinPhi1, cosTheta * cosPhi1);
                        break;
                    case X:
                        vertex(buffer, pose, x + tubeRadius * sinPhi, y + sinTheta * dist0, z + cosTheta * dist0, color, 0, 0, combinedOverlay, combinedLight, sinPhi, sinTheta * cosPhi, cosTheta * cosPhi);
                        vertex(buffer, pose, x + tubeRadius * sinPhi, y + sinTheta1 * dist0, z + cosTheta1 * dist0, color, 0, 0, combinedOverlay, combinedLight, sinPhi, sinTheta1 * cosPhi, cosTheta1 * cosPhi);
                        vertex(buffer, pose, x + tubeRadius * sinPhi1, y + sinTheta1 * dist1, z + cosTheta1 * dist1, color, 0, 0, combinedOverlay, combinedLight, sinPhi1, sinTheta1 * cosPhi1, cosTheta1 * cosPhi1);
                        vertex(buffer, pose, x + tubeRadius * sinPhi1, y + sinTheta * dist1, z + cosTheta * dist1, color, 0, 0, combinedOverlay, combinedLight, sinPhi1, sinTheta * cosPhi1, cosTheta * cosPhi1);
                        break;
                    case Z:
                        vertex(buffer, pose, x + cosTheta * dist0, y + sinTheta * dist0, z + tubeRadius * sinPhi, color, 0, 0, combinedOverlay, combinedLight, cosTheta * cosPhi, sinTheta * cosPhi, sinPhi);
                        vertex(buffer, pose, x + cosTheta1 * dist0, y + sinTheta1 * dist0, z + tubeRadius * sinPhi, color, 0, 0, combinedOverlay, combinedLight, cosTheta1 * cosPhi, sinTheta1 * cosPhi, sinPhi);
                        vertex(buffer, pose, x + cosTheta1 * dist1, y + sinTheta1 * dist1, z + tubeRadius * sinPhi1, color, 0, 0, combinedOverlay, combinedLight, cosTheta1 * cosPhi1, sinTheta1 * cosPhi1, sinPhi1);
                        vertex(buffer, pose, x + cosTheta * dist1, y + sinTheta * dist1, z + tubeRadius * sinPhi1, color, 0, 0, combinedOverlay, combinedLight, cosTheta * cosPhi1, sinTheta * cosPhi1, sinPhi1);
                        break;
                }

                phi = phi1;
                cosPhi = cosPhi1;
                sinPhi = sinPhi1;
            }
            theta = theta1;
            cosTheta = cosTheta1;
            sinTheta = sinTheta1;
        }
    }

    public static void renderParamCube(VertexConsumer buffer, PoseStack.Pose pose,
                                       int color, int combinedLight, int combinedOverlay,
                                       float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        renderParamCube(buffer, pose, EnumSet.allOf(Direction.class),
            color, combinedLight, combinedOverlay,
            minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static void renderParamCube(VertexConsumer buffer, PoseStack.Pose pose, Set<Direction> sidesToRender,
                                       int color, int combinedLight, int combinedOverlay,
                                       float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        if (sidesToRender.contains(Direction.UP))
            renderParamCubeFace(buffer, pose, color, combinedLight, combinedOverlay, Direction.UP,
                minX, maxY, minZ, 0.0F, 0.0F,
                minX, maxY, maxZ, 0.0F, 1.0F,
                maxX, maxY, maxZ, 1.0F, 1.0F,
                maxX, maxY, minZ, 1.0F, 0.0F);

        if (sidesToRender.contains(Direction.DOWN))
            renderParamCubeFace(buffer, pose, color, combinedLight, combinedOverlay, Direction.DOWN,
                minX, minY, minZ, 0.0F, 0.0F,
                maxX, minY, minZ, 1.0F, 0.0F,
                maxX, minY, maxZ, 1.0F, 1.0F,
                minX, minY, maxZ, 0.0F, 1.0F);

        if (sidesToRender.contains(Direction.NORTH))
            renderParamCubeFace(buffer, pose, color, combinedLight, combinedOverlay, Direction.NORTH,
                minX, minY, minZ, 0.0F, 0.0F,
                minX, maxY, minZ, 0.0F, 1.0F,
                maxX, maxY, minZ, 1.0F, 1.0F,
                maxX, minY, minZ, 1.0F, 0.0F);

        if (sidesToRender.contains(Direction.SOUTH))
            renderParamCubeFace(buffer, pose, color, combinedLight, combinedOverlay, Direction.SOUTH,
                minX, minY, maxZ, 0.0F, 0.0F,
                maxX, minY, maxZ, 1.0F, 0.0F,
                maxX, maxY, maxZ, 1.0F, 1.0F,
                minX, maxY, maxZ, 0.0F, 1.0F);

        if (sidesToRender.contains(Direction.WEST))
            renderParamCubeFace(buffer, pose, color, combinedLight, combinedOverlay, Direction.WEST,
                minX, minY, minZ, 0.0F, 0.0F,
                minX, minY, maxZ, 1.0F, 0.0F,
                minX, maxY, maxZ, 1.0F, 1.0F,
                minX, maxY, minZ, 0.0F, 1.0F);

        if (sidesToRender.contains(Direction.EAST))
            renderParamCubeFace(buffer, pose, color, combinedLight, combinedOverlay, Direction.EAST,
                maxX, minY, minZ, 0.0F, 0.0F,
                maxX, maxY, minZ, 0.0F, 1.0F,
                maxX, maxY, maxZ, 1.0F, 1.0F,
                maxX, minY, maxZ, 1.0F, 0.0F);
    }

    public static void renderParamCubeFace(VertexConsumer buffer, PoseStack.Pose pose,
                                           int color, int combinedLight, int combinedOverlay, Direction normalDir,
                                           float x1, float y1, float z1, float u1, float v1,
                                           float x2, float y2, float z2, float u2, float v2,
                                           float x3, float y3, float z3, float u3, float v3,
                                           float x4, float y4, float z4, float u4, float v4) {
        Vector3fc normal = MatrixHelper.getDirectionAxis(normalDir);

        vertex(buffer, pose, x1, y1, z1, color, u1, v1, combinedOverlay, combinedLight, normal.x(), normal.y(), normal.z());
        vertex(buffer, pose, x2, y2, z2, color, u2, v2, combinedOverlay, combinedLight, normal.x(), normal.y(), normal.z());
        vertex(buffer, pose, x3, y3, z3, color, u3, v3, combinedOverlay, combinedLight, normal.x(), normal.y(), normal.z());
        vertex(buffer, pose, x4, y4, z4, color, u4, v4, combinedOverlay, combinedLight, normal.x(), normal.y(), normal.z());
    }

    public static void renderParamRod(VertexConsumer buffer, PoseStack.Pose pose,
                                      int color, int combinedLight, int combinedOverlay,
                                      int axis, int dir, float cx, float cy, float cz,
                                      float length, float halfSize) {
        float ix = cx, iy = cy, iz = cz;
        float ox = cx, oy = cy, oz = cz;
        switch (axis) {
            case 0 -> ox = cx + dir * length;
            case 1 -> oy = cy + dir * length;
            default -> oz = cz + dir * length;
        }
        final float h = halfSize;
        if (axis == 0) {
            renderParamQuad(buffer, pose, color, combinedLight, combinedOverlay,
                ix, iy + h, iz - h, 0.0F, 0.0F,
                ox, oy + h, oz - h, 1.0F, 0.0F,
                ox, oy + h, oz + h, 1.0F, 1.0F,
                ix, iy + h, iz + h, 0.0F, 1.0F,
                0.0F, 1.0F, 0.0F);
            renderParamQuad(buffer, pose, color, combinedLight, combinedOverlay,
                ix, iy - h, iz + h, 0.0F, 0.0F,
                ox, oy - h, oz + h, 1.0F, 0.0F,
                ox, oy - h, oz - h, 1.0F, 1.0F,
                ix, iy - h, iz - h, 0.0F, 1.0F,
                0.0F, -1.0F, 0.0F);
            renderParamQuad(buffer, pose, color, combinedLight, combinedOverlay,
                ix, iy - h, iz + h, 0.0F, 0.0F,
                ox, oy - h, oz + h, 1.0F, 0.0F,
                ox, oy + h, oz + h, 1.0F, 1.0F,
                ix, iy + h, iz + h, 0.0F, 1.0F,
                0.0F, 0.0F, 1.0F);
            renderParamQuad(buffer, pose, color, combinedLight, combinedOverlay,
                ix, iy + h, iz - h, 0.0F, 0.0F,
                ox, oy + h, oz - h, 1.0F, 0.0F,
                ox, oy - h, oz - h, 1.0F, 1.0F,
                ix, iy - h, iz - h, 0.0F, 1.0F,
                0.0F, 0.0F, -1.0F);
            renderParamQuad(buffer, pose, color, combinedLight, combinedOverlay,
                ox, oy - h, oz - h, 0.0F, 0.0F,
                ox, oy - h, oz + h, 1.0F, 0.0F,
                ox, oy + h, oz + h, 1.0F, 1.0F,
                ox, oy + h, oz - h, 0.0F, 1.0F,
                dir, 0.0F, 0.0F);
        } else if (axis == 1) {
            renderParamQuad(buffer, pose, color, combinedLight, combinedOverlay,
                ix + h, iy, iz - h, 0.0F, 0.0F,
                ox + h, oy, oz - h, 1.0F, 0.0F,
                ox - h, oy, oz - h, 1.0F, 1.0F,
                ix - h, iy, iz - h, 0.0F, 1.0F,
                0.0F, 0.0F, -1.0F);
            renderParamQuad(buffer, pose, color, combinedLight, combinedOverlay,
                ix - h, iy, iz + h, 0.0F, 0.0F,
                ox - h, oy, oz + h, 1.0F, 0.0F,
                ox + h, oy, oz + h, 1.0F, 1.0F,
                ix + h, iy, iz + h, 0.0F, 1.0F,
                0.0F, 0.0F, 1.0F);
            renderParamQuad(buffer, pose, color, combinedLight, combinedOverlay,
                ix - h, iy, iz + h, 0.0F, 0.0F,
                ox - h, oy, oz + h, 1.0F, 0.0F,
                ox - h, oy, oz - h, 1.0F, 1.0F,
                ix - h, iy, iz - h, 0.0F, 1.0F,
                -1.0F, 0.0F, 0.0F);
            renderParamQuad(buffer, pose, color, combinedLight, combinedOverlay,
                ix + h, iy, iz - h, 0.0F, 0.0F,
                ox + h, oy, oz - h, 1.0F, 0.0F,
                ox + h, oy, oz + h, 1.0F, 1.0F,
                ix + h, iy, iz + h, 0.0F, 1.0F,
                1.0F, 0.0F, 0.0F);
            renderParamQuad(buffer, pose, color, combinedLight, combinedOverlay,
                ox - h, oy, oz - h, 0.0F, 0.0F,
                ox + h, oy, oz - h, 1.0F, 0.0F,
                ox + h, oy, oz + h, 1.0F, 1.0F,
                ox - h, oy, oz + h, 0.0F, 1.0F,
                0.0F, dir, 0.0F);
        } else {
            renderParamQuad(buffer, pose, color, combinedLight, combinedOverlay,
                ix + h, iy + h, iz, 0.0F, 0.0F,
                ox + h, oy + h, oz, 1.0F, 0.0F,
                ox - h, oy + h, oz, 1.0F, 1.0F,
                ix - h, iy + h, iz, 0.0F, 1.0F,
                0.0F, 1.0F, 0.0F);
            renderParamQuad(buffer, pose, color, combinedLight, combinedOverlay,
                ix - h, iy - h, iz, 0.0F, 0.0F,
                ox - h, oy - h, oz, 1.0F, 0.0F,
                ox + h, oy - h, oz, 1.0F, 1.0F,
                ix + h, iy - h, iz, 0.0F, 1.0F,
                0.0F, -1.0F, 0.0F);
            renderParamQuad(buffer, pose, color, combinedLight, combinedOverlay,
                ix - h, iy + h, iz, 0.0F, 0.0F,
                ox - h, oy + h, oz, 1.0F, 0.0F,
                ox - h, oy - h, oz, 1.0F, 1.0F,
                ix - h, iy - h, iz, 0.0F, 1.0F,
                -1.0F, 0.0F, 0.0F);
            renderParamQuad(buffer, pose, color, combinedLight, combinedOverlay,
                ix + h, iy - h, iz, 0.0F, 0.0F,
                ox + h, oy - h, oz, 1.0F, 0.0F,
                ox + h, oy + h, oz, 1.0F, 1.0F,
                ix + h, iy + h, iz, 0.0F, 1.0F,
                1.0F, 0.0F, 0.0F);
            renderParamQuad(buffer, pose, color, combinedLight, combinedOverlay,
                ox - h, oy - h, oz, 0.0F, 0.0F,
                ox + h, oy - h, oz, 1.0F, 0.0F,
                ox + h, oy + h, oz, 1.0F, 1.0F,
                ox - h, oy + h, oz, 0.0F, 1.0F,
                0.0F, 0.0F, dir);
        }
    }

    public static void renderParamQuad(VertexConsumer buffer, PoseStack.Pose pose,
                                       int color, int combinedLight, int combinedOverlay,
                                       float x1, float y1, float z1, float u1, float v1,
                                       float x2, float y2, float z2, float u2, float v2,
                                       float x3, float y3, float z3, float u3, float v3,
                                       float x4, float y4, float z4, float u4, float v4,
                                       float normalX, float normalY, float normalZ) {
        vertex(buffer, pose, x1, y1, z1, color, u1, v1, combinedOverlay, combinedLight, normalX, normalY, normalZ);
        vertex(buffer, pose, x2, y2, z2, color, u2, v2, combinedOverlay, combinedLight, normalX, normalY, normalZ);
        vertex(buffer, pose, x3, y3, z3, color, u3, v3, combinedOverlay, combinedLight, normalX, normalY, normalZ);
        vertex(buffer, pose, x4, y4, z4, color, u4, v4, combinedOverlay, combinedLight, normalX, normalY, normalZ);
    }

    public static void renderCube(VertexConsumer buffer, PoseStack.Pose pose,
                                  int color, int combinedLight, TextureAtlasSprite sprite,
                                  float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        renderCube(buffer, pose, EnumSet.allOf(Direction.class),
                color, combinedLight, sprite,
                minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static void renderCube(VertexConsumer buffer, PoseStack.Pose pose, Set<Direction> sidesToRender,
                                  int color, int combinedLight, TextureAtlasSprite sprite,
                                  float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        float uMin = sprite.getU0(), uMax = sprite.getU1(), vMin = sprite.getV0(), vMax = sprite.getV1();

        if (sidesToRender.contains(Direction.UP))
            renderCubeFace(buffer, pose, color, combinedLight, Direction.UP,
                    minX, maxY, minZ, uMin, vMax,
                    minX, maxY, maxZ, uMax, vMax,
                    maxX, maxY, maxZ, uMax, vMin,
                    maxX, maxY, minZ, uMin, vMin);

        if (sidesToRender.contains(Direction.DOWN))
            renderCubeFace(buffer, pose, color, combinedLight, Direction.DOWN,
                    minX, minY, minZ, uMin, vMax,
                    maxX, minY, minZ, uMax, vMax,
                    maxX, minY, maxZ, uMax, vMin,
                    minX, minY, maxZ, uMin, vMin);

        if (sidesToRender.contains(Direction.NORTH))
            renderCubeFace(buffer, pose, color, combinedLight, Direction.NORTH,
                    minX, minY, minZ, uMin, vMax,
                    minX, maxY, minZ, uMax, vMax,
                    maxX, maxY, minZ, uMax, vMin,
                    maxX, minY, minZ, uMin, vMin);

        if (sidesToRender.contains(Direction.SOUTH))
            renderCubeFace(buffer, pose, color, combinedLight, Direction.SOUTH,
                    minX, minY, maxZ, uMin, vMax,
                    maxX, minY, maxZ, uMax, vMax,
                    maxX, maxY, maxZ, uMax, vMin,
                    minX, maxY, maxZ, uMin, vMin);

        if (sidesToRender.contains(Direction.WEST))
            renderCubeFace(buffer, pose, color, combinedLight, Direction.WEST,
                    minX, minY, minZ, uMin, vMax,
                    minX, minY, maxZ, uMax, vMax,
                    minX, maxY, maxZ, uMax, vMin,
                    minX, maxY, minZ, uMin, vMin);

        if (sidesToRender.contains(Direction.EAST))
            renderCubeFace(buffer, pose, color, combinedLight, Direction.EAST,
                    maxX, minY, minZ, uMin, vMax,
                    maxX, maxY, minZ, uMax, vMax,
                    maxX, maxY, maxZ, uMax, vMin,
                    maxX, minY, maxZ, uMin, vMin);
    }

    public static void renderCubeFace(VertexConsumer buffer, PoseStack.Pose pose,
                                      int color, int combinedLight, Direction normalDir,
                                      float x1, float y1, float z1, float u1, float v1,
                                      float x2, float y2, float z2, float u2, float v2,
                                      float x3, float y3, float z3, float u3, float v3,
                                      float x4, float y4, float z4, float u4, float v4) {
        Vector3fc normal = MatrixHelper.getDirectionAxis(normalDir);

        vertex(buffer, pose, x1, y1, z1, color, u1, v1, combinedLight, normal.x(), normal.y(), normal.z());
        vertex(buffer, pose, x2, y2, z2, color, u2, v2, combinedLight, normal.x(), normal.y(), normal.z());
        vertex(buffer, pose, x3, y3, z3, color, u3, v3, combinedLight, normal.x(), normal.y(), normal.z());
        vertex(buffer, pose, x4, y4, z4, color, u4, v4, combinedLight, normal.x(), normal.y(), normal.z());
    }

    public static void vertex(VertexConsumer buffer, PoseStack.Pose pose,
                              float x, float y, float z,
                              int color, float texU, float texV, int lightmapUV,
                              float normalX, float normalY, float normalZ) {
        vertex(buffer, pose, x, y, z, color,
                texU, texV, OverlayTexture.NO_OVERLAY, lightmapUV,
                normalX, normalY, normalZ);
    }

    public static void vertex(VertexConsumer buffer, PoseStack.Pose pose,
                              float x, float y, float z, int color,
                              float texU, float texV, int overlayUV, int lightmapUV,
                              float normalX, float normalY, float normalZ) {
        buffer.addVertex(pose.pose(), x, y, z);
        buffer.setColor(color);
        buffer.setUv(texU, texV);
        buffer.setOverlay(overlayUV);
        buffer.setLight(lightmapUV);
        buffer.setNormal(pose, normalX, normalY, normalZ);
    }
}
