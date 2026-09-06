package dev.alyti.fluxnautics.content.blocks.thruster;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.alyti.fluxnautics.FluxnauticsConfig;
import dev.alyti.fluxnautics.client.render.SFRenderTypes;
import dev.alyti.fluxnautics.client.render.ThrusterCompartmentCache;
import dev.alyti.fluxnautics.client.render.ThrusterCompartmentCache.Compartment;
import dev.alyti.fluxnautics.client.render.ThrusterCompartmentCache.Wall;
import dev.alyti.fluxnautics.client.render.ThrusterTrailManager;
import dev.alyti.fluxnautics.client.util.RenderBufferHelper;
import dev.alyti.fluxnautics.index.ThrusterEffect;
import dev.alyti.fluxnautics.index.ThrusterPlumeEffect;
import dev.alyti.fluxnautics.math.LerpedFloat;
import dev.alyti.fluxnautics.multiblock.ThrusterArrayMultiblockData;
import dev.alyti.fluxnautics.multiblock.ThrusterArrayMultiblockData.GroupData;
import dev.alyti.fluxnautics.multiblock.ThrusterArrayMultiblockData.ThrusterData;
import dev.alyti.fluxnautics.util.SFColors;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.client.render.shader.uniform.ShaderUniform;
import foundry.veil.api.client.render.shader.uniform.ShaderUniformAccess;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.client.render.tileentity.MultiblockTileEntityRenderer;
import net.minecraft.Util;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.FastColor;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.textures.UnitTextureAtlasSprite;
import org.jetbrains.annotations.Nullable;
import org.joml.*;

import java.awt.*;
import java.lang.Math;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@NothingNullByDefault
public class IonThrusterRenderer extends MultiblockTileEntityRenderer<ThrusterArrayMultiblockData, ThrusterArrayControllerBlockEntity> {

    public IonThrusterRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    private static final Quaternionf PLUME_TILT = new Quaternionf().rotationX(-(float) Math.PI / 2.0F);

    protected void render(ThrusterArrayControllerBlockEntity tileEntityMultiblock, ThrusterArrayMultiblockData data, float partialTicks, PoseStack pose, MultiBufferSource buffer, int light, int overlayLight, ProfilerFiller profiler) {
        if (tileEntityMultiblock.getLevel() == null) {
            return;
        }

        float ticks = (Util.getMillis() % 3600000L) / 1000f;
        float wheelTicks = -(Util.getMillis() % 100000L) / 10f;

        List<Compartment> compartments = ThrusterCompartmentCache.get(data);
        if (compartments.isEmpty()) {
            return;
        }

        int currentGroupId = Integer.MIN_VALUE;
        ThrusterPlumeEffect plumeEffect = null;
        GroupData groupData = null;
        Vector4f colors = null;
        Vector4f wallColors = null;
        float length = 0f;

        for (int ci = 0; ci < compartments.size(); ci++) {
            Compartment compartment = compartments.get(ci);
            int groupId = compartment.groupId;
            if (groupId < 0 || groupId > data.thrusters.groups.size() - 1) {
                continue;
            }
            if (groupId != currentGroupId) {
                currentGroupId = groupId;
                groupData = data.thrusters.groups.get(groupId);
                colors = getColors((int) groupData.getConfig("color").get(), (boolean) groupData.getConfig("rainbow").get(), wheelTicks);
                plumeEffect = (ThrusterPlumeEffect) groupData.getConfig("plume_effect").get();
                wallColors = getWallColors((int) groupData.getConfig("color").get(), (boolean) groupData.getConfig("rainbow").get(), wheelTicks);
                groupData.clientIntensity.chase(groupData.thrustLevel / 2.0, (float) FluxnauticsConfig.client.thrusterPlumeChaseSpeed.getOrDefault(), LerpedFloat.Chaser.EXP);
                groupData.clientIntensity.tickChaser();
                // client config: plumeIntensity scales the plume length (0 hides plumes entirely)
                length = (float) (groupData.clientIntensity.getValue(partialTicks) * FluxnauticsConfig.client.thrusterPlumeIntensity.getOrDefault());
            }
            if (length <= 0.001f) {
                continue;
            }

            float fade = layerFade(compartments, ci);
            float seed = compartmentSeed(compartment.anchor.pos, tileEntityMultiblock.getBlockPos());

            pose.pushPose();
            pose.translate(compartment.anchor.pos.getX(), compartment.anchor.pos.getY(), compartment.anchor.pos.getZ());
            transformPose(compartment.facing, pose);

            VertexConsumer cuboidConsumer = beginCuboid(buffer, (ThrusterEffect) groupData.getConfig("effect").get(), ticks);
            if (cuboidConsumer != null) {
                int capColor = FastColor.ARGB32.colorFromFloat(colors.w * 5 * fade, colors.x, colors.z, colors.y);
                for (ThrusterCompartmentCache.Cap cap : compartment.caps) {
                    RenderBufferHelper.renderCube(cuboidConsumer, pose.last(), Set.of(Direction.SOUTH), capColor, light, UnitTextureAtlasSprite.INSTANCE,
                        cap.minX(), cap.y(), 0f, cap.maxX(), cap.y() + 1f, 0.05f);
                }
            }
            Vector2f shift = ThrusterCompartmentCache.compartmentShift(compartment, groupData).mul(length + 0.9f);
            float span = 1.0f;
            for (Wall wall : compartment.walls) {
                span = Math.max(span, (float) (wall.b() - wall.a()));
            }
            RenderType plumeType = plumeEffect == null ? null : VeilRenderType.get(plumeEffect.getShader());
            renderWalls(tileEntityMultiblock, compartment, pose, buffer, length, ticks, wallColors, shift.x, shift.y, span, fade, seed, plumeType);
            pose.popPose();
        }
    }

