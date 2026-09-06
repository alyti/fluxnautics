package dev.alyti.fluxnautics.util;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class RegistryFilterList<T> {
    private final Set<TagKey<T>> tags;
    private final Set<ResourceKey<T>> keys;
    private @Nullable HolderSet<T> combined;

    private RegistryFilterList(Set<TagKey<T>> tags, Set<ResourceKey<T>> keys, @Nullable HolderSet<T> combined) {
        this.tags = tags;
        this.keys = keys;
        this.combined = combined;
    }

    public static <T> RegistryFilterList<T> empty() {
        return new RegistryFilterList<>(new ObjectOpenHashSet<>(), new ObjectOpenHashSet<>(), HolderSet.direct(List.of()));
    }

    public static <T> RegistryFilterList<T> create(Registry<T> registry, Set<TagKey<T>> tags, Set<ResourceKey<T>> keys) {
        Set<Holder<T>> allHolders = new ObjectOpenHashSet<>();
        for (TagKey<T> tag : tags) {
            for (Holder<T> holder : registry.getOrCreateTag(tag)) {
                allHolders.add(holder);
            }
        }
        for (ResourceKey<T> key : keys) {
            registry.getHolder(key).ifPresent(allHolders::add);
        }
        return new RegistryFilterList<>(tags, keys, HolderSet.direct(List.copyOf(allHolders)));
    }

    public boolean isEmpty() {
        return tags.isEmpty() && keys.isEmpty();
    }

    public boolean contains(Holder<T> holder) {
        return combined != null && combined.contains(holder);
    }

    public boolean contains(Registry<T> registry, T value) {
        return contains(registry.wrapAsHolder(value));
    }

    public Set<TagKey<T>> getTags() {
        return tags;
    }

    public Set<ResourceKey<T>> getKeys() {
        return keys;
    }

    public static <T> Codec<RegistryFilterList<T>> codec(Registry<T> registry) {
        Codec<Either<TagKey<T>, ResourceKey<T>>> entryCodec = Codec.STRING.xmap(
                s -> s.startsWith("#")
                        ? Either.left(TagKey.create(registry.key(), ResourceLocation.parse(s.substring(1))))
                        : Either.right(ResourceKey.create(registry.key(), ResourceLocation.parse(s))),
                e -> e.map(
                        t -> "#" + t.location().toString(),
                        k -> k.location().toString()
                )
        );

        return FNCodecUtil.ccCompatibleList(entryCodec).xmap(
                list -> {
                    Set<TagKey<T>> tags = new ObjectOpenHashSet<>();
                    Set<ResourceKey<T>> keys = new ObjectOpenHashSet<>();
                    for (Either<TagKey<T>, ResourceKey<T>> entry : list) {
                        entry.ifLeft(tags::add).ifRight(keys::add);
                    }
                    return create(registry, tags, keys);
                },
                rfl -> {
                    List<Either<TagKey<T>, ResourceKey<T>>> result = new ArrayList<>(rfl.tags.size() + rfl.keys.size());
                    for (TagKey<T> tag : rfl.tags) {
                        result.add(Either.left(tag));
                    }
                    for (ResourceKey<T> key : rfl.keys) {
                        result.add(Either.right(key));
                    }
                    return result;
                }
        );
    }

    public static <T> StreamCodec<ByteBuf, RegistryFilterList<T>> streamCodec(ResourceKey<? extends Registry<T>> registryKey) {
        return StreamCodec.of(
                (buf, rfl) -> {
                    ByteBufCodecs.VAR_INT.encode(buf, rfl.tags.size());
                    for (TagKey<T> tag : rfl.tags) {
                        ByteBufCodecs.STRING_UTF8.encode(buf, tag.location().toString());
                    }
                    ByteBufCodecs.VAR_INT.encode(buf, rfl.keys.size());
                    for (ResourceKey<T> key : rfl.keys) {
                        ByteBufCodecs.STRING_UTF8.encode(buf, key.location().toString());
                    }
                },
                (buf) -> {
                    int tagCount = ByteBufCodecs.VAR_INT.decode(buf);
                    Set<TagKey<T>> tags = new ObjectOpenHashSet<>(tagCount);
                    for (int i = 0; i < tagCount; i++) {
                        tags.add(TagKey.create(registryKey, ResourceLocation.parse(ByteBufCodecs.STRING_UTF8.decode(buf))));
                    }
                    int keyCount = ByteBufCodecs.VAR_INT.decode(buf);
                    Set<ResourceKey<T>> keys = new ObjectOpenHashSet<>(keyCount);
                    for (int i = 0; i < keyCount; i++) {
                        keys.add(ResourceKey.create(registryKey, ResourceLocation.parse(ByteBufCodecs.STRING_UTF8.decode(buf))));
                    }
                    return new RegistryFilterList<>(tags, keys, null);
                }
        );
    }

    public RegistryFilterList<T> resolveTags(Registry<T> registry) {
        if (combined != null) {
            return this;
        }
        return create(registry, tags, keys);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RegistryFilterList<?> that)) return false;
        return tags.equals(that.tags) && keys.equals(that.keys);
    }

    @Override
    public int hashCode() {
        return 31 * tags.hashCode() + keys.hashCode();
    }
}