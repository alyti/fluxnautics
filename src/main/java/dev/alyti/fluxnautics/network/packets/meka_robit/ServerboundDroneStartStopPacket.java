package dev.alyti.fluxnautics.network.packets.meka_robit;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.content.entities.gear.EntityMekaRobit;
import dev.alyti.fluxnautics.content.items.drone.MekaRobitItem;
import dev.alyti.fluxnautics.registry.FNDataComponents;
import mekanism.common.network.IMekanismPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public record ServerboundDroneStartStopPacket(int entityID, boolean start) implements IMekanismPacket {

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundDroneStartStopPacket> STREAM_CODEC = StreamCodec.of((buf, value) -> value.write(buf), ServerboundDroneStartStopPacket::read);
    public static Type<ServerboundDroneStartStopPacket> TYPE = new Type<>(Fluxnautics.id("drone_fpv_toggle"));


    @NotNull
    @Override
    public CustomPacketPayload.Type<ServerboundDroneStartStopPacket> type() {
        return TYPE;
    }

    private static ServerboundDroneStartStopPacket read(final RegistryFriendlyByteBuf buf) {
        int entityID = buf.readInt();
        boolean start = buf.readBoolean();

        return new ServerboundDroneStartStopPacket(entityID, start);
    }

    private void write(final RegistryFriendlyByteBuf buf) {
        buf.writeInt(entityID);
        buf.writeBoolean(start);
    }

    @Override
    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            final ServerPlayer serverPlayer = (ServerPlayer) context.player();
            if (!(serverPlayer.level().getEntity(entityID) instanceof final EntityMekaRobit drone))
                return;

            final UUID ownerUuid = drone.getOwnerUUID();

            if (ownerUuid == null || !ownerUuid.equals(serverPlayer.getUUID()))
                return;

            if (start) {
                if (!isHoldingManualDrone(serverPlayer, drone))
                    return;
                drone.enterFullControl();
            } else {
                drone.exitFullControl();
            }
        });
    }

    private static boolean isHoldingManualDrone(final Player player, final EntityMekaRobit drone) {
        final UUID droneUuid = drone.getUUID();
        for (final InteractionHand hand : InteractionHand.values()) {
            final ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof MekaRobitItem)) continue;

            final Optional<MekaRobitItem.DroneMode> mode = MekaRobitItem.getDroneMode(stack);
            if (mode.isPresent() && mode.get() == MekaRobitItem.DroneMode.MANUAL) {
                final UUID boundId = stack.get(FNDataComponents.MEKA_ROBIT_BOUND_ENTITY.get());
                if (boundId != null && boundId.equals(droneUuid)) return true;
            }
        }
        return false;
    }
}
