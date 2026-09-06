package dev.alyti.fluxnautics.network.packets.conduit;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.content.blocks.conduit.AbstractConduitHolderBlockEntity;
import dev.alyti.fluxnautics.content.blocks.conduit.TileComponentConduitStrandHolder;
import dev.ryanhcode.sable.util.SableBufferUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mekanism.common.network.IMekanismPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.List;
import java.util.UUID;

public record ClientboundConduitDataPacket(int interpolationTick, BlockPos ownerPos,
                                           UUID uuid, List<Vector3d> points,
                                           @Nullable BlockPos startAttachmentPos,
                                           @Nullable BlockPos endAttachmentPos) implements IMekanismPacket {

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundConduitDataPacket> STREAM_CODEC = StreamCodec.of((buf, value) -> value.write(buf), ClientboundConduitDataPacket::read);
    public static Type<ClientboundConduitDataPacket> TYPE = new Type<>(Fluxnautics.id("conduit_data"));


    @NotNull
    @Override
    public CustomPacketPayload.Type<ClientboundConduitDataPacket> type() {
        return TYPE;
    }

    private static ClientboundConduitDataPacket read(final RegistryFriendlyByteBuf buf) {
        final int interpolationTick = buf.readInt();
        final BlockPos ownerPos = buf.readBlockPos();
        final UUID uuid = buf.readUUID();

        final int size = buf.readInt();
        final List<Vector3d> points = new ObjectArrayList<>(size);
        for (int i = 0; i < size; i++) {
            points.add(SableBufferUtils.read(buf, new Vector3d()));
        }

        final BlockPos startAttachment = buf.readBoolean() ? buf.readBlockPos() : null;
        final BlockPos endAttachment = buf.readBoolean() ? buf.readBlockPos() : null;

        return new ClientboundConduitDataPacket(interpolationTick, ownerPos, uuid, points, startAttachment, endAttachment);
    }

    private void write(final RegistryFriendlyByteBuf buf) {
        buf.writeInt(this.interpolationTick);
        buf.writeBlockPos(this.ownerPos);
        buf.writeUUID(this.uuid);

        buf.writeInt(this.points.size());
        for (final Vector3dc point : this.points) {
            SableBufferUtils.write(buf, point);
        }

        buf.writeBoolean(this.startAttachmentPos != null);
        if (this.startAttachmentPos != null) {
            buf.writeBlockPos(this.startAttachmentPos);
        }

        buf.writeBoolean(this.endAttachmentPos != null);
        if (this.endAttachmentPos != null) {
            buf.writeBlockPos(this.endAttachmentPos);
        }
    }

    @Override
    public void handle(IPayloadContext context) {
        final Level level = context.player().level();

        final BlockEntity blockEntity = level.getBlockEntity(this.ownerPos);

        if (!(blockEntity instanceof final AbstractConduitHolderBlockEntity smartBlockEntity)) {
            return;
        }

        final TileComponentConduitStrandHolder<?> ropeHolder = smartBlockEntity.getStrandHolder();

        if (ropeHolder == null) {
            return;
        }

        ropeHolder.receiveClientStrand(this.interpolationTick, this.points, this.uuid, this.startAttachmentPos, this.endAttachmentPos);
    }
}
