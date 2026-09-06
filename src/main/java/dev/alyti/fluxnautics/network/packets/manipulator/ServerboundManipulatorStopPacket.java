package dev.alyti.fluxnautics.network.packets.manipulator;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.content.items.manipulator.MekaToolServerHandler;
import foundry.veil.api.network.VeilPacketManager;
import mekanism.common.network.IMekanismPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ServerboundManipulatorStopPacket() implements IMekanismPacket {

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundManipulatorStopPacket> STREAM_CODEC = StreamCodec.of((buf, value) -> value.write(buf), ServerboundManipulatorStopPacket::read);
    public static Type<ServerboundManipulatorStopPacket> TYPE = new Type<>(Fluxnautics.id("manipulator_stop"));

    @NotNull
    @Override
    public CustomPacketPayload.Type<ServerboundManipulatorStopPacket> type() {
        return TYPE;
    }

    public static void send() {
        VeilPacketManager.server().sendPacket(new ServerboundManipulatorStopPacket());
    }

    private static ServerboundManipulatorStopPacket read(final RegistryFriendlyByteBuf buf) {
        return new ServerboundManipulatorStopPacket();
    }

    private void write(final RegistryFriendlyByteBuf buf) {
        // No data to write
    }

    @Override
    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            final ServerPlayer serverPlayer = (ServerPlayer) context.player();
            MekaToolServerHandler.get(serverPlayer.serverLevel()).stopDragging(serverPlayer);
        });
    }
}