    private void transformPose(Direction facing, PoseStack pose) {
        pose.translate(0.5, 0.5, 0.5);

        pose.mulPose(facing.getRotation());
        pose.mulPose(PLUME_TILT);
        pose.translate(0, 0, -(4.0 / 16.0));


        float scale = 0.9999f;
        pose.scale(scale, scale, 1);

        pose.translate(-0.5, -0.5, 0.0);
    }

    private VertexConsumer beginCuboid(MultiBufferSource buffer, ThrusterEffect effect, float ticks) {
        switch (effect) {
            case END_PORTAL -> {
                RenderType renderType = SFRenderTypes.endPortalEffect();
                if (renderType == null) {
                    return null;
                }
                VertexConsumer consumer = buffer.getBuffer(renderType);
                RenderStateShard.ShaderStateShard shard = VeilRenderType.getShards(renderType).shaderState();
                shard.setupRenderState();
                ShaderProgram shader = VeilRenderSystem.getShader();
                if (shader != null) {
                    ShaderUniformAccess time = shader.getUniformSafe("time");
                    time.setFloat(ticks);
                    ShaderUniform scale = shader.getUniform("vColorPost");
                    if (scale != null) {
                        Vector4f v = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
                        v.mul(0.1f);
                        scale.setVector(v);
                    }
                }
                return consumer;
            }
            default -> {
                return buffer.getBuffer(VeilRenderType.lightning());
            }
        }
    }

