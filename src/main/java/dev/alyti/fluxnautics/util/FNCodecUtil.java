package dev.alyti.fluxnautics.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.util.SableBufferUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3d;
import org.joml.Matrix3dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.List;
import java.util.stream.Stream;

public class FNCodecUtil {
    private static Matrix3d matFromList(List<Double> list) {
        return new Matrix3d(
                new Vector3d(list.get(0), list.get(3), list.get(6)),
                new Vector3d(list.get(1), list.get(4), list.get(7)),
                new Vector3d(list.get(2), list.get(5), list.get(8)));
    }

    public static final StreamCodec<ByteBuf, Vector3d> STREAM_VECTOR3D = StreamCodec.of(
            SableBufferUtils::write,
            x -> SableBufferUtils.read(x, new Vector3d()));


    public static final StreamCodec<ByteBuf, Vector3dc> STREAM_VECTOR3DC = ByteBufCodecs.DOUBLE.apply(ByteBufCodecs.list(3))
            .map(l -> new Vector3d(l.getFirst(), l.get(1), l.get(2)), (v) -> List.of(v.x(), v.y(), v.z()));

    public static final StreamCodec<ByteBuf, Matrix3dc> STREAM_MATRIX3D = ByteBufCodecs.DOUBLE.apply(ByteBufCodecs.list(9))
            .map(FNCodecUtil::matFromList, FNCodecUtil::listFromMat);

    public static final StreamCodec<ByteBuf, BoundingBox3d> BOUNDING_BOX_3D_STREAM_CODEC = ByteBufCodecs.DOUBLE.apply(ByteBufCodecs.list(6))
            .map(l -> new BoundingBox3d(l.getFirst(), l.get(1), l.get(2), l.get(3), l.get(4), l.get(5)), bb -> List.of(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ));

    public static final StreamCodec<ByteBuf, ForceGroup> STREAM_FORCE_GROUP = ResourceLocation.STREAM_CODEC.map(ForceGroups.REGISTRY::get, ForceGroups.REGISTRY::getKey);

    public static final StreamCodec<ByteBuf, QueuedForceGroup.PointForce> STREAM_POINT_FORCE = STREAM_VECTOR3DC.apply(ByteBufCodecs.list(2))
            .map(l -> new QueuedForceGroup.PointForce(l.getFirst(), l.get(1)), p -> List.of(p.point(), p.force()));


    private static List<Double> listFromMat(Matrix3dc mat) {
        return List.of(
                mat.m00(), mat.m01(), mat.m02(),
                mat.m10(), mat.m11(), mat.m12(),
                mat.m20(), mat.m21(), mat.m22()
        );
    }

    /**
     * Creates a codec that accepts both lists and CC-style indexed maps ({"1.0": val, "2.0": val, ...}).
     */
    public static <T> Codec<List<T>> ccCompatibleList(Codec<T> elementCodec) {
        return new Codec<>() {
            @Override
            public <U> DataResult<com.mojang.datafixers.util.Pair<List<T>, U>> decode(DynamicOps<U> ops, U input) {
                // Try as list first
                DataResult<Stream<U>> listResult = ops.getStream(input);
                if (listResult.isSuccess()) {
                    List<T> result = listResult.getOrThrow()
                            .map(e -> elementCodec.decode(ops, e).getOrThrow().getFirst())
                            .toList();
                    return DataResult.success(com.mojang.datafixers.util.Pair.of(result, ops.empty()));
                }

                // Try as CC-style map
                DataResult<Stream<com.mojang.datafixers.util.Pair<U, U>>> mapResult = ops.getMapValues(input);
                if (mapResult.isSuccess()) {
                    try {
                        List<T> result = mapResult.getOrThrow()
                                .sorted((a, b) -> {
                                    double ka = ops.getNumberValue(a.getFirst()).getOrThrow().doubleValue();
                                    double kb = ops.getNumberValue(b.getFirst()).getOrThrow().doubleValue();
                                    return Double.compare(ka, kb);
                                })
                                .map(e -> elementCodec.decode(ops, e.getSecond()).getOrThrow().getFirst())
                                .toList();
                        return DataResult.success(com.mojang.datafixers.util.Pair.of(result, ops.empty()));
                    } catch (Exception e) {
                        return DataResult.error(e::getMessage);
                    }
                }

                return DataResult.error(() -> "Expected list or indexed map, got " + input);
            }

            @Override
            public <U> DataResult<U> encode(List<T> value, DynamicOps<U> ops, U rest) {
                return Codec.list(elementCodec).encode(value, ops, rest);
            }
        };
    }

}
