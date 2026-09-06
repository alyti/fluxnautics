package dev.alyti.fluxnautics.network.packets.meka_robit;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.content.entities.gear.EntityMekaRobit;
import mekanism.common.network.IMekanismPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ServerboundDroneRecallPacket(int droneId) implements IMekanismPacket {

    public static final Type<ServerboundDroneRecallPacket> TYPE = new Type<>(Fluxnautics.id("drone_recall"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundDroneRecallPacket> STREAM_CODEC = StreamCodec.of(
        (buf, value) -> buf.writeInt(value.droneId),
        buf -> new ServerboundDroneRecallPacket(buf.readInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }


    @Override
    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player.level().getEntity(droneId) instanceof EntityMekaRobit drone) {
                if (drone == null || drone.getOwnerUUID() == null || !drone.getOwnerUUID().equals(player.getUUID()))
                    return;
                drone.discard();
            }
        });
    }
}