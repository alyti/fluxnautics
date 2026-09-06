package dev.alyti.fluxnautics.content.blocks.flux_tether;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;
import java.util.Optional;

public class FluxTetherConfig {

    public enum Mode {
        FREE,
        STATIC,
        SERVO
    }

    private static final Codec<Mode> MODE_CODEC = Codec.STRING.xmap(
        Mode::valueOf,
        Mode::name
    );

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        FluxTetherConfig that = (FluxTetherConfig) o;
        return linearMode == that.linearMode &&
            angularMode == that.angularMode &&
            Double.compare(linearTarget, that.linearTarget) == 0 &&
            Double.compare(linearServoRate, that.linearServoRate) == 0 &&
            Double.compare(linearServoLoop, that.linearServoLoop) == 0 &&
            Double.compare(linearForceBoost, that.linearForceBoost) == 0 &&
            Double.compare(angularTarget, that.angularTarget) == 0 &&
            Double.compare(angularServoRate, that.angularServoRate) == 0 &&
            Double.compare(angularForceBoost, that.angularForceBoost) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(linearMode, angularMode,
            linearTarget, linearServoRate, linearServoLoop, linearForceBoost,
            angularTarget, angularServoRate, angularForceBoost);
    }

    public static final Codec<FluxTetherConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        MODE_CODEC.optionalFieldOf("linear_mode", Mode.FREE).forGetter(FluxTetherConfig::linearMode),
        MODE_CODEC.optionalFieldOf("angular_mode", Mode.FREE).forGetter(FluxTetherConfig::angularMode),
        Codec.DOUBLE.optionalFieldOf("linear_target", FluxTetherBlockEntity.MIN_EXTENSION_BLOCKS).forGetter(FluxTetherConfig::linearTarget),
        Codec.DOUBLE.optionalFieldOf("linear_servo_rate", 1.0).forGetter(FluxTetherConfig::linearServoRate),
        Codec.DOUBLE.optionalFieldOf("linear_servo_loop", 32.0).forGetter(FluxTetherConfig::linearServoLoop),
        Codec.DOUBLE.optionalFieldOf("linear_force_boost", 1.0).forGetter(FluxTetherConfig::linearForceBoost),
        Codec.DOUBLE.optionalFieldOf("angular_target", 0.0).forGetter(FluxTetherConfig::angularTarget),
        Codec.DOUBLE.optionalFieldOf("angular_servo_rate", 0.0).forGetter(FluxTetherConfig::angularServoRate),
        Codec.DOUBLE.optionalFieldOf("angular_force_boost", 1.0).forGetter(FluxTetherConfig::angularForceBoost)
    ).apply(instance, FluxTetherConfig::new));

    public static final StreamCodec<ByteBuf, FluxTetherConfig> STREAM_CODEC = StreamCodec.of(
        (buf, config) -> {
            ByteBufCodecs.STRING_UTF8.encode(buf, config.linearMode().name());
            ByteBufCodecs.STRING_UTF8.encode(buf, config.angularMode().name());
            ByteBufCodecs.DOUBLE.encode(buf, config.linearTarget());
            ByteBufCodecs.DOUBLE.encode(buf, config.linearServoRate());
            ByteBufCodecs.DOUBLE.encode(buf, config.linearServoLoop());
            ByteBufCodecs.DOUBLE.encode(buf, config.linearForceBoost());
            ByteBufCodecs.DOUBLE.encode(buf, config.angularTarget());
            ByteBufCodecs.DOUBLE.encode(buf, config.angularServoRate());
            ByteBufCodecs.DOUBLE.encode(buf, config.angularForceBoost());
        },
        (buf) -> {
            Mode linearMode = Mode.valueOf(ByteBufCodecs.STRING_UTF8.decode(buf));
            Mode angularMode = Mode.valueOf(ByteBufCodecs.STRING_UTF8.decode(buf));
            double linearTarget = ByteBufCodecs.DOUBLE.decode(buf);
            double linearServoRate = ByteBufCodecs.DOUBLE.decode(buf);
            double linearServoLoop = ByteBufCodecs.DOUBLE.decode(buf);
            double linearForceBoost = ByteBufCodecs.DOUBLE.decode(buf);
            double angularTarget = ByteBufCodecs.DOUBLE.decode(buf);
            double angularServoRate = ByteBufCodecs.DOUBLE.decode(buf);
            double angularForceBoost = ByteBufCodecs.DOUBLE.decode(buf);
            return new FluxTetherConfig(linearMode, angularMode,
                linearTarget, linearServoRate, linearServoLoop, linearForceBoost,
                angularTarget, angularServoRate, angularForceBoost);
        }
    );

    private Mode linearMode;
    private Mode angularMode;
    private double linearTarget;
    private double linearServoRate;
    private double linearServoLoop;
    private double linearForceBoost;
    private double angularTarget;
    private double angularServoRate;
    private double angularForceBoost;

    public FluxTetherConfig(Mode linearMode, Mode angularMode,
                            double linearTarget, double linearServoRate, double linearServoLoop, double linearForceBoost,
                            double angularTarget, double angularServoRate, double angularForceBoost) {
        this.linearMode = linearMode;
        this.angularMode = angularMode;
        this.linearTarget = Math.max(0.0, linearTarget);
        this.linearServoRate = linearServoRate;
        this.linearServoLoop = Math.max(1.0, linearServoLoop);
        this.linearForceBoost = Math.clamp(linearForceBoost, 0.0, 4.0);
        this.angularTarget = angularTarget;
        this.angularServoRate = angularServoRate;
        this.angularForceBoost = Math.clamp(angularForceBoost, 0.0, 4.0);
    }

    public static FluxTetherConfig makeDefault() {
        return new FluxTetherConfig(Mode.FREE, Mode.FREE,
            FluxTetherBlockEntity.MIN_EXTENSION_BLOCKS, 1.0, 32.0, 1.0,
            0.0, 0.0, 1.0);
    }

    public Mode linearMode() { return linearMode; }
    public Mode angularMode() { return angularMode; }
    public double linearTarget() { return linearTarget; }
    public double linearServoRate() { return linearServoRate; }
    public double linearServoLoop() { return linearServoLoop; }
    public double linearForceBoost() { return linearForceBoost; }
    public double angularTarget() { return angularTarget; }
    public double angularServoRate() { return angularServoRate; }
    public double angularForceBoost() { return angularForceBoost; }

    public void setLinearMode(Mode linearMode) { this.linearMode = linearMode; }
    public void setAngularMode(Mode angularMode) { this.angularMode = angularMode; }
    public void setLinearTarget(double linearTarget) { this.linearTarget = Math.max(0.0, linearTarget); }
    public void setLinearServoRate(double linearServoRate) { this.linearServoRate = linearServoRate; }
    public void setLinearServoLoop(double linearServoLoop) { this.linearServoLoop = Math.max(1.0, linearServoLoop); }
    public void setLinearForceBoost(double linearForceBoost) { this.linearForceBoost = Math.clamp(linearForceBoost, 0.0, 4.0); }
    public void setAngularTarget(double angularTarget) { this.angularTarget = angularTarget; }
    public void setAngularServoRate(double angularServoRate) { this.angularServoRate = angularServoRate; }
    public void setAngularForceBoost(double angularForceBoost) { this.angularForceBoost = Math.clamp(angularForceBoost, 0.0, 4.0); }

    public boolean linearEngaged() { return linearMode != Mode.FREE; }
    public boolean angularEngaged() { return angularMode != Mode.FREE; }

    public record PartialUpdate(
        Optional<Mode> linearMode,
        Optional<Mode> angularMode,
        Optional<Double> linearTarget,
        Optional<Double> linearServoRate,
        Optional<Double> linearServoLoop,
        Optional<Double> linearForceBoost,
        Optional<Double> angularTarget,
        Optional<Double> angularServoRate,
        Optional<Double> angularForceBoost
    ) {
        public static final Codec<PartialUpdate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MODE_CODEC.optionalFieldOf("linear_mode").forGetter(PartialUpdate::linearMode),
            MODE_CODEC.optionalFieldOf("angular_mode").forGetter(PartialUpdate::angularMode),
            Codec.DOUBLE.optionalFieldOf("linear_target").forGetter(PartialUpdate::linearTarget),
            Codec.DOUBLE.optionalFieldOf("linear_servo_rate").forGetter(PartialUpdate::linearServoRate),
            Codec.DOUBLE.optionalFieldOf("linear_servo_loop").forGetter(PartialUpdate::linearServoLoop),
            Codec.DOUBLE.optionalFieldOf("linear_force_boost").forGetter(PartialUpdate::linearForceBoost),
            Codec.DOUBLE.optionalFieldOf("angular_target").forGetter(PartialUpdate::angularTarget),
            Codec.DOUBLE.optionalFieldOf("angular_servo_rate").forGetter(PartialUpdate::angularServoRate),
            Codec.DOUBLE.optionalFieldOf("angular_force_boost").forGetter(PartialUpdate::angularForceBoost)
        ).apply(instance, PartialUpdate::new));

        public void applyTo(FluxTetherConfig target) {
            linearMode.ifPresent(target::setLinearMode);
            angularMode.ifPresent(target::setAngularMode);
            linearTarget.ifPresent(target::setLinearTarget);
            linearServoRate.ifPresent(target::setLinearServoRate);
            linearServoLoop.ifPresent(target::setLinearServoLoop);
            linearForceBoost.ifPresent(target::setLinearForceBoost);
            angularTarget.ifPresent(target::setAngularTarget);
            angularServoRate.ifPresent(target::setAngularServoRate);
            angularForceBoost.ifPresent(target::setAngularForceBoost);
        }
    }
}
