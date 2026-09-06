package dev.alyti.fluxnautics.network.packets;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.client.CloakedSublevelCache;
import mekanism.common.network.IMekanismPacket;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


public record CloakingSyncPacket(HashMap<ResourceKey<Level>, Set<UUID>> cloaked) implements IMekanismPacket {

    public static final CustomPacketPayload.Type<CloakingSyncPacket> TYPE = new CustomPacketPayload.Type<>(Fluxnautics.id("cloaking_sync"));

    private static final StreamCodec<RegistryFriendlyByteBuf, Set<UUID>> UUID_SET_CODEC =
            ByteBufCodecs.collection(HashSet::new, UUIDUtil.STREAM_CODEC);

    private static final StreamCodec<RegistryFriendlyByteBuf, HashMap<ResourceKey<Level>, Set<UUID>>> CLOAKED_MAP_CODEC =
            ByteBufCodecs.map(
                    HashMap::new,
                    ResourceKey.streamCodec(Registries.DIMENSION),
                    UUID_SET_CODEC
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, CloakingSyncPacket> STREAM_CODEC =
            CLOAKED_MAP_CODEC.map(CloakingSyncPacket::new, CloakingSyncPacket::cloaked);

    @NotNull
    @Override
    public CustomPacketPayload.Type<CloakingSyncPacket> type() {
        return TYPE;
    }

    @Override
    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            CloakedSublevelCache.updateCloakedSublevels(cloaked());
        });
    }
}
