package dev.alyti.fluxnautics.index;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JavaOps;
import dev.alyti.fluxnautics.FluxnauticsConfig;
import dev.alyti.fluxnautics.content.blocks.thruster.IonThrusterBlockEntity;
import dev.alyti.fluxnautics.multiblock.ThrusterArrayMultiblockData;
import dev.alyti.fluxnautics.multiblock.ThrusterArrayMultiblockData.GroupData;
import dev.alyti.fluxnautics.util.Couple;
import dev.ryanhcode.sable.api.block.propeller.BlockEntityPropeller;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.Pair;
import mekanism.api.text.IHasTextComponent;
import mekanism.api.text.TextComponentUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

public enum ThrusterDriver implements StringRepresentable, IHasTextComponent {
    DIRECT("direct", new DriverCallback<>() {
        @Override
        public Pair<ImpulseGenerator, Optional<Void>> drive(IonThrusterBlockEntity tile, ThrusterArrayMultiblockData multiblock, GroupData group, BlockPos relPos, @org.jspecify.annotations.Nullable Void cache, Void driver, ServerSubLevel subLevel) {
            return Pair.of(new DirectImpulse(), Optional.empty());
        }

        @Override
        public long cost(ThrusterArrayMultiblockData multiblock, GroupData group, Void driver) {
            return Math.round(group.thrustLevel * (group.thrusters.size() * FluxnauticsConfig.common.thrusterArrayJoulePerThrustLevel.getOrDefault()));
        }

        @Override
        public Map<Integer, BiConsumer<Integer, Void>> redstoneToDriver(Void driver) {
            return Map.of();
        }

    }, Void.class, Void.class, null),

    HOVER("hover", new DriverCallback<>(){
        @Override
        public Pair<ImpulseGenerator, Optional<ThrusterDriverHelper.HoverCacheData>> drive(IonThrusterBlockEntity tile, ThrusterArrayMultiblockData multiblock, GroupData group, BlockPos relPos, ThrusterDriverHelper.@org.jspecify.annotations.Nullable HoverCacheData cache, Void driver, ServerSubLevel subLevel) {
            boolean updateCache = false;
            if (cache == null) {
                cache = ThrusterDriverHelper.HoverCacheData.gatherData(tile.getBlockState(), 8.0D);
                updateCache = true;
            }

            assert cache != null;
            return Pair.of(new HoverImpulse(cache.raycastPoints()), (updateCache) ? Optional.of(cache) : Optional.empty() );
        }

        @Override
        public long cost(ThrusterArrayMultiblockData multiblock, GroupData group, Void driver) {
            return Math.round(group.thrustLevel * (group.thrusters.size() * FluxnauticsConfig.common.thrusterArrayJoulePerThrustLevel.getOrDefault()));
        }

        @Override
        public Map<Integer, BiConsumer<Integer, Void>> redstoneToDriver(Void driver) {
            return Map.of();
        }

    }, ThrusterDriverHelper.HoverCacheData.class, Void.class, null),

    VECTOR("vector", new DriverCallback<>() {
        @Override
        public Pair<ImpulseGenerator, Optional<Void>> drive(IonThrusterBlockEntity tile, ThrusterArrayMultiblockData multiblock, GroupData group, BlockPos relPos, @org.jspecify.annotations.Nullable Void cache, ThrusterDriverHelper.VectorDriverData driver, ServerSubLevel subLevel) {
            if (driver == null) {
                driver = new ThrusterDriverHelper.VectorDriverData(0.0, 0.0, true);
            }
            return Pair.of(
                    new VectorImpulse(
                            driver.auto() ?
                                    ThrusterDriverHelper.VectorDriverData.autoVectoring(tile.facingSupplier.get(), tile.getBlockPos(), tile.getLevel(), subLevel) :
                                    driver.manualVectoring(tile.facingSupplier.get())
                    ),
                    Optional.empty());
        }

        @Override
        public long cost(ThrusterArrayMultiblockData multiblock, GroupData group, ThrusterDriverHelper.VectorDriverData driver) {
            return Math.round(group.thrustLevel * (group.thrusters.size() * FluxnauticsConfig.common.thrusterArrayJoulePerThrustLevel.getOrDefault()));
        }

        @Override
        public Map<Integer, BiConsumer<Integer, ThrusterDriverHelper.VectorDriverData>> redstoneToDriver(ThrusterDriverHelper.VectorDriverData driver) {
            return Map.of(
                    1, (x, d) -> d.setX( x / 15.0),
                    2, (x, d) -> d.setX( -(x / 15.0)),
                    3, (x, d) -> d.setY( x / 15.0),
                    4, (x, d) -> d.setY( -(x / 15.0))
            );
        }
    }, Void.class, ThrusterDriverHelper.VectorDriverData.class, ThrusterDriverHelper.VectorDriverData.CODEC),
    ;