    private void renderWalls(ThrusterArrayControllerBlockEntity controller, Compartment compartment, PoseStack pose, MultiBufferSource buffer, float length, float ticks, Vector4f color, float shiftX, float shiftY, float span, float fade, float seed, @Nullable RenderType renderType) {
        if (renderType == null)
            renderType = SFRenderTypes.classicLaser();
        VertexConsumer builder = buffer.getBuffer(renderType);

        RenderStateShard.ShaderStateShard shard = VeilRenderType.getShards(renderType).shaderState();
        shard.setupRenderState();
        ShaderProgram shader = VeilRenderSystem.getShader();
        if (shader != null) {
            ShaderUniformAccess time = shader.getUniformSafe("time");
            time.setFloat(ticks);
        }

        final float lengthFrac = length / length;
        final float offset = lengthFrac / 10;
        final float endU = 1f + 1f / length;

        final float red = color.x();
        final float blue = color.y();
        final float green = color.z();
        final float alpha = color.w();
        final boolean rainbowWall = alpha > 0.99f;
        final float endAlpha = rainbowWall ? alpha : alpha * (1 - lengthFrac);

        Matrix4f matrix = pose.last().pose();

        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (Wall wall : compartment.walls) {
            float lo = Math.min(wall.a(), wall.b());
            float hi = Math.max(wall.a(), wall.b());
            switch (wall.side()) {
                case 0, 2 -> {
                    minX = Math.min(minX, lo); maxX = Math.max(maxX, hi);
                    minY = Math.min(minY, wall.c()); maxY = Math.max(maxY, wall.c());
                }
                default -> {
                    minX = Math.min(minX, wall.c()); maxX = Math.max(maxX, wall.c());
                    minY = Math.min(minY, lo); maxY = Math.max(maxY, hi);
                }
            }
        }
        float centerX = (minX + maxX) / 2f;
        float centerY = (minY + maxY) / 2f;
        float radius = Math.max(Math.max(centerX - minX, maxX - centerX),
            Math.max(centerY - minY, maxY - centerY));

        if (ThrusterTrailManager.enabled && length > 0.02f) {
            ThrusterTrailManager.recordFromRenderer(controller, compartment.anchor.pos, matrix,
                centerX, centerY, radius,
                buildFootprintPolygon(compartment.walls, centerX, centerY),
                color, rainbowWall, fade, length, seed);
        }

        for (Wall wall : compartment.walls) {
            emitWall(builder, matrix, wall, shiftX, shiftY, offset, red, green, blue, alpha, endAlpha,
                length + 0.9f, endU);
        }

        if (shader != null) {
            shader.getUniformSafe("uPlumeSpan").setFloat(span);
            shader.getUniformSafe("uPlumePower").setFloat(length / Math.max(span, 1.0f));
            shader.getUniformSafe("uPlumeLength").setFloat(length);
            shader.getUniformSafe("uLayerFade").setFloat(fade);
            shader.getUniformSafe("uPlumeSeed").setFloat(seed);
            shader.getUniformSafe("uPlumeCenter").setVector(new Vector3f(centerX, centerY, 0f).mulPosition(matrix));
            shader.getUniformSafe("uPlumeRadius").setFloat(radius);
            shader.getUniformSafe("uPlumeXAxis").setVector(new Vector3f(1f, 0f, 0f).mulDirection(matrix));
            shader.getUniformSafe("uPlumeYAxis").setVector(new Vector3f(0f, 1f, 0f).mulDirection(matrix));
        }
        if (buffer instanceof MultiBufferSource.BufferSource immediate) {
            immediate.endBatch(renderType);
        }
    }

    private static void emitWall(VertexConsumer builder, Matrix4f matrix, Wall wall, float shiftX, float shiftY, float offset, float red, float green, float blue, float nearAlpha, float endAlpha, float zEnd, float vEnd) {
        float flare = wall.flared() ? offset : 0f;

        float ax, ay, bx, by, nx, ny, dx, dy;
        switch (wall.side()) {
            case 0 -> { ax = wall.a();  ay = wall.c(); bx = wall.b();  by = wall.c(); nx = 0;  ny = -1; dx = 1;  dy = 0; }
            case 1 -> { ax = wall.c();  ay = wall.b(); bx = wall.c();  by = wall.a(); nx = 1;  ny = 0;  dx = 0;  dy = -1; }
            case 2 -> { ax = wall.b();  ay = wall.c(); bx = wall.a();  by = wall.c(); nx = 0;  ny = 1;  dx = -1; dy = 0; }
            default -> { ax = wall.c(); ay = wall.a(); bx = wall.c();  by = wall.b(); nx = -1; ny = 0;  dx = 0;  dy = 1; }
        }

        float fax = ax + shiftX + nx * flare - dx * flare;
        float fay = ay + shiftY + ny * flare - dy * flare;
        float fbx = bx + shiftX + nx * flare + dx * flare;
        float fby = by + shiftY + ny * flare + dy * flare;

        float wallSpan = wall.b() - wall.a();
        builder.addVertex(matrix, ax, ay, 0).setColor(red, green, blue, nearAlpha).setUv(0, 0).setLight(LightTexture.FULL_BRIGHT).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(nx, ny, 0);
        builder.addVertex(matrix, bx, by, 0).setColor(red, green, blue, nearAlpha).setUv(wallSpan, 0).setLight(LightTexture.FULL_BRIGHT).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(nx, ny, 0);
        builder.addVertex(matrix, fbx, fby, zEnd).setColor(red, green, blue, endAlpha).setUv(wallSpan, vEnd).setLight(LightTexture.FULL_BRIGHT).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(nx, ny, 0);
        builder.addVertex(matrix, fax, fay, zEnd).setColor(red, green, blue, endAlpha).setUv(0, vEnd).setLight(LightTexture.FULL_BRIGHT).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(nx, ny, 0);
    }

