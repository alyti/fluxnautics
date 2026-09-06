package dev.alyti.fluxnautics.network.packets;

import java.util.ArrayList;
import java.util.List;

import dev.alyti.fluxnautics.content.blocks.thruster.ThrusterPlacementHelper;
import dev.alyti.fluxnautics.content.blocks.thruster.ThrusterRadials;
import io.netty.buffer.ByteBuf;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import dev.alyti.fluxnautics.Fluxnautics;
import mekanism.common.network.IMekanismPacket;

public record ThrusterPlacementSyncPacket(int facingOrdinal, int snapOrdinal, boolean groupAuto, int forcedGroup,
        boolean hasBound, ResourceLocation boundDimension, BlockPos boundPos, List<Integer> knownGroups)
        implements IMekanismPacket {

    public static final CustomPacketPayload.Type<ThrusterPlacementSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(Fluxnautics.id("thruster_placement_sync"));

    public static final StreamCodec<ByteBuf, ThrusterPlacementSyncPacket> STREAM_CODEC = StreamCodec.of(
            ThrusterPlacementSyncPacket::encode, ThrusterPlacementSyncPacket::decode);

    private static void encode(ByteBuf buf, ThrusterPlacementSyncPacket packet) {
        buf.writeInt(packet.facingOrdinal);
        buf.writeInt(packet.snapOrdinal);
        buf.writeBoolean(packet.groupAuto);
        buf.writeInt(packet.forcedGroup);
        buf.writeBoolean(packet.hasBound);
        if (packet.hasBound) {
            ResourceLocation.STREAM_CODEC.encode(buf, packet.boundDimension);
            BlockPos.STREAM_CODEC.encode(buf, packet.boundPos);
        }
        buf.writeInt(packet.knownGroups.size());
        for (int group : packet.knownGroups) {
            buf.writeInt(group);
        }
    }

    private static ThrusterPlacementSyncPacket decode(ByteBuf buf) {
        int facing = buf.readInt();
        int snap = buf.readInt();
        boolean groupAuto = buf.readBoolean();
        int forcedGroup = buf.readInt();
        boolean hasBound = buf.readBoolean();
        ResourceLocation dimension = ResourceLocation.withDefaultNamespace("overworld");
        BlockPos pos = BlockPos.ZERO;
        if (hasBound) {
            dimension = ResourceLocation.STREAM_CODEC.decode(buf);
            pos = BlockPos.STREAM_CODEC.decode(buf);
        }
        int groupCount = buf.readInt();
        List<Integer> groups = new ArrayList<>(groupCount);
        for (int i = 0; i < groupCount; i++) {
            groups.add(buf.readInt());
        }
        return new ThrusterPlacementSyncPacket(facing, snap, groupAuto, forcedGroup, hasBound, dimension, pos, groups);
    }

    public static ThrusterPlacementSyncPacket of(ThrusterPlacementHelper.ThrusterPlacementSettings settings) {
        return new ThrusterPlacementSyncPacket(settings.facing().ordinal(), settings.snap().ordinal(),
                settings.groupAuto(), settings.forcedGroup(), settings.isBound(),
                settings.bound() == null ? ResourceLocation.withDefaultNamespace("overworld") : settings.bound().dimension().location(),
                settings.bound() == null ? BlockPos.ZERO : settings.bound().controllerPos(),
                settings.knownGroups());
    }

    public static void send(ServerPlayer player, ThrusterPlacementHelper.ThrusterPlacementSettings settings) {
        PacketDistributor.sendToPlayer(player, of(settings));
    }

    @NotNull
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handle(IPayloadContext context) {
        ResourceKey<Level> dimension = hasBound
                ? ResourceKey.create(Registries.DIMENSION, boundDimension)
                : null;
        ThrusterPlacementHelper.ThrusterPlacementSettings.BoundArray bound = dimension == null
                ? null
                : new ThrusterPlacementHelper.ThrusterPlacementSettings.BoundArray(dimension, boundPos);
        ThrusterPlacementHelper.ThrusterPlacementSettings settings = new ThrusterPlacementHelper.ThrusterPlacementSettings(
                ThrusterRadials.ThrusterFacingMode.VALUES[Math.floorMod(facingOrdinal, ThrusterRadials.ThrusterFacingMode.VALUES.length)],
                ThrusterRadials.ThrusterSnapMode.VALUES[Math.floorMod(snapOrdinal, ThrusterRadials.ThrusterSnapMode.VALUES.length)],
                groupAuto, forcedGroup, bound, List.copyOf(knownGroups));
        ThrusterPlacementHelper.ThrusterPlacementSettings.setClient(settings);
    }
}
