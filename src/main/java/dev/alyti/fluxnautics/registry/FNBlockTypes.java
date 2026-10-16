package dev.alyti.fluxnautics.registry;

import dev.alyti.fluxnautics.FluxnauticsConfig;
import dev.alyti.fluxnautics.attribute.AttributeAxis;
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
import dev.alyti.fluxnautics.index.BlockShapes;
import dev.alyti.fluxnautics.index.BoosterTier;
import dev.alyti.fluxnautics.lang.FNLang;
import mekanism.api.Upgrade;
import mekanism.common.block.attribute.*;
import mekanism.common.block.attribute.AttributeHasBounding.HandleBoundingBlock;
import mekanism.common.block.attribute.AttributeHasBounding.TriBooleanFunction;
import mekanism.common.content.blocktype.BlockTypeTile;
import mekanism.common.content.blocktype.Machine;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.registration.impl.BlockRegistryObject;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.function.Supplier;

public class FNBlockTypes {
    public static final Machine<FieldDriveBlockEntity> FIELD_DRIVE = Machine.MachineBuilder
        .createMachine(() -> FNBlockEntityTypes.FIELD_DRIVE, FNLang.DESCRIPTION_FIELD_DRIVE)
        .withEnergyConfig(FluxnauticsConfig.common.fieldDriveJoulesPerImpulseMult, FluxnauticsConfig.common.fieldDriveJoulesBaseStorage)
        .withSupportedUpgrades(Upgrade.SPEED)
        .withGui(() -> FNContainerTypes.FIELD_DRIVE)
        .with(AttributeCustomSelectionBox.JSON)
        .withSideConfig(TransmissionType.ENERGY, TransmissionType.CHEMICAL, TransmissionType.ITEM)
        .withCustomShape(BlockShapes.FIELD_DRIVE)
        .withBounding(new AttributeHasBounding.HandleBoundingBlock() {
            @Override
            public <DATA> boolean handle(Level level, BlockPos pos, BlockState state, DATA data, AttributeHasBounding.TriBooleanFunction<Level, BlockPos, DATA> consumer) {
                BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
                for (int y = 0; y <= 1; y++) {
                    if (y != 0) {
                        mutable.setWithOffset(pos, 0, y, 0);
                        if (!consumer.accept(level, mutable, data)) {
                            return false;
                        }
                    }
                }
                return true;
            }
        })
        .withComputerSupport("fieldDrive")
        .build();

