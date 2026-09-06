package dev.alyti.fluxnautics.network.packets.blueprint;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.content.blocks.assembly_terminal.AssemblyTerminalBlockEntity;
import dev.alyti.fluxnautics.content.blocks.assembly_terminal.BlueprintConfig;
import mekanism.common.lib.security.BlockSecurityUtils;
import mekanism.common.network.IMekanismPacket;
import mekanism.common.network.PacketUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record UpdateBlueprintConfigPacket(BlockPos tilePosition,
                                          BlueprintConfig config) implements IMekanismPacket {

    public static final CustomPacketPayload.Type<UpdateBlueprintConfigPacket> TYPE = new CustomPacketPayload.Type<>(Fluxnautics.id("blueprint_config_save"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateBlueprintConfigPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, UpdateBlueprintConfigPacket::tilePosition,
            BlueprintConfig.STREAM_CODEC, UpdateBlueprintConfigPacket::config,
            UpdateBlueprintConfigPacket::new
    );

    @NotNull
    @Override
    public CustomPacketPayload.Type<UpdateBlueprintConfigPacket> type() {
        return TYPE;
    }

    @Override
    public void handle(IPayloadContext context) {
        if (PacketUtils.blockEntity(context, tilePosition) instanceof AssemblyTerminalBlockEntity tile) {
            assert tile.getLevel() != null;
            if (BlockSecurityUtils.INSTANCE.canAccess(context.player(), tile.getLevel(), tile.getBlockPos(), tile.getBlockState(), tile))
                tile.setBlueprintConfig(config);
        }
    }

}
