package dev.alyti.fluxnautics.network.packets.meka_robit;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.content.entities.gear.EntityMekaRobit;
import mekanism.common.network.IMekanismPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record ServerboundDroneDataPacket(int entityID, float forward, float strafe, float up, float yaw, float pitch) implements IMekanismPacket {

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundDroneDataPacket> STREAM_CODEC = StreamCodec.of((buf, value) -> value.write(buf), ServerboundDroneDataPacket::read);
    public static Type<ServerboundDroneDataPacket> TYPE = new Type<>(Fluxnautics.id("drone_data"));


    @NotNull
    @Override
    public CustomPacketPayload.Type<ServerboundDroneDataPacket> type() {
        return TYPE;
    }

    private static ServerboundDroneDataPacket read(final RegistryFriendlyByteBuf buf) {
        int entityID = buf.readInt();
        float forward = buf.readFloat();
        float strafe = buf.readFloat();
        float up = buf.readFloat();
        float yaw = buf.readFloat();
        float pitch = buf.readFloat();

        return new ServerboundDroneDataPacket(entityID, forward, strafe, up, yaw, pitch);
    }

    private void write(final RegistryFriendlyByteBuf buf) {
        buf.writeInt(entityID);
        buf.writeFloat(forward);
        buf.writeFloat(strafe);
        buf.writeFloat(up);
        buf.writeFloat(yaw);
        buf.writeFloat(pitch);
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
            if (drone.getMode() != EntityMekaRobit.MODE_FULL_CONTROL)
                return;

            if (!Float.isFinite(forward) || !Float.isFinite(strafe) || !Float.isFinite(up)
                    || !Float.isFinite(yaw) || !Float.isFinite(pitch))
                return;

            drone.clientYRot = Mth.wrapDegrees(yaw);
            drone.clientXRot = Mth.clamp(pitch, -180.0F, 180.0F);
            drone.setYRot(drone.clientYRot);
            drone.setXRot(drone.clientXRot);
            drone.yHeadRot  = drone.clientYRot;

            drone.clientForward = Mth.clamp(forward, -1.0F, 1.0F);
            drone.clientStrafe  = Mth.clamp(strafe, -1.0F, 1.0F);
            drone.clientUp      = Mth.clamp(up, -1.0F, 1.0F);
        });
    }
}
