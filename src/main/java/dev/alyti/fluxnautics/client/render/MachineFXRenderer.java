package dev.alyti.fluxnautics.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.alyti.fluxnautics.api.render.IMachineFX;
import dev.alyti.fluxnautics.content.blocks.spatial_anchor.SpatialAnchorBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import mekanism.client.render.tileentity.MekanismTileEntityRenderer;
import mekanism.common.lib.Color;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;
import java.util.WeakHashMap;

public abstract class MachineFXRenderer<T extends TileEntityMekanism, P> extends MekanismTileEntityRenderer<T> {

    private static final long TIME_WRAP_TICKS = 432_000L;

    private static final float MATTER_LERP_SECONDS = 0.25F;

    private final Map<SpatialAnchorBlockEntity, MatterSmooth> matterStates = new WeakHashMap<>();

    public Quaternionf subLevelInverseOrientation;

    protected MachineFXRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void render(T blockEntity, float partialTick, PoseStack matrix, MultiBufferSource renderer,
                          int light, int overlayLight, ProfilerFiller profiler) {
        if (blockEntity.getLevel() == null || !this.shouldRender(blockEntity)) {
            return;
        }
        final IMachineFX<T, P> fx = this.fx();

        this.captureSubLevelFrame(blockEntity);

        matrix.pushPose();
        if (blockEntity instanceof SpatialAnchorBlockEntity anchorLike) {
            final Vec3 matterPos = this.smoothedMatterPos(anchorLike, machineTime(blockEntity, partialTick));
            matrix.translate(matterPos.x, matterPos.y, matterPos.z);
        }
        P capture = fx.capture(blockEntity, partialTick);
        if (capture instanceof IWorldOriented worldOriented && worldOriented.shouldReorient())
            alignToWorldFrame(matrix);
        IMachineFX.draw(fx, capture, matrix, renderer, light, overlayLight);
        matrix.popPose();
    }

    protected abstract IMachineFX<T, P> fx();

    protected boolean shouldRender(T blockEntity) {
        return blockEntity.getActive();
    }

    protected float activityOf(T blockEntity) {
        return 1.0F;
    }

    protected static <T extends TileEntityMekanism>  float machineTime(T blockEntity, float partialTick) {
        return machineSeconds(blockEntity.getLevel().getGameTime() + partialTick);
    }

    public static float machineSeconds(double ticksPlusPartial) {
        return (float) (ticksPlusPartial % TIME_WRAP_TICKS) / 20.0F;
    }

    @Override
    protected String getProfilerSection() {
        return "fluxnautics_physics";
    }

    private Vec3 smoothedMatterPos(SpatialAnchorBlockEntity blockEntity, float time) {
        final MatterSmooth state = this.matterStates.computeIfAbsent(blockEntity, be -> new MatterSmooth());
        final Vec3 target = blockEntity.getMatterPos();

        if (!state.initialized) {
            state.target.set((float) target.x, (float) target.y, (float) target.z);
            state.start.set(state.target);
            state.display.set(state.target);
            state.startTime = time;
            state.initialized = true;
        } else if (this.matterMoved(state.target, target)) {
            state.start.set(state.display);
            state.target.set((float) target.x, (float) target.y, (float) target.z);
            state.startTime = time;
        }

        final float p = Mth.clamp((time - state.startTime) / MATTER_LERP_SECONDS, 0.0F, 1.0F);
        final float e = p * p * (3.0F - 2.0F * p);   // smoothstep: no velocity pop at either end
        state.display.set(
            Mth.lerp(e, state.start.x, state.target.x),
            Mth.lerp(e, state.start.y, state.target.y),
            Mth.lerp(e, state.start.z, state.target.z));
        return new Vec3(state.display.x, state.display.y, state.display.z);
    }

    private boolean matterMoved(Vector3f current, Vec3 target) {
        return Mth.abs(current.x - (float) target.x) > 1.0e-5F
            || Mth.abs(current.y - (float) target.y) > 1.0e-5F
            || Mth.abs(current.z - (float) target.z) > 1.0e-5F;
    }

    private static final class MatterSmooth {
        final Vector3f display = new Vector3f();
        final Vector3f start = new Vector3f();
        final Vector3f target = new Vector3f();
        float startTime;
        boolean initialized;
    }

    private void captureSubLevelFrame(T blockEntity) {
        this.subLevelInverseOrientation = null;
        if (Sable.HELPER == null) {
            return;
        }
        final ClientSubLevel subLevel = Sable.HELPER.getContainingClient(blockEntity);
        if (subLevel != null) {
            this.subLevelInverseOrientation = subLevel.renderPose().orientation().get(new Quaternionf()).invert();
        }
    }

    protected void alignToWorldFrame(PoseStack matrix) {
        if (this.subLevelInverseOrientation == null) {
            return;
        }
        matrix.translate(0.5F, 0.5F, 0.5F);
        matrix.mulPose(this.subLevelInverseOrientation);
        matrix.translate(-0.5F, -0.5F, -0.5F);
    }

    protected void toLocalFrame(Vector3f direction) {
        if (this.subLevelInverseOrientation != null) {
            this.subLevelInverseOrientation.transform(direction);
        }
    }

    protected static int argb(float a, float r, float g, float b) {
        return Color.argbd(a, r, g, b).argb();
    }

    protected static Color ringColor(float a, float r, float g, float b) {
        return Color.argbd(Mth.clamp(a, 0.0F, 1.0F), r, g, b);
    }

    protected static float pulse(float time, float periodSeconds) {
        return 0.5F + 0.5F * Mth.sin(time * Mth.TWO_PI / periodSeconds);
    }

    protected static float easeOut(float p) {
        p = Mth.clamp(p, 0.0F, 1.0F);
        return 1.0F - (1.0F - p) * (1.0F - p);
    }

    public interface IWorldOriented {
        boolean shouldReorient();
    }
}