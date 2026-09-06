package dev.alyti.fluxnautics.network.packets;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.content.blocks.conduit.ConduitConnectorBlockEntity;
import dev.alyti.fluxnautics.content.blocks.field.FieldDriveBlockEntity;
import io.netty.buffer.ByteBuf;
import mekanism.common.lib.security.BlockSecurityUtils;
import mekanism.common.network.IMekanismPacket;
import mekanism.common.network.PacketUtils;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.ByIdMap;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.IntFunction;


public record PacketFluxGuiInteract(GuiFluxInteraction interaction, BlockPos tilePosition, CompoundTag configData) implements IMekanismPacket {

    public static final CustomPacketPayload.Type<PacketFluxGuiInteract> TYPE = new CustomPacketPayload.Type<>(Fluxnautics.id("gui_interact"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketFluxGuiInteract> STREAM_CODEC = StreamCodec.composite(
        GuiFluxInteraction.STREAM_CODEC, PacketFluxGuiInteract::interaction,
        BlockPos.STREAM_CODEC, packet -> packet.tilePosition,
        ByteBufCodecs.COMPOUND_TAG, PacketFluxGuiInteract::configData,
        PacketFluxGuiInteract::new
    );

    @NotNull
    @Override
    public CustomPacketPayload.Type<PacketFluxGuiInteract> type() {
        return TYPE;
    }

    @Override
    public void handle(IPayloadContext context) {
        if (PacketUtils.blockEntity(context, tilePosition) instanceof TileEntityMekanism tile
                && tile.getLevel() != null
                && BlockSecurityUtils.INSTANCE.canAccess(context.player(), tile.getLevel(), tile.getBlockPos(), tile.getBlockState(), tile)) {
            interaction.consume(tile, configData);
        }
    }

    public enum GuiFluxInteraction {
        IS_GLOBAL_BUTTON((tile, value) -> {
           if (tile instanceof FieldDriveBlockEntity fd) {
               fd.setGlobal(!fd.isGlobal());
           }
        }),
        IMPULSE_MULT((tile, value) -> {
            if (tile instanceof FieldDriveBlockEntity fd) {
                double v = value.getDouble("value");
                if (Double.isFinite(v)) fd.setImpulseMultiplier(v);
            }
        }),
        IMPULSE_VEC_X((tile, value) -> {
            if (tile instanceof FieldDriveBlockEntity fd) {
                double v = value.getDouble("value");
                if (Double.isFinite(v)) fd.setImpulseVectorX(v);
            }
        }),
        IMPULSE_VEC_Y((tile, value) -> {
            if (tile instanceof FieldDriveBlockEntity fd) {
                double v = value.getDouble("value");
                if (Double.isFinite(v)) fd.setImpulseVectorY(v);
            }
        }),
        IMPULSE_VEC_Z((tile, value) -> {
            if (tile instanceof FieldDriveBlockEntity fd) {
                double v = value.getDouble("value");
                if (Double.isFinite(v)) fd.setImpulseVectorZ(v);
            }
        }),
        CONDUIT_MOTOR((tile, value) -> {
           if (tile instanceof ConduitConnectorBlockEntity cc) {
               double v = value.getDouble("value");
               if (Double.isFinite(v)) cc.setMotorSpeed(v);
           }
        });

        public static final IntFunction<GuiFluxInteraction> BY_ID = ByIdMap.continuous(GuiFluxInteraction::ordinal, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
        public static final StreamCodec<ByteBuf, GuiFluxInteraction> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, GuiFluxInteraction::ordinal);

        private final BiConsumer<TileEntityMekanism, CompoundTag> consumerForTile;

        GuiFluxInteraction(BiConsumer<TileEntityMekanism, CompoundTag> consumerForTile) {
            this.consumerForTile = consumerForTile;
        }

        public void consume(TileEntityMekanism tile, CompoundTag value) {
            consumerForTile.accept(tile, value);
        }
    }
}