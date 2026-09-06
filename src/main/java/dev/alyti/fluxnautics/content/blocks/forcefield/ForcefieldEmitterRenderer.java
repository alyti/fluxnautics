package dev.alyti.fluxnautics.content.blocks.forcefield;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import dev.alyti.fluxnautics.util.SFColors;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import mekanism.client.render.tileentity.MekanismTileEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard.LineStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.*;
import org.jspecify.annotations.NonNull;

import java.awt.*;
import java.lang.Math;
import java.util.*;
import java.util.List;

public class ForcefieldEmitterRenderer extends MekanismTileEntityRenderer<ForcefieldEmitterBlockEntity> implements BlockEntityRenderer<ForcefieldEmitterBlockEntity> {
    private static final RenderType SHIELD_SURFACE = RenderType.create(
            "forcefield_surface",
            DefaultVertexFormat.POSITION_COLOR,
            Mode.QUADS,
            2097152,
            false,
            true,
            CompositeState.builder()
                    .setShaderState(RenderType.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderType.NO_CULL)
                    .setOutputState(RenderType.TRANSLUCENT_TARGET)
                    .setWriteMaskState(RenderType.COLOR_WRITE)
                    .createCompositeState(false)
    );
    private static final RenderType SHIELD_LINES = RenderType.create(
            "forcefield_lines",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            Mode.LINES,
            1536,
            false,
            false,
            CompositeState.builder()
                    .setShaderState(RenderType.RENDERTYPE_LINES_SHADER)
                    .setLineState(new LineStateShard(OptionalDouble.of(5)))
                    .setLayeringState(RenderType.VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
                    .setOutputState(RenderType.TRANSLUCENT_TARGET)
                    .setWriteMaskState(RenderType.COLOR_WRITE)
                    .setCullState(RenderType.NO_CULL)
                    .createCompositeState(false)
    );

    private static final RenderType SHIELD_LINES_INNER = RenderType.create(
            "forcefield_lines_inner",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            Mode.LINES,
            1536,
            false,
            false,
            CompositeState.builder()
                    .setShaderState(RenderType.RENDERTYPE_LINES_SHADER)
                    .setLineState(new LineStateShard(OptionalDouble.of(2.5)))
                    .setLayeringState(RenderType.VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(RenderType.NO_TRANSPARENCY)
//                    .setOutputState(RenderType.WEATHER_TARGET)
//                    .setDepthTestState(RenderType.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderType.COLOR_DEPTH_WRITE)
                    .setCullState(RenderType.NO_CULL)
                    .createCompositeState(false)
    );

    private static final Map<Integer, SphereMesh> MESH_CACHE = new Int2ObjectOpenHashMap<>();
    private static final Map<Integer, SphereMesh> MESH_CACHE_HEX = new Int2ObjectOpenHashMap<>();
    private record SphereMesh(List<Vector3f> vertices, List<int[]> edges) {}
    private record DisturbanceResult(Vector3f pos, float impactIntensity) {}


    public ForcefieldEmitterRenderer(BlockEntityRendererProvider.Context context) {
        super(context);

        for (int i = 0; i < 5; i++) {
            getOrCreateMesh(i, false);
        }
        for (int i = 0; i < 4; i++) {
            getOrCreateMesh(i, true);
        }
    }

    @Override
    protected void render(@NotNull ForcefieldEmitterBlockEntity tile, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int light, int overlay, @NonNull ProfilerFiller profiler) {
        long now = System.currentTimeMillis();
        if (tile.isRemoved())
            return;

        if (tile.clientRadii == null || tile.clientRadii.length() == 0 || !tile.getActive()) return;

        Level level = tile.getLevel();
        if (level == null) return;

        VertexConsumer consumer = buffer.getBuffer(SHIELD_LINES);


        ClientSubLevel containingClient = Sable.HELPER.getContainingClient(tile);
        Pose3dc renderPose;
        Vector3d relativeCenter = JOMLConversion.toJOML(tile.getBlockPos().getCenter()).sub(tile.clientCenter).negate();
        if (containingClient != null) {
            renderPose = containingClient.renderPose(partialTick);
        } else {
            renderPose = new Pose3d(tile.clientCenter, new Quaterniond(), (Vector3d) JOMLConversion.ZERO, (Vector3d) JOMLConversion.ONE);
            relativeCenter.set(tile.clientCenter);
        }


        float ticks = (now % 3_600_000) / 1000.0F;
        float scanY = Mth.sin(tile.scanOffset + ticks * 0.25f) * 5f;
        float scanWidth = 0.85f;

        boolean impactedRecently = tile.lastImpactTime != 0 && (now - tile.lastImpactTime) < 5_000;

        Quaterniond invRot = renderPose.orientation().invert(new Quaterniond());
        Vector3d invRadii = new Vector3d(1.0/tile.clientRadii.x, 1.0/tile.clientRadii.y, 1.0/tile.clientRadii.z);
        Vector3d centerVec = new Vector3d(relativeCenter.x, relativeCenter.y, relativeCenter.z);
        renderPose.position().get(centerVec);

        boolean activeRainbow = tile.config != null && tile.config.activeColorRainbow();
        boolean passiveRainbow = tile.config != null && tile.config.passiveColorRainbow();
        int configActiveColor = (tile.config != null) ? tile.config.activeColor() : SFColors.MEDIA_OURPLE_ARGB;
        int configPassiveColor = (tile.config != null) ? tile.config.passiveColor() : SFColors.DARK_GREY_ARGB;

        int subdivisions = getSubdivisions(tile);

        SphereMesh mesh = getOrCreateMesh(subdivisions, false);
        poseStack.pushPose();
        poseStack.translate(relativeCenter.x, relativeCenter.y, relativeCenter.z);
        poseStack.scale((float) tile.clientRadii.x, (float) tile.clientRadii.y, (float) tile.clientRadii.z);
        renderMeshEdges(consumer, poseStack, mesh, tile, invRot, centerVec, invRadii, now, scanY, scanWidth, impactedRecently,
                activeRainbow, passiveRainbow, configActiveColor, configPassiveColor, ticks, light, overlay);
        poseStack.popPose();


        VertexConsumer consumer_two = buffer.getBuffer(SHIELD_LINES_INNER);
        SphereMesh secondMesh = getOrCreateMesh(2, true);
        poseStack.pushPose();
        Vector3d normalized = tile.clientRadii.normalize(1.5, new Vector3d());
        Direction facing = tile.facingSupplier.get();
        poseStack.translate(0.5f, (facing.getAxis() == Direction.Axis.Y && facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE) ?  -0.5f : 1.5f , 0.5f);
        poseStack.scale((float) normalized.x, (float) normalized.y, (float) normalized.z);
        renderMeshEdges(consumer_two, poseStack, secondMesh, tile, invRot, centerVec, invRadii, now, scanY, scanWidth, impactedRecently,
                activeRainbow, passiveRainbow, configActiveColor, configPassiveColor, ticks, light, overlay);
        poseStack.popPose();
    }


    private void renderMeshEdges(VertexConsumer consumer, PoseStack poseStack, SphereMesh mesh,
                                 ForcefieldEmitterBlockEntity tile, Quaterniond invRot, Vector3d centerVec,
                                 Vector3d invRadii, long now, float scanY, float scanWidth, boolean impactedRecently,
                                 boolean activeRainbow, boolean passiveRainbow, int configActiveColor, int configPassiveColor,
                                 float ticks, int light, int overlay) {
        Matrix4f poseMatrix = poseStack.last().pose();

        Vector4f baseColorVec1 = new Vector4f();
        Vector4f baseColorVec2 = new Vector4f();
        Vector4f c1 = new Vector4f();
        Vector4f c2 = new Vector4f();


        for (int[] edge : mesh.edges()) {
            Vector3f v1Raw = mesh.vertices.get(edge[0]);
            Vector3f v2Raw = mesh.vertices.get(edge[1]);

            DisturbanceResult res1 = applyDisturbance(v1Raw, tile.activeImpacts, invRot, centerVec, invRadii, now, scanY, scanWidth, impactedRecently);
            DisturbanceResult res2 = applyDisturbance(v2Raw, tile.activeImpacts, invRot, centerVec, invRadii, now, scanY, scanWidth, impactedRecently);

            Vector3f v1 = res1.pos();
            Vector3f v2 = res2.pos();

            float impactInt1 = res1.impactIntensity();
            float impactInt2 = res2.impactIntensity();

            float dist1 = Mth.abs(v1.y - scanY);
            float dist2 = Mth.abs(v2.y - scanY);
            float intensity1 = impactedRecently ? 0.0f : Mth.clamp(1.0f - (dist1 / scanWidth), 0.0f, 1.0f);
            float intensity2 = impactedRecently ? 0.0f : Mth.clamp(1.0f - (dist2 / scanWidth), 0.0f, 1.0f);

            float totalInt1 = Mth.clamp(intensity1 + impactInt1, 0.0f, 1.0f);
            float totalInt2 = Mth.clamp(intensity2 + impactInt2, 0.0f, 1.0f);

            setColor(v1, passiveRainbow, configPassiveColor, ticks, baseColorVec1);
            setColor(v2, passiveRainbow, configPassiveColor, ticks, baseColorVec2);
            setColor(v1, activeRainbow, configActiveColor, ticks, c1).lerp(baseColorVec1, 1.0f - totalInt1);
            setColor(v2, activeRainbow, configActiveColor, ticks, c2).lerp(baseColorVec2, 1.0f - totalInt2);

            consumer.addVertex(poseMatrix, v1.x, v1.y, v1.z)
                    .setNormal(v1.x, v1.y, v1.z)
                    .setUv(0.5f, 0.5f)
                    .setOverlay(overlay)
                    .setLight(light)
                    .setColor(FastColor.ARGB32.colorFromFloat(c1.w, c1.x, c1.z, c1.y));

            consumer.addVertex(poseMatrix, v2.x, v2.y, v2.z)
                    .setNormal(v2.x, v2.y, v2.z)
                    .setUv(0.5f, 0.5f)
                    .setOverlay(overlay)
                    .setLight(light)
                    .setColor(FastColor.ARGB32.colorFromFloat(c2.w, c2.x, c2.z, c2.y));
        }
    }

    private @NonNull Vector4f setColor(Vector3f pos, boolean rainbow, int configColor, float ticks, Vector4f dest) {
        Color c = new Color(configColor, true);
        int a = c.getAlpha();
        if (rainbow) {
            final Vector3d baseLCh = SFColors.LabToLCh(SFColors.toOklab(c));
//            final float t = (float) ((((double) Util.getMillis() / 50) % 100 + ticks + pos.y * 50f) * 2 * Math.PI / 100f);
            float hue = (ticks * 0.1f + pos.z * 0.3f) % 1.0f;
            if (hue < 0) hue += 1.0f;
            c = SFColors.LChOklab(0.9f, 0.55f, (float) (hue*36f + baseLCh.z()));
//            c = new Color(Color.HSBtoRGB(hue, 0.8f, 1.0f));
        }
        dest.set(c.getRed() / 255f, c.getBlue() / 255f, c.getGreen() / 255f, a / 255f);
        return dest;
    }

    private static int getSubdivisions(@NonNull ForcefieldEmitterBlockEntity tile) {
        double maxRadius = Math.max(tile.clientRadii.x, Math.max(tile.clientRadii.y, tile.clientRadii.z));
        int subdivisions;
         if (maxRadius < 64.0) {
            subdivisions = 3;
        } else if (maxRadius < 96.0) {
            subdivisions = 4;
        } else {
            subdivisions = 5;
        }
        return subdivisions;
    }

    @Override
    protected @NonNull String getProfilerSection() {
        return "forcefield";
    }

    public boolean shouldRenderOffScreen(@NonNull ForcefieldEmitterBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean shouldRender(@NonNull ForcefieldEmitterBlockEntity blockEntity, @NonNull Vec3 cameraPos) {
        return true;
    }

    @Override
    public @NonNull AABB getRenderBoundingBox(ForcefieldEmitterBlockEntity tile) {
        ClientSubLevel sbl = Sable.HELPER.getContainingClient(tile);
        if (sbl == null) {
            if (tile.clientRadii == null) {
                return AABB.ofSize(tile.getBlockPos().getCenter(), 100d, 100d, 100d);
            }
            return AABB.ofSize(tile.getBlockPos().getCenter(), tile.clientRadii.x*2, tile.clientRadii.y*2, tile.clientRadii.z*2);
        }
        return sbl.boundingBox().toMojang().inflate(10+tile.config.fieldExpansion());
    }

    private SphereMesh getOrCreateMesh(int subdivisions, boolean full) {
        if (full) {
            return MESH_CACHE_HEX.computeIfAbsent(subdivisions, (i) -> this.generateGeodesicMesh(i, true));
        } else {
            return MESH_CACHE.computeIfAbsent(subdivisions, (i) -> this.generateGeodesicMesh(i, false));
        }
    }

    private SphereMesh generateGeodesicMesh(int subdivisions, boolean fullHex) {
        double t = (1.0 + Math.sqrt(5.0)) / 2.0;
        List<Vector3f> vertices = new ArrayList<>();
        vertices.add(new Vector3f(-1,  (float)t,  0));
        vertices.add(new Vector3f( 1,  (float)t,  0));
        vertices.add(new Vector3f(-1, -(float)t,  0));
        vertices.add(new Vector3f( 1, -(float)t,  0));
        vertices.add(new Vector3f( 0, -1,  (float)t));
        vertices.add(new Vector3f( 0,  1,  (float)t));
        vertices.add(new Vector3f( 0, -1, -(float)t));
        vertices.add(new Vector3f( 0,  1, -(float)t));
        vertices.add(new Vector3f( (float)t,  0, -1));
        vertices.add(new Vector3f( (float)t,  0,  1));
        vertices.add(new Vector3f(-(float)t,  0, -1));
        vertices.add(new Vector3f(-(float)t,  0,  1));
        for(Vector3f v : vertices) v.normalize();

        List<int[]> faces = new ArrayList<>();
        int[] indices = {
                0, 11, 5,   0, 5, 1,   0, 1, 7,   0, 7, 10,  0, 10, 11,
                1, 5, 9,    5, 11, 4,  11, 10, 2,  10, 7, 6,   7, 1, 8,
                3, 9, 4,    3, 4, 2,   3, 2, 6,   3, 6, 8,    3, 8, 9,
                4, 9, 5,    2, 4, 11,  6, 2, 10,  8, 6, 7,    9, 8, 1
        };
        for (int i = 0; i < indices.length; i += 3) {
            faces.add(new int[]{indices[i], indices[i+1], indices[i+2]});
        }

        if (fullHex) {
            Map<Long, Integer> middleCache = new HashMap<>();

            for (int i = 0; i < subdivisions; i++) {
                List<int[]> newFaces = new ArrayList<>();
                for (int[] face : faces) {
                    int a = face[0], b = face[1], c = face[2];
                    int ab = getMiddlePoint(a, b, vertices, middleCache);
                    int bc = getMiddlePoint(b, c, vertices, middleCache);
                    int ca = getMiddlePoint(c, a, vertices, middleCache);

                    newFaces.add(new int[]{a, ab, ca});
                    newFaces.add(new int[]{b, bc, ab});
                    newFaces.add(new int[]{c, ca, bc});
                    newFaces.add(new int[]{ab, bc, ca});
                }
                faces = newFaces;
            }
        } else {
            for (int i = 0; i < subdivisions; i++) {
                List<int[]> newFaces = new ArrayList<>();
                for (int[] face : faces) {
                    Vector3f a = vertices.get(face[0]);
                    Vector3f b = vertices.get(face[1]);
                    Vector3f c = vertices.get(face[2]);
                    Vector3f ab = getMidpoint(a, b);
                    Vector3f bc = getMidpoint(b, c);
                    Vector3f ca = getMidpoint(c, a);
                    int iAb = addVertex(vertices, ab);
                    int iBc = addVertex(vertices, bc);
                    int iCa = addVertex(vertices, ca);
                    newFaces.add(new int[]{face[0], iAb, iCa});
                    newFaces.add(new int[]{face[1], iBc, iAb});
                    newFaces.add(new int[]{face[2], iCa, iBc});
                    newFaces.add(new int[]{iAb, iBc, iCa});
                }
                faces = newFaces;
            }
        }

        return buildDualMesh(vertices, faces);
    }

    private int getMiddlePoint(int p1, int p2, List<Vector3f> vertices, Map<Long, Integer> cache) {
        long key = ((long) Math.min(p1, p2) << 32L) | (Math.max(p1, p2) & 0xFFFFFFFFL);
        Integer index = cache.get(key);
        if (index != null) return index;

        Vector3f v1 = vertices.get(p1);
        Vector3f v2 = vertices.get(p2);
        Vector3f mid = new Vector3f((v1.x + v2.x) / 2, (v1.y + v2.y) / 2, (v1.z + v2.z) / 2);
        mid.normalize();

        int i = vertices.size();
        vertices.add(mid);
        cache.put(key, i);
        return i;
    }

    private SphereMesh buildDualMesh(List<Vector3f> originalVertices, List<int[]> originalFaces) {
        List<Vector3f> dualVertices = new ArrayList<>();
        for (int[] face : originalFaces) {
            Vector3f a = originalVertices.get(face[0]);
            Vector3f b = originalVertices.get(face[1]);
            Vector3f c = originalVertices.get(face[2]);

            Vector3f center = new Vector3f((a.x + b.x + c.x)/3, (a.y + b.y + c.y)/3, (a.z + b.z + c.z)/3);
            center.normalize();
            dualVertices.add(center);
        }

        Map<Long, int[]> edgeToFace = new HashMap<>();
        List<int[]> dualEdges = new ArrayList<>();

        for (int i = 0; i < originalFaces.size(); i++) {
            int[] face = originalFaces.get(i);
            int[] edges = {face[0], face[1], face[1], face[2], face[2], face[0]};

            for (int j = 0; j < edges.length; j += 2) {
                int u = edges[j];
                int v = edges[j+1];
                long key = ((long) Math.min(u, v) << 32L) | (Math.max(u, v) & 0xFFFFFFFFL);

                int[] facesOnEdge = edgeToFace.get(key);
                if (facesOnEdge == null) {
                    edgeToFace.put(key, new int[]{i, -1});
                } else {
                    int neighbor = facesOnEdge[0];
                    dualEdges.add(new int[]{i, neighbor});
                    edgeToFace.remove(key);
                }
            }
        }

        return new SphereMesh(dualVertices, dualEdges);
    }

    private int addVertex(List<Vector3f> vertices, Vector3f v) {
        vertices.add(v);
        return vertices.size() - 1;
    }

    private Vector3f getMidpoint(Vector3f v1, Vector3f v2) {
        Vector3f mid = new Vector3f((v1.x + v2.x) / 2, (v1.y + v2.y) / 2, (v1.z + v2.z) / 2);
        mid.normalize();
        return mid;
    }

    private DisturbanceResult applyDisturbance(Vector3f v, Set<ForcefieldEmitterBlockEntity.ImpactData> impacts,
                                               Quaterniond invRot, Vector3d center, Vector3d invRadii,
                                               long now, float scanY, float scanWidth, boolean impactedRecently) {

        Vector3f disturbedV = new Vector3f(v);
        float maxImpactIntensity = 0f;

        for (ForcefieldEmitterBlockEntity.ImpactData impact : impacts) {
            Vector3d iPos = new Vector3d(impact.pos().x, impact.pos().y, impact.pos().z);
            iPos.sub(center);
            invRot.transform(iPos);
            iPos.mul(invRadii);

            Vector3f impactUnit = new Vector3f((float)iPos.x, (float)iPos.y, (float)iPos.z);
            float dist = v.distance(impactUnit);

            float waveAge = (now - impact.time()) / 1000.0f; // For wave phase (continuous)
            float fadeAge = (now - impact.lastHitTime()) / 1000.0f; // For fading (stays fresh)

            if (fadeAge > 2.5f) continue;

            if (dist < 5.9f) {
                double amplitude = (0.145f * impact.energy()) * Math.exp(-dist * 2f);
                float wave = (float) (Math.cos(dist * 5.0f - waveAge * 20.0f) * amplitude);
                wave *= (1.0f - (fadeAge / 2.5f));
                disturbedV.add(v.x * wave, v.y * wave, v.z * wave);
            }

            float colorWaveFront = fadeAge * 1.5f;
            float distFromWaveFront = Math.abs(dist - colorWaveFront);
            float impactIntensity = (float)Math.exp(-distFromWaveFront * 4.0f);
            impactIntensity *= (1.0f - (fadeAge / 2.0f));
            if (impactIntensity > maxImpactIntensity) {
                maxImpactIntensity = impactIntensity;
            }
        }

        if (disturbedV.equals(v) && !impactedRecently) {
            float dist = Mth.abs(v.y - scanY);
            float intensity = Mth.clamp(1.0f - (dist / scanWidth), -0.01f, 0.01f);

            if (intensity > 0.0f)
                disturbedV.add((float) (v.x * (Math.random()*intensity)), (float) (v.y * (Math.random()*intensity)), (float) (v.z * (Math.random()*intensity)));
        }

        return new DisturbanceResult(disturbedV, maxImpactIntensity);
    }
    private static float spatialNoise(float x, float y, float z, float seed) {
        float dot = x * 12.9898f + y * 78.233f + z * 45.164f + seed;
        float s = (float) Math.sin(dot) * 43758.5453f;
        return s - (int) s; // Fast fractional part (0.0 to 1.0)
    }
}