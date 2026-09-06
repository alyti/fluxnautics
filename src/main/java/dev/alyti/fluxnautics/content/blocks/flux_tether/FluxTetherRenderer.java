// dev/alyti/fluxnautics/content/blocks/quantum_lock/QuantumLockRenderer.java
package dev.alyti.fluxnautics.content.blocks.flux_tether;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.alyti.fluxnautics.content.blocks.flux_tether.link_block.FluxTetherLinkBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import mekanism.client.render.lib.effect.BoltRenderer;
import mekanism.client.render.tileentity.MekanismTileEntityRenderer;
import mekanism.common.lib.Color;
import mekanism.common.lib.effect.BoltEffect;
import mekanism.common.util.WorldUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FluxTetherRenderer extends MekanismTileEntityRenderer<FluxTetherBlockEntity> implements BlockEntityRenderer<FluxTetherBlockEntity> {

    private static final Map<BlockPos, BoltRenderer> boltRendererMap = new HashMap<>();

    public FluxTetherRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void render(FluxTetherBlockEntity tile, float partialTick, PoseStack matrix, MultiBufferSource renderer, int light, int overlayLight, ProfilerFiller profiler) {
        if (tile.localPlatePos == null)
            return;
        BoltRenderer bolts = boltRendererMap.computeIfAbsent(tile.getBlockPos(), mb -> new BoltRenderer());

        final Minecraft minecraft = Minecraft.getInstance();
        final ClientSubLevelContainer container = SubLevelContainer.getContainer(minecraft.level);
        assert container != null;

        final BlockState state = tile.getBlockState();
        final UUID otherSubLevelID = tile.getSubLevelID();
        final ClientSubLevel otherSubLevel = otherSubLevelID != null ? (ClientSubLevel) container.getSubLevel(otherSubLevelID) : null;
        final ClientSubLevel subLevel = Sable.HELPER.getContainingClient(tile);

        final BlockPos blockPos = tile.getBlockPos();

        final FluxTetherLinkBlockEntity other = WorldUtils.getTileEntity(FluxTetherLinkBlockEntity.class, tile.getLevel(), tile.localPlatePos);
        if (other == null) {
            return;
        }

        final Vector3d center = tile.getCenter();
        final Vector3d otherCenter = other.getCenter();

        final Direction facing = state.getValue(BlockStateProperties.FACING);
        final Direction otherFacing = other.getBlockState().getValue(BlockStateProperties.FACING);
        final Vector3dc normalA = JOMLConversion.atLowerCornerOf(facing.getNormal());
        final Vector3d normalB = JOMLConversion.atLowerCornerOf(otherFacing.getNormal());

        final Pose3dc renderPose = subLevel != null ? subLevel.renderPose() : null;
        final Pose3dc otherRenderPose = otherSubLevel != null ? otherSubLevel.renderPose() : null;

        if (otherRenderPose != null) {
            otherRenderPose.transformNormal(normalB);
            otherRenderPose.transformPosition(otherCenter);
        }

        if (renderPose != null) {
            renderPose.transformNormalInverse(normalB);
            renderPose.transformPositionInverse(otherCenter);
        }

        matrix.translate(center.x() - blockPos.getX(), center.y() - blockPos.getY(), center.z() - blockPos.getZ());

        final Vec3 rawStart = new Vec3(normalA.x() * 0.5, normalA.y() * 0.5, normalA.z() * 0.5);
        final Vec3 rawEnd = new Vec3(
            (otherCenter.x() - center.x()) + normalB.x() * 0.5,
            (otherCenter.y() - center.y()) + normalB.y() * 0.5,
            (otherCenter.z() - center.z()) + normalB.z() * 0.5
        );

        final double realDistance = Math.sqrt(
            Sable.HELPER.distanceSquaredWithSubLevels(tile.getLevel(), center, otherCenter));

        final long gameTime = tile.getLevel().getGameTime();
        final float pulse = 1.0f + 0.15f * (float) Math.sin((gameTime + partialTick) * 0.4);

        final float baseSize = (float) (0.10 + 0.04 * Math.exp(-realDistance * 0.04));
        final float boltSize = baseSize * pulse;

        final int length = (int) Mth.clamp(2 + Math.sqrt(realDistance) * 1.5, 3, 24);
        final float spread = (float) (0.04 / (1.0 + realDistance * 0.08));

        if (isTickingNormally(tile)) {
            Vec3[] forward = computeCenteredEndpoints(rawStart, rawEnd, boltSize);
            bolts.update(0,
                getBoltFromData(forward[0], forward[1], length, boltSize, spread), partialTick);
            Vec3[] reverse = computeCenteredEndpoints(rawEnd, rawStart, boltSize);
            bolts.update(1,
                getBoltFromData(reverse[0], reverse[1], length, boltSize, spread), partialTick);
        }

        bolts.render(partialTick, matrix, renderer);
    }

    private static Vec3[] computeCenteredEndpoints(Vec3 rawStart, Vec3 rawEnd, float boltSize) {
        final Vec3 diff = rawEnd.subtract(rawStart);
        final Vec3 rightAdd = diff.cross(new Vec3(0.5, 0.5, 0.5)).normalize().scale(boltSize);
        final Vec3 backAdd = diff.cross(rightAdd).normalize().scale(boltSize);
        final Vec3 shift = new Vec3(
            -(rightAdd.x() * 0.5 + backAdd.x() * (1.0 / 3.0)),
            -(rightAdd.y() * 0.5 + backAdd.y() * (1.0 / 3.0)),
            -(rightAdd.z() * 0.5 + backAdd.z() * (1.0 / 3.0))
        );
        return new Vec3[] { rawStart.add(shift), rawEnd.add(shift) };
    }

    private static BoltEffect getBoltFromData(Vec3 start, Vec3 end, int length, float size, float spread) {
        return new BoltEffect(
            new BoltEffect.BoltRenderInfo()
                .color(Color.rgbad(0.54F, 0.91F, 1F, 0.85F))
                .noise(0.1F, spread)
                .branching(0.05F, 0.3F)
                .spreader(BoltEffect.SegmentSpreader.memory(0.92F)),
            start, end, length)
            .count(1)
            .size(size)
            .lifespan(3)
            .spawn(BoltEffect.SpawnFunction.NO_DELAY)
            .fade(BoltEffect.FadeFunction.fade(0.4f));
    }

    @Override
    protected String getProfilerSection() {
        return "quantum_lock";
    }

    @Override
    public boolean shouldRender(FluxTetherBlockEntity blockEntity, Vec3 cameraPos) {
        return true;
    }

    public boolean shouldRenderOffScreen(@NonNull FluxTetherBlockEntity blockEntity) {
        return true;
    }

    @Override
    public @NonNull AABB getRenderBoundingBox(FluxTetherBlockEntity tile) {
        if (tile.isAssembled()) {
            return AABB.INFINITE;
        }
        return super.getRenderBoundingBox(tile);
    }

    @Override
    public int getViewDistance() {
        return 512;
    }
}
