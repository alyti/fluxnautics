package dev.alyti.fluxnautics.index;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.alyti.fluxnautics.math.VecHelper;
import dev.alyti.fluxnautics.util.Couple;
import dev.alyti.fluxnautics.util.Iterate;
import dev.alyti.fluxnautics.util.MathUtils;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class ThrusterDriverHelper {
    public record HoverCacheData(Double maxDistance, List<Couple<Vec3>> raycastPoints) {
        public static HoverCacheData gatherData(final BlockState state, double endScaling) {
            if (state.hasProperty(BlockStateProperties.FACING)) {
                final List<Couple<Vec3>> rayPoints = new ArrayList<>();

                Direction facing = state.getValue(BlockStateProperties.FACING);
                if (facing.getAxis() == Direction.Axis.Y) {
                    facing = facing.getOpposite();
                }
                final Direction startingDir;

                if (facing.getAxis() == Direction.Axis.Y) {
                    startingDir = Direction.NORTH;
                } else {
                    startingDir = facing.getClockWise();
                }

                final int horizontalSamplePoints = 6;
                final int theta = 360 / horizontalSamplePoints;

                final double startScaling = 0.8;
                for (final boolean diagonal : Iterate.trueAndFalse) {
                    for (int i = 0; i < horizontalSamplePoints; i++) {
                        Vec3 start = Vec3.atLowerCornerOf(startingDir.getNormal()).scale(startScaling).add(0.5, 0.5, 0.5);
                        if (diagonal) {
                            final Direction.Axis axis;
                            final double angle;
                            if (facing.getAxis().isHorizontal()) {
                                axis = Direction.Axis.Y;
                                angle = 45;
                            } else {
                                axis = startingDir.getClockWise().getAxis();
                                angle = facing.getAxisDirection().getStep() * 45;
                            }

                            start = VecHelper.rotateCentered(start, angle, axis);
                        }

                        start = VecHelper.rotateCentered(start, theta * i, facing.getAxis());
                        final Vec3 end = start.add(start.subtract(0.5, 0.5, 0.5).scale(endScaling));
                        rayPoints.add(Couple.create(start, end));
                    }
                }

                final Vec3 start = Vec3.atLowerCornerOf(facing.getNormal()).scale(startScaling).add(0.5, 0.5, 0.5);
                final Vec3 end = start.add(start.subtract(0.5, 0.5, 0.5).scale(endScaling));
                rayPoints.add(Couple.create(start, end));

                return new HoverCacheData(endScaling, rayPoints);
            }

            return null;
        }

        public static Vector3d gatherForceFromRays(final SubLevel parentSublevel, final double timeStep, final Level level, final BlockPos blockStart, final double thrustValue, final List<Couple<Vec3>> rayPoints) {
            final Vector3d force = new Vector3d();

            final Couple<Vec3> firstRay = rayPoints.getFirst();
            final double startEndDistance = firstRay.getSecond().subtract(firstRay.getFirst()).length();
            final Vec3 blockCorner = Vec3.atLowerCornerOf(blockStart);

            for (final Couple<Vec3> rayPoint : rayPoints) {
                final Vec3 start = blockCorner.add(rayPoint.getFirst());
                final Vec3 end = blockCorner.add(rayPoint.getSecond());

                final ClipContext context = new ClipContext(
                        start,
                        end,
                        ClipContext.Block.OUTLINE,
                        ClipContext.Fluid.ANY,
                        CollisionContext.empty()
                );

                final BlockHitResult clip = level.clip(context);
                if (clip.getType() == HitResult.Type.MISS) {
                    continue;
                }

                final ActiveSableCompanion helper = Sable.HELPER;
                final SubLevel hitSublevel = helper.getContaining(level, clip.getBlockPos());
                if (hitSublevel == parentSublevel) {
                    continue;
                }

                final Vec3 hitDiff = helper.projectOutOfSubLevel(level, clip.getLocation())
                        .subtract(helper.projectOutOfSubLevel(level, start));

                final double inverseHitPercentage;
                if (clip.isInside()) {
                    inverseHitPercentage = 1;
                } else {
                    final float curveScaling = 2f; //2 == full power at 1/2 distance, 1 == full power at 0 distance

                    inverseHitPercentage = Math.clamp(curveScaling - ((hitDiff.length() / startEndDistance) * curveScaling), 0, 1);
                }

                final Vec3 modifiedDiff = hitDiff
                        .normalize()
                        .scale(inverseHitPercentage)
                        .scale(1d / rayPoints.size());
                force.add(modifiedDiff.x, modifiedDiff.y, modifiedDiff.z);

                if (hitSublevel instanceof final ServerSubLevel hitServerSubLevel) {
                    final ForceTotal forceTotal = hitServerSubLevel.getOrCreateQueuedForceGroup(ForceGroups.PROPULSION.get()).getForceTotal();
                    final Vector3d impulseLocation = JOMLConversion.toJOML(clip.getLocation());
                    final Vector3d impulse = hitServerSubLevel.logicalPose().transformNormalInverse(JOMLConversion.toJOML(modifiedDiff)).mul(-1).mul(getFanMagnitudeCalculation(parentSublevel, level, thrustValue) * timeStep);
                    forceTotal.applyImpulseAtPoint(hitServerSubLevel.getMassTracker(), impulseLocation, impulse);
                }
            }

            if (force.length() > 1e-8) {
                force.mul(getFanMagnitudeCalculation(parentSublevel, level, thrustValue) * timeStep);
                parentSublevel.logicalPose().transformNormalInverse(force);
            }

            return force;
        }

        private static double getFanMagnitudeCalculation(final SubLevel parentSublevel, final Level level, final double thrustValue) {
            return thrustValue * 5; // magnitude * scale * thrustValue * airPressure;
        }
    }

    public static final class VectorDriverData {
        public static final Codec<VectorDriverData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.optionalFieldOf("x", 0.0D).xmap((v) -> Math.clamp(v, -1.0, 1.0), (v) -> v).forGetter(VectorDriverData::x),
                Codec.DOUBLE.optionalFieldOf("y", 0.0D).xmap((v) -> Math.clamp(v, -1.0, 1.0), (v) -> v).forGetter(VectorDriverData::y),
                Codec.BOOL.optionalFieldOf("auto", false).forGetter(VectorDriverData::auto)
        ).apply(instance, VectorDriverData::new));

        private static final double MAX_TILT = 1.0;

        private Double x;
        private Double y;
        private boolean auto;

        public VectorDriverData(Double x, Double y, boolean auto) {
            this.x = x;
            this.y = y;
            this.auto = auto;
        }

        public static Vector3d autoVectoring(final Direction facing, final BlockPos blockPos, final Level level, final ServerSubLevel subLevel) {
            Vector3d blockNormal = new Vector3d(facing.getStepX(), facing.getStepY(), facing.getStepZ());
            final Vector3d gravityVector = DimensionPhysicsData.getGravity(level, Sable.HELPER.projectOutOfSubLevel(level, JOMLConversion.atCenterOf(blockPos)));

            Vector3d target;
            if (gravityVector.lengthSquared() > 1e-8 && subLevel != null) {
                target = subLevel.logicalPose().orientation().transformInverse(gravityVector.negate().normalize());
            } else {
                target = blockNormal;
            }

            MathUtils.clampIntoCone(target, blockNormal, Math.toRadians(45));
            return target.negate();
        }


        public Vector3d manualVectoring(Direction facing) {
            Vector3d forward = new Vector3d(facing.getStepX(), facing.getStepY(), facing.getStepZ()).normalize();
            Vector3d right = computeRight(forward);
            Vector3d up = new Vector3d(right).cross(forward).normalize();
            double tiltScale = Math.tan(Math.toRadians(45.0f));

            Vector3d exhaust = new Vector3d(forward).negate()
                    .fma(x * tiltScale, right)
                    .fma(y * tiltScale, up);

            if (exhaust.lengthSquared() < 1e-8) {
                exhaust.set(forward).negate();
            } else {
                exhaust.normalize();
            }
            return exhaust;
        }

        private static Vector3d computeRight(Vector3d forward) {
            Vector3d reference = Math.abs(forward.y) > 0.999 ? new Vector3d(0, 0, 1) : new Vector3d(0, 1, 0);
            Vector3d right = new Vector3d(forward).cross(reference);
            if (right.lengthSquared() < 1e-8) return new Vector3d(1, 0, 0);
            right.normalize();
            if (Math.abs(forward.y) > 0.999) right.negate();
            return right;
        }

        public Double x() {
            return x;
        }

        public Double y() {
            return y;
        }

        public boolean auto() {
            return auto;
        }

        public void setX(Double x) {
            this.x = x;
        }

        public void setY(Double y) {
            this.y = y;
        }

        public void setAuto(boolean auto) {
            this.auto = auto;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (VectorDriverData) obj;
            return Objects.equals(this.x, that.x) &&
                    Objects.equals(this.y, that.y) &&
                    this.auto == that.auto;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, auto);
        }

        @Override
        public String toString() {
            return "VectorDriverData[" +
                    "x=" + x + ", " +
                    "y=" + y + ", " +
                    "auto=" + auto + ']';
        }

    }
}