    public static final Machine<HoloBlockEntity> HOLO = Machine.MachineBuilder
        .createMachine(() -> FNBlockEntityTypes.HOLO, FNLang.DESCRIPTION_HOLO)
        .withEnergyConfig(() -> 100L, () -> 200_000L)
        .withSupportedUpgrades(Upgrade.SPEED, Upgrade.ENERGY)
        .withCustomShape(BlockShapes.HOLO_TABLE)
        .with(AttributeCustomSelectionBox.JSON)
        .withBounding(new HandleBoundingBlock() {
            @Override
            public <DATA> boolean handle(Level level, BlockPos pos, BlockState state, DATA data, TriBooleanFunction<Level, BlockPos, DATA> consumer) {
                BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x != 0 || z != 0) {
                            mutable.setWithOffset(pos, x, 0, z);
                            if (!consumer.accept(level, mutable, data)) {
                                return false;
                            }
                        }
                    }
                }
                return true;
            }
        })
        .withComputerSupport("holo")
        .build();

    public static final Machine<StabilizerBlockEntity> STABILIZER = Machine.MachineBuilder
        .createMachine(() -> FNBlockEntityTypes.STABILIZER, FNLang.DESCRIPTION_STABILIZER)
        .withEnergyConfig(() -> 0L, () -> 1_248_000_000L)
        .withGui(() -> FNContainerTypes.STABILIZER)
        .withSupportedUpgrades(Upgrade.ENERGY)
        .with(new AttributeStateFacing(BlockStateProperties.FACING, AttributeStateFacing.FacePlacementType.SELECTED_FACE))
        .withSideConfig(TransmissionType.ENERGY, TransmissionType.ITEM)
        .withCustomShape(BlockShapes.SPATIAL_ANCHOR)
        .with(AttributeCustomSelectionBox.JSON)
        .without(AttributeParticleFX.class)
        .withComputerSupport("stabilizer")
        .build();

    public static final Machine<InertiaDampenerBlockEntity> INERTIA_DAMPENER = Machine.MachineBuilder
        .createMachine(() -> FNBlockEntityTypes.INERTIA_DAMPENER, FNLang.DESCRIPTION_INERTIA_DAMPENER)
        .withEnergyConfig(() -> 0L, () -> 640_000_000L)
        .withGui(() -> FNContainerTypes.INERTIA_DAMPENER)
        .withSupportedUpgrades(Upgrade.ENERGY, Upgrade.SPEED)
        .with(new AttributeStateFacing(BlockStateProperties.FACING, AttributeStateFacing.FacePlacementType.SELECTED_FACE))
        .withSideConfig(TransmissionType.ENERGY, TransmissionType.ITEM)
        .withCustomShape(BlockShapes.SPATIAL_ANCHOR)
        .with(AttributeCustomSelectionBox.JSON)
        .without(AttributeParticleFX.class)
        .withComputerSupport("inertiaDampener")
        .build();

    public static final Machine<FluxTetherBlockEntity> FLUX_TETHER = Machine.MachineBuilder
        .createMachine(() -> FNBlockEntityTypes.FLUX_TETHER, FNLang.DESCRIPTION_FLUX_TETHER)
        .withEnergyConfig(() -> 0L, () -> 640_000_000L)
        .withGui(() -> FNContainerTypes.FLUX_TETHER)
        .withSupportedUpgrades(Upgrade.ENERGY, Upgrade.SPEED)
        .with(new AttributeStateFacing(BlockStateProperties.FACING, AttributeStateFacing.FacePlacementType.SELECTED_FACE))
        .withSideConfig(TransmissionType.ENERGY, TransmissionType.ITEM)
        .with(AttributeCustomSelectionBox.JSON)
        .without(AttributeParticleFX.class)
        .withComputerSupport("fluxTether")
        .build();

    public static final Machine<FluxTetherLinkBlockEntity> FLUX_TETHER_LINK = Machine.MachineBuilder
        .createMachine(() -> FNBlockEntityTypes.FLUX_TETHER_LINK, FNLang.DESCRIPTION_FLUX_TETHER_LINK)
        .with(new AttributeStateFacing(BlockStateProperties.FACING, AttributeStateFacing.FacePlacementType.SELECTED_FACE))
        .with(AttributeCustomSelectionBox.JSON)
        .without(AttributeParticleFX.class)
        .build();

    public static final Machine<DaemonProtocolBlockEntity> DAEMON_PROTOCOL = Machine.MachineBuilder
        .createMachine(() -> FNBlockEntityTypes.DAEMON_PROTOCOL, FNLang.DESCRIPTION_DAEMON_PROTOCOL)
        .withEnergyConfig(() -> 16_000_000L, () -> 128_000_000L)
        .withGui(() -> FNContainerTypes.DAEMON_PROTOCOL)
        .withSupportedUpgrades(Upgrade.ENERGY)
        .with(new AttributeStateFacing(BlockStateProperties.FACING, AttributeStateFacing.FacePlacementType.SELECTED_FACE))
        .withSideConfig(TransmissionType.ENERGY, TransmissionType.ITEM)
        .withCustomShape(BlockShapes.SPATIAL_ANCHOR)
        .with(AttributeCustomSelectionBox.JSON)
        .without(AttributeParticleFX.class)
        .withComputerSupport("daemonProtocol")
        .build();

    public static final Machine<SpatialAnchorBlockEntity> SPATIAL_ANCHOR = Machine.MachineBuilder
        .createMachine(() -> FNBlockEntityTypes.SPATIAL_ANCHOR, FNLang.DESCRIPTION_SPATIAL_ANCHOR)
        .withEnergyConfig(() -> 0L, () -> 320_000_000_000L)
        .withGui(() -> FNContainerTypes.SPATIAL_ANCHOR)
        .withSupportedUpgrades(Upgrade.ENERGY)
        .withSideConfig(TransmissionType.ENERGY, TransmissionType.ITEM)
        .withCustomShape(BlockShapes.SPATIAL_ANCHOR)
        .with(AttributeCustomSelectionBox.JSON)
        .withComputerSupport("spatialAnchor")
        .build();

    public static final Machine<ForcefieldEmitterBlockEntity> FORCEFIELD_EMITTER = Machine.MachineBuilder
        .createMachine(() -> FNBlockEntityTypes.FORCEFIELD_EMITTER, FNLang.DESCRIPTION_FORCEFIELD_EMITTER)
        .withEnergyConfig(() -> 0L, () -> 320_000_000_000L)
        .withGui(() -> FNContainerTypes.FORCEFIELD_EMITTER)
        .withSupportedUpgrades(Upgrade.MUFFLING, Upgrade.ENERGY)
        .withSideConfig(TransmissionType.ENERGY, TransmissionType.ITEM)
        .withCustomShape(BlockShapes.HOLO_GRAM)
        .with(AttributeCustomSelectionBox.JSON)
        .withSound(FNSounds.FORCEFIELD_EMITTER)
        .without(AttributeParticleFX.class)
        .with(new AttributeStateFacing(BlockStateProperties.FACING, AttributeStateFacing.FacePlacementType.SELECTED_FACE))
        .withBounding(new HandleBoundingBlock() {
            @Override
            public <DATA> boolean handle(Level level, BlockPos pos, BlockState state, DATA data, TriBooleanFunction<Level, BlockPos, DATA> consumer) {
                BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x != 0 || z != 0) {
                            mutable.setWithOffset(pos, x, 0, z);
                            if (!consumer.accept(level, mutable, data)) {
                                return false;
                            }
                        }
                    }
                }
                return true;
            }
        })
        .withComputerSupport("forcefieldEmitter")
        .build();

    public static final Machine<ForcefieldHarmonizerBlockEntity> FORCEFIELD_HARMONIZER = Machine.MachineBuilder
        .createMachine(() -> FNBlockEntityTypes.FORCEFIELD_HARMONIZER, FNLang.DESCRIPTION_FORCEFIELD_HARMONIZER)
        .withEnergyConfig(() -> 500L, () -> 256_000L)
        .withGui(() -> FNContainerTypes.FORCEFIELD_HARMONIZER)
        .withSupportedUpgrades(Upgrade.ENERGY)
        .withSideConfig(TransmissionType.ENERGY, TransmissionType.ITEM)
        .withCustomShape(BlockShapes.SPATIAL_ANCHOR)
        .with(AttributeCustomSelectionBox.JSON)
        .withComputerSupport("forcefieldHarmonizer")
        .build();

    public static final Machine<CloakGeneratorBlockEntity> CLOAK_GENERATOR = Machine.MachineBuilder
        .createMachine(() -> FNBlockEntityTypes.CLOAK_GENERATOR, FNLang.DESCRIPTION_CLOAK_GENERATOR)
        .withEnergyConfig(() -> 500L, () -> 256_000L)
        .withGui(() -> FNContainerTypes.CLOAK_GENERATOR)
        .withSupportedUpgrades(Upgrade.ENERGY)
        .withSideConfig(TransmissionType.ENERGY, TransmissionType.ITEM)
        .withCustomShape(BlockShapes.SPATIAL_ANCHOR)
        .with(AttributeCustomSelectionBox.JSON)
        .withComputerSupport("cloakGenerator")
        .build();

    public static final Machine<AntiGravBlockEntity> ANTI_GRAV = Machine.MachineBuilder
        .createMachine(() -> FNBlockEntityTypes.ANTI_GRAV, FNLang.DESCRIPTION_ANTI_GRAV)
        .withEnergyConfig(() -> 0L, () -> 320_000_000_000L)
        .withGui(() -> FNContainerTypes.ANTI_GRAV)
        .withSupportedUpgrades(Upgrade.ENERGY)
        .withSideConfig(TransmissionType.ENERGY, TransmissionType.ITEM)
        .withCustomShape(BlockShapes.SPATIAL_ANCHOR)
        .with(AttributeCustomSelectionBox.JSON)
        .withComputerSupport("antiGrav")
        .build();

    public static final Machine<AssemblyTerminalBlockEntity> ASSEMBLY_TERMINAL = Machine.MachineBuilder
        .createMachine(() -> FNBlockEntityTypes.ASSEMBLY_TERMINAL, FNLang.DESCRIPTION_ASSEMBLY_TERMINAL)
        .withEnergyConfig(() -> 0L, () -> 512_000_000L)
        .withGui(() -> FNContainerTypes.ASSEMBLY_TERMINAL)
        .without(AttributeUpgradeSupport.class)
        .withSideConfig(TransmissionType.ENERGY, TransmissionType.ITEM)
        .with(Attributes.INVENTORY, new AttributeStateFacing(), new Attributes.AttributeCustomResistance(-1))
        .withCustomShape(mekanism.common.content.blocktype.BlockShapes.SECURITY_DESK)
        .with(AttributeCustomSelectionBox.JSON)
        .with(AttributeHasBounding.ABOVE_ONLY)
        .without(AttributeParticleFX.class)
        .withComputerSupport("assemblyTerminal")
        .build();

    public static final Machine<ConduitConnectorBlockEntity> CONDUIT_CONNECTOR = Machine.MachineBuilder
        .createMachine(() -> FNBlockEntityTypes.CONDUIT_CONNECTOR, FNLang.DESCRIPTION_CONDUIT_CONNECTOR)
        .withGui(() -> FNContainerTypes.CONDUIT_CONNECTOR)
        .without(AttributeUpgradeSupport.class)
        .without(AttributeParticleFX.class)
        .with(new AttributeStateFacing(BlockStateProperties.FACING, AttributeStateFacing.FacePlacementType.SELECTED_FACE))
        .withComputerSupport("conduitConnector")
        .build();

    public static final Machine<ConduitPortBlockEntity> CONDUIT_PORT = Machine.MachineBuilder
        .createMachine(() -> FNBlockEntityTypes.CONDUIT_PORT, FNLang.DESCRIPTION_CONDUIT_PORT)
        .without(AttributeUpgradeSupport.class)
        .without(AttributeParticleFX.class)
        .with(new AttributeStateFacing(BlockStateProperties.FACING, AttributeStateFacing.FacePlacementType.SELECTED_FACE))
        .build();

    public static final BlockTypeTile<ThrusterArrayStructuralBlockEntity> THRUSTER_ARRAY_STRUCTURAL = BlockTypeTile.BlockTileBuilder
        .createBlock(() -> FNBlockEntityTypes.THRUSTER_ARRAY_STRUCTURAL, FNLang.DESCRIPTION_THRUSTER_ARRAY_STRUCTURAL)
        .externalMultiblock()
        .with(Attributes.INVENTORY, Attributes.ACTIVE, new AttributeStateFacing(), new Attributes.AttributeCustomResistance(9))
        .build();

    public static final BlockTypeTile<ThrusterArrayControllerBlockEntity> THRUSTER_ARRAY_CONTROLLER = BlockTypeTile.BlockTileBuilder
        .createBlock(() -> FNBlockEntityTypes.THRUSTER_ARRAY_CONTROLLER, FNLang.DESCRIPTION_THRUSTER_ARRAY_CONTROLLER)
        .externalMultiblock()
        .withGui(() -> FNContainerTypes.THRUSTER_ARRAY)
        .with(Attributes.INVENTORY, Attributes.ACTIVE, new AttributeStateFacing(), new Attributes.AttributeCustomResistance(9))
        .withComputerSupport("thrusterArrayController")
        .build();

    public static final BlockTypeTile<ThrusterArraySubControllerBlockEntity> THRUSTER_ARRAY_SUBCONTROLLER = BlockTypeTile.BlockTileBuilder
        .createBlock(() -> FNBlockEntityTypes.THRUSTER_ARRAY_SUBCONTROLLER, FNLang.DESCRIPTION_THRUSTER_ARRAY_SUBCONTROLLER)
        .externalMultiblock()
        .with(Attributes.INVENTORY, Attributes.ACTIVE, new AttributeStateFacing(), new Attributes.AttributeCustomResistance(9))
        .build();

    public static final BlockTypeTile<IonThrusterBlockEntity> ION_THRUSTER = BlockTypeTile.BlockTileBuilder
        .createBlock(() -> FNBlockEntityTypes.ION_THRUSTER, FNLang.DESCRIPTION_ION_THRUSTER)
        .externalMultiblock()
        .with(new AttributeStateFacing(BlockStateProperties.FACING, AttributeStateFacing.FacePlacementType.SELECTED_FACE))
        .without(AttributeParticleFX.class, Attributes.AttributeRedstone.class)
        .with(AttributeCustomSelectionBox.JSON)
        .withCustomShape(BlockShapes.THRUSTER)
        .build();

    public static final BlockTypeTile<ThrusterArrayBoosterBlockEntity> THRUSTER_ARRAY_BOOSTER_BASIC = createBooster(BoosterTier.BASIC, () -> FNBlockEntityTypes.THRUSTER_ARRAY_BOOSTER_BASIC, () -> FNBlocks.THRUSTER_ARRAY_BOOSTER_ADVANCED);
    public static final BlockTypeTile<ThrusterArrayBoosterBlockEntity> THRUSTER_ARRAY_BOOSTER_ADVANCED = createBooster(BoosterTier.ADVANCED, () -> FNBlockEntityTypes.THRUSTER_ARRAY_BOOSTER_ADVANCED, () -> FNBlocks.THRUSTER_ARRAY_BOOSTER_ELITE);
    public static final BlockTypeTile<ThrusterArrayBoosterBlockEntity> THRUSTER_ARRAY_BOOSTER_ELITE = createBooster(BoosterTier.ELITE, () -> FNBlockEntityTypes.THRUSTER_ARRAY_BOOSTER_ELITE, () -> FNBlocks.THRUSTER_ARRAY_BOOSTER_ULTIMATE);
    public static final BlockTypeTile<ThrusterArrayBoosterBlockEntity> THRUSTER_ARRAY_BOOSTER_ULTIMATE = createBooster(BoosterTier.ULTIMATE, () -> FNBlockEntityTypes.THRUSTER_ARRAY_BOOSTER_ULTIMATE, () -> null);
    public static final BlockTypeTile<ThrusterArrayBoosterBlockEntity> THRUSTER_ARRAY_BOOSTER_CREATIVE = createBooster(BoosterTier.CREATIVE, () -> FNBlockEntityTypes.THRUSTER_ARRAY_BOOSTER_CREATIVE, () -> null);

    private static <TILE extends ThrusterArrayBoosterBlockEntity> BlockTypeTile<TILE> createBooster(BoosterTier tier, Supplier<TileEntityTypeRegistryObject<TILE>> tile, Supplier<BlockRegistryObject<?, ?>> upgradeBlock) {
        return BlockTypeTile.BlockTileBuilder
            .createBlock(tile, FNLang.DESCRIPTION_THRUSTER_ARRAY_BOOSTER)
            .with(new AttributeTier<>(tier), new AttributeUpgradeable(upgradeBlock))
            .externalMultiblock()
            .with(new AttributeStateFacing(BlockStateProperties.FACING, AttributeStateFacing.FacePlacementType.SELECTED_FACE))
            .with(Attributes.INVENTORY, Attributes.ACTIVE, new Attributes.AttributeCustomResistance(9))
            .build();
    }


    public static final BlockTypeTile<ThrusterArrayCenterOfMassAlignmentBlockEntity> THRUSTER_ARRAY_ALIGNMENT_X = createAlignment(Direction.Axis.X, () -> FNBlockEntityTypes.THRUSTER_ARRAY_ALIGNMENT_X);
    public static final BlockTypeTile<ThrusterArrayCenterOfMassAlignmentBlockEntity> THRUSTER_ARRAY_ALIGNMENT_Y = createAlignment(Direction.Axis.Y, () -> FNBlockEntityTypes.THRUSTER_ARRAY_ALIGNMENT_Y);
    public static final BlockTypeTile<ThrusterArrayCenterOfMassAlignmentBlockEntity> THRUSTER_ARRAY_ALIGNMENT_Z = createAlignment(Direction.Axis.Z, () -> FNBlockEntityTypes.THRUSTER_ARRAY_ALIGNMENT_Z);

    private static <TILE extends ThrusterArrayCenterOfMassAlignmentBlockEntity> BlockTypeTile<TILE> createAlignment(Direction.Axis axis, Supplier<TileEntityTypeRegistryObject<TILE>> tile) {
        return BlockTypeTile.BlockTileBuilder
            .createBlock(tile, FNLang.DESCRIPTION_THRUSTER_ARRAY_ALIGNMENT)
            .with(new AttributeAxis(axis))
            .externalMultiblock()
            .with(new AttributeStateFacing(BlockStateProperties.FACING, AttributeStateFacing.FacePlacementType.SELECTED_FACE))
            .with(Attributes.INVENTORY, Attributes.ACTIVE, new Attributes.AttributeCustomResistance(9))
            .build();
    }
}