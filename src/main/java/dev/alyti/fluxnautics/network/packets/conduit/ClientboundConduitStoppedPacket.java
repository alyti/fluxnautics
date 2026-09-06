package dev.alyti.fluxnautics.network.packets.conduit;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.content.blocks.conduit.AbstractConduitHolderBlockEntity;
import dev.alyti.fluxnautics.content.blocks.conduit.TileComponentConduitStrandHolder;
import foundry.veil.api.network.handler.ClientPacketContext;
import io.netty.buffer.ByteBuf;
import mekanism.common.network.IMekanismPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;


public record ClientboundConduitStoppedPacket(BlockPos ownerPos) implements IMekanismPacket {
    public static final StreamCodec<ByteBuf, ClientboundConduitStoppedPacket> STREAM_CODEC = BlockPos.STREAM_CODEC.map(ClientboundConduitStoppedPacket::new, ClientboundConduitStoppedPacket::ownerPos);
    public static Type<ClientboundConduitStoppedPacket> TYPE = new Type<>(Fluxnautics.id("conduit_stopped"));

    public void handle(final ClientPacketContext context) {
        handleClient(context.player().level());
    }

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handle(IPayloadContext context) {
        handleClient(context.player().level());
    }

    private void handleClient(final Level level) {
        final BlockEntity blockEntity = level.getBlockEntity(this.ownerPos);

        if (!(blockEntity instanceof final AbstractConduitHolderBlockEntity smartBlockEntity)) {
            return;
        }

        final TileComponentConduitStrandHolder<?> ropeHolder = smartBlockEntity.getStrandHolder();

        if (ropeHolder == null) {
            return;
        }

        ropeHolder.receiveClientStrandStopped();
    }
}
