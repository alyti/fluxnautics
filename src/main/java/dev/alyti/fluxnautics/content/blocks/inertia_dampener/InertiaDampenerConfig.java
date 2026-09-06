package dev.alyti.fluxnautics.content.blocks.inertia_dampener;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;
import java.util.Optional;

public record InertiaDampenerConfig(AxisConfig linearX, AxisConfig linearY, AxisConfig linearZ) {

    public static final InertiaDampenerConfig DEFAULT = new InertiaDampenerConfig(
        AxisConfig.DEFAULT, AxisConfig.DEFAULT, AxisConfig.DEFAULT
    );

    public static final Codec<InertiaDampenerConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        AxisConfig.CODEC.optionalFieldOf("linearX", AxisConfig.DEFAULT).forGetter(InertiaDampenerConfig::linearX),
        AxisConfig.CODEC.optionalFieldOf("linearY", AxisConfig.DEFAULT).forGetter(InertiaDampenerConfig::linearY),
        AxisConfig.CODEC.optionalFieldOf("linearZ", AxisConfig.DEFAULT).forGetter(InertiaDampenerConfig::linearZ)
    ).apply(instance, InertiaDampenerConfig::new));

    public static final StreamCodec<ByteBuf, InertiaDampenerConfig> STREAM_CODEC = StreamCodec.composite(
        AxisConfig.STREAM_CODEC, InertiaDampenerConfig::linearX,
        AxisConfig.STREAM_CODEC, InertiaDampenerConfig::linearY,
        AxisConfig.STREAM_CODEC, InertiaDampenerConfig::linearZ,
        InertiaDampenerConfig::new
    );

    public AxisConfig getAxis(final DampenerAxis axis) {
        return switch (axis) {
            case LINEAR_X -> this.linearX;
            case LINEAR_Y -> this.linearY;
            case LINEAR_Z -> this.linearZ;
        };
    }

    public InertiaDampenerConfig withAxis(final DampenerAxis axis, final AxisConfig config) {
        return new InertiaDampenerConfig(
            axis == DampenerAxis.LINEAR_X ? config : this.linearX,
            axis == DampenerAxis.LINEAR_Y ? config : this.linearY,
            axis == DampenerAxis.LINEAR_Z ? config : this.linearZ
        );
    }

    public boolean isIdle() {
        for (final DampenerAxis axis : DampenerAxis.ORDER) {
            if (this.getAxis(axis).mode().isActive()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        InertiaDampenerConfig that = (InertiaDampenerConfig) o;
        return Objects.equals(linearX(), that.linearX()) && Objects.equals(linearY(), that.linearY()) && Objects.equals(linearZ(), that.linearZ());
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(linearX());
        result = 31 * result + Objects.hashCode(linearY());
        result = 31 * result + Objects.hashCode(linearZ());
        return result;
    }

    public record PartialUpdate(
        Optional<DampenerMode> linearXMode, Optional<Double> linearXStrength,
        Optional<DampenerMode> linearYMode, Optional<Double> linearYStrength,
        Optional<DampenerMode> linearZMode, Optional<Double> linearZStrength
    ) {

        public static final PartialUpdate EMPTY = new PartialUpdate(
            Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty()
        );

        public static final Codec<PartialUpdate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DampenerMode.CODEC.optionalFieldOf("linearXMode").forGetter(PartialUpdate::linearXMode),
            Codec.doubleRange(0.0, 1.0).optionalFieldOf("linearXStrength").forGetter(PartialUpdate::linearXStrength),
            DampenerMode.CODEC.optionalFieldOf("linearYMode").forGetter(PartialUpdate::linearYMode),
            Codec.doubleRange(0.0, 1.0).optionalFieldOf("linearYStrength").forGetter(PartialUpdate::linearYStrength),
            DampenerMode.CODEC.optionalFieldOf("linearZMode").forGetter(PartialUpdate::linearZMode),
            Codec.doubleRange(0.0, 1.0).optionalFieldOf("linearZStrength").forGetter(PartialUpdate::linearZStrength)
        ).apply(instance, PartialUpdate::new));

        public static final StreamCodec<ByteBuf, PartialUpdate> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public PartialUpdate decode(final ByteBuf buffer) {
                return new PartialUpdate(
                    decodeOpt(buffer, DampenerMode.STREAM_CODEC), decodeOpt(buffer, ByteBufCodecs.DOUBLE),
                    decodeOpt(buffer, DampenerMode.STREAM_CODEC), decodeOpt(buffer, ByteBufCodecs.DOUBLE),
                    decodeOpt(buffer, DampenerMode.STREAM_CODEC), decodeOpt(buffer, ByteBufCodecs.DOUBLE)
                );
            }

            @Override
            public void encode(final ByteBuf buffer, final PartialUpdate update) {
                encodeOpt(buffer, DampenerMode.STREAM_CODEC, update.linearXMode());
                encodeOpt(buffer, ByteBufCodecs.DOUBLE, update.linearXStrength());
                encodeOpt(buffer, DampenerMode.STREAM_CODEC, update.linearYMode());
                encodeOpt(buffer, ByteBufCodecs.DOUBLE, update.linearYStrength());
                encodeOpt(buffer, DampenerMode.STREAM_CODEC, update.linearZMode());
                encodeOpt(buffer, ByteBufCodecs.DOUBLE, update.linearZStrength());
            }
        };

        public static PartialUpdate ofAxis(final DampenerAxis axis, final AxisConfig config) {
            final Optional<DampenerMode> mode = Optional.of(config.mode());
            final Optional<Double> strength = Optional.of(config.strength());
            return switch (axis) {
                case LINEAR_X -> new PartialUpdate(mode, strength, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
                case LINEAR_Y -> new PartialUpdate(Optional.empty(), Optional.empty(), mode, strength, Optional.empty(), Optional.empty());
                case LINEAR_Z -> new PartialUpdate(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), mode, strength);
            };
        }

        public static PartialUpdate ofMode(final DampenerAxis axis, final DampenerMode mode) {
            return ofAxis(axis, new AxisConfig(mode, 1.0)).withOnlyMode(axis);
        }

        public static PartialUpdate ofStrength(final DampenerAxis axis, final double strength) {
            return ofAxis(axis, new AxisConfig(DampenerMode.OFF, strength)).withOnlyStrength(axis);
        }

        public static PartialUpdate ofLinearModes(final DampenerMode mode) {
            final Optional<DampenerMode> m = Optional.of(mode);
            return new PartialUpdate(m, Optional.empty(), m, Optional.empty(), m, Optional.empty());
        }


        public static PartialUpdate ofLinearStrengths(final double strength) {
            final Optional<Double> s = Optional.of(Math.clamp(strength, 0.0, 1.0));
            return new PartialUpdate(Optional.empty(), s, Optional.empty(), s, Optional.empty(), s);
        }

        private PartialUpdate withOnlyMode(final DampenerAxis axis) {
            final PartialUpdate full = this;
            return switch (axis) {
                case LINEAR_X -> new PartialUpdate(full.linearXMode(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
                case LINEAR_Y -> new PartialUpdate(Optional.empty(), Optional.empty(), full.linearYMode(), Optional.empty(), Optional.empty(), Optional.empty());
                case LINEAR_Z -> new PartialUpdate(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), full.linearZMode(), Optional.empty());
            };
        }

        private PartialUpdate withOnlyStrength(final DampenerAxis axis) {
            final PartialUpdate full = this;
            return switch (axis) {
                case LINEAR_X -> new PartialUpdate(Optional.empty(), full.linearXStrength(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
                case LINEAR_Y -> new PartialUpdate(Optional.empty(), Optional.empty(), Optional.empty(), full.linearYStrength(), Optional.empty(), Optional.empty());
                case LINEAR_Z -> new PartialUpdate(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), full.linearZStrength());
            };
        }

        public InertiaDampenerConfig applyTo(final InertiaDampenerConfig base) {
            return new InertiaDampenerConfig(
                base.linearX().withPatch(this.linearXMode, this.linearXStrength),
                base.linearY().withPatch(this.linearYMode, this.linearYStrength),
                base.linearZ().withPatch(this.linearZMode, this.linearZStrength)
            );
        }

        private static <T> void encodeOpt(final ByteBuf buffer, final StreamCodec<ByteBuf, T> codec, final Optional<T> value) {
            if (value.isPresent()) {
                buffer.writeBoolean(true);
                codec.encode(buffer, value.get());
            } else {
                buffer.writeBoolean(false);
            }
        }

        private static <T> Optional<T> decodeOpt(final ByteBuf buffer, final StreamCodec<ByteBuf, T> codec) {
            return buffer.readBoolean() ? Optional.of(codec.decode(buffer)) : Optional.empty();
        }
    }

    public static record AxisConfig(DampenerMode mode, double strength) {

        public static final AxisConfig DEFAULT = new AxisConfig(DampenerMode.DAMPEN, 1.0);

        public static final Codec<AxisConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DampenerMode.CODEC.optionalFieldOf("mode", DampenerMode.OFF).forGetter(AxisConfig::mode),
            Codec.doubleRange(0.0, 1.0).optionalFieldOf("strength", 1.0).forGetter(AxisConfig::strength)
        ).apply(instance, AxisConfig::new));

        public static final StreamCodec<ByteBuf, AxisConfig> STREAM_CODEC = StreamCodec.composite(
            DampenerMode.STREAM_CODEC, AxisConfig::mode,
            ByteBufCodecs.DOUBLE, AxisConfig::strength,
            AxisConfig::new
        );

        public AxisConfig {
            strength = Double.isFinite(strength) ? Math.clamp(strength, 0.0, 1.0) : 1.0;
        }

        public AxisConfig withPatch(final Optional<DampenerMode> patchedMode, final Optional<Double> patchedStrength) {
            return new AxisConfig(patchedMode.orElse(this.mode), patchedStrength.orElse(this.strength));
        }
    }

    public enum DampenerAxis {
        LINEAR_X("linearX"),
        LINEAR_Y("linearY"),
        LINEAR_Z("linearZ");

        public static final DampenerAxis[] ORDER = DampenerAxis.values();

        public static final Codec<DampenerAxis> CODEC = Codec.STRING.xmap(DampenerAxis::byName, DampenerAxis::key);

        public static final StreamCodec<ByteBuf, DampenerAxis> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public DampenerAxis decode(final ByteBuf buffer) {
                final int index = buffer.readByte();
                return index >= 0 && index < ORDER.length ? ORDER[index] : LINEAR_X;
            }

            @Override
            public void encode(final ByteBuf buffer, final DampenerAxis axis) {
                buffer.writeByte(axis.ordinal());
            }
        };

        private final String key;

        DampenerAxis(final String key) {
            this.key = key;
        }

        public String key() {
            return this.key;
        }

        public int component() {
            return this.ordinal() % 3;
        }

        public static DampenerAxis byName(final String name) {
            if (name == null) {
                return null;
            }
            final String lower = name.toLowerCase();
            return switch (lower) {
                case "linearx", "linear_x", "x" -> LINEAR_X;
                case "lineary", "linear_y", "y" -> LINEAR_Y;
                case "linearz", "linear_z", "z" -> LINEAR_Z;
                default -> null;
            };
        }
    }

    public enum DampenerMode {
        OFF,
        DAMPEN,
        BRAKE;

        public static final Codec<DampenerMode> CODEC = Codec.STRING.xmap(DampenerMode::byName, DampenerMode::name);

        public static final StreamCodec<ByteBuf, DampenerMode> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public DampenerMode decode(final ByteBuf buffer) {
                final int index = buffer.readByte();
                final DampenerMode[] values = DampenerMode.values();
                return index >= 0 && index < values.length ? values[index] : DampenerMode.OFF;
            }

            @Override
            public void encode(final ByteBuf buffer, final DampenerMode mode) {
                buffer.writeByte(mode.ordinal());
            }
        };

        public static DampenerMode byName(final String name) {
            for (final DampenerMode mode : DampenerMode.values()) {
                if (mode.name().equalsIgnoreCase(name)) {
                    return mode;
                }
            }
            return DampenerMode.OFF;
        }

        public boolean isActive() {
            return this != OFF;
        }
    }
}
