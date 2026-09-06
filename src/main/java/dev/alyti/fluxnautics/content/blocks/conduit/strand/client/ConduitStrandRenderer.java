package dev.alyti.fluxnautics.content.blocks.conduit.strand.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.alyti.fluxnautics.client.render.SFRenderTypes;
import dev.alyti.fluxnautics.content.blocks.conduit.AbstractConduitHolderBlockEntity;
import dev.alyti.fluxnautics.content.blocks.conduit.TileComponentConduitStrandHolder;
import dev.alyti.fluxnautics.registry.FNModelCache;
import dev.alyti.fluxnautics.util.MathUtils;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.List;

public class ConduitStrandRenderer {
    public record ConduitRenderPoint(Quaternionf orientation, Vector3d position) { }

    public static void render(final AbstractConduitHolderBlockEntity be, final TileComponentConduitStrandHolder<?> conduitHolder, final float partialTick, final PoseStack ps, final MultiBufferSource buffer) {

        final Level level = be.getLevel();
        assert level != null;

        final BlockPos ownerPos = be.getBlockPos();

        final List<BakedQuad> middle = FNModelCache.INSTANCE.CONDUIT_WIRE.getQuads(level.getRandom());
        final List<BakedQuad> knot = FNModelCache.INSTANCE.CONDUIT_NODE.getQuads(level.getRandom());

//        final SuperByteBuffer middle = CachedBuffers.partialFacing(SimPartialModels.ROPE, AllBlocks.ROPE.getDefaultState(), Direction.NORTH);
//        final SuperByteBuffer knot = CachedBuffers.partialFacing(SimPartialModels.ROPE_KNOT, AllBlocks.ROPE.getDefaultState(), Direction.NORTH);
        final VertexConsumer vb = buffer.getBuffer(SFRenderTypes.LIGHTNING);

        final SubLevel subLevel = Sable.HELPER.getContaining(be);
        Pose3dc containingPose = null;

        if (subLevel instanceof final ClientSubLevel clientSubLevel) {
            containingPose = clientSubLevel.renderPose();
        }

        final ClientConduitStrand rope = conduitHolder.getClientStrand();

        final float rad = 3.0f / 16.0f;

        if (conduitHolder.ownsConduit() && rope != null && conduitHolder.getClientStrand() != null) {
            final ClientConduitStrand clientStrand = conduitHolder.getClientStrand();

            final List<ClientConduitPoint> points = clientStrand.getPoints();

            if (points.size() <= 1) {
                return;
            }

            final ObjectArrayList<ConduitRenderPoint> ropeRenderPoints = buildRenderPoints(partialTick, points);

            if (ropeRenderPoints.isEmpty()) return;

            ps.pushPose();
            for (int i = 1; i < ropeRenderPoints.size(); i++) {
                final ConduitRenderPoint renderPoint0 = ropeRenderPoints.get(i - 1);
                final ConduitRenderPoint renderPoint1 = ropeRenderPoints.get(i);
                final Vector3d globalRenderPos = new Vector3d(renderPoint0.position());
                final Vector3d renderPos = renderPoint0.position();
                final Quaternionf orientation = renderPoint0.orientation();

                final double length = renderPoint1.position().distance(renderPoint0.position());

                if (containingPose != null) {
                    containingPose.transformPositionInverse(renderPos);
                    orientation.premul(new Quaternionf(containingPose.orientation()).conjugate());
                }

                ps.pushPose();
                ps.translate(renderPos.x - (ownerPos.getX()), renderPos.y - (ownerPos.getY()), renderPos.z - (ownerPos.getZ()));
                ps.mulPose(orientation);
                ps.translate(-0.5, -0.5, -0.5);
                final BlockPos pos = BlockPos.containing(globalRenderPos.x, globalRenderPos.y, globalRenderPos.z);
                final int worldLight = LevelRenderer.getLightColor(level, pos);

//                if (i > 1) {
//                    for (BakedQuad quad : knot) {
//                        vb.putBulkData(ps.last(), quad, 1f, 1f, 1f, 1f, 15728880, worldLight, true);
//                    }
//                }
                ps.translate(0.0, 0.5, 0.0);
                ps.scale(1.0f, (float) length, 1.0f);

                for (BakedQuad quad : middle) {
                    vb.putBulkData(ps.last(), quad, .1f, 1f, 1f, 0.33f, 15728880, worldLight, true);
                }
                ps.popPose();
            }
            ps.popPose();

            final ConduitRenderPoint last = ropeRenderPoints.getLast();
            if (containingPose != null) {
                final Vector3d renderPos = last.position();
                final Quaternionf orientation = last.orientation();

                containingPose.transformPositionInverse(renderPos);
                orientation.premul(new Quaternionf(containingPose.orientation()).conjugate());
            }

//            if (Objects.equals(ZiplineClientManager.hoveringRope, clientStrand.getUuid())) {
//                renderOutline(ps, buffer, rad, ropeRenderPoints, ownerPos);
//            }
        }
    }

