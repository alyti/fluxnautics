package dev.alyti.fluxnautics.content.blocks.stabilizer;

import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import static dev.alyti.fluxnautics.content.blocks.stabilizer.StabilizerBlockEntity.MAX_DAMPING_PERCENT;

public final class GyroPrecisionController {
    public static final double PRECISION_ZONE_RAD = Math.toRadians(1.0);
    public static final double PRECISION_HOLD_RAD = Math.toRadians(0.001);
    private static final int STUCK_TICKS = 5;
    private static final double FORCE_BOOST = 1.18;
    private static final double MAX_FORCE_MULT = 10.0;
    private static final double MIN_PROGRESS_RAD = Math.toRadians(0.002);

    private double forceMultiplier = 1.0;
    private int stuckTicks;
    private double lastAngle = Double.NaN;

    public record PrecisionState(
        boolean active,
        double dampingPercent,
        double forceMultiplier
    ) {
        public static PrecisionState inactive() {
            return new PrecisionState(false, -1.0, 1.0);
        }
    }

    public PrecisionState update(double angleRad) {
        if (angleRad < PRECISION_HOLD_RAD) {
            forceMultiplier = 1.0;
            stuckTicks = 0;
            lastAngle = angleRad;
            return new PrecisionState(true, MAX_DAMPING_PERCENT, 1.0);
        }
        if (angleRad > PRECISION_ZONE_RAD) {
            reset();
            return new PrecisionState(false, -1.0, 1.0);
        }

        if (!Double.isNaN(lastAngle)) {
            double progress = lastAngle - angleRad;
            if (progress >= MIN_PROGRESS_RAD) {
                // Making progress, reset stuck ticks and decay the force multiplier
                stuckTicks = 0;
                forceMultiplier = Math.max(1.0, forceMultiplier * 0.8);
            } else {
                stuckTicks++;
            }

            if (stuckTicks >= STUCK_TICKS) {
                // Scale the maximum allowed boost by how far we are from the target.
                // This prevents massive 10x impulses when the ship is basically already at the target.
                double errorFactor = Math.clamp((angleRad - PRECISION_HOLD_RAD) / (PRECISION_ZONE_RAD - PRECISION_HOLD_RAD), 0.0, 1.0);
                double maxMult = 1.0 + errorFactor * (MAX_FORCE_MULT - 1.0);
                forceMultiplier = Math.min(maxMult, forceMultiplier * FORCE_BOOST);
                stuckTicks = 0;
            }
        }
        lastAngle = angleRad;

        return new PrecisionState(true, MAX_DAMPING_PERCENT, forceMultiplier);
    }

    public void reset() {
        forceMultiplier = 1.0;
        stuckTicks = 0;
        lastAngle = Double.NaN;
    }

    public static Vector3d scaleCorrectionTorque(
        Vector3d localCorrection,
        double angleRad,
        double angleScale,
        double maxTorque,
        boolean stabilizePitch,
        boolean stabilizeYaw,
        boolean stabilizeRoll,
        double kp,
        double forceMultiplier
    ) {
        Vector3d torque = new Vector3d();
        boolean holdBand = angleRad <= PRECISION_HOLD_RAD;
        double holdScale = holdBand ? Math.max(angleScale, angleRad / PRECISION_HOLD_RAD) : angleScale;

        if (stabilizePitch) {
            torque.x = correctionComponent(localCorrection.x, holdScale, kp) * maxTorque * forceMultiplier;
        }
        if (stabilizeYaw) {
            torque.y = correctionComponent(localCorrection.y, holdScale, kp) * maxTorque * forceMultiplier;
        }
        if (stabilizeRoll) {
            torque.z = correctionComponent(localCorrection.z, holdScale, kp) * maxTorque * forceMultiplier;
        }
        return torque;
    }

    private static double correctionComponent(double correction, double angleScale, double kp) {
        return Math.clamp(correction * kp * angleScale, -1.0, 1.0);
    }

    private static final double STOP_ROTATE_ANGLE_RAD = Math.toRadians(15);
    private static final double NAV_KP = 2.4;
    private static final double NAV_PRECISION_KP = 6.0;
    private static final double CORRECTION_RAMP = 1e-4;

