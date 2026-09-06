package dev.alyti.fluxnautics.registry;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.content.blocks.daemon_protocol.DaemonProtocolBlockEntity;
import dev.alyti.fluxnautics.content.blocks.antigrav.AntiGravBlockEntity;
import dev.alyti.fluxnautics.content.blocks.assembly_terminal.AssemblyTerminalBlockEntity;
import dev.alyti.fluxnautics.content.blocks.cloak.CloakGeneratorBlockEntity;
import dev.alyti.fluxnautics.content.blocks.conduit.ConduitConnectorBlockEntity;
import dev.alyti.fluxnautics.content.blocks.conduit.ConduitPortBlockEntity;
import dev.alyti.fluxnautics.content.blocks.field.FieldDriveBlockEntity;
import dev.alyti.fluxnautics.content.blocks.forcefield.ForcefieldEmitterBlockEntity;
import dev.alyti.fluxnautics.content.blocks.forcefield.ForcefieldHarmonizerBlockEntity;
import dev.alyti.fluxnautics.content.blocks.holomap.HoloBlockEntity;
import dev.alyti.fluxnautics.content.blocks.flux_tether.FluxTetherBlockEntity;
import dev.alyti.fluxnautics.content.blocks.flux_tether.link_block.FluxTetherLinkBlockEntity;
import dev.alyti.fluxnautics.content.blocks.inertia_dampener.InertiaDampenerBlockEntity;
import dev.alyti.fluxnautics.content.blocks.spatial_anchor.SpatialAnchorBlockEntity;
import dev.alyti.fluxnautics.content.blocks.stabilizer.StabilizerBlockEntity;
import dev.alyti.fluxnautics.content.blocks.thruster.*;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.registration.impl.TileEntityTypeDeferredRegister;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.tile.base.TileEntityMekanism;

public class FNBlockEntityTypes {


    public static final TileEntityTypeDeferredRegister BLOCK_ENTITY_TYPES = new TileEntityTypeDeferredRegister(Fluxnautics.MODID);

    public static final TileEntityTypeRegistryObject<FieldDriveBlockEntity> FIELD_DRIVE = BLOCK_ENTITY_TYPES.mekBuilder(FNBlocks.FIELD_DRIVE, FieldDriveBlockEntity::new)
        .serverTicker(TileEntityMekanism::tickServer).clientTicker(TileEntityMekanism::tickClient).withSimple(Capabilities.CONFIGURABLE).build();

    public static final TileEntityTypeRegistryObject<HoloBlockEntity> HOLO = BLOCK_ENTITY_TYPES.mekBuilder(FNBlocks.HOLO, HoloBlockEntity::new)
        .serverTicker(TileEntityMekanism::tickServer).clientTicker(TileEntityMekanism::tickClient).withSimple(Capabilities.CONFIGURABLE).build();

    public static final TileEntityTypeRegistryObject<StabilizerBlockEntity> STABILIZER = BLOCK_ENTITY_TYPES.mekBuilder(FNBlocks.STABILIZER, StabilizerBlockEntity::new)
        .serverTicker(TileEntityMekanism::tickServer).clientTicker(TileEntityMekanism::tickClient).withSimple(Capabilities.CONFIGURABLE).build();

    public static final TileEntityTypeRegistryObject<DaemonProtocolBlockEntity> DAEMON_PROTOCOL = BLOCK_ENTITY_TYPES.mekBuilder(FNBlocks.DAEMON_PROTOCOL, DaemonProtocolBlockEntity::new)
        .serverTicker(TileEntityMekanism::tickServer).clientTicker(TileEntityMekanism::tickClient).withSimple(Capabilities.CONFIGURABLE).build();

    public static final TileEntityTypeRegistryObject<InertiaDampenerBlockEntity> INERTIA_DAMPENER = BLOCK_ENTITY_TYPES.mekBuilder(FNBlocks.INERTIA_DAMPENER, InertiaDampenerBlockEntity::new)
        .serverTicker(TileEntityMekanism::tickServer).clientTicker(TileEntityMekanism::tickClient).withSimple(Capabilities.CONFIGURABLE).withSimple(Capabilities.CONFIG_CARD).build();

