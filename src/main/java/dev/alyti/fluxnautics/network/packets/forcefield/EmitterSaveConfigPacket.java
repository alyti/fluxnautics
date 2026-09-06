package dev.alyti.fluxnautics.network.packets.forcefield;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.content.blocks.forcefield.ForcefieldBubbleEmitterConfig;
import dev.alyti.fluxnautics.content.blocks.forcefield.ForcefieldEmitterBlockEntity;
import mekanism.common.lib.security.BlockSecurityUtils;
import mekanism.common.network.IMekanismPacket;
import mekanism.common.network.PacketUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record EmitterSaveConfigPacket(BlockPos tilePosition, ForcefieldBubbleEmitterConfig config) implements IMekanismPacket {

    public static final CustomPacketPayload.Type<EmitterSaveConfigPacket> TYPE = new CustomPacketPayload.Type<>(Fluxnautics.id("forcefield_emitter_save_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EmitterSaveConfigPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, EmitterSaveConfigPacket::tilePosition,
            ForcefieldBubbleEmitterConfig.STREAM_CODEC, EmitterSaveConfigPacket::config,
            EmitterSaveConfigPacket::new
    );

    @NotNull
    @Override
    public CustomPacketPayload.Type<EmitterSaveConfigPacket> type() {
        return TYPE;
    }

    @Override
    public void handle(IPayloadContext context) {
        if (PacketUtils.blockEntity(context, tilePosition) instanceof ForcefieldEmitterBlockEntity tile) {
            assert tile.getLevel() != null;
            if (BlockSecurityUtils.INSTANCE.canAccess(context.player(), tile.getLevel(), tile.getBlockPos(), tile.getBlockState(), tile))
                tile.setConfig(config);
        }
    }

}
