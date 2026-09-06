package dev.alyti.fluxnautics.network.packets;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.content.blocks.inertia_dampener.InertiaDampenerBlockEntity;
import dev.alyti.fluxnautics.content.blocks.inertia_dampener.InertiaDampenerConfig;
import mekanism.common.lib.security.BlockSecurityUtils;
import mekanism.common.network.IMekanismPacket;
import mekanism.common.network.PacketUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record InertiaDampenerSaveConfigPacket(BlockPos tilePosition, InertiaDampenerConfig.PartialUpdate update) implements IMekanismPacket {

    public static final Type<InertiaDampenerSaveConfigPacket> TYPE = new Type<>(Fluxnautics.id("inertia_dampener_save_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InertiaDampenerSaveConfigPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, InertiaDampenerSaveConfigPacket::tilePosition,
            InertiaDampenerConfig.PartialUpdate.STREAM_CODEC, InertiaDampenerSaveConfigPacket::update,
            InertiaDampenerSaveConfigPacket::new
    );

    @NotNull
    @Override
    public CustomPacketPayload.Type<InertiaDampenerSaveConfigPacket> type() {
        return TYPE;
    }

    @Override
    public void handle(IPayloadContext context) {
        if (PacketUtils.blockEntity(context, tilePosition) instanceof InertiaDampenerBlockEntity tile) {
            assert tile.getLevel() != null;
            if (BlockSecurityUtils.INSTANCE.canAccess(context.player(), tile.getLevel(), tile.getBlockPos(), tile.getBlockState(), tile))
                tile.applyConfiguration(update);
        }
    }
}