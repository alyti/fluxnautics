package dev.alyti.fluxnautics.content.blocks.conduit.strand.server;

import dev.alyti.fluxnautics.content.blocks.conduit.AbstractConduitHolderBlockEntity;
import dev.alyti.fluxnautics.content.blocks.conduit.TileComponentConduitStrandHolder;
import dev.ryanhcode.sable.api.sublevel.SubLevelTrackingPlugin;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ServerConduitTrackingSystem implements SubLevelTrackingPlugin {
    private final ServerLevel level;

    public ServerConduitTrackingSystem(final ServerLevel level) {
        this.level = level;
    }

    private ServerLevelConduitManager getRopeManager() {
        return ServerLevelConduitManager.getOrCreate(this.level);
    }

    @Override
    public Iterable<UUID> neededPlayers() {
        final ServerLevelConduitManager ropeManager = this.getRopeManager();
        final Collection<ServerConduitStrand> strands = ropeManager.getAllStrands();

        if (strands.isEmpty()) return List.of();

        final Set<UUID> players = new ObjectOpenHashSet<>();

        for (final ServerConduitStrand strand : strands) {
            if (!strand.isActive()) continue;

            strand.updatePose();
            if (!strand.needsSync() && strand.networkingStopped) {
                continue;
            }

            final ConduitAttachment attachment = strand.getAttachment(ConduitAttachmentPoint.START);
            final BlockPos block = attachment.blockAttachment();

            final BlockEntity blockEntity = level.getBlockEntity(block);

            if (!(blockEntity instanceof final AbstractConduitHolderBlockEntity smartBlockEntity)) continue;

            final TileComponentConduitStrandHolder<?> holder = smartBlockEntity.getStrandHolder();

            if (holder == null) {
                continue;
            }

            for (final ServerPlayer player : holder.getStrandTrackingPlayers()) {
                players.add(player.getUUID());
            }
        }

        return players;
    }

    @Override
    public void sendTrackingData(final int interpolationTick) {
        final ServerLevelConduitManager ropeManager = this.getRopeManager();
        final Iterable<ServerConduitStrand> strands = ropeManager.getAllStrands();

        for (final ServerConduitStrand strand : strands) {
            if (!strand.isActive()) continue;

            if (strand.needsSync()) {
                strand.networkingStopped = false;

                final ConduitAttachment attachment = strand.getAttachment(ConduitAttachmentPoint.START);
                final BlockPos block = attachment.blockAttachment();

                final BlockEntity blockEntity = level.getBlockEntity(block);

                if (!(blockEntity instanceof final AbstractConduitHolderBlockEntity smartBlockEntity)) continue;

                final TileComponentConduitStrandHolder<?> holder = smartBlockEntity.getStrandHolder();

                if (holder == null) {
                    continue;
                }

                holder.getStrandPacketSink().sendPacket(holder.makeUpdatePacket());
                strand.justSynced();
            } else if (!strand.networkingStopped) {
                strand.networkingStopped = true;

                final ConduitAttachment attachment = strand.getAttachment(ConduitAttachmentPoint.START);
                final BlockPos block = attachment.blockAttachment();
                final BlockEntity blockEntity = level.getBlockEntity(block);

                if (!(blockEntity instanceof final AbstractConduitHolderBlockEntity smartBlockEntity)) continue;

                final TileComponentConduitStrandHolder<?> holder = smartBlockEntity.getStrandHolder();

                if (holder == null) {
                    continue;
                }

                holder.getStrandPacketSink().sendPacket(holder.makeStopPacket());
            }
        }
    }
}