    public static final TileEntityTypeRegistryObject<FluxTetherBlockEntity> FLUX_TETHER = BLOCK_ENTITY_TYPES.mekBuilder(FNBlocks.FLUX_TETHER, FluxTetherBlockEntity::new)
        .serverTicker(TileEntityMekanism::tickServer).clientTicker(TileEntityMekanism::tickClient).withSimple(Capabilities.CONFIGURABLE).withSimple(Capabilities.CONFIG_CARD).build();

    public static final TileEntityTypeRegistryObject<FluxTetherLinkBlockEntity> FLUX_TETHER_LINK = BLOCK_ENTITY_TYPES.mekBuilder(FNBlocks.FLUX_TETHER_LINK, FluxTetherLinkBlockEntity::new)
        .serverTicker(TileEntityMekanism::tickServer).clientTicker(TileEntityMekanism::tickClient).build();

    public static final TileEntityTypeRegistryObject<AssemblyTerminalBlockEntity> ASSEMBLY_TERMINAL = BLOCK_ENTITY_TYPES.mekBuilder(FNBlocks.ASSEMBLY_TERMINAL, AssemblyTerminalBlockEntity::new)
        .serverTicker(TileEntityMekanism::tickServer).clientTicker(TileEntityMekanism::tickClient).withSimple(Capabilities.CONFIGURABLE).build();

    public static final TileEntityTypeRegistryObject<ConduitConnectorBlockEntity> CONDUIT_CONNECTOR = BLOCK_ENTITY_TYPES.mekBuilder(FNBlocks.CONDUIT_CONNECTOR, ConduitConnectorBlockEntity::new)
        .serverTicker(TileEntityMekanism::tickServer).clientTicker(TileEntityMekanism::tickClient).withSimple(Capabilities.CONFIGURABLE).build();

    public static final TileEntityTypeRegistryObject<ConduitPortBlockEntity> CONDUIT_PORT = BLOCK_ENTITY_TYPES.mekBuilder(FNBlocks.CONDUIT_PORT, ConduitPortBlockEntity::new)
        .serverTicker(TileEntityMekanism::tickServer).clientTicker(TileEntityMekanism::tickClient).withSimple(Capabilities.CONFIGURABLE).build();

    public static final TileEntityTypeRegistryObject<SpatialAnchorBlockEntity> SPATIAL_ANCHOR = BLOCK_ENTITY_TYPES.mekBuilder(FNBlocks.SPATIAL_ANCHOR, SpatialAnchorBlockEntity::new)
        .serverTicker(TileEntityMekanism::tickServer).clientTicker(TileEntityMekanism::tickClient).withSimple(Capabilities.CONFIGURABLE).build();

    public static final TileEntityTypeRegistryObject<ForcefieldEmitterBlockEntity> FORCEFIELD_EMITTER = BLOCK_ENTITY_TYPES.mekBuilder(FNBlocks.FORCEFIELD_EMITTER, ForcefieldEmitterBlockEntity::new)
        .serverTicker(TileEntityMekanism::tickServer).clientTicker(TileEntityMekanism::tickClient).withSimple(Capabilities.CONFIGURABLE).withSimple(Capabilities.CONFIG_CARD).build();

    public static final TileEntityTypeRegistryObject<ForcefieldHarmonizerBlockEntity> FORCEFIELD_HARMONIZER = BLOCK_ENTITY_TYPES.mekBuilder(FNBlocks.FORCEFIELD_HARMONIZER, ForcefieldHarmonizerBlockEntity::new)
        .serverTicker(TileEntityMekanism::tickServer).clientTicker(TileEntityMekanism::tickClient).withSimple(Capabilities.CONFIGURABLE).withSimple(Capabilities.CONFIG_CARD).build();