    public static GyroNavTorque computeTowardOrientation(
        Vector3d rotBody,
        Vector3d localAngVel,
        boolean stabilizePitch,
        boolean stabilizeYaw,
        boolean stabilizeRoll,
        double acceptAngleDeg
    ) {
        double totalErr = rotBody.length();

        boolean blockTranslation = totalErr > STOP_ROTATE_ANGLE_RAD;
        if (totalErr < 1e-8) {
            return new GyroNavTorque(new Vector3d(), blockTranslation, totalErr);
        }

        double acceptRad = Math.toRadians(Math.max(0.0, acceptAngleDeg));

        // Compute base angle scale
        double angleScale;
        if (totalErr <= acceptRad) {
            angleScale = 0.0;
        } else {
            angleScale = Math.clamp((totalErr - acceptRad) / CORRECTION_RAMP, 0.0, 1.0);
        }

        // Ensure minimum response near zero to prevent deadzone drift ONLY if acceptRad is effectively 0
        if (acceptRad < 1e-6) {
            double zoneScale = Math.clamp(totalErr / PRECISION_ZONE_RAD, 0.05, 1.0);
            angleScale = Math.max(angleScale, zoneScale);
        }

        // Smoothly interpolate kp and damp to prevent discontinuities at PRECISION_ZONE_RAD
        double zoneFactor = Math.clamp(totalErr / PRECISION_ZONE_RAD, 0.0, 1.0);
        double kp = NAV_PRECISION_KP + (NAV_KP - NAV_PRECISION_KP) * zoneFactor;
        double damp = 1.2 + (0.5 - 1.2) * zoneFactor;

        // Boost kp near zero to hold position tightly, but keep it continuous
        if (totalErr < PRECISION_HOLD_RAD) {
            double holdFactor = PRECISION_HOLD_RAD / Math.max(totalErr, 1e-6);
            kp = Math.max(kp, kp * holdFactor);
        }

        Vector3d torque = new Vector3d();
        if (stabilizePitch) {
            torque.x = Math.clamp(rotBody.x * kp * angleScale, -Math.PI, Math.PI);
            torque.x -= localAngVel.x * damp;
        }
        if (stabilizeYaw) {
            torque.y = Math.clamp(rotBody.y * kp * angleScale, -Math.PI, Math.PI);
            torque.y -= localAngVel.y * damp;
        }
        if (stabilizeRoll) {
            torque.z = Math.clamp(rotBody.z * kp * angleScale, -Math.PI, Math.PI);
            torque.z -= localAngVel.z * damp;
        }

        return new GyroNavTorque(torque, blockTranslation, totalErr);
    }

    public static Vector3dc getBodyRotationError(Quaterniond currentOrientation, Quaterniond qTarget, Vector3d dest) {
        Quaterniond qDelta = currentOrientation.conjugate(new Quaterniond()).mul(qTarget);
        qDelta.normalize();
        if (qDelta.w < 0.0) {
            qDelta.set(-qDelta.x, -qDelta.y, -qDelta.z, -qDelta.w);
        }

        return rotationVectorBody(qDelta, dest);
    }

    public static Quaterniond orientationFromLevelAttitude(GyroTargetAngles angles) {
        double yawRad = Math.toRadians(angles.yawDeg());
        double pitchRad = Math.toRadians(angles.pitchDeg());
        double rollRad = Math.toRadians(angles.rollDeg());
        return new Quaterniond()
            .rotateY(yawRad)
            .rotateX(pitchRad)
            .rotateZ(rollRad);
    }

    public static GyroNavTorque compute(
        Quaterniond currentOrientation,
        GyroTargetAngles targetAngles,
        Vector3d localAngVel,
        boolean stabilizePitch,
        boolean stabilizeYaw,
        boolean stabilizeRoll,
        double acceptAngleDeg
    ) {
        Quaterniond qTarget = orientationFromLevelAttitude(targetAngles);
        return computeTowardOrientation(
            (Vector3d) getBodyRotationError(currentOrientation, qTarget, new Vector3d()),
            localAngVel,
            stabilizePitch,
            stabilizeYaw,
            stabilizeRoll,
            acceptAngleDeg
        );
    }

    public static Vector3dc rotationVectorBody(Quaterniond qDelta, Vector3d dest) {
        double w = Math.clamp(qDelta.w, -1.0, 1.0);
        Vector3d axis = dest.set(qDelta.x, qDelta.y, qDelta.z);
        if (w < 0.0) {
            w = -w;
            axis.negate();
        }
        double sinHalf = axis.length();
        if (sinHalf < 1e-8) {
            return new Vector3d();
        }
        double angle = 2.0 * Math.atan2(sinHalf, w);
        return axis.div(sinHalf).mul(angle);
    }

    public static Quaterniond rotationBetween(Vector3d from, Vector3d to) {
        Vector3d cross = from.cross(to, new Vector3d());
        double dot = Math.clamp(from.dot(to), -1.0, 1.0);
        Quaterniond q = new Quaterniond(cross.x, cross.y, cross.z, 1.0 + dot);
        if (q.lengthSquared() < 1e-12) {
            if (dot < 0.0) {
                Vector3d ortho = Math.abs(from.x) < 0.9
                    ? new Vector3d(1, 0, 0).cross(from, new Vector3d())
                    : new Vector3d(0, 1, 0).cross(from, new Vector3d());
                ortho.normalize();
                return new Quaterniond().rotateAxis(Math.PI, ortho);
            }
            return new Quaterniond();
        }
        q.normalize();
        return q;
    }

    public record GyroNavTorque(Vector3d localTorque, boolean blockTranslation, double headingErrorRad) {}
}