    private static final int TRAIL_MAX_POLY_VERTS = 64;

    private static float[] polySegScratch = new float[256];
    private static boolean[] polyUsedScratch = new boolean[64];
    private static float[] polyChainScratch = new float[512];
    private static float[] polyHullTmp = new float[128];
    private static int[] polyHullStack = new int[130];

    @Nullable
    private static float[] buildFootprintPolygon(List<Wall> walls, float centerX, float centerY) {
        int n = walls.size();
        if (n < 3) {
            return null;
        }
        if (polySegScratch.length < n * 4) {
            polySegScratch = new float[n * 4];
        }
        if (polyUsedScratch.length < n) {
            polyUsedScratch = new boolean[n];
        }
        int segs = 0;
        for (Wall w : walls) {
            float ax, ay, bx, by;
            switch (w.side()) {
                case 0 -> { ax = w.a(); ay = w.c(); bx = w.b(); by = w.c(); }
                case 1 -> { ax = w.c(); ay = w.b(); bx = w.c(); by = w.a(); }
                case 2 -> { ax = w.b(); ay = w.c(); bx = w.a(); by = w.c(); }
                default -> { ax = w.c(); ay = w.a(); bx = w.c(); by = w.b(); }
            }
            if (Math.abs(ax - bx) < 1.0e-4f && Math.abs(ay - by) < 1.0e-4f) {
                continue;
            }
            polySegScratch[segs * 4] = ax;
            polySegScratch[segs * 4 + 1] = ay;
            polySegScratch[segs * 4 + 2] = bx;
            polySegScratch[segs * 4 + 3] = by;
            segs++;
        }
        if (segs < 3) {
            return null;
        }
        if (polyChainScratch.length < segs * 4 + 4) {
            polyChainScratch = new float[segs * 4 + 4];
        }
        float[] poly = polyChainScratch;
        int vc = 0;
        boolean closed = false;
        Arrays.fill(polyUsedScratch, 0, segs, false);
        float px = polySegScratch[0], py = polySegScratch[1];
        for (int k = 0; k < segs; k++) {
            poly[vc++] = px;
            poly[vc++] = py;
            int found = -1;
            float nx = 0f, ny = 0f;
            for (int i = 0; i < segs; i++) {
                if (polyUsedScratch[i]) {
                    continue;
                }
                float x0 = polySegScratch[i * 4], y0 = polySegScratch[i * 4 + 1];
                float x1 = polySegScratch[i * 4 + 2], y1 = polySegScratch[i * 4 + 3];
                if (Math.abs(x0 - px) < 1.0e-3f && Math.abs(y0 - py) < 1.0e-3f) {
                    found = i;
                    nx = x1;
                    ny = y1;
                    break;
                }
                if (Math.abs(x1 - px) < 1.0e-3f && Math.abs(y1 - py) < 1.0e-3f) {
                    found = i;
                    nx = x0;
                    ny = y0;
                    break;
                }
            }
            if (found < 0) {
                break;
            }
            polyUsedScratch[found] = true;
            px = nx;
            py = ny;
            if (Math.abs(px - poly[0]) < 1.0e-3f && Math.abs(py - poly[1]) < 1.0e-3f) {
                closed = true;
                break;
            }
        }
        int count;
        if (closed) {
            count = simplifyPoly(poly, vc / 2);
        } else {
            int pts = 0;
            for (int i = 0; i < segs; i++) {
                poly[pts++] = polySegScratch[i * 4];
                poly[pts++] = polySegScratch[i * 4 + 1];
                poly[pts++] = polySegScratch[i * 4 + 2];
                poly[pts++] = polySegScratch[i * 4 + 3];
            }
            count = convexHull(poly, pts / 2);
        }
        while (count > TRAIL_MAX_POLY_VERTS) {
            count = decimateHalf(poly, count);
        }
        if (count < 3) {
            return null;
        }
        float[] out = new float[count * 2];
        for (int i = 0; i < count; i++) {
            out[i * 2] = poly[i * 2] - centerX;
            out[i * 2 + 1] = poly[i * 2 + 1] - centerY;
        }
        return out;
    }

