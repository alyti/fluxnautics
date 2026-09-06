package dev.alyti.fluxnautics.network.packets;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.api.content.redstone.IRedstoneSideConfigurable;
import dev.alyti.fluxnautics.api.content.redstone.TileComponentRedstoneControl;
import mekanism.api.RelativeSide;
import mekanism.api.security.IBlockSecurityUtils;
import mekanism.common.network.IMekanismPacket;
import mekanism.common.network.PacketUtils;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

public class PacketSetRedstoneMode implements IMekanismPacket {

    public static final CustomPacketPayload.Type<PacketSetRedstoneMode> TYPE = new CustomPacketPayload.Type<>(Fluxnautics.id("set_redstone_mode"));

    public static final StreamCodec<FriendlyByteBuf, PacketSetRedstoneMode> STREAM_CODEC = StreamCodec.of(
          PacketSetRedstoneMode::encode, PacketSetRedstoneMode::decode);

    private final BlockPos pos;
    private final RelativeSide side;
    private final Action action;
    private final int data;

    public static PacketSetRedstoneMode cycle(BlockPos pos, RelativeSide side, boolean forward) {
        return new PacketSetRedstoneMode(pos, side, Action.CYCLE, forward ? 1 : -1);
    }

    public static PacketSetRedstoneMode setMode(BlockPos pos, RelativeSide side, int ordinal) {
        return new PacketSetRedstoneMode(pos, side, Action.SET_MODE, ordinal);
    }

    public static PacketSetRedstoneMode setInverted(BlockPos pos, RelativeSide side, boolean inverted) {
        return new PacketSetRedstoneMode(pos, side, Action.SET_INVERTED, inverted ? 1 : 0);
    }

    public static PacketSetRedstoneMode resetAll(BlockPos pos) {
        return new PacketSetRedstoneMode(pos, RelativeSide.FRONT, Action.RESET_ALL, 0);
    }

    private PacketSetRedstoneMode(BlockPos pos, RelativeSide side, Action action, int data) {
        this.pos = pos;
        this.side = side;
        this.action = action;
        this.data = data;
    }

    @Override
    public CustomPacketPayload.@NonNull Type<PacketSetRedstoneMode> type() {
        return TYPE;
    }

    private static void encode(FriendlyByteBuf buf, PacketSetRedstoneMode message) {
        buf.writeLong(message.pos.asLong());
        RelativeSide.STREAM_CODEC.encode(buf, message.side);
        buf.writeEnum(message.action);
        buf.writeVarInt(message.data);
    }

    private static PacketSetRedstoneMode decode(FriendlyByteBuf buf) {
        BlockPos pos = BlockPos.of(buf.readLong());
        RelativeSide side = RelativeSide.STREAM_CODEC.decode(buf);
        Action action = buf.readEnum(Action.class);
        int data = buf.readVarInt();
        return new PacketSetRedstoneMode(pos, side, action, data);
    }

    @Override
    public void handle(IPayloadContext context) {
        Player player = context.player();
        if (!(PacketUtils.blockEntity(context, pos) instanceof TileEntityMekanism tile)
              || !IBlockSecurityUtils.INSTANCE.canAccess(player, player.level(), pos)) {
            return;
        }
        if (!(tile instanceof IRedstoneSideConfigurable sideConfig) || sideConfig.getRedstoneConfig() == null) {
            return;
        }
        TileComponentRedstoneControl<?, ?> config = sideConfig.getRedstoneConfig();
        switch (action) {
            case CYCLE -> config.cycleMode(side, data);
            case SET_MODE -> config.setModeOrdinal(side, data);
            case SET_INVERTED -> config.setInverted(side, data == 1);
            case RESET_ALL -> config.resetAll();
        }
    }

    public enum Action {
        CYCLE,
        SET_MODE,
        SET_INVERTED,
        RESET_ALL
    }
}