package dev.alyti.fluxnautics.content.blocks.stabilizer;


import org.joml.Quaterniond;
import org.joml.Vector3d;

/**
 * World-space attitude target for gyro stabilization. Pitch is nose-down positive from level,
 * yaw is heading of local +Z, roll is bank (right wing down positive).
 */
public record GyroTargetAngles(double pitchDeg, double yawDeg, double rollDeg) {
    public static final GyroTargetAngles ZERO = new GyroTargetAngles(0, 0, 0);

    public static GyroTargetAngles fromOrientation(Quaterniond orientation) {
        return measureAttitude(orientation);
    }

    /** Level-flight attitude: pitch is nose-down from horizontal, yaw is horizontal heading of +Z. */
    public static GyroTargetAngles measureAttitude(Quaterniond orientation) {
        Vector3d forward = orientation.transform(new Vector3d(0, 0, 1), new Vector3d()).normalize();
        Vector3d up = orientation.transform(new Vector3d(0, 1, 0), new Vector3d()).normalize();

        double pitchDeg = Math.toDegrees(Math.atan2(-forward.y, Math.hypot(forward.x, forward.z)));
        double yawDeg = Math.toDegrees(Math.atan2(forward.x, forward.z));

        Vector3d flatForward = new Vector3d(forward.x, 0, forward.z);
        if (flatForward.lengthSquared() < 1e-12) {
            flatForward.set(0, 0, 1);
        } else {
            flatForward.normalize();
        }
        Vector3d right = new Vector3d(0, 1, 0).cross(flatForward, new Vector3d()).normalize();
        double rollDeg = Math.toDegrees(Math.atan2(up.dot(right), up.y));

        return new GyroTargetAngles(pitchDeg, yawDeg, rollDeg);
    }

    /** Signed axis errors in radians: x=pitch, y=yaw, z=roll. */
    public Vector3d errorRadians(GyroTargetAngles current) {
        return new Vector3d(
            Math.toRadians(pitchDeg - current.pitchDeg),
            Math.toRadians(wrapDegrees(yawDeg - current.yawDeg)),
            Math.toRadians(rollDeg - current.rollDeg)
        );
    }

    public double totalErrorRad(GyroTargetAngles current, boolean pitch, boolean yaw, boolean roll) {
        Vector3d err = errorRadians(current);
        double sum = 0;
        if (pitch) {
            sum += err.x * err.x;
        }
        if (yaw) {
            sum += err.y * err.y;
        }
        if (roll) {
            sum += err.z * err.z;
        }
        return Math.sqrt(sum);
    }

    private static double wrapDegrees(double degrees) {
        double wrapped = degrees % 360.0;
        if (wrapped > 180.0) {
            wrapped -= 360.0;
        } else if (wrapped < -180.0) {
            wrapped += 360.0;
        }
        return wrapped;
    }

    public static int pack(int pitchDeg, int yawDeg, int rollDeg) {
        int p = Math.clamp(pitchDeg + 90, 0, 180);
        int y = Math.floorMod(yawDeg + 180, 360);
        int r = Math.clamp(rollDeg + 90, 0, 180);
        return p | (y << 9) | (r << 18);
    }

    public static GyroTargetAngles unpack(int packed) {
        int p = (packed & 0x1FF) - 90;
        int y = ((packed >> 9) & 0x1FF) - 180;
        int r = ((packed >> 18) & 0x1FF) - 90;
        return new GyroTargetAngles(p, y, r);
    }
}