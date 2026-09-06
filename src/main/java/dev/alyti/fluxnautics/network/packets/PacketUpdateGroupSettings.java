package dev.alyti.fluxnautics.network.packets;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.config.group.GroupConfig;
import dev.alyti.fluxnautics.content.blocks.thruster.ThrusterArrayControllerBlockEntity;
import dev.alyti.fluxnautics.multiblock.ThrusterArrayMultiblockData;
import io.netty.buffer.ByteBuf;
import mekanism.common.lib.security.BlockSecurityUtils;
import mekanism.common.network.IMekanismPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

public record PacketUpdateGroupSettings(BlockPos pos, int groupId, String configName, CompoundTag configData) implements IMekanismPacket {

    public static final CustomPacketPayload.Type<PacketUpdateGroupSettings> TYPE = new Type<>(Fluxnautics.id("update_group_settings"));
    public static final StreamCodec<ByteBuf, PacketUpdateGroupSettings> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PacketUpdateGroupSettings::pos,
            ByteBufCodecs.VAR_INT, PacketUpdateGroupSettings::groupId,
            ByteBufCodecs.STRING_UTF8, PacketUpdateGroupSettings::configName,
            ByteBufCodecs.COMPOUND_TAG, PacketUpdateGroupSettings::configData,
            PacketUpdateGroupSettings::new
    );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static final int MAX_GROUPS = 32;

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Level world = player.level();
            if (world.isLoaded(pos)) {
                BlockEntity tile = world.getBlockEntity(pos);
                if (tile instanceof ThrusterArrayControllerBlockEntity controller) {
//                    if (!BlockSecurityUtils.INSTANCE.canAccess(player, world, pos, controller.getBlockState(), controller)) {
//                        return;
//                    }
                    if (groupId < 0 || groupId > MAX_GROUPS) {
                        return;
                    }
                    ThrusterArrayMultiblockData multiblock = controller.getMultiblock();
                    if (multiblock.isFormed()) {
                        if (configName.equals("delete")) {
                            if (groupId >= multiblock.thrusters.groups.size()) {
                                return;
                            }
                            multiblock.thrusters.groups.remove(groupId);
                            for (ThrusterArrayMultiblockData.ThrusterData thruster : multiblock.thrusters.thrusterMap.values()) {
                                if (thruster.groupId == groupId)
                                    thruster.groupId = 0;
                            }
                            multiblock.markDirty();
                            return;
                        }
                        if (groupId > multiblock.thrusters.groups.size()) {
                            return;
                        }
                        ThrusterArrayMultiblockData.GroupData group = multiblock.thrusters.getOrCreateGroup(groupId);
                        GroupConfig<?, ?> config = group.getConfig(configName);
                        if (config != null) {
                            config.read(configData);
                            multiblock.markDirty();
                        }
                        group.markUserEdited();
                    }
                }
            }
        });
    }
}
