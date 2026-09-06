package dev.alyti.fluxnautics.registry;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.content.blocks.daemon_protocol.DaemonProtocolBlockEntity;
import dev.alyti.fluxnautics.content.blocks.antigrav.AntiGravBlockEntity;
import dev.alyti.fluxnautics.content.blocks.assembly_terminal.AssemblyTerminalBlockEntity;
import dev.alyti.fluxnautics.content.blocks.cloak.CloakGeneratorBlockEntity;
import dev.alyti.fluxnautics.content.blocks.conduit.ConduitConnectorBlockEntity;
import dev.alyti.fluxnautics.content.blocks.field.FieldDriveBlockEntity;
import dev.alyti.fluxnautics.content.blocks.flux_tether.FluxTetherBlockEntity;
import dev.alyti.fluxnautics.content.blocks.forcefield.ForcefieldEmitterContainer;
import dev.alyti.fluxnautics.content.blocks.forcefield.ForcefieldHarmonizerBlockEntity;
//import dev.alyti.sable.fluxnautics.common.content.blocks.graygoo.GrayGooCoreBlockEntity;
import dev.alyti.fluxnautics.content.blocks.inertia_dampener.InertiaDampenerBlockEntity;
import dev.alyti.fluxnautics.content.blocks.spatial_anchor.SpatialAnchorBlockEntity;
import dev.alyti.fluxnautics.content.blocks.stabilizer.StabilizerBlockEntity;
import dev.alyti.fluxnautics.content.blocks.thruster.ThrusterArrayControllerBlockEntity;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.registration.impl.ContainerTypeDeferredRegister;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;

public class FNContainerTypes {

    public static final ContainerTypeDeferredRegister CONTAINER_TYPES = new ContainerTypeDeferredRegister(Fluxnautics.MODID);

    public static final ContainerTypeRegistryObject<MekanismTileContainer<ThrusterArrayControllerBlockEntity>> THRUSTER_ARRAY = CONTAINER_TYPES.custom(FNBlocks.THRUSTER_ARRAY_CONTROLLER, ThrusterArrayControllerBlockEntity.class).offset(45, 32).build();
    public static final ContainerTypeRegistryObject<MekanismTileContainer<FieldDriveBlockEntity>> FIELD_DRIVE = CONTAINER_TYPES.custom(FNBlocks.FIELD_DRIVE, FieldDriveBlockEntity.class).offset(0, 5).build();
    public static final ContainerTypeRegistryObject<MekanismTileContainer<SpatialAnchorBlockEntity>> SPATIAL_ANCHOR = CONTAINER_TYPES.custom(FNBlocks.SPATIAL_ANCHOR, SpatialAnchorBlockEntity.class).build();
    public static final ContainerTypeRegistryObject<MekanismTileContainer<AntiGravBlockEntity>> ANTI_GRAV = CONTAINER_TYPES.custom(FNBlocks.ANTI_GRAV, AntiGravBlockEntity.class).build();
    public static final ContainerTypeRegistryObject<MekanismTileContainer<ForcefieldHarmonizerBlockEntity>> FORCEFIELD_HARMONIZER = CONTAINER_TYPES.custom(FNBlocks.FORCEFIELD_HARMONIZER, ForcefieldHarmonizerBlockEntity.class).offset(30, 74).build();
    public static final ContainerTypeRegistryObject<ForcefieldEmitterContainer> FORCEFIELD_EMITTER = CONTAINER_TYPES.register(FNBlocks.FORCEFIELD_EMITTER, ForcefieldEmitterContainer::configContainer);
    public static final ContainerTypeRegistryObject<MekanismTileContainer<CloakGeneratorBlockEntity>> CLOAK_GENERATOR = CONTAINER_TYPES.custom(FNBlocks.CLOAK_GENERATOR, CloakGeneratorBlockEntity.class).build();
    public static final ContainerTypeRegistryObject<MekanismTileContainer<StabilizerBlockEntity>> STABILIZER = CONTAINER_TYPES.custom(FNBlocks.STABILIZER, StabilizerBlockEntity.class).offset(45, 92).build();
    public static final ContainerTypeRegistryObject<MekanismTileContainer<AssemblyTerminalBlockEntity>> ASSEMBLY_TERMINAL = CONTAINER_TYPES.custom(FNBlocks.ASSEMBLY_TERMINAL, AssemblyTerminalBlockEntity.class).offset(45, 32).build();
    public static final ContainerTypeRegistryObject<MekanismTileContainer<ConduitConnectorBlockEntity>> CONDUIT_CONNECTOR = CONTAINER_TYPES.custom(FNBlocks.CONDUIT_CONNECTOR, ConduitConnectorBlockEntity.class).offset(45, 92).build();
    public static final ContainerTypeRegistryObject<MekanismTileContainer<DaemonProtocolBlockEntity>> DAEMON_PROTOCOL = CONTAINER_TYPES.custom(FNBlocks.DAEMON_PROTOCOL, DaemonProtocolBlockEntity.class).build();
    public static final ContainerTypeRegistryObject<MekanismTileContainer<FluxTetherBlockEntity>> FLUX_TETHER = CONTAINER_TYPES.custom(FNBlocks.FLUX_TETHER, FluxTetherBlockEntity.class).offset(45, 180).build();
    public static final ContainerTypeRegistryObject<MekanismTileContainer<InertiaDampenerBlockEntity>> INERTIA_DAMPENER = CONTAINER_TYPES.custom(FNBlocks.INERTIA_DAMPENER, InertiaDampenerBlockEntity.class).offset(45, 80).build();

}