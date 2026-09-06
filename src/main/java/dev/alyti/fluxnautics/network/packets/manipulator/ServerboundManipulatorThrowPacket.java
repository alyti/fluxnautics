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
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.UUID;

public record ServerboundManipulatorThrowPacket(int typeId, UUID targetId, double vX, double vY, double vZ) implements IMekanismPacket {

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundManipulatorThrowPacket> STREAM_CODEC = StreamCodec.of((buf, value) -> value.write(buf), ServerboundManipulatorThrowPacket::read);
    public static Type<ServerboundManipulatorThrowPacket> TYPE = new Type<>(Fluxnautics.id("manipulator_throw"));

    @NotNull
    @Override
    public CustomPacketPayload.Type<ServerboundManipulatorThrowPacket> type() {
        return TYPE;
    }

    public static void send(MekaToolClientHandler.TargetType type, UUID id, Vector3dc impulse) {
        VeilPacketManager.server().sendPacket(new ServerboundManipulatorThrowPacket(type.ordinal(), id, impulse.x(), impulse.y(), impulse.z()));
    }

    private static ServerboundManipulatorThrowPacket read(final RegistryFriendlyByteBuf buf) {
        return new ServerboundManipulatorThrowPacket(buf.readInt(), buf.readUUID(), buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    private void write(final RegistryFriendlyByteBuf buf) {
        buf.writeInt(typeId);
        buf.writeUUID(targetId);
        buf.writeDouble(vX);
        buf.writeDouble(vY);
        buf.writeDouble(vZ);
    }

    @Override
    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            final ServerPlayer serverPlayer = (ServerPlayer) context.player();
            final MekaToolClientHandler.TargetType[] types = MekaToolClientHandler.TargetType.values();
            if (typeId < 0 || typeId >= types.length) return;
            if (!Double.isFinite(vX) || !Double.isFinite(vY) || !Double.isFinite(vZ)) return;
            MekaToolServerHandler.get(serverPlayer.serverLevel()).throwTarget(serverPlayer, types[typeId], targetId, new Vector3d(vX, vY, vZ));
        });
    }
}