    public static final TileEntityTypeRegistryObject<CloakGeneratorBlockEntity> CLOAK_GENERATOR = BLOCK_ENTITY_TYPES.mekBuilder(FNBlocks.CLOAK_GENERATOR, CloakGeneratorBlockEntity::new)
        .serverTicker(TileEntityMekanism::tickServer).clientTicker(TileEntityMekanism::tickClient).withSimple(Capabilities.CONFIGURABLE).withSimple(Capabilities.CONFIG_CARD).build();

    public static final TileEntityTypeRegistryObject<AntiGravBlockEntity> ANTI_GRAV = BLOCK_ENTITY_TYPES.mekBuilder(FNBlocks.ANTI_GRAV, AntiGravBlockEntity::new)
        .serverTicker(TileEntityMekanism::tickServer).clientTicker(TileEntityMekanism::tickClient).withSimple(Capabilities.CONFIGURABLE).build();

    public static final TileEntityTypeRegistryObject<ThrusterArrayStructuralBlockEntity> THRUSTER_ARRAY_STRUCTURAL = BLOCK_ENTITY_TYPES.mekBuilder(FNBlocks.THRUSTER_ARRAY_STRUCTURAL, ThrusterArrayStructuralBlockEntity::new)
        .serverTicker(TileEntityMekanism::tickServer).clientTicker(TileEntityMekanism::tickClient).withSimple(Capabilities.CONFIGURABLE).build();

    public static final TileEntityTypeRegistryObject<ThrusterArrayControllerBlockEntity> THRUSTER_ARRAY_CONTROLLER = BLOCK_ENTITY_TYPES.mekBuilder(FNBlocks.THRUSTER_ARRAY_CONTROLLER, ThrusterArrayControllerBlockEntity::new)
        .serverTicker(TileEntityMekanism::tickServer).clientTicker(TileEntityMekanism::tickClient).withSimple(Capabilities.CONFIGURABLE).build();

    public static final TileEntityTypeRegistryObject<ThrusterArraySubControllerBlockEntity> THRUSTER_ARRAY_SUBCONTROLLER = BLOCK_ENTITY_TYPES.builder(FNBlocks.THRUSTER_ARRAY_SUBCONTROLLER, ThrusterArraySubControllerBlockEntity::new)
        .serverTicker(TileEntityMekanism::tickServer).clientTicker(TileEntityMekanism::tickClient)
        .with(Capabilities.CONFIGURABLE, ThrusterArraySubControllerBlockEntity.CONFIGURABLE_PROVIDER)
        .build();

    public static final TileEntityTypeRegistryObject<IonThrusterBlockEntity> ION_THRUSTER = BLOCK_ENTITY_TYPES.mekBuilder(FNBlocks.ION_THRUSTER, IonThrusterBlockEntity::new)
        .serverTicker(TileEntityMekanism::tickServer).clientTicker(TileEntityMekanism::tickClient).withSimple(Capabilities.CONFIGURABLE).withSimple(Capabilities.CONFIG_CARD).build();

