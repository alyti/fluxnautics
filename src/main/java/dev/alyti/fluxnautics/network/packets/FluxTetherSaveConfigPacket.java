package dev.alyti.fluxnautics.network.packets;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.content.blocks.flux_tether.FluxTetherBlockEntity;
import dev.alyti.fluxnautics.content.blocks.flux_tether.FluxTetherConfig;
import mekanism.common.lib.security.BlockSecurityUtils;
import mekanism.common.network.IMekanismPacket;
import mekanism.common.network.PacketUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record FluxTetherSaveConfigPacket(BlockPos tilePosition, FluxTetherConfig config) implements IMekanismPacket {

    public static final Type<FluxTetherSaveConfigPacket> TYPE = new Type<>(Fluxnautics.id("flux_tether_save_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FluxTetherSaveConfigPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, FluxTetherSaveConfigPacket::tilePosition,
            FluxTetherConfig.STREAM_CODEC, FluxTetherSaveConfigPacket::config,
            FluxTetherSaveConfigPacket::new
    );

    @NotNull
    @Override
    public CustomPacketPayload.Type<FluxTetherSaveConfigPacket> type() {
        return TYPE;
    }

    @Override
    public void handle(IPayloadContext context) {
        if (PacketUtils.blockEntity(context, tilePosition) instanceof FluxTetherBlockEntity tile) {
            assert tile.getLevel() != null;
            if (BlockSecurityUtils.INSTANCE.canAccess(context.player(), tile.getLevel(), tile.getBlockPos(), tile.getBlockState(), tile))
                tile.setConfig(config);
        }
    }

}