    private static int simplifyPoly(float[] p, int n) {
        if (n < 3) {
            return n;
        }
        int out = 2;
        for (int i = 1; i < n; i++) {
            float x = p[i * 2], y = p[i * 2 + 1];
            if (Math.abs(x - p[out - 2]) < 1.0e-3f && Math.abs(y - p[out - 1]) < 1.0e-3f) {
                continue;
            }
            p[out] = x;
            p[out + 1] = y;
            out += 2;
            while (out >= 6) {
                float ax = p[out - 6], ay = p[out - 5];
                float bx = p[out - 4], by = p[out - 3];
                float cx = p[out - 2], cy = p[out - 1];
                float cr = (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
                if (Math.abs(cr) < 1.0e-3f) {
                    p[out - 4] = cx;
                    p[out - 3] = cy;
                    out -= 2;
                } else {
                    break;
                }
            }
        }
        if (out >= 4 && Math.abs(p[out - 2] - p[0]) < 1.0e-3f && Math.abs(p[out - 1] - p[1]) < 1.0e-3f) {
            out -= 2;
        }
        return out / 2;
    }

    private static int convexHull(float[] p, int n) {
        if (n < 3) {
            return n;
        }
        for (int i = 1; i < n; i++) {
            float x = p[i * 2], y = p[i * 2 + 1];
            int j = i - 1;
            while (j >= 0 && (p[j * 2] > x || (p[j * 2] == x && p[j * 2 + 1] > y))) {
                p[j * 2 + 2] = p[j * 2];
                p[j * 2 + 3] = p[j * 2 + 1];
                j--;
            }
            p[j * 2 + 2] = x;
            p[j * 2 + 3] = y;
        }
        if (polyHullStack.length < n + 1) {
            polyHullStack = new int[n + 1];
        }
        int k = 0;
        for (int i = 0; i < n; i++) {
            while (k >= 2 && hullCross(p, polyHullStack[k - 2], polyHullStack[k - 1], i) <= 0) {
                k--;
            }
            polyHullStack[k++] = i;
        }
        int t = k + 1;
        for (int i = n - 2; i >= 0; i--) {
            while (k >= t && hullCross(p, polyHullStack[k - 2], polyHullStack[k - 1], i) <= 0) {
                k--;
            }
            polyHullStack[k++] = i;
        }
        int h = k - 1;
        if (polyHullTmp.length < h * 2) {
            polyHullTmp = new float[h * 2];
        }
        for (int i = 0; i < h; i++) {
            polyHullTmp[i * 2] = p[polyHullStack[i] * 2];
            polyHullTmp[i * 2 + 1] = p[polyHullStack[i] * 2 + 1];
        }
        System.arraycopy(polyHullTmp, 0, p, 0, h * 2);
        return h;
    }

    private static float hullCross(float[] p, int a, int b, int c) {
        return (p[b * 2] - p[a * 2]) * (p[c * 2 + 1] - p[a * 2 + 1])
            - (p[b * 2 + 1] - p[a * 2 + 1]) * (p[c * 2] - p[a * 2]);
    }

    private static int decimateHalf(float[] p, int n) {
        int out = 0;
        for (int i = 0; i < n; i += 2) {
            p[out * 2] = p[i * 2];
            p[out * 2 + 1] = p[i * 2 + 1];
            out++;
        }
        return out;
    }

    private static float compartmentSeed(BlockPos anchor, BlockPos master) {
        int h = anchor.hashCode() * 31 + master.hashCode();
        h ^= h >>> 16;
        h *= 0x7feb352d;
        h ^= h >>> 15;
        return (h & 0xFFFF) / 65536.0f;
    }

    private static float layerFade(List<Compartment> compartments, int index) {
        Compartment c = compartments.get(index);
        int plumeAxis = c.facing.getAxis().ordinal();
        float[] cMin = new float[3], cMax = new float[3];
        compartmentBounds(c, cMin, cMax);
        float fade = 1.0f;
        for (int i = 0; i < compartments.size(); i++) {
            if (i == index) {
                continue;
            }
            Compartment o = compartments.get(i);
            if (o.groupId != c.groupId || o.side != c.side) {
                continue;
            }
            float[] oMin = new float[3], oMax = new float[3];
            compartmentBounds(o, oMin, oMax);
            if (oMin[plumeAxis] <= cMin[plumeAxis] + 0.5f) {
                continue;
            }
            boolean overlap = true;
            for (int a = 0; a < 3 && overlap; a++) {
                if (a == plumeAxis) {
                    continue;
                }
                overlap = cMin[a] < oMax[a] - 0.05f && oMin[a] < cMax[a] - 0.05f;
            }
            if (overlap) {
                fade *= 0.62f;
            }
        }
        return fade;
    }

    private static void compartmentBounds(Compartment c, float[] min, float[] max) {
        min[0] = min[1] = min[2] = Float.MAX_VALUE;
        max[0] = max[1] = max[2] = -Float.MAX_VALUE;
        for (ThrusterData t : c.members) {
            BlockPos p = t.pos;
            float x = p.getX(), y = p.getY(), z = p.getZ();
            if (x < min[0]) min[0] = x;
            if (x > max[0]) max[0] = x;
            if (y < min[1]) min[1] = y;
            if (y > max[1]) max[1] = y;
            if (z < min[2]) min[2] = z;
            if (z > max[2]) max[2] = z;
        }
        max[0] += 1f;
        max[1] += 1f;
        max[2] += 1f;
    }

    private Vector4f getColors(int colorInt, boolean isRainbow, float wheelTicks) {
        Color c = new Color(colorInt);
        if (isRainbow) {
            final Vector3d baseLCh = SFColors.LabToLCh(SFColors.toOklab(c));
            final float hue01 = (float) (baseLCh.z() / (2.0 * Math.PI)) + wheelTicks / 100f;
            c = SFColors.LChOklab(0.8f, 0.3f, (float) (hue01 * 2.0 * Math.PI));
        }
        return new Vector4f(c.getRed() / 255f, c.getBlue() / 255f, c.getGreen() / 255f, 10 / 60f);
    }

    private Vector4f getWallColors(int colorInt, boolean isRainbow, float wheelTicks) {
        if (!isRainbow) {
            return getColors(colorInt, false, 0.0f);
        }
        Color c = new Color(colorInt);
        final Vector3d baseLCh = SFColors.LabToLCh(SFColors.toOklab(c));
        float hue01 = (float) (baseLCh.z() / (2.0 * Math.PI)) + wheelTicks / 100f;
        hue01 -= (float) Math.floor(hue01);
        return new Vector4f(hue01, 0.0f, 0.3f, 1.0f);
    }

    @Override
    protected String getProfilerSection() {
        return "ion_thruster_plume";
    }

    @Override
    public AABB getRenderBoundingBox(ThrusterArrayControllerBlockEntity tile) {
        if (tile.isMaster()) {
            return AABB.INFINITE;
        }
        return super.getRenderBoundingBox(tile);
    }

    @Override
    public int getViewDistance() {
        return 512;
    }
}