    public static final TileEntityTypeRegistryObject<ThrusterArrayBoosterBlockEntity> THRUSTER_ARRAY_BOOSTER_BASIC = BLOCK_ENTITY_TYPES.mekBuilder(FNBlocks.THRUSTER_ARRAY_BOOSTER_BASIC, (pos, state) -> new ThrusterArrayBoosterBlockEntity(FNBlocks.THRUSTER_ARRAY_BOOSTER_BASIC, pos, state)).serverTicker(TileEntityMekanism::tickServer).clientTicker(TileEntityMekanism::tickClient).withSimple(Capabilities.CONFIGURABLE).build();
    public static final TileEntityTypeRegistryObject<ThrusterArrayBoosterBlockEntity> THRUSTER_ARRAY_BOOSTER_ADVANCED = BLOCK_ENTITY_TYPES.mekBuilder(FNBlocks.THRUSTER_ARRAY_BOOSTER_ADVANCED, (pos, state) -> new ThrusterArrayBoosterBlockEntity(FNBlocks.THRUSTER_ARRAY_BOOSTER_ADVANCED, pos, state)).serverTicker(TileEntityMekanism::tickServer).clientTicker(TileEntityMekanism::tickClient).withSimple(Capabilities.CONFIGURABLE).build();
    public static final TileEntityTypeRegistryObject<ThrusterArrayBoosterBlockEntity> THRUSTER_ARRAY_BOOSTER_ELITE = BLOCK_ENTITY_TYPES.mekBuilder(FNBlocks.THRUSTER_ARRAY_BOOSTER_ELITE, (pos, state) -> new ThrusterArrayBoosterBlockEntity(FNBlocks.THRUSTER_ARRAY_BOOSTER_ELITE, pos, state)).serverTicker(TileEntityMekanism::tickServer).clientTicker(TileEntityMekanism::tickClient).withSimple(Capabilities.CONFIGURABLE).build();
    public static final TileEntityTypeRegistryObject<ThrusterArrayBoosterBlockEntity> THRUSTER_ARRAY_BOOSTER_ULTIMATE = BLOCK_ENTITY_TYPES.mekBuilder(FNBlocks.THRUSTER_ARRAY_BOOSTER_ULTIMATE, (pos, state) -> new ThrusterArrayBoosterBlockEntity(FNBlocks.THRUSTER_ARRAY_BOOSTER_ULTIMATE, pos, state)).serverTicker(TileEntityMekanism::tickServer).clientTicker(TileEntityMekanism::tickClient).withSimple(Capabilities.CONFIGURABLE).build();
    public static final TileEntityTypeRegistryObject<ThrusterArrayBoosterBlockEntity> THRUSTER_ARRAY_BOOSTER_CREATIVE = BLOCK_ENTITY_TYPES.mekBuilder(FNBlocks.THRUSTER_ARRAY_BOOSTER_CREATIVE, (pos, state) -> new ThrusterArrayBoosterBlockEntity(FNBlocks.THRUSTER_ARRAY_BOOSTER_CREATIVE, pos, state)).serverTicker(TileEntityMekanism::tickServer).clientTicker(TileEntityMekanism::tickClient).withSimple(Capabilities.CONFIGURABLE).build();

    public static final TileEntityTypeRegistryObject<ThrusterArrayCenterOfMassAlignmentBlockEntity> THRUSTER_ARRAY_ALIGNMENT_X = BLOCK_ENTITY_TYPES.mekBuilder(FNBlocks.THRUSTER_ARRAY_ALIGNMENT_X, (pos, state) -> new ThrusterArrayCenterOfMassAlignmentBlockEntity(FNBlocks.THRUSTER_ARRAY_ALIGNMENT_X, pos, state)).serverTicker(TileEntityMekanism::tickServer).clientTicker(TileEntityMekanism::tickClient).withSimple(Capabilities.CONFIGURABLE).build();
    public static final TileEntityTypeRegistryObject<ThrusterArrayCenterOfMassAlignmentBlockEntity> THRUSTER_ARRAY_ALIGNMENT_Y = BLOCK_ENTITY_TYPES.mekBuilder(FNBlocks.THRUSTER_ARRAY_ALIGNMENT_Y, (pos, state) -> new ThrusterArrayCenterOfMassAlignmentBlockEntity(FNBlocks.THRUSTER_ARRAY_ALIGNMENT_Y, pos, state)).serverTicker(TileEntityMekanism::tickServer).clientTicker(TileEntityMekanism::tickClient).withSimple(Capabilities.CONFIGURABLE).build();
    public static final TileEntityTypeRegistryObject<ThrusterArrayCenterOfMassAlignmentBlockEntity> THRUSTER_ARRAY_ALIGNMENT_Z = BLOCK_ENTITY_TYPES.mekBuilder(FNBlocks.THRUSTER_ARRAY_ALIGNMENT_Z, (pos, state) -> new ThrusterArrayCenterOfMassAlignmentBlockEntity(FNBlocks.THRUSTER_ARRAY_ALIGNMENT_Z, pos, state)).serverTicker(TileEntityMekanism::tickServer).clientTicker(TileEntityMekanism::tickClient).withSimple(Capabilities.CONFIGURABLE).build();
}