    private static void renderOutline(final PoseStack ps, final MultiBufferSource buffer, final float rad, final ObjectArrayList<ConduitRenderPoint> ropeRenderPoints, final BlockPos ownerPos) {
        final Vector3d previousCorner = new Vector3d();
        final Vector3d currentCorner = new Vector3d();
        final Vector3d cornerDiff = new Vector3d();

        final Vector3d[] ropeCorners = {
            new Vector3d(-rad, 0, -rad),
            new Vector3d(-rad, 0, rad),
            new Vector3d(rad, 0, rad),
            new Vector3d(rad, 0, -rad),
        };

        final VertexConsumer linesVB = buffer.getBuffer(RenderType.lines());
        final Matrix4f pose = ps.last().pose();

        for (int i = 0; i < ropeRenderPoints.size() + 1; i++) {
            final ConduitRenderPoint renderPoint0 = ropeRenderPoints.get(Math.max(0, i - 1));
            final ConduitRenderPoint renderPoint1 = ropeRenderPoints.get(Math.min(ropeRenderPoints.size() - 1, i));
            final boolean start = i == 0;
            final boolean end = i == ropeRenderPoints.size();

            for (final Vector3d ropeCorner : ropeCorners) {
                renderPoint0.orientation().transform(start ? ropeCorner.rotateY(Math.PI / 2.0, previousCorner) : ropeCorner, previousCorner)
                    .add(renderPoint0.position())
                    .sub(ownerPos.getX(), ownerPos.getY(), ownerPos.getZ());

                renderPoint1.orientation().transform(end ? ropeCorner.rotateY(Math.PI / 2.0, ropeCorner) : ropeCorner, currentCorner)
                    .add(renderPoint1.position())
                    .sub(ownerPos.getX(), ownerPos.getY(), ownerPos.getZ());

                currentCorner.sub(previousCorner, cornerDiff).normalize();

                linesVB.addVertex(pose, (float) previousCorner.x, (float) previousCorner.y, (float) previousCorner.z)
                    .setColor(0f, 0f, 0f, .4f)
                    .setNormal(ps.last(), (float) cornerDiff.x, (float) cornerDiff.y, (float) cornerDiff.z);

                linesVB.addVertex(pose, (float) currentCorner.x, (float) currentCorner.y, (float) currentCorner.z)
                    .setColor(0f, 0f, 0f, .4f)
                    .setNormal(ps.last(), (float) cornerDiff.x, (float) cornerDiff.y, (float) cornerDiff.z);
            }
        }
    }

    private static @NotNull ObjectArrayList<ConduitRenderPoint> buildRenderPoints(final float partialTick, final List<ClientConduitPoint> inputPoints) {
        final ObjectArrayList<ConduitRenderPoint> ropeRenderPoints = new ObjectArrayList<>();
        final ObjectArrayList<ClientConduitPoint> points = new ObjectArrayList<>(inputPoints);

        while (points.size() >= 2 && points.getFirst().position().distanceSquared(points.get(1).position()) < 1e-3) {
            points.removeFirst();
        }

        if (points.size() <= 1) {
            return new ObjectArrayList<>();
        }

        final Vector3dc pointZeroPosition = points.get(0).renderPos(partialTick, new Vector3d());
        final Vector3dc pointOnePosition = points.get(1).renderPos(partialTick, new Vector3d());

        final Vector3d normal = pointOnePosition.sub(pointZeroPosition, new Vector3d()).normalize();

        final Quaternionf runningRotation;
        if (normal.dot(OrientedBoundingBox3d.UP) < 0) {
            runningRotation = MathUtils.getQuaternionfFromVectorRotation(new Vector3d(0, -1, 0), normal);
            runningRotation.rotateZ((float) Math.PI);
        } else {
            runningRotation = MathUtils.getQuaternionfFromVectorRotation(new Vector3d(0, 1, 0), normal);
        }

        ropeRenderPoints.add(new ConduitRenderPoint(new Quaternionf(runningRotation), new Vector3d(pointZeroPosition)));

        final Vector3d runningNormal = new Vector3d();

        final Vector3d bPos = new Vector3d();
        final Vector3d aPos = new Vector3d();

        for (int i = 2; i < points.size(); i++) {
            final ClientConduitPoint pointA = points.get(i - 1);
            final ClientConduitPoint pointB = points.get(i);

            runningNormal.set(pointB.renderPos(partialTick, bPos))
                .sub(pointA.renderPos(partialTick, aPos))
                .normalize();

            if (runningNormal.dot(OrientedBoundingBox3d.UP) < -0.15) {
                runningRotation.set(MathUtils.getQuaternionfFromVectorRotation(new Vector3d(0, -1, 0), runningNormal));
                runningRotation.rotateZ((float) Math.PI);
            } else {
                runningRotation.set(MathUtils.getQuaternionfFromVectorRotation(new Vector3d(0, 1, 0), runningNormal));
            }

            ropeRenderPoints.add(new ConduitRenderPoint(new Quaternionf(runningRotation), pointA.renderPos(partialTick, new Vector3d())));
            normal.set(runningNormal);
        }

        ropeRenderPoints.add(new ConduitRenderPoint(new Quaternionf(runningRotation), points.getLast().renderPos(partialTick, new Vector3d())));
        return ropeRenderPoints;
    }
}