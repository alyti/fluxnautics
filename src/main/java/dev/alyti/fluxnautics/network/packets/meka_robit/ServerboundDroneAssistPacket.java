package dev.alyti.fluxnautics.network.packets.meka_robit;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.content.entities.gear.EntityMekaRobit;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.gear.IModuleContainer;
import mekanism.api.gear.IModuleHelper;
import mekanism.common.network.IMekanismPacket;
import mekanism.common.registries.MekanismModules;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;

import java.util.*;

public record ServerboundDroneAssistPacket(int droneId, boolean isAssisting, boolean isMining, boolean isDelete, BlockPos pos, int entityId, Vector3f visualPos) implements IMekanismPacket {

    public static final Type<ServerboundDroneAssistPacket> TYPE = new Type<>(Fluxnautics.id("drone_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundDroneAssistPacket> STREAM_CODEC = StreamCodec.of((buf, value) -> value.write(buf), ServerboundDroneAssistPacket::read);

    private static ServerboundDroneAssistPacket read(final RegistryFriendlyByteBuf buf) {

        return new ServerboundDroneAssistPacket(buf.readInt(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBlockPos(), buf.readInt(), buf.readVector3f());
    }

    private void write(final RegistryFriendlyByteBuf buf) {
        buf.writeInt(droneId);
        buf.writeBoolean(isAssisting);
        buf.writeBoolean(isMining);
        buf.writeBoolean(isDelete);
        buf.writeBlockPos(pos);
        buf.writeInt(entityId);
        buf.writeVector3f(visualPos);
    }

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            final ServerPlayer serverPlayer = (ServerPlayer) context.player();
            if (!(serverPlayer.level().getEntity(droneId) instanceof final EntityMekaRobit drone))
                return;

            final UUID ownerUuid = drone.getOwnerUUID();

            if (ownerUuid == null || !ownerUuid.equals(serverPlayer.getUUID()))
                return;

            if (isMining) {
                if (pos == null || !drone.isWithinAssistRange(pos)) {
                    return;
                }
            } else if (entityId > 0) {
                Entity assistTarget = serverPlayer.level().getEntity(entityId);
                if (assistTarget == null || !drone.isWithinAssistRange(assistTarget)) {
                    return;
                }
            }

            drone.isAssisting = isAssisting;
            drone.isAssistMining = isMining;
            drone.isAssistDelete = isDelete;
            drone.assistBlockPos = pos;
            drone.assistEntityId = entityId;
            drone.setLaserTarget(visualPos);
        });
    }
}