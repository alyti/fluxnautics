package dev.alyti.fluxnautics.client.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class BlackHoleRendererHelpers {

    public static boolean isRenderingMinecraftTranslucentLayer = false;
    public static boolean isTranslucentShader;

    public static void addBoxTriangles(PoseStack poseStack, BufferBuilder builder,
                                       float x0, float y0, float z0,
                                       float x1, float y1, float z1,
                                       float r, float g, float b, float a) {
        Matrix4f mat = poseStack.last().pose();
        // Front face (z1)
        builder.addVertex(mat, x0, y0, z1).setColor(r, g, b, a);
        builder.addVertex(mat, x1, y0, z1).setColor(r, g, b, a);
        builder.addVertex(mat, x1, y1, z1).setColor(r, g, b, a);
        builder.addVertex(mat, x0, y0, z1).setColor(r, g, b, a);
        builder.addVertex(mat, x1, y1, z1).setColor(r, g, b, a);
        builder.addVertex(mat, x0, y1, z1).setColor(r, g, b, a);
        // Back face (z0)
        builder.addVertex(mat, x1, y0, z0).setColor(r, g, b, a);
        builder.addVertex(mat, x0, y0, z0).setColor(r, g, b, a);
        builder.addVertex(mat, x0, y1, z0).setColor(r, g, b, a);
        builder.addVertex(mat, x1, y0, z0).setColor(r, g, b, a);
        builder.addVertex(mat, x0, y1, z0).setColor(r, g, b, a);
        builder.addVertex(mat, x1, y1, z0).setColor(r, g, b, a);
        // Top face (y1)
        builder.addVertex(mat, x0, y1, z1).setColor(r, g, b, a);
        builder.addVertex(mat, x1, y1, z1).setColor(r, g, b, a);
        builder.addVertex(mat, x1, y1, z0).setColor(r, g, b, a);
        builder.addVertex(mat, x0, y1, z1).setColor(r, g, b, a);
        builder.addVertex(mat, x1, y1, z0).setColor(r, g, b, a);
        builder.addVertex(mat, x0, y1, z0).setColor(r, g, b, a);
        // Bottom face (y0)
        builder.addVertex(mat, x0, y0, z0).setColor(r, g, b, a);
        builder.addVertex(mat, x1, y0, z0).setColor(r, g, b, a);
        builder.addVertex(mat, x1, y0, z1).setColor(r, g, b, a);
        builder.addVertex(mat, x0, y0, z0).setColor(r, g, b, a);
        builder.addVertex(mat, x1, y0, z1).setColor(r, g, b, a);
        builder.addVertex(mat, x0, y0, z1).setColor(r, g, b, a);
        // Right face (x1)
        builder.addVertex(mat, x1, y0, z1).setColor(r, g, b, a);
        builder.addVertex(mat, x1, y0, z0).setColor(r, g, b, a);
        builder.addVertex(mat, x1, y1, z0).setColor(r, g, b, a);
        builder.addVertex(mat, x1, y0, z1).setColor(r, g, b, a);
        builder.addVertex(mat, x1, y1, z0).setColor(r, g, b, a);
        builder.addVertex(mat, x1, y1, z1).setColor(r, g, b, a);
        // Left face (x0)
        builder.addVertex(mat, x0, y0, z0).setColor(r, g, b, a);
        builder.addVertex(mat, x0, y0, z1).setColor(r, g, b, a);
        builder.addVertex(mat, x0, y1, z1).setColor(r, g, b, a);
        builder.addVertex(mat, x0, y0, z0).setColor(r, g, b, a);
        builder.addVertex(mat, x0, y1, z1).setColor(r, g, b, a);
        builder.addVertex(mat, x0, y1, z0).setColor(r, g, b, a);
    }

    public static AABB createAABBAt(Vector3f center, float size) {
        double halfX = (double) size / 2.0;
        double halfY = (double) size / 2.0;
        double halfZ = (double) size / 2.0;

        return new AABB(
                center.x - halfX, center.y - halfY, center.z - halfZ,
                center.x + halfX, center.y + halfY, center.z + halfZ);
    }
}