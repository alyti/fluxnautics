package dev.alyti.fluxnautics.network.packets.manipulator;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.content.items.manipulator.MekaToolClientHandler;
import dev.alyti.fluxnautics.content.items.manipulator.MekaToolServerHandler;
import foundry.veil.api.network.VeilPacketManager;
import mekanism.common.network.IMekanismPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.UUID;

public record ServerboundManipulatorDragPacket(
        int typeId, UUID targetId,
        double gX, double gY, double gZ,
        double lX, double lY, double lZ,
        double oX, double oY, double oZ, double oW
) implements IMekanismPacket {

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundManipulatorDragPacket> STREAM_CODEC = StreamCodec.of((buf, value) -> value.write(buf), ServerboundManipulatorDragPacket::read);
    public static Type<ServerboundManipulatorDragPacket> TYPE = new Type<>(Fluxnautics.id("manipulator_drag"));

    @NotNull
    @Override
    public CustomPacketPayload.Type<ServerboundManipulatorDragPacket> type() {
        return TYPE;
    }

    public static void send(MekaToolClientHandler.TargetType type, UUID targetId, Vector3dc globalAnchor, Vector3dc localAnchor, Quaterniondc orientation) {
        VeilPacketManager.server().sendPacket(new ServerboundManipulatorDragPacket(
                type.ordinal(), targetId,
                globalAnchor.x(), globalAnchor.y(), globalAnchor.z(),
                localAnchor.x(), localAnchor.y(), localAnchor.z(),
                orientation.x(), orientation.y(), orientation.z(), orientation.w()
        ));
    }

    private static ServerboundManipulatorDragPacket read(final RegistryFriendlyByteBuf buf) {
        return new ServerboundManipulatorDragPacket(
                buf.readInt(),
                buf.readUUID(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble()
        );
    }

    private void write(final RegistryFriendlyByteBuf buf) {
        buf.writeInt(typeId);
        buf.writeUUID(targetId);
        buf.writeDouble(gX); buf.writeDouble(gY); buf.writeDouble(gZ);
        buf.writeDouble(lX); buf.writeDouble(lY); buf.writeDouble(lZ);
        buf.writeDouble(oX); buf.writeDouble(oY); buf.writeDouble(oZ); buf.writeDouble(oW);
    }

    @Override
    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            final ServerPlayer serverPlayer = (ServerPlayer) context.player();
            final MekaToolClientHandler.TargetType[] types = MekaToolClientHandler.TargetType.values();
            if (typeId < 0 || typeId >= types.length) return;
            if (!isFinite(gX, gY, gZ) || !isFinite(lX, lY, lZ) || !isFinite(oX, oY, oZ, oW)) return;
            MekaToolServerHandler.get(serverPlayer.serverLevel()).drag(
                    serverPlayer, types[typeId], targetId,
                    new Vector3d(gX, gY, gZ),
                    new Vector3d(lX, lY, lZ),
                    new Quaterniond(oX, oY, oZ, oW)
            );
        });
    }

    private static boolean isFinite(double... values) {
        for (double v : values) {
            if (!Double.isFinite(v)) return false;
        }
        return true;
    }
}