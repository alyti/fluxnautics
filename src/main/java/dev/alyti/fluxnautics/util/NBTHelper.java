package dev.alyti.fluxnautics.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class NBTHelper {

    public static void putMarker(CompoundTag nbt, String marker) {
        nbt.putBoolean(marker, true);
    }

    public static BlockPos readBlockPos(CompoundTag nbt, String key) {
        Optional<BlockPos> pos = NbtUtils.readBlockPos(nbt, key);
        if (pos.isPresent())
            return pos.get();
        CompoundTag oldTag = nbt.getCompound(key);
        return new BlockPos(oldTag.getInt("X"), oldTag.getInt("Y"), oldTag.getInt("Z"));
    }

    public static <T extends Enum<?>> T readEnum(CompoundTag nbt, String key, Class<T> enumClass) {
        T[] enumConstants = enumClass.getEnumConstants();
        if (enumConstants == null)
            throw new IllegalArgumentException("Non-Enum class passed to readEnum: " + enumClass.getName());
        if (nbt.contains(key, Tag.TAG_STRING)) {
            String name = nbt.getString(key);
            for (T t : enumConstants) {
                if (t.name()
                        .equals(name))
                    return t;
            }
        }
        return enumConstants[0];
    }

    public static <T extends Enum<?>> void writeEnum(CompoundTag nbt, String key, T enumConstant) {
        nbt.putString(key, enumConstant.name());
    }

    public static <T> ListTag writeCompoundList(Iterable<T> list, Function<T, CompoundTag> serializer) {
        ListTag listNBT = new ListTag();
        list.forEach(t -> {
            CompoundTag apply = serializer.apply(t);
            if (apply == null)
                return;
            listNBT.add(apply);
        });
        return listNBT;
    }

    public static <T> List<T> readCompoundList(ListTag listNBT, Function<CompoundTag, T> deserializer) {
        List<T> list = new ArrayList<>(listNBT.size());
        listNBT.forEach(inbt -> list.add(deserializer.apply((CompoundTag) inbt)));
        return list;
    }

    public static void iterateCompoundList(ListTag listNBT, Consumer<CompoundTag> consumer) {
        listNBT.forEach(inbt -> consumer.accept((CompoundTag) inbt));
    }

    public static ListTag writeItemList(Iterable<ItemStack> stacks, HolderLookup.Provider registries) {
        ListTag listNBT = new ListTag();
        for (ItemStack stack : stacks)
            listNBT.add(stack.saveOptional(registries));
        return listNBT;
    }

    public static List<ItemStack> readItemList(ListTag stacks, HolderLookup.Provider registries) {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < stacks.size(); i++)
            list.add(i, ItemStack.parseOptional(registries, stacks.getCompound(i)));
        return list;
    }

    public static ListTag writeAABB(AABB bb) {
        ListTag bbtag = new ListTag();
        bbtag.add(FloatTag.valueOf((float) bb.minX));
        bbtag.add(FloatTag.valueOf((float) bb.minY));
        bbtag.add(FloatTag.valueOf((float) bb.minZ));
        bbtag.add(FloatTag.valueOf((float) bb.maxX));
        bbtag.add(FloatTag.valueOf((float) bb.maxY));
        bbtag.add(FloatTag.valueOf((float) bb.maxZ));
        return bbtag;
    }

    @Nullable
    public static AABB readAABB(ListTag bbTag) {
        if (bbTag.isEmpty())
            return null;
        return new AABB(bbTag.getFloat(0), bbTag.getFloat(1), bbTag.getFloat(2), bbTag.getFloat(3),
                bbTag.getFloat(4), bbTag.getFloat(5));
    }

    public static ListTag writeVec3i(Vec3i vec) {
        ListTag tag = new ListTag();
        tag.add(IntTag.valueOf(vec.getX()));
        tag.add(IntTag.valueOf(vec.getY()));
        tag.add(IntTag.valueOf(vec.getZ()));
        return tag;
    }

    public static Vec3i readVec3i(ListTag tag) {
        return new Vec3i(tag.getInt(0), tag.getInt(1), tag.getInt(2));
    }

    public static ListTag writeVec3(Vec3 vec) {
        ListTag tag = new ListTag();
        tag.add(DoubleTag.valueOf(vec.x));
        tag.add(DoubleTag.valueOf(vec.y));
        tag.add(DoubleTag.valueOf(vec.z));
        return tag;
    }

    public static Vec3 readVec3(ListTag tag) {
        return new Vec3(tag.getDouble(0), tag.getDouble(1), tag.getDouble(2));
    }

    public static ListTag writeVector3d(Vector3d vec) {
        ListTag tag = new ListTag();
        tag.add(DoubleTag.valueOf(vec.x));
        tag.add(DoubleTag.valueOf(vec.y));
        tag.add(DoubleTag.valueOf(vec.z));
        return tag;
    }

    public static Vector3d readVector3d(ListTag tag) {
        return new Vector3d(tag.getDouble(0), tag.getDouble(1), tag.getDouble(2));
    }

    public static Vector3dc readVector3dc(ListTag list, Vector3d dest) {
        dest.x = list.getDouble(0);
        dest.y = list.getDouble(1);
        dest.z = list.getDouble(2);
        return dest;
    }

    public static Tag getINBT(CompoundTag nbt, String id) {
        Tag inbt = nbt.get(id);
        if (inbt != null)
            return inbt;
        return new CompoundTag();
    }

    public static CompoundTag intToCompound(int i) {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putInt("V", i);
        return compoundTag;
    }

    public static int intFromCompound(CompoundTag compoundTag) {
        return compoundTag.getInt("V");
    }

    public static void writeResourceLocation(CompoundTag nbt, String key, ResourceLocation location) {
        nbt.putString(key, location.toString());
    }

    public static ResourceLocation readResourceLocation(CompoundTag nbt, String key) {
        return ResourceLocation.parse(nbt.getString(key));
    }

}