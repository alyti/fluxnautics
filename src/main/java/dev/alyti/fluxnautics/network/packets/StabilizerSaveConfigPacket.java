package dev.alyti.fluxnautics.network.packets;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.content.blocks.stabilizer.StabilizerBlockEntity;
import dev.alyti.fluxnautics.content.blocks.stabilizer.StabilizerConfig;
import mekanism.common.lib.security.BlockSecurityUtils;
import mekanism.common.network.IMekanismPacket;
import mekanism.common.network.PacketUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record StabilizerSaveConfigPacket(BlockPos tilePosition, StabilizerConfig config) implements IMekanismPacket {

    public static final Type<StabilizerSaveConfigPacket> TYPE = new Type<>(Fluxnautics.id("forcefield_stabilizer_save_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StabilizerSaveConfigPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, StabilizerSaveConfigPacket::tilePosition,
            StabilizerConfig.STREAM_CODEC, StabilizerSaveConfigPacket::config,
            StabilizerSaveConfigPacket::new
    );

    @NotNull
    @Override
    public CustomPacketPayload.Type<StabilizerSaveConfigPacket> type() {
        return TYPE;
    }

    @Override
    public void handle(IPayloadContext context) {
        if (PacketUtils.blockEntity(context, tilePosition) instanceof StabilizerBlockEntity tile) {
            assert tile.getLevel() != null;
            if (BlockSecurityUtils.INSTANCE.canAccess(context.player(), tile.getLevel(), tile.getBlockPos(), tile.getBlockState(), tile))
                tile.setConfig(config);
        }
    }

}
