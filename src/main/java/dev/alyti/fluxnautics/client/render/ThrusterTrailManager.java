package dev.alyti.fluxnautics.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.alyti.fluxnautics.client.util.SimpleSubLevelGroupRenderer;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.client.render.shader.uniform.ShaderUniformAccess;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class ThrusterTrailManager {

    public record Sample(double x, double y, double z,
                         float axX, float axY, float axZ,
                         float ayX, float ayY, float ayZ,
                         float c0, float c1, float c2,
                         float intensity, float radius, long timeMs) {

        float axisScale() {
            return (float) Math.sqrt(axX * axX + axY * axY + axZ * axZ);
        }
    }

    static final class Trail {
        final SampleRing samples = new SampleRing();
        long lastEmitMs;
        long lastSeenMs;
        float seed;
        boolean rainbow;
        boolean thrusting;
        float[] poly;
        int polyN;
        float extX, extY;
    }

    static final class SampleRing {
        Sample[] buf = new Sample[16];
        int head;
        int size;

        void addLast(Sample s) {
            if (this.size == this.buf.length) {
                Sample[] next = new Sample[this.buf.length * 2];
                System.arraycopy(this.buf, this.head, next, 0, this.size - this.head);
                System.arraycopy(this.buf, 0, next, this.size - this.head, this.head);
                this.buf = next;
                this.head = 0;
            }
            this.buf[(this.head + this.size) % this.buf.length] = s;
            this.size++;
        }

        Sample last() {
            return this.buf[(this.head + this.size - 1) % this.buf.length];
        }

        void pollFirst() {
            this.buf[this.head] = null;
            this.head = (this.head + 1) % this.buf.length;
            this.size--;
        }

        void clear() {
            Arrays.fill(this.buf, null);
            this.head = 0;
            this.size = 0;
        }

        public int size() {
            return size;
        }
    }

    static final class ShipTrack {
        static final int LEN = 34;
        final long[] ms = new long[LEN];
        final float[] xs = new float[LEN];
        final float[] ys = new float[LEN];
        final float[] zs = new float[LEN];
        int head;
        int size;

        void push(long t, double x, double y, double z) {
            if (this.size > 0) {
                int last = (this.head + this.size - 1) % LEN;
                if (t - this.ms[last] < TRACK_MIN_STEP_MS) {
                    return;
                }
            }
            if (this.size == LEN) {
                this.ms[this.head] = t;
                this.xs[this.head] = (float) x;
                this.ys[this.head] = (float) y;
                this.zs[this.head] = (float) z;
                this.head = (this.head + 1) % LEN;
            } else {
                int i = (this.head + this.size) % LEN;
                this.ms[i] = t;
                this.xs[i] = (float) x;
                this.ys[i] = (float) y;
                this.zs[i] = (float) z;
                this.size++;
            }
        }

        boolean velocity(long now, Vector3f out) {
            if (this.size < 2) {
                return false;
            }
            int last = (this.head + this.size - 1) % LEN;
            if (now - this.ms[last] > 250L) {
                return false;
            }
            int oldest = last;
            for (int k = 0; k < this.size; k++) {
                int i = (last - k + LEN) % LEN;
                if (this.ms[last] - this.ms[i] <= VEL_WINDOW_MS) {
                    oldest = i;
                } else {
                    break;
                }
            }
            long dtMs = this.ms[last] - this.ms[oldest];
            if (dtMs < 40L) {
                return false;
            }
            float dt = dtMs / 1000f;
            out.set((this.xs[last] - this.xs[oldest]) / dt,
                (this.ys[last] - this.ys[oldest]) / dt,
                (this.zs[last] - this.zs[oldest]) / dt);
            return true;
        }

        void clear() {
            this.head = 0;
            this.size = 0;
        }
    }

    static final class ControllerEntry {
        final Map<BlockPos, Trail> trails = new IdentityHashMap<>();
        final ShipTrack track = new ShipTrack();
        long lastSeenMs;
    }

    public static boolean enabled = true;
    public static float lifetimeS = 2.0f;
    public static float widthScale = 1.0f;
    public static float minDist = 0.5f;
    public static float fadeTauS = 0.95f;

    public static final int SHAPE_RIBBON = 0;
    public static final int SHAPE_RECT = 1;
    public static final int SHAPE_PROFILE = 2;
    public static int shapeMode = SHAPE_PROFILE;
    public static float coreGlow = 0.45f;

    public static float gateDegrees = 75.0f;

    public static float hueSpan = 0.0f;

    static final int MAX_SAMPLES = 96;
    static final int MAX_TRAILS = 24;

    static final int MAX_POLY_VERTS = 64;
    static final int FACE_BUDGET = 24;
    static final float MIN_INTENSITY = 0.02f;
    static final long IDLE_MS = 250L;
    static final long PURGE_MS = 3000L;
    static final long TRACK_MIN_STEP_MS = 15L;
    static final long VEL_WINDOW_MS = 400L;
    static final float MIN_SPEED = 0.05f;
    static final long SEGMENT_GAP_MS = 200L;
    static final float SEGMENT_JUMP_BLOCKS = 15.0f;

    private static final Map<Object, ControllerEntry> TRAILS = new IdentityHashMap<>();

    private static RenderType trailType;
    private static boolean typeResolved;
    private static Level lastWorld;
    private static final List<Trail> LIVE = new ArrayList<>();
    private static final List<Vec3> LIVE_ANCHOR = new ArrayList<>();

    private ThrusterTrailManager() {
    }

    public static void recordFromRenderer(BlockEntity controller, BlockPos anchor, Matrix4f matrix,
                                          float centerX, float centerY, float radius,
                                          float[] footprint,
                                          Vector4f color, boolean rainbow,
                                          float layerFade, float length, float seed) {
        if (!enabled || length <= MIN_INTENSITY || SimpleSubLevelGroupRenderer.RENDERING_SIMPLE) {
            return;
        }
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();

        float scale = new Vector3f(1f, 0f, 0f).mulDirection(matrix).length();

        Vector3f nozzle = new Vector3f(centerX, centerY, 0f).mulPosition(matrix);
        Vec3 cam = camera.getPosition();
        double wx = nozzle.x + cam.x;
        double wy = nozzle.y + cam.y;
        double wz = nozzle.z + cam.z;

        Vector3f xAxis = new Vector3f(1f, 0f, 0f).mulDirection(matrix);
        Vector3f yAxis = new Vector3f(0f, 1f, 0f).mulDirection(matrix);
        Vector3f thrust = new Vector3f(0f, 0f, -1f).mulDirection(matrix);

        float c0, c1, c2;
        if (rainbow) {
            c0 = color.x();
            c1 = color.z();
            c2 = 0.0f;
        } else {
            c0 = color.x();
            c1 = color.z();
            c2 = color.y();
        }
        float intensity = Math.min(1.0f, length * 2.0f) * layerFade;

        recordSample(controller, anchor, wx, wy, wz,
            xAxis.x, xAxis.y, xAxis.z, yAxis.x, yAxis.y, yAxis.z,
            thrust.x, thrust.y, thrust.z,
            c0, c1, c2, intensity,
            Math.max(radius * scale, 0.05f), rainbow, seed,
            footprint, footprint == null ? 0 : footprint.length / 2);
    }

    static void recordSample(Object controller, BlockPos anchor,
                             double x, double y, double z,
                             float axX, float axY, float axZ,
                             float ayX, float ayY, float ayZ,
                             float thX, float thY, float thZ,
                             float c0, float c1, float c2, float intensity, float radius,
                             boolean rainbow, float seed,
                             float[] poly, int polyN) {
        long now = Util.getMillis();
        ControllerEntry entry = TRAILS.get(controller);
        if (entry == null) {
            entry = new ControllerEntry();
            TRAILS.put(controller, entry);
        }
        entry.lastSeenMs = now;
        Trail trail = entry.trails.get(anchor);
        if (trail == null) {
            trail = new Trail();
            trail.seed = seed;
            entry.trails.put(anchor, trail);
        }
        trail.rainbow = rainbow;
        trail.lastSeenMs = now;

        entry.track.push(now, x, y, z);

        if (gateDegrees < 179.5f && entry.track.velocity(now, velScratch)) {
            float speed = velScratch.length();
            float thrustLen = (float) Math.sqrt(thX * thX + thY * thY + thZ * thZ);
            if (speed > MIN_SPEED && thrustLen > 1.0e-5f) {
                float dot = (thX * velScratch.x + thY * velScratch.y + thZ * velScratch.z)
                    / (thrustLen * speed);
                if (dot < (float) Math.cos(Math.toRadians(gateDegrees))) {
                    return;
                }
            }
        }

        trail.thrusting = true;
        trail.lastEmitMs = now;

        if (trail.poly != poly || trail.polyN != polyN) {
            trail.poly = poly;
            trail.polyN = polyN;
            float ex = 0.0f, ey = 0.0f;
            for (int i = 0; i < polyN; i++) {
                ex = Math.max(ex, Math.abs(poly[i * 2]));
                ey = Math.max(ey, Math.abs(poly[i * 2 + 1]));
            }
            trail.extX = ex;
            trail.extY = ey;
        }

        Sample newest = trail.samples.size() > 0 ? trail.samples.last() : null;
        if (newest != null) {
            double dx = x - newest.x();
            double dy = y - newest.y();
            double dz = z - newest.z();
            double minSq = (double) minDist * minDist;
            if (dx * dx + dy * dy + dz * dz < minSq) {
                return;
            }
        }
        trail.samples.addLast(new Sample(x, y, z,
            axX, axY, axZ, ayX, ayY, ayZ,
            c0, c1, c2, intensity, radius, now));
        while (trail.samples.size() > MAX_SAMPLES) {
            trail.samples.pollFirst();
        }
    }

    public static void onWorldRender(MultiBufferSource.BufferSource buffers, Camera camera, Matrix4fc modelView, Matrix4fc projection) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level != lastWorld) {
            TRAILS.clear();
            lastWorld = level;
        }
        if (!enabled || trailTypeNull(buffers)) {
            return;
        }
        long now = Util.getMillis();
        Vec3 cam = camera.getPosition();

        Iterator<Map.Entry<Object, ControllerEntry>> entries = TRAILS.entrySet().iterator();
        while (entries.hasNext()) {
            ControllerEntry entry = entries.next().getValue();
            Iterator<Map.Entry<BlockPos, Trail>> trails = entry.trails.entrySet().iterator();
            while (trails.hasNext()) {
                Trail trail = trails.next().getValue();
                float cutoff = cutoff(trail, now);
                long cutoffMs = now - (long) (lifetimeS * 1000f);
                while (trail.samples.size() > 0 && trail.samples.buf[trail.samples.head].timeMs() < cutoffMs) {
                    trail.samples.pollFirst();
                }
                trail.thrusting = now - trail.lastEmitMs < IDLE_MS;
                boolean dead = trail.samples.size() < 2 && !trail.thrusting && cutoff < 0.02f;
                if (dead) {
                    trails.remove();
                }
            }
            if (entry.trails.isEmpty() && now - entry.lastSeenMs > PURGE_MS) {
                entries.remove();
            }
        }

        LIVE.clear();
        LIVE_ANCHOR.clear();
        for (Map.Entry<Object, ControllerEntry> e : TRAILS.entrySet()) {
            for (Map.Entry<BlockPos, Trail> t : e.getValue().trails.entrySet()) {
                Trail trail = t.getValue();
                if (trail.samples.size() < 2 || cutoff(trail, now) <= 0.01f) {
                    continue;
                }
                Sample newest = trail.samples.last();
                LIVE.add(trail);
                LIVE_ANCHOR.add(new Vec3(newest.x(), newest.y(), newest.z()));
            }
        }
        while (LIVE.size() > MAX_TRAILS) {
            int worst = 0;
            double worstD = -1.0;
            for (int i = 0; i < LIVE.size(); i++) {
                Vec3 p = LIVE_ANCHOR.get(i);
                double d = p.distanceToSqr(cam);
                if (d > worstD) {
                    worstD = d;
                    worst = i;
                }
            }
            LIVE.remove(worst);
            LIVE_ANCHOR.remove(worst);
        }

        float ticks = (Util.getMillis() % 3600000L) / 1000f;
        for (Trail trail : LIVE) {
            emitTrail(buffers, trail, cam, ticks, now, modelView, projection);
        }
    }

    private static boolean trailTypeNull(MultiBufferSource.BufferSource buffers) {
        if (!typeResolved) {
            typeResolved = true;
            trailType = VeilRenderType.get(ResourceLocation.fromNamespaceAndPath("fluxnautics", "trail_fbm"));
        }
        return trailType == null;
    }

    private static float cutoff(Trail trail, long now) {
        float dt = (now - trail.lastEmitMs) / Math.max(fadeTauS * 1000f, 1.0f);
        return (float) Math.exp(-dt);
    }

    private static void emitTrail(MultiBufferSource.BufferSource buffers, Trail trail, Vec3 cam, float ticks, long now, Matrix4fc modelView, Matrix4fc projection) {
        RenderType rt = trailType;
        int n = trail.samples.size();
        Sample[] s = snapshot(trail);

        if (runOfScratch.length < n) {
            runOfScratch = new int[n];
            runStartScratch = new int[n];
        }
        int runs = 0;
        runOfScratch[0] = 0;
        runStartScratch[0] = 0;
        for (int i = 1; i < n; i++) {
            if (isGap(s[i - 1], s[i])) {
                runs++;
                runStartScratch[runs] = i;
            }
            runOfScratch[i] = runs;
        }
        runs++;

        double pathLen = 0.0;
        for (int i = 1; i < n; i++) {
            if (runOfScratch[i] == runOfScratch[i - 1]) {
                pathLen += dist(s[i], s[i - 1]);
            }
        }

        float cutoff = cutoff(trail, now);

        VertexConsumer out = buffers.getBuffer(rt);
        RenderStateShard.ShaderStateShard shard = VeilRenderType.getShards(rt).shaderState();
        shard.setupRenderState();
        ShaderProgram shader = VeilRenderSystem.getShader();
        if (shader == null) {
            return;
        }
        ShaderUniformAccess time = shader.getUniformSafe("time");
        time.setFloat(ticks);
        shader.getUniformSafe("uTrailLen").setFloat(Math.max((float) pathLen, 1.0f));
        shader.getUniformSafe("uTrailSeed").setFloat(trail.seed);
        shader.getUniformSafe("uTrailRainbow").setFloat(0.0f);

        shader.getUniformSafe("uTrailModelView").setMatrix(modelView);
        shader.getUniformSafe("uTrailProjMat").setMatrix(projection);

        if (taperScratch.length < n) {
            taperScratch = new float[n];
        }
        if (tScratch.length < n) {
            tScratch = new float[n];
        }
        for (int r = 0; r < runs; r++) {
            int a = runStartScratch[r];
            int b = r + 1 < runs ? runStartScratch[r + 1] - 1 : n - 1;
            int len = b - a + 1;
            for (int i = a; i <= b; i++) {
                float t = len > 1 ? 1.0f - (i - a) / (float) (len - 1) : 0.0f;
                tScratch[i] = t;
                taperScratch[i] = (1.15f - 0.15f * t) * (float) Math.pow(1.0f - t, 0.45);
            }
        }

        if (rgbScratch.length < n * 3) {
            rgbScratch = new float[n * 3];
        }
        if (trail.rainbow) {
            for (int i = 0; i < n; i++) {
                decodeRainbow(s[i].c0() + tScratch[i] * hueSpan, s[i].c1(), i);
            }
        } else {
            for (int i = 0; i < n; i++) {
                rgbScratch[i * 3] = s[i].c0();
                rgbScratch[i * 3 + 1] = s[i].c1();
                rgbScratch[i * 3 + 2] = s[i].c2();
            }
        }

        float[] poly = trail.poly;
        int polyN = trail.polyN;
        boolean tube = shapeMode != SHAPE_RIBBON && poly != null && polyN >= 3;
        if (tube) {
            if (shapeMode == SHAPE_RECT) {
                poly = rectScratch;
                polyN = 4;
                rectScratch[0] = -trail.extX;
                rectScratch[1] = -trail.extY;
                rectScratch[2] = trail.extX;
                rectScratch[3] = -trail.extY;
                rectScratch[4] = trail.extX;
                rectScratch[5] = trail.extY;
                rectScratch[6] = -trail.extX;
                rectScratch[7] = trail.extY;
            }
            polyN = Math.min(polyN, ringA.length / 3);
            int stride = polyN <= FACE_BUDGET ? 1 : (polyN + FACE_BUDGET - 1) / FACE_BUDGET;
            int m = 0;
            for (int r = 0; r < runs; r++) {
                int a = runStartScratch[r];
                int b = r + 1 < runs ? runStartScratch[r + 1] - 1 : n - 1;
                ringIdxScratch[m++] = a;
                for (int i = a + stride; i < b; i += stride) {
                    ringIdxScratch[m++] = i;
                }
                if (b > a) {
                    ringIdxScratch[m++] = b;
                }
            }
            emitTube(out, s, ringIdxScratch, m, poly, polyN, cam, cutoff);
        }
        float coreExtent = tube && coreGlow > 0.001f
            ? Math.min(trail.extX, trail.extY)
            : -1.0f;
        if (!tube || coreExtent >= 0.0f) {
            emitRibbon(out, s, n, cam, cutoff, coreExtent, runs);
        }

        buffers.endBatch(rt);
    }

    private static void emitTube(VertexConsumer out, Sample[] s, int[] ringIdx, int m, float[] poly, int polyN, Vec3 cam, float cutoff) {
        double camX = cam.x, camY = cam.y, camZ = cam.z;
        for (int j = 0; j + 1 < m; j++) {
            int i = ringIdx[j];
            int i2 = ringIdx[j + 1];
            if (runOfScratch[i] != runOfScratch[i2]) {
                continue;
            }
            Sample a = s[i];
            Sample b = s[i2];
            float aA = a.intensity() * cutoff;
            float bA = b.intensity() * cutoff;
            if (aA < 0.004f && bA < 0.004f) {
                continue;
            }
            float ta = tScratch[i];
            float tb = tScratch[i2];
            fillRing(ringA, a, poly, polyN, taperScratch[i], camX, camY, camZ);
            fillRing(ringB, b, poly, polyN, taperScratch[i2], camX, camY, camZ);
            for (int k = 0; k < polyN; k++) {
                int g = (k + 1) % polyN;
                float u0 = (k & 1) == 0 ? -1f : 1f;
                float u1 = -u0;
                quad(out,
                    ringA[k * 3], ringA[k * 3 + 1], ringA[k * 3 + 2],
                    ringA[g * 3], ringA[g * 3 + 1], ringA[g * 3 + 2],
                    ringB[g * 3], ringB[g * 3 + 1], ringB[g * 3 + 2],
                    ringB[k * 3], ringB[k * 3 + 1], ringB[k * 3 + 2],
                    u0, u1, ta, tb, i, i2, aA, bA);
            }
        }
    }

    private static void emitRibbon(VertexConsumer out, Sample[] s, int n, Vec3 cam, float cutoff, float coreExtent, int runs) {
        boolean core = coreExtent >= 0.0f;
        float coreMul = core ? coreGlow : 1.0f;
        double camX = cam.x, camY = cam.y, camZ = cam.z;
        float prevSideX = 0f, prevSideY = 1f, prevSideZ = 0f;
        float[] halfW = new float[n];
        float[][] side = new float[n][3];
        for (int i = 0; i < n; i++) {
            int run = runOfScratch[i];
            int rs = runStartScratch[run];
            int re = run + 1 < runs ? runStartScratch[run + 1] - 1 : n - 1;
            Sample prev = s[Math.max(i - 1, rs)];
            Sample next = s[Math.min(i + 1, re)];
            double dx = next.x() - prev.x();
            double dy = next.y() - prev.y();
            double dz = next.z() - prev.z();
            double dl = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dl < 1.0e-7) {
                dx = 1;
                dy = 0;
                dz = 0;
                dl = 1;
            }
            dx /= dl;
            dy /= dl;
            dz /= dl;
            double tx = camX - s[i].x();
            double ty = camY - s[i].y();
            double tz = camZ - s[i].z();
            double cx = dy * tz - dz * ty;
            double cy = dz * tx - dx * tz;
            double cz = dx * ty - dy * tx;
            double cl = Math.sqrt(cx * cx + cy * cy + cz * cz);
            if (cl < 1.0e-6) {
                side[i][0] = prevSideX;
                side[i][1] = prevSideY;
                side[i][2] = prevSideZ;
            } else {
                side[i][0] = (float) (cx / cl);
                side[i][1] = (float) (cy / cl);
                side[i][2] = (float) (cz / cl);
                prevSideX = side[i][0];
                prevSideY = side[i][1];
                prevSideZ = side[i][2];
            }
            float base = core ? coreExtent * s[i].axisScale() : s[i].radius();
            halfW[i] = base * widthScale * taperScratch[i] * coreMul;
        }

        for (int i = 0; i + 1 < n; i++) {
            int i2 = i + 1;
            if (runOfScratch[i] != runOfScratch[i2]) {
                continue;
            }
            Sample a = s[i];
            Sample b = s[i2];
            float ta = tScratch[i];
            float tb = tScratch[i2];
            float aA = a.intensity() * cutoff * coreMul;
            float bA = b.intensity() * cutoff * coreMul;
            if (aA < 0.004f && bA < 0.004f) {
                continue;
            }
            float ax = (float) (a.x() - camX), ay = (float) (a.y() - camY), az = (float) (a.z() - camZ);
            float bx = (float) (b.x() - camX), by = (float) (b.y() - camY), bz = (float) (b.z() - camZ);
            float asx = side[i][0] * halfW[i], asy = side[i][1] * halfW[i], asz = side[i][2] * halfW[i];
            float bsx = side[i2][0] * halfW[i2], bsy = side[i2][1] * halfW[i2], bsz = side[i2][2] * halfW[i2];

            out.addVertex(ax - asx, ay - asy, az - asz).setUv(-1f, ta).setColor(rgbScratch[i * 3], rgbScratch[i * 3 + 1], rgbScratch[i * 3 + 2], aA);
            out.addVertex(ax + asx, ay + asy, az + asz).setUv(1f, ta).setColor(rgbScratch[i * 3], rgbScratch[i * 3 + 1], rgbScratch[i * 3 + 2], aA);
            out.addVertex(bx + bsx, by + bsy, bz + bsz).setUv(1f, tb).setColor(rgbScratch[i2 * 3], rgbScratch[i2 * 3 + 1], rgbScratch[i2 * 3 + 2], bA);
            out.addVertex(bx - bsx, by - bsy, bz - bsz).setUv(-1f, tb).setColor(rgbScratch[i2 * 3], rgbScratch[i2 * 3 + 1], rgbScratch[i2 * 3 + 2], bA);
        }
    }

    private static float[] taperScratch = new float[MAX_SAMPLES];
    private static float[] tScratch = new float[MAX_SAMPLES];
    private static int[] runOfScratch = new int[MAX_SAMPLES];
    private static int[] runStartScratch = new int[MAX_SAMPLES];
    private static float[] rgbScratch = new float[MAX_SAMPLES * 3];
    private static final float[] ringA = new float[MAX_POLY_VERTS * 3];
    private static final float[] ringB = new float[MAX_POLY_VERTS * 3];
    private static final float[] rectScratch = new float[8];
    private static final Vector3f velScratch = new Vector3f();
    private static final int[] ringIdxScratch = new int[MAX_SAMPLES * 3 + 2];

    private static void fillRing(float[] ring, Sample s, float[] poly, int polyN,
                                 float taper, double camX, double camY, double camZ) {
        float cxx = (float) (s.x() - camX);
        float cyy = (float) (s.y() - camY);
        float czz = (float) (s.z() - camZ);
        float xaX = s.axX(), xaY = s.axY(), xaZ = s.axZ();
        float yaX = s.ayX(), yaY = s.ayY(), yaZ = s.ayZ();
        for (int k = 0; k < polyN; k++) {
            float vx = poly[k * 2] * taper;
            float vy = poly[k * 2 + 1] * taper;
            ring[k * 3] = cxx + xaX * vx + yaX * vy;
            ring[k * 3 + 1] = cyy + xaY * vx + yaY * vy;
            ring[k * 3 + 2] = czz + xaZ * vx + yaZ * vy;
        }
    }

    private static void quad(VertexConsumer out,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float u0, float u1, float ta, float tb,
                             int ia, int ib, float aA, float bA) {
        out.addVertex(x0, y0, z0).setUv(u0, ta).setColor(rgbScratch[ia * 3], rgbScratch[ia * 3 + 1], rgbScratch[ia * 3 + 2], aA);
        out.addVertex(x1, y1, z1).setUv(u1, ta).setColor(rgbScratch[ia * 3], rgbScratch[ia * 3 + 1], rgbScratch[ia * 3 + 2], aA);
        out.addVertex(x2, y2, z2).setUv(u1, tb).setColor(rgbScratch[ib * 3], rgbScratch[ib * 3 + 1], rgbScratch[ib * 3 + 2], bA);
        out.addVertex(x3, y3, z3).setUv(u0, tb).setColor(rgbScratch[ib * 3], rgbScratch[ib * 3 + 1], rgbScratch[ib * 3 + 2], bA);
    }

    private static void decodeRainbow(float hue01, float chroma, int i) {
        float h = hue01 * 6.2831853f;
        float a_ = chroma * (float) Math.cos(h);
        float b_ = chroma * (float) Math.sin(h);
        float l_ = 0.8f + 0.3963377774f * a_ + 0.2158037573f * b_;
        float m_ = 0.8f - 0.1055613458f * a_ - 0.0638541728f * b_;
        float s_ = 0.8f - 0.0894841775f * a_ - 1.2914855480f * b_;
        float l = l_ * l_ * l_;
        float m = m_ * m_ * m_;
        float s = s_ * s_ * s_;
        float lr = +4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s;
        float lg = -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s;
        float lb = -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s;
        rgbScratch[i * 3] = srgbTransfer(lr);
        rgbScratch[i * 3 + 1] = srgbTransfer(lg);
        rgbScratch[i * 3 + 2] = srgbTransfer(lb);
    }

    private static float srgbTransfer(float lin) {
        if (lin < 0.0031308f) {
            lin = 12.92f * lin;
        } else {
            lin = 1.055f * (float) Math.pow(lin, 1.0 / 2.4) - 0.055f;
        }
        return Math.clamp(lin, 0.0f, 1.0f);
    }

    private static Sample[] snapshotScratch = new Sample[MAX_SAMPLES];

    private static Sample[] snapshot(Trail trail) {
        int n = trail.samples.size();
        if (snapshotScratch.length < n) {
            snapshotScratch = new Sample[n];
        }
        for (int i = 0; i < n; i++) {
            snapshotScratch[i] = trail.samples.buf[(trail.samples.head + i) % trail.samples.buf.length];
        }
        return snapshotScratch;
    }

    private static double dist(Sample a, Sample b) {
        double dx = a.x() - b.x();
        double dy = a.y() - b.y();
        double dz = a.z() - b.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static boolean isGap(Sample a, Sample b) {
        return b.timeMs() - a.timeMs() > SEGMENT_GAP_MS
            && dist(a, b) > SEGMENT_JUMP_BLOCKS;
    }

    public static void clearAll() {
        TRAILS.clear();
    }
}