    public static final IntFunction<ThrusterDriver> BY_ID = ByIdMap.continuous(ThrusterDriver::ordinal, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
    public static final StreamCodec<ByteBuf, ThrusterDriver> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, ThrusterDriver::ordinal);


    final String label;
    final RawDriverCallback driver;
    private final RawDriverSerializer serializer;
    private final RawDriverParser parser;


    <C, D> ThrusterDriver(String label, DriverCallback<C, D> callback, Class<C> cacheType, Class<D> driverType, Codec<D> codec) {
        this.label = label;
        this.driver = wrap(callback, cacheType, driverType);

        this.serializer = wrapSerializer(codec, driverType);
        this.parser = wrapParser(codec);
    }

    private static <C, D> RawDriverCallback wrap(
            DriverCallback<C, D> callback,
            Class<C> cacheType,
            Class<D> driverType
    ) {
        return new RawDriverCallback() {
            @Override
            public Pair<ImpulseGenerator, Optional<Object>> driveRaw(
                    IonThrusterBlockEntity tile,
                    ThrusterArrayMultiblockData multiblock,
                    GroupData group,
                    BlockPos relPos,
                    Object rawCache,
                    Object rawDriver,
                    ServerSubLevel subLevel
            ) {
                C cache = cacheType.cast(rawCache);
                D driver = driverType.cast(rawDriver);

                Pair<ImpulseGenerator, Optional<C>> result = callback.drive(tile, multiblock, group, relPos, cache, driver, subLevel);
                Optional<Object> widenedCache = result.right().map(c -> c);
                return Pair.of(result.left(), widenedCache);
            }

            @Override
            public long costRaw(
                    ThrusterArrayMultiblockData multiblock,
                    GroupData group,
                    Object rawDriver
            ) {
                D driver = driverType.cast(rawDriver);
                return callback.cost(multiblock, group, driver);
            }

            @Override
            public Map<Integer, BiConsumer<Integer, Object>> redstoneToDriver(Object rawDriver) {
                D driver = driverType.cast(rawDriver);
                Map<Integer, BiConsumer<Integer, D>> result = callback.redstoneToDriver(driver);

                return result.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> (i, obj) -> entry.getValue().accept(i, driverType.cast(obj))
                        ));
            }
        };
    }

    private static <D> RawDriverSerializer wrapSerializer(Codec<D> codec, Class<D> type) {
        return new RawDriverSerializer() {
            public Tag serialize(Object driver) {
                if (codec == null) {
                    return null;
                }

                D d = type.cast(driver);
                return codec.encodeStart(NbtOps.INSTANCE, d)
                        .getOrThrow();
            }

            public Object deserialize(Tag tag) {
                if (codec == null) {
                    return null;
                }

                DataResult<com.mojang.datafixers.util.Pair<D, Tag>> result = codec.decode(NbtOps.INSTANCE, tag);
                return result.getOrThrow().getFirst();
            }
        };
    }

    private static <D> RawDriverParser wrapParser(Codec<D> codec) {
        return dataMap -> {
            if (codec == null) {
                return null;
            }
            DataResult<com.mojang.datafixers.util.Pair<D, Object>> result = codec.decode(JavaOps.INSTANCE,dataMap);
            return result.getOrThrow().getFirst();
        };
    }

    public Tag serializeDriver(Object driverValue) {
        return serializer.serialize(driverValue);
    }

    public Object deserializeDriver(Tag tag) {
        return serializer.deserialize(tag);
    }

    public Object parseDriver(Map<?, ?> configData) {
        return parser.parse(configData);
    }

    @NotNull
    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Pair<ImpulseGenerator, Optional<Object>> drive(
            IonThrusterBlockEntity tile,
            ThrusterArrayMultiblockData multiblock,
            GroupData group,
            BlockPos relPos,
            Object rawCache,
            Object rawDriver,
            ServerSubLevel subLevel
    ) {
        return driver.driveRaw(tile, multiblock, group, relPos, rawCache, rawDriver, subLevel);
    }

    public long cost(
            ThrusterArrayMultiblockData multiblock,
            GroupData group,
            Object rawDriver
    ) {
        return driver.costRaw(multiblock, group, rawDriver);
    }

    public Map<Integer, BiConsumer<Integer, Object>> redstoneToDriver(
            Object rawDriver
    ) {
        return driver.redstoneToDriver(rawDriver);
    }

    @Override
    public Component getTextComponent() {
        return TextComponentUtil.translate("gui.fluxnautics.group.driver.%s".formatted(getSerializedName()));
    }

    public interface DriverCallback<C, D> {
        Pair<ImpulseGenerator, Optional<C>> drive(
                IonThrusterBlockEntity tile,
                ThrusterArrayMultiblockData multiblock,
                GroupData group,
                BlockPos relPos,
                @Nullable C cache,
                D driver,
                final ServerSubLevel subLevel
        );

        long cost(
                ThrusterArrayMultiblockData multiblock,
                GroupData group,
                D driver
        );

        Map<Integer, BiConsumer<Integer, D>> redstoneToDriver(D driver);
    }

    interface RawDriverCallback {
        Pair<ImpulseGenerator, Optional<Object>> driveRaw(
                IonThrusterBlockEntity tile,
                ThrusterArrayMultiblockData multiblock,
                GroupData group,
                BlockPos relPos,
                Object rawCache,
                Object rawDriver,
                ServerSubLevel subLevel
        );

        long costRaw(
                ThrusterArrayMultiblockData multiblock,
                GroupData group,
                Object rawDriver
        );

        Map<Integer, BiConsumer<Integer, Object>> redstoneToDriver(Object rawDriver);
    }

    public interface ImpulseGenerator {
        void impulse(
                Vector3d impulse,
                final BlockEntityPropeller prop,
                double percentagePaid,
                final ServerSubLevel subLevel,
                final RigidBodyHandle handle,
                final double timeStep
        );
    }

    public record DirectImpulse() implements ImpulseGenerator {
        @Override
        public void impulse(Vector3d impulse,  BlockEntityPropeller prop, double percentagePaid, ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
            Vec3 facingVector = Vec3.atLowerCornerOf(prop.getBlockDirection().getNormal());
            facingVector = facingVector.scale(prop.getScaledThrust() * timeStep);
            impulse.set(facingVector.x, facingVector.y, facingVector.z);
        }
    }

    public record VectorImpulse(Vector3d direction) implements ImpulseGenerator {
        @Override
        public void impulse(Vector3d impulse,  BlockEntityPropeller prop, double percentagePaid, ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
            impulse.set(direction).mul(prop.getScaledThrust() * timeStep);
        }
    }

    public record HoverImpulse(List<Couple<Vec3>> raycasts) implements ImpulseGenerator {
        @Override
        public void impulse(Vector3d impulse, BlockEntityPropeller prop, double percentagePaid, ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
            impulse.set(ThrusterDriverHelper.HoverCacheData.gatherForceFromRays(subLevel, timeStep, prop.getLevel(), prop.getBlockPos(), prop.getScaledThrust(), raycasts));
        }
    }

    interface RawDriverSerializer {
        Tag serialize(Object driver);
        Object deserialize(Tag tag);
    }

    interface RawDriverParser {
        Object parse(Map<?, ?> dataMap);
    }
}
