package dev.alyti.fluxnautics.content.blocks.stabilizer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;
import java.util.Optional;

public class StabilizerConfig {
    private static final Codec<StabilizerBlockEntity.GyroMode> GYRO_MODE_CODEC = Codec.STRING.xmap(
        StabilizerBlockEntity.GyroMode::valueOf,
        StabilizerBlockEntity.GyroMode::name
    );

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        StabilizerConfig that = (StabilizerConfig) o;
        return autoStabilize == that.autoStabilize &&
            Double.compare(forcePercent, that.forcePercent) == 0 &&
            Double.compare(dampingPercent, that.dampingPercent) == 0 &&
            Double.compare(acceptAngleDeg, that.acceptAngleDeg) == 0 &&
            stabilizePitch == that.stabilizePitch &&
            stabilizeYaw == that.stabilizeYaw &&
            stabilizeRoll == that.stabilizeRoll &&
            bidirectionalTorque == that.bidirectionalTorque &&
            Double.compare(targetPitchDeg, that.targetPitchDeg) == 0 &&
            Double.compare(targetYawDeg, that.targetYawDeg) == 0 &&
            Double.compare(targetRollDeg, that.targetRollDeg) == 0 &&
            mode == that.mode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, autoStabilize, forcePercent, dampingPercent, acceptAngleDeg,
            stabilizePitch, stabilizeYaw, stabilizeRoll, bidirectionalTorque,
            targetPitchDeg, targetYawDeg, targetRollDeg);
    }

    public static final Codec<StabilizerConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        GYRO_MODE_CODEC.optionalFieldOf("mode", StabilizerBlockEntity.GyroMode.AUTO).forGetter(StabilizerConfig::mode),
        Codec.BOOL.optionalFieldOf("auto_stabilize", true).forGetter(StabilizerConfig::autoStabilize),
        Codec.DOUBLE.optionalFieldOf("force_percent", 0.5).forGetter(StabilizerConfig::forcePercent),
        Codec.DOUBLE.optionalFieldOf("damping_percent", 0.25).forGetter(StabilizerConfig::dampingPercent),
        Codec.DOUBLE.optionalFieldOf("accept_angle_deg", 0.0).forGetter(StabilizerConfig::acceptAngleDeg),
        Codec.BOOL.optionalFieldOf("stabilize_pitch", true).forGetter(StabilizerConfig::stabilizePitch),
        Codec.BOOL.optionalFieldOf("stabilize_yaw", true).forGetter(StabilizerConfig::stabilizeYaw),
        Codec.BOOL.optionalFieldOf("stabilize_roll", true).forGetter(StabilizerConfig::stabilizeRoll),
        Codec.BOOL.optionalFieldOf("bidirectional_torque", true).forGetter(StabilizerConfig::bidirectionalTorque),
        Codec.DOUBLE.optionalFieldOf("target_pitch_deg", 0.0).forGetter(StabilizerConfig::targetPitchDeg),
        Codec.DOUBLE.optionalFieldOf("target_yaw_deg", 0.0).forGetter(StabilizerConfig::targetYawDeg),
        Codec.DOUBLE.optionalFieldOf("target_roll_deg", 0.0).forGetter(StabilizerConfig::targetRollDeg)
    ).apply(instance, StabilizerConfig::new));

    public static final StreamCodec<ByteBuf, StabilizerConfig> STREAM_CODEC = StreamCodec.of(
        (buf, config) -> {
            ByteBufCodecs.STRING_UTF8.encode(buf, config.mode().name());
            ByteBufCodecs.BOOL.encode(buf, config.autoStabilize());
            ByteBufCodecs.DOUBLE.encode(buf, config.forcePercent());
            ByteBufCodecs.DOUBLE.encode(buf, config.dampingPercent());
            ByteBufCodecs.DOUBLE.encode(buf, config.acceptAngleDeg());
            ByteBufCodecs.BOOL.encode(buf, config.stabilizePitch());
            ByteBufCodecs.BOOL.encode(buf, config.stabilizeYaw());
            ByteBufCodecs.BOOL.encode(buf, config.stabilizeRoll());
            ByteBufCodecs.BOOL.encode(buf, config.bidirectionalTorque());
            ByteBufCodecs.DOUBLE.encode(buf, config.targetPitchDeg());
            ByteBufCodecs.DOUBLE.encode(buf, config.targetYawDeg());
            ByteBufCodecs.DOUBLE.encode(buf, config.targetRollDeg());
        },
        (buf) -> {
            StabilizerBlockEntity.GyroMode mode = StabilizerBlockEntity.GyroMode.valueOf(ByteBufCodecs.STRING_UTF8.decode(buf));
            boolean autoStabilize = ByteBufCodecs.BOOL.decode(buf);
            double forcePercent = ByteBufCodecs.DOUBLE.decode(buf);
            double dampingPercent = ByteBufCodecs.DOUBLE.decode(buf);
            double acceptAngleDeg = ByteBufCodecs.DOUBLE.decode(buf);
            boolean stabilizePitch = ByteBufCodecs.BOOL.decode(buf);
            boolean stabilizeYaw = ByteBufCodecs.BOOL.decode(buf);
            boolean stabilizeRoll = ByteBufCodecs.BOOL.decode(buf);
            boolean bidirectionalTorque = ByteBufCodecs.BOOL.decode(buf);
            double targetPitchDeg = ByteBufCodecs.DOUBLE.decode(buf);
            double targetYawDeg = ByteBufCodecs.DOUBLE.decode(buf);
            double targetRollDeg = ByteBufCodecs.DOUBLE.decode(buf);

            return new StabilizerConfig(mode, autoStabilize, forcePercent, dampingPercent, acceptAngleDeg,
                stabilizePitch, stabilizeYaw, stabilizeRoll, bidirectionalTorque,
                targetPitchDeg, targetYawDeg, targetRollDeg);
        }
    );

    private StabilizerBlockEntity.GyroMode mode;
    private boolean autoStabilize;
    private double forcePercent;
    private double dampingPercent;
    private double acceptAngleDeg;
    private boolean stabilizePitch;
    private boolean stabilizeYaw;
    private boolean stabilizeRoll;
    private boolean bidirectionalTorque;
    private double targetPitchDeg;
    private double targetYawDeg;
    private double targetRollDeg;

    public StabilizerConfig(StabilizerBlockEntity.GyroMode mode, boolean autoStabilize, double forcePercent, double dampingPercent,
                            double acceptAngleDeg, boolean stabilizePitch, boolean stabilizeYaw, boolean stabilizeRoll,
                            boolean bidirectionalTorque, double targetPitchDeg, double targetYawDeg,
                            double targetRollDeg) {
        this.mode = mode;
        this.autoStabilize = autoStabilize;
        this.forcePercent = Math.clamp(forcePercent, 0.0, 2.0);
        this.dampingPercent = Math.clamp(dampingPercent, 0.0, 0.99);
        this.acceptAngleDeg = Math.clamp(acceptAngleDeg, 0.0, 45.0);
        this.stabilizePitch = stabilizePitch;
        this.stabilizeYaw = stabilizeYaw;
        this.stabilizeRoll = stabilizeRoll;
        this.bidirectionalTorque = bidirectionalTorque;
        this.targetPitchDeg = Math.clamp(targetPitchDeg, -90.0, 90.0);
        this.targetYawDeg = wrapYaw(targetYawDeg);
        this.targetRollDeg = Math.clamp(targetRollDeg, -90.0, 90.0);
    }

    public static StabilizerConfig makeDefault() {
        return new StabilizerConfig(StabilizerBlockEntity.GyroMode.AUTO, true, 0.5, 0.25, 0.0, true, true, true, true, 0.0, 0.0, 0.0);
    }

    public StabilizerBlockEntity.GyroMode mode() { return mode; }
    public boolean autoStabilize() { return autoStabilize; }
    public double forcePercent() { return forcePercent; }
    public double dampingPercent() { return dampingPercent; }
    public double acceptAngleDeg() { return acceptAngleDeg; }
    public boolean stabilizePitch() { return stabilizePitch; }
    public boolean stabilizeYaw() { return stabilizeYaw; }
    public boolean stabilizeRoll() { return stabilizeRoll; }
    public boolean bidirectionalTorque() { return bidirectionalTorque; }
    public double targetPitchDeg() { return targetPitchDeg; }
    public double targetYawDeg() { return targetYawDeg; }
    public double targetRollDeg() { return targetRollDeg; }

    public void setMode(StabilizerBlockEntity.GyroMode mode) { this.mode = mode; }
    public void setAutoStabilize(boolean autoStabilize) { this.autoStabilize = autoStabilize; }
    public void setForcePercent(double forcePercent) { this.forcePercent = Math.clamp(forcePercent, 0.0, 2.0); }
    public void setDampingPercent(double dampingPercent) { this.dampingPercent = Math.clamp(dampingPercent, 0.0, 0.99); }
    public void setAcceptAngleDeg(double acceptAngleDeg) { this.acceptAngleDeg = Math.clamp(acceptAngleDeg, 0.0, 45.0); }
    public void setStabilizePitch(boolean stabilizePitch) { this.stabilizePitch = stabilizePitch; }
    public void setStabilizeYaw(boolean stabilizeYaw) { this.stabilizeYaw = stabilizeYaw; }
    public void setStabilizeRoll(boolean stabilizeRoll) { this.stabilizeRoll = stabilizeRoll; }
    public void setBidirectionalTorque(boolean bidirectionalTorque) { this.bidirectionalTorque = bidirectionalTorque; }
    public void setTargetPitchDeg(double targetPitchDeg) { this.targetPitchDeg = Math.clamp(targetPitchDeg, -90.0, 90.0); }
    public void setTargetYawDeg(double targetYawDeg) { this.targetYawDeg = wrapYaw(targetYawDeg); }
    public void setTargetRollDeg(double targetRollDeg) { this.targetRollDeg = Math.clamp(targetRollDeg, -90.0, 90.0); }

    public record PartialUpdate(
        Optional<StabilizerBlockEntity.GyroMode> mode,
        Optional<Boolean> autoStabilize,
        Optional<Double> forcePercent,
        Optional<Double> dampingPercent,
        Optional<Double> acceptAngleDeg,
        Optional<Boolean> stabilizePitch,
        Optional<Boolean> stabilizeYaw,
        Optional<Boolean> stabilizeRoll,
        Optional<Boolean> bidirectionalTorque,
        Optional<Double> targetPitchDeg,
        Optional<Double> targetYawDeg,
        Optional<Double> targetRollDeg
    ) {
        public static final Codec<PartialUpdate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GYRO_MODE_CODEC.optionalFieldOf("mode").forGetter(PartialUpdate::mode),
            Codec.BOOL.optionalFieldOf("auto_stabilize").forGetter(PartialUpdate::autoStabilize),
            Codec.DOUBLE.optionalFieldOf("force_percent").forGetter(PartialUpdate::forcePercent),
            Codec.DOUBLE.optionalFieldOf("damping_percent").forGetter(PartialUpdate::dampingPercent),
            Codec.DOUBLE.optionalFieldOf("accept_angle_deg").forGetter(PartialUpdate::acceptAngleDeg),
            Codec.BOOL.optionalFieldOf("stabilize_pitch").forGetter(PartialUpdate::stabilizePitch),
            Codec.BOOL.optionalFieldOf("stabilize_yaw").forGetter(PartialUpdate::stabilizeYaw),
            Codec.BOOL.optionalFieldOf("stabilize_roll").forGetter(PartialUpdate::stabilizeRoll),
            Codec.BOOL.optionalFieldOf("bidirectional_torque").forGetter(PartialUpdate::bidirectionalTorque),
            Codec.DOUBLE.optionalFieldOf("target_pitch_deg").forGetter(PartialUpdate::targetPitchDeg),
            Codec.DOUBLE.optionalFieldOf("target_yaw_deg").forGetter(PartialUpdate::targetYawDeg),
            Codec.DOUBLE.optionalFieldOf("target_roll_deg").forGetter(PartialUpdate::targetRollDeg)
        ).apply(instance, PartialUpdate::new));

        public void applyTo(StabilizerConfig target) {
            mode.ifPresent(target::setMode);
            autoStabilize.ifPresent(target::setAutoStabilize);
            forcePercent.ifPresent(target::setForcePercent);
            dampingPercent.ifPresent(target::setDampingPercent);
            acceptAngleDeg.ifPresent(target::setAcceptAngleDeg);
            stabilizePitch.ifPresent(target::setStabilizePitch);
            stabilizeYaw.ifPresent(target::setStabilizeYaw);
            stabilizeRoll.ifPresent(target::setStabilizeRoll);
            bidirectionalTorque.ifPresent(target::setBidirectionalTorque);
            targetPitchDeg.ifPresent(target::setTargetPitchDeg);
            targetYawDeg.ifPresent(target::setTargetYawDeg);
            targetRollDeg.ifPresent(target::setTargetRollDeg);
        }
    }

    private static double wrapYaw(double yaw) {
        yaw = yaw % 360.0;
        if (yaw >= 180.0) yaw -= 360.0;
        if (yaw < -180.0) yaw += 360.0;
        return yaw;
    }
}