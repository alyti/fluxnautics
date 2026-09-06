package dev.alyti.fluxnautics.client.util;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.AdvancedFboTextureAttachment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;

public class SimpleHeightmapRenderer {
    public static void generateHeightmapTexture(AdvancedFbo fbo, BlockPos minPos, BlockPos maxPos) {
        Minecraft mc = Minecraft.getInstance();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        bufferSource.endBatch();

        Level level = mc.level;

        if (level == null) {
            return;
        }

        int width = maxPos.getX() - minPos.getX();
        int depth = maxPos.getZ() - minPos.getZ();

        ByteBuffer buffer = ByteBuffer.allocateDirect(width * depth * 4);

        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        float heightRange = maxY - minY;

        for (int x = minPos.getX(); x < maxPos.getX(); x++) {
            for (int z = maxPos.getZ(); z > minPos.getZ(); z--) {
                ChunkAccess chunk = level.getChunk(x >> 4, z >> 4);

                // MOTION_BLOCKING WORLD_SURFACE OCEAN_FLOOR_WG
                int height = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                float normalized = (height - minY) / heightRange;
                normalized = Math.max(0.0f, Math.min(1.0f, normalized));
                byte alphaVal = (byte) (normalized * 255);

                BlockPos surfacePos = new BlockPos(x, height, z);
                BlockState state = chunk.getBlockState(surfacePos);

                MapColor mapColor = state.getMapColor(chunk, surfacePos);

                int packedColor = mapColor.col;

                byte r = (byte) ((packedColor >> 16) & 0xFF);
                byte g = (byte) ((packedColor >> 8) & 0xFF);
                byte b = (byte) (packedColor & 0xFF);

                buffer.put(r); // R
                buffer.put(g); // G
                buffer.put(b); // B
                buffer.put(alphaVal); // A (Alpha)
            }
        }

        buffer.flip();

        AdvancedFbo drawFbo = VeilRenderSystem.renderer().getDynamicBufferManger().getDynamicFbo(fbo);
        final AdvancedFboTextureAttachment fboColor = drawFbo.getColorTextureAttachment(0);
        fboColor.bindAttachment();

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL11.GL_RGBA,
                width,
                depth,
                0,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                buffer
        );
        fboColor.unbindAttachment();
        return;
    }
}