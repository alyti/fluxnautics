package dev.alyti.fluxnautics.network;

import dev.alyti.fluxnautics.network.packets.*;
import dev.alyti.fluxnautics.network.packets.blueprint.BlueprintDataPacket;
import dev.alyti.fluxnautics.network.packets.blueprint.RequestBlueprintDataPacket;
import dev.alyti.fluxnautics.network.packets.blueprint.UpdateBlueprintConfigPacket;
import dev.alyti.fluxnautics.network.packets.conduit.ClientboundConduitDataPacket;
import dev.alyti.fluxnautics.network.packets.conduit.ClientboundConduitStoppedPacket;
import dev.alyti.fluxnautics.network.packets.forcefield.EmitterSaveConfigPacket;
import dev.alyti.fluxnautics.network.packets.manipulator.ServerboundManipulatorDragPacket;
import dev.alyti.fluxnautics.network.packets.manipulator.ServerboundManipulatorStopPacket;
import dev.alyti.fluxnautics.network.packets.manipulator.ServerboundManipulatorThrowPacket;
import dev.alyti.fluxnautics.network.packets.meka_robit.ServerboundDroneAssistPacket;
import dev.alyti.fluxnautics.network.packets.meka_robit.ServerboundDroneDataPacket;
import dev.alyti.fluxnautics.network.packets.meka_robit.ServerboundDroneStartStopPacket;
import mekanism.common.lib.Version;
import mekanism.common.network.BasePacketHandler;
import net.neoforged.bus.api.IEventBus;

public class FluxPacketHandler extends BasePacketHandler {


    public FluxPacketHandler(IEventBus modEventBus, Version version) {
        super(modEventBus, version);
    }

    @Override
    protected void registerClientToServer(PacketRegistrar registrar) {
        registrar.play(PacketSetRedstoneMode.TYPE, PacketSetRedstoneMode.STREAM_CODEC);

        registrar.play(PacketUpdateGroupSettings.TYPE, PacketUpdateGroupSettings.STREAM_CODEC);
        registrar.play(PacketFluxGuiInteract.TYPE, PacketFluxGuiInteract.STREAM_CODEC);
        registrar.play(EmitterSaveConfigPacket.TYPE, EmitterSaveConfigPacket.STREAM_CODEC);
        registrar.play(RequestBlueprintDataPacket.TYPE, RequestBlueprintDataPacket.STREAM_CODEC);
        registrar.play(UpdateBlueprintConfigPacket.TYPE, UpdateBlueprintConfigPacket.STREAM_CODEC);
        registrar.play(StabilizerSaveConfigPacket.TYPE, StabilizerSaveConfigPacket.STREAM_CODEC);
        registrar.play(FluxTetherSaveConfigPacket.TYPE, FluxTetherSaveConfigPacket.STREAM_CODEC);
        registrar.play(InertiaDampenerSaveConfigPacket.TYPE, InertiaDampenerSaveConfigPacket.STREAM_CODEC);

        registrar.play(ServerboundDroneStartStopPacket.TYPE, ServerboundDroneStartStopPacket.STREAM_CODEC);
        registrar.play(ServerboundDroneDataPacket.TYPE, ServerboundDroneDataPacket.STREAM_CODEC);
        registrar.play(ServerboundDroneAssistPacket.TYPE, ServerboundDroneAssistPacket.STREAM_CODEC);

        registrar.play(ServerboundManipulatorDragPacket.TYPE, ServerboundManipulatorDragPacket.STREAM_CODEC);
        registrar.play(ServerboundManipulatorStopPacket.TYPE, ServerboundManipulatorStopPacket.STREAM_CODEC);
        registrar.play(ServerboundManipulatorThrowPacket.TYPE, ServerboundManipulatorThrowPacket.STREAM_CODEC);
    }

    @Override
    protected void registerServerToClient(PacketRegistrar registrar) {
        registrar.play(CloakingSyncPacket.TYPE, CloakingSyncPacket.STREAM_CODEC);
        registrar.play(BlueprintDataPacket.TYPE, BlueprintDataPacket.STREAM_CODEC);
        registrar.play(ClientboundConduitDataPacket.TYPE, ClientboundConduitDataPacket.STREAM_CODEC);
        registrar.play(ClientboundConduitStoppedPacket.TYPE, ClientboundConduitStoppedPacket.STREAM_CODEC);
        registrar.play(ThrusterPlacementSyncPacket.TYPE, ThrusterPlacementSyncPacket.STREAM_CODEC);
    }
}
