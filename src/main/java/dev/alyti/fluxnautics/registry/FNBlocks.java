package dev.alyti.fluxnautics.registry;

import dev.alyti.fluxnautics.attribute.AttributeAxis;
import dev.alyti.fluxnautics.content.blocks.daemon_protocol.DaemonProtocolBlockEntity;
import dev.alyti.fluxnautics.content.blocks.antigrav.AntiGravBlockEntity;
import dev.alyti.fluxnautics.content.blocks.assembly_terminal.AssemblyTerminalBlockEntity;
import dev.alyti.fluxnautics.content.blocks.cloak.CloakGeneratorBlockEntity;
import dev.alyti.fluxnautics.content.blocks.conduit.ConduitConnectorBlock;
import dev.alyti.fluxnautics.content.blocks.conduit.ConduitPortBlock;
import dev.alyti.fluxnautics.content.blocks.conduit.ConduitPortBlockItem;
import dev.alyti.fluxnautics.content.blocks.field.FieldDriveBlock;
import dev.alyti.fluxnautics.content.blocks.flux_tether.FluxTetherConfig;
import dev.alyti.fluxnautics.content.blocks.forcefield.ForcefieldBubbleEmitterConfig;
import dev.alyti.fluxnautics.content.blocks.forcefield.ForcefieldEmitterBlockEntity;
import dev.alyti.fluxnautics.content.blocks.forcefield.ForcefieldHarmonizerBlockEntity;
import dev.alyti.fluxnautics.content.blocks.holomap.HoloBlock;
import dev.alyti.fluxnautics.content.blocks.flux_tether.FluxTetherBlock;
import dev.alyti.fluxnautics.content.blocks.flux_tether.link_block.FluxTetherLinkBlock;
import dev.alyti.fluxnautics.content.blocks.inertia_dampener.InertiaDampenerBlockEntity;
import dev.alyti.fluxnautics.content.blocks.inertia_dampener.InertiaDampenerConfig;
import dev.alyti.fluxnautics.content.blocks.spatial_anchor.SpatialAnchorBlockEntity;
import dev.alyti.fluxnautics.content.blocks.stabilizer.StabilizerBlock;
import dev.alyti.fluxnautics.content.blocks.stabilizer.StabilizerConfig;
import dev.alyti.fluxnautics.content.blocks.thruster.*;
import dev.alyti.fluxnautics.content.items.ItemBlockBooster;
import mekanism.api.tier.ITier;
import mekanism.common.attachments.FilterAware;
import mekanism.common.attachments.component.AttachedEjector;
import mekanism.common.attachments.component.AttachedSideConfig;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.attachments.containers.chemical.ChemicalTanksBuilder;
import mekanism.common.attachments.containers.item.ItemSlotsBuilder;
import mekanism.common.block.attribute.AttributeTier;
import mekanism.common.block.interfaces.IHasDescription;
import mekanism.common.block.prefab.BlockBasicMultiblock;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.BlockType;
import mekanism.common.content.blocktype.BlockTypeTile;
import mekanism.common.content.blocktype.Machine;
import mekanism.common.item.block.ItemBlockTooltip;
import mekanism.common.registration.impl.BlockDeferredRegister;
import mekanism.common.registration.impl.BlockRegistryObject;
import mekanism.common.registries.MekanismDataComponents;
import mekanism.common.resource.BlockResourceInfo;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static dev.alyti.fluxnautics.Fluxnautics.MODID;
import static dev.alyti.fluxnautics.content.blocks.field.FieldDriveBlockEntity.HAS_WARPFUEL;
import static dev.alyti.fluxnautics.content.blocks.field.FieldDriveBlockEntity.MAX_WARPFUEL;

public class FNBlocks {
    public static final BlockDeferredRegister BLOCKS = new BlockDeferredRegister(MODID);


    public static final BlockRegistryObject<FieldDriveBlock, ItemBlockTooltip<FieldDriveBlock>> FIELD_DRIVE = BLOCKS.register("field_drive", () -> new FieldDriveBlock(FNBlockTypes.FIELD_DRIVE), (block, properties) -> new ItemBlockTooltip<>(block, true, properties
        .component(MekanismDataComponents.RADIUS, 1000)
        .component(FNDataComponents.IMPULSE_MULT, 2.0)
        .component(FNDataComponents.IMPULSE_VEC_X, 1.0)
        .component(FNDataComponents.IMPULSE_VEC_Y, 0.0)
        .component(FNDataComponents.IMPULSE_VEC_Z, 0.0)
        .component(FNDataComponents.IS_GLOBAL, false)
        .component(MekanismDataComponents.EJECTOR, AttachedEjector.DEFAULT)
        .component(MekanismDataComponents.SIDE_CONFIG, AttachedSideConfig.CRYSTALLIZER)
    )).forItemHolder(holder ->
        holder
            .addAttachmentOnlyContainers(ContainerType.ITEM, () -> ItemSlotsBuilder.builder()
                .addEnergy()
                .addChemicalFillOrConvertSlot(0)
                .build())
            .addAttachmentOnlyContainers(ContainerType.CHEMICAL, () -> ChemicalTanksBuilder.builder()
                .addBasic(MAX_WARPFUEL, HAS_WARPFUEL)
                .build())
    );

    public static final BlockRegistryObject<HoloBlock, ItemBlockTooltip<HoloBlock>> HOLO =
        BLOCKS.register("holo", () -> new HoloBlock(
            properties -> properties.mapColor(BlockResourceInfo.STEEL.getMapColor())), (block, properties) -> new ItemBlockTooltip<>(block, true, properties
            .component(MekanismDataComponents.RADIUS, 128)
        )).forItemHolder(holder -> holder.addAttachmentOnlyContainers(ContainerType.ITEM, () -> ItemSlotsBuilder.builder()
            .addEnergy()
            .build())
        );


    public static final BlockRegistryObject<StabilizerBlock, ItemBlockTooltip<StabilizerBlock>> STABILIZER =
        BLOCKS.register(
            "stabilizer", () -> new StabilizerBlock(FNBlockTypes.STABILIZER),
            (block, properties) -> new ItemBlockTooltip<>(block, true, properties
                .component(MekanismDataComponents.EJECTOR, AttachedEjector.DEFAULT)
                .component(MekanismDataComponents.SIDE_CONFIG, AttachedSideConfig.ELECTRIC_MACHINE)
                .component(FNDataComponents.STABILIZER_CONFIG, StabilizerConfig.makeDefault())
            )).forItemHolder(holder -> holder.addAttachmentOnlyContainers(ContainerType.ITEM, () -> ItemSlotsBuilder.builder()
            .addEnergy()
            .addDrainEnergy()
            .build())
        );

    public static final BlockRegistryObject<BlockTile.BlockTileModel<DaemonProtocolBlockEntity, Machine<DaemonProtocolBlockEntity>>, ItemBlockTooltip<BlockTile.BlockTileModel<DaemonProtocolBlockEntity, Machine<DaemonProtocolBlockEntity>>>> DAEMON_PROTOCOL =
        BLOCKS.register(
            "daemon_protocol", () -> new BlockTile.BlockTileModel<>(FNBlockTypes.DAEMON_PROTOCOL, properties -> properties.noOcclusion().mapColor(BlockResourceInfo.URANIUM.getMapColor())),
            (block, properties) -> new ItemBlockTooltip<>(block, true, properties
                .component(MekanismDataComponents.EJECTOR, AttachedEjector.DEFAULT)
                .component(MekanismDataComponents.SIDE_CONFIG, AttachedSideConfig.ELECTRIC_MACHINE)
            )).forItemHolder(holder -> holder.addAttachmentOnlyContainers(ContainerType.ITEM, () -> ItemSlotsBuilder.builder()
            .addEnergy()
            .addDrainEnergy()
            .addInput((stack) -> stack.is(FNItems.MEKA_ROBIT))
            .build())
        );

    public static final BlockRegistryObject<BlockTile.BlockTileModel<InertiaDampenerBlockEntity, Machine<InertiaDampenerBlockEntity>>, ItemBlockTooltip<BlockTile.BlockTileModel<InertiaDampenerBlockEntity, Machine<InertiaDampenerBlockEntity>>>> INERTIA_DAMPENER =
        BLOCKS.register(
            "inertia_dampener", () -> new BlockTile.BlockTileModel<>(FNBlockTypes.INERTIA_DAMPENER, properties -> properties.noOcclusion().mapColor(BlockResourceInfo.BRONZE.getMapColor())),
            (block, properties) -> new ItemBlockTooltip<>(block, true, properties
                .component(MekanismDataComponents.EJECTOR, AttachedEjector.DEFAULT)
                .component(MekanismDataComponents.SIDE_CONFIG, AttachedSideConfig.ELECTRIC_MACHINE)
                .component(FNDataComponents.INERTIA_DAMPENER_CONFIG, InertiaDampenerConfig.DEFAULT)
            )).forItemHolder(holder -> holder.addAttachmentOnlyContainers(ContainerType.ITEM, () -> ItemSlotsBuilder.builder()
            .addEnergy()
            .addDrainEnergy()
            .build())
        );

    public static final BlockRegistryObject<FluxTetherBlock, ItemBlockTooltip<FluxTetherBlock>> FLUX_TETHER =
        BLOCKS.register(
            "flux_tether", () -> new FluxTetherBlock(FNBlockTypes.FLUX_TETHER),
            (block, properties) -> new ItemBlockTooltip<>(block, true, properties
                .component(MekanismDataComponents.EJECTOR, AttachedEjector.DEFAULT)
                .component(MekanismDataComponents.SIDE_CONFIG, AttachedSideConfig.ELECTRIC_MACHINE)
                .component(FNDataComponents.FLUX_TETHER_CONFIG, FluxTetherConfig.makeDefault())
            )).forItemHolder(holder -> holder.addAttachmentOnlyContainers(ContainerType.ITEM, () -> ItemSlotsBuilder.builder()
            .addEnergy()
            .addDrainEnergy()
            .build())
        );

    public static final BlockRegistryObject<FluxTetherLinkBlock, ItemBlockTooltip<FluxTetherLinkBlock>> FLUX_TETHER_LINK =
        BLOCKS.register(
            "flux_tether_link", () -> new FluxTetherLinkBlock(FNBlockTypes.FLUX_TETHER_LINK),
            (block, properties) -> new ItemBlockTooltip<>(block, true, properties
                .component(MekanismDataComponents.EJECTOR, AttachedEjector.DEFAULT)
                .component(MekanismDataComponents.SIDE_CONFIG, AttachedSideConfig.ELECTRIC_MACHINE)
            )).forItemHolder(holder -> holder.addAttachmentOnlyContainers(ContainerType.ITEM, () -> ItemSlotsBuilder.builder()
            .addEnergy()
            .addDrainEnergy()
            .build())
        );

    public static final BlockRegistryObject<BlockTile.BlockTileModel<SpatialAnchorBlockEntity, Machine<SpatialAnchorBlockEntity>>, ItemBlockTooltip<BlockTile.BlockTileModel<SpatialAnchorBlockEntity, Machine<SpatialAnchorBlockEntity>>>> SPATIAL_ANCHOR =
        BLOCKS.register(
            "spatial_anchor", () -> new BlockTile.BlockTileModel<>(FNBlockTypes.SPATIAL_ANCHOR, properties -> properties.mapColor(BlockResourceInfo.STEEL.getMapColor())),
            (block, properties) -> new ItemBlockTooltip<>(block, true, properties
                .component(MekanismDataComponents.EJECTOR, AttachedEjector.DEFAULT)
                .component(MekanismDataComponents.SIDE_CONFIG, AttachedSideConfig.ELECTRIC_MACHINE)
            )).forItemHolder(holder -> holder.addAttachmentOnlyContainers(ContainerType.ITEM, () -> ItemSlotsBuilder.builder()
            .addEnergy()
            .addDrainEnergy()
            .build())
        );

    public static final BlockRegistryObject<BlockTile.BlockTileModel<ForcefieldEmitterBlockEntity, Machine<ForcefieldEmitterBlockEntity>>, ItemBlockTooltip<BlockTile.BlockTileModel<ForcefieldEmitterBlockEntity, Machine<ForcefieldEmitterBlockEntity>>>> FORCEFIELD_EMITTER =
        BLOCKS.register(
            "forcefield_emitter", () -> new BlockTile.BlockTileModel<>(FNBlockTypes.FORCEFIELD_EMITTER, properties -> properties.mapColor(BlockResourceInfo.STEEL.getMapColor())),
            (block, properties) -> new ItemBlockTooltip<>(block, true, properties
                .component(MekanismDataComponents.EJECTOR, AttachedEjector.DEFAULT)
                .component(MekanismDataComponents.SIDE_CONFIG, AttachedSideConfig.ELECTRIC_MACHINE)
                .component(FNDataComponents.COOLDOWN_TICKS, 0L)
                .component(FNDataComponents.EMITTER_CONFIG, ForcefieldBubbleEmitterConfig.makeDefault(null))
                .component(MekanismDataComponents.ITEM_TARGET, Optional.empty())
            )).forItemHolder(holder -> holder.addAttachmentOnlyContainers(ContainerType.ITEM, () -> ItemSlotsBuilder.builder()
            .addInput(1)
            .addEnergy()
            .addDrainEnergy()
            .build())
        );

    public static final BlockRegistryObject<BlockTile.BlockTileModel<ForcefieldHarmonizerBlockEntity, Machine<ForcefieldHarmonizerBlockEntity>>, ItemBlockTooltip<BlockTile.BlockTileModel<ForcefieldHarmonizerBlockEntity, Machine<ForcefieldHarmonizerBlockEntity>>>> FORCEFIELD_HARMONIZER =
        BLOCKS.register(
            "forcefield_harmonizer", () -> new BlockTile.BlockTileModel<>(FNBlockTypes.FORCEFIELD_HARMONIZER, properties -> properties.mapColor(BlockResourceInfo.STEEL.getMapColor())),
            (block, properties) -> new ItemBlockTooltip<>(block, true, properties
                .component(MekanismDataComponents.EJECTOR, AttachedEjector.DEFAULT)
                .component(MekanismDataComponents.SIDE_CONFIG, AttachedSideConfig.ELECTRIC_MACHINE)
                .component(MekanismDataComponents.FILTER_AWARE, FilterAware.EMPTY)
            )).forItemHolder(holder -> holder.addAttachmentOnlyContainers(ContainerType.ITEM, () -> ItemSlotsBuilder.builder()
            .addEnergy()
            .addDrainEnergy()
            .build())
        );

    public static final BlockRegistryObject<BlockTile.BlockTileModel<CloakGeneratorBlockEntity, Machine<CloakGeneratorBlockEntity>>, ItemBlockTooltip<BlockTile.BlockTileModel<CloakGeneratorBlockEntity, Machine<CloakGeneratorBlockEntity>>>> CLOAK_GENERATOR =
        BLOCKS.register(
            "cloak_generator", () -> new BlockTile.BlockTileModel<>(FNBlockTypes.CLOAK_GENERATOR, properties -> properties.mapColor(BlockResourceInfo.STEEL.getMapColor())),
            (block, properties) -> new ItemBlockTooltip<>(block, true, properties
                .component(MekanismDataComponents.EJECTOR, AttachedEjector.DEFAULT)
                .component(MekanismDataComponents.SIDE_CONFIG, AttachedSideConfig.ELECTRIC_MACHINE)
            )).forItemHolder(holder -> holder.addAttachmentOnlyContainers(ContainerType.ITEM, () -> ItemSlotsBuilder.builder()
            .addEnergy()
            .addDrainEnergy()
            .build())
        );

    public static final BlockRegistryObject<BlockTile.BlockTileModel<AntiGravBlockEntity, Machine<AntiGravBlockEntity>>, ItemBlockTooltip<BlockTile.BlockTileModel<AntiGravBlockEntity, Machine<AntiGravBlockEntity>>>> ANTI_GRAV =
        BLOCKS.register(
            "anti_grav", () -> new BlockTile.BlockTileModel<>(FNBlockTypes.ANTI_GRAV, properties -> properties.mapColor(BlockResourceInfo.STEEL.getMapColor())),
            (block, properties) -> new ItemBlockTooltip<>(block, true, properties
                .component(MekanismDataComponents.EJECTOR, AttachedEjector.DEFAULT)
                .component(MekanismDataComponents.SIDE_CONFIG, AttachedSideConfig.ELECTRIC_MACHINE)
            )).forItemHolder(holder -> holder.addAttachmentOnlyContainers(ContainerType.ITEM, () -> ItemSlotsBuilder.builder()
            .addEnergy()
            .addDrainEnergy()
            .build())
        );

    public static final BlockRegistryObject<BlockTile.BlockTileModel<AssemblyTerminalBlockEntity, Machine<AssemblyTerminalBlockEntity>>, ItemBlockTooltip<BlockTile.BlockTileModel<AssemblyTerminalBlockEntity, Machine<AssemblyTerminalBlockEntity>>>> ASSEMBLY_TERMINAL =
        BLOCKS.register(
            "assembly_terminal", () -> new BlockTile.BlockTileModel<>(FNBlockTypes.ASSEMBLY_TERMINAL, properties -> properties.mapColor(BlockResourceInfo.STEEL.getMapColor())),
            (block, properties) -> new ItemBlockTooltip<>(block, true, properties
            )).forItemHolder(holder -> holder.addAttachmentOnlyContainers(ContainerType.ITEM, () -> ItemSlotsBuilder.builder().build())
        );

    public static final BlockRegistryObject<ConduitConnectorBlock, ItemBlockTooltip<ConduitConnectorBlock>> CONDUIT_CONNECTOR =
        BLOCKS.register(
            "conduit_connector", ConduitConnectorBlock::new,
            (block, properties) -> new ItemBlockTooltip<>(block, true, properties
            )).forItemHolder(holder -> holder.addAttachmentOnlyContainers(ContainerType.ITEM, () -> ItemSlotsBuilder.builder().build())
        );

    public static final BlockRegistryObject<ConduitPortBlock, ConduitPortBlockItem> CONDUIT_PORT =
        BLOCKS.register(
            "conduit_port", ConduitPortBlock::new,
            (block, properties) -> new ConduitPortBlockItem()).forItemHolder(holder -> holder.addAttachmentOnlyContainers(ContainerType.ITEM, () -> ItemSlotsBuilder.builder().build())
        );

    public static final BlockRegistryObject<BlockBasicMultiblock<ThrusterArrayStructuralBlockEntity>, ItemBlockTooltip<BlockBasicMultiblock<ThrusterArrayStructuralBlockEntity>>> THRUSTER_ARRAY_STRUCTURAL = registerBlock("thruster_array_structural", () -> new BlockBasicMultiblock<>(FNBlockTypes.THRUSTER_ARRAY_STRUCTURAL, properties -> properties.mapColor(BlockResourceInfo.STEEL.getMapColor())));
    public static final BlockRegistryObject<BlockBasicMultiblock<ThrusterArrayControllerBlockEntity>, ItemBlockTooltip<BlockBasicMultiblock<ThrusterArrayControllerBlockEntity>>> THRUSTER_ARRAY_CONTROLLER = registerBlock("thruster_array_controller", () -> new BlockBasicMultiblock<>(FNBlockTypes.THRUSTER_ARRAY_CONTROLLER, properties -> properties.mapColor(BlockResourceInfo.STEEL.getMapColor())));
    public static final BlockRegistryObject<BlockBasicMultiblock<ThrusterArraySubControllerBlockEntity>, ItemBlockTooltip<BlockBasicMultiblock<ThrusterArraySubControllerBlockEntity>>> THRUSTER_ARRAY_SUBCONTROLLER = registerBlock("thruster_array_subcontroller", () -> new BlockBasicMultiblock<>(FNBlockTypes.THRUSTER_ARRAY_SUBCONTROLLER, properties -> properties.mapColor(BlockResourceInfo.STEEL.getMapColor())));
    public static final BlockRegistryObject<IonThrusterBlock, IonThrusterBlockItem> ION_THRUSTER = BLOCKS.register("ion_thruster", () -> new IonThrusterBlock(FNBlockTypes.ION_THRUSTER), IonThrusterBlockItem::new);

    public static final BlockRegistryObject<BlockBasicMultiblock<ThrusterArrayBoosterBlockEntity>, ItemBlockBooster> THRUSTER_ARRAY_BOOSTER_BASIC = registerBooster(FNBlockTypes.THRUSTER_ARRAY_BOOSTER_BASIC);
    public static final BlockRegistryObject<BlockBasicMultiblock<ThrusterArrayBoosterBlockEntity>, ItemBlockBooster> THRUSTER_ARRAY_BOOSTER_ADVANCED = registerBooster(FNBlockTypes.THRUSTER_ARRAY_BOOSTER_ADVANCED);
    public static final BlockRegistryObject<BlockBasicMultiblock<ThrusterArrayBoosterBlockEntity>, ItemBlockBooster> THRUSTER_ARRAY_BOOSTER_ELITE = registerBooster(FNBlockTypes.THRUSTER_ARRAY_BOOSTER_ELITE);
    public static final BlockRegistryObject<BlockBasicMultiblock<ThrusterArrayBoosterBlockEntity>, ItemBlockBooster> THRUSTER_ARRAY_BOOSTER_ULTIMATE = registerBooster(FNBlockTypes.THRUSTER_ARRAY_BOOSTER_ULTIMATE);
    public static final BlockRegistryObject<BlockBasicMultiblock<ThrusterArrayBoosterBlockEntity>, ItemBlockBooster> THRUSTER_ARRAY_BOOSTER_CREATIVE = registerBooster(FNBlockTypes.THRUSTER_ARRAY_BOOSTER_CREATIVE);

    public static final BlockRegistryObject<BlockBasicMultiblock<ThrusterArrayCenterOfMassAlignmentBlockEntity>, ItemBlockTooltip<BlockBasicMultiblock<ThrusterArrayCenterOfMassAlignmentBlockEntity>>> THRUSTER_ARRAY_ALIGNMENT_X = registerAlignment(FNBlockTypes.THRUSTER_ARRAY_ALIGNMENT_X);
    public static final BlockRegistryObject<BlockBasicMultiblock<ThrusterArrayCenterOfMassAlignmentBlockEntity>, ItemBlockTooltip<BlockBasicMultiblock<ThrusterArrayCenterOfMassAlignmentBlockEntity>>> THRUSTER_ARRAY_ALIGNMENT_Y = registerAlignment(FNBlockTypes.THRUSTER_ARRAY_ALIGNMENT_Y);
    public static final BlockRegistryObject<BlockBasicMultiblock<ThrusterArrayCenterOfMassAlignmentBlockEntity>, ItemBlockTooltip<BlockBasicMultiblock<ThrusterArrayCenterOfMassAlignmentBlockEntity>>> THRUSTER_ARRAY_ALIGNMENT_Z = registerAlignment(FNBlockTypes.THRUSTER_ARRAY_ALIGNMENT_Z);

    private static BlockRegistryObject<BlockBasicMultiblock<ThrusterArrayCenterOfMassAlignmentBlockEntity>, ItemBlockTooltip<BlockBasicMultiblock<ThrusterArrayCenterOfMassAlignmentBlockEntity>>> registerAlignment(BlockTypeTile<ThrusterArrayCenterOfMassAlignmentBlockEntity> type) {
        Direction.Axis axis = type.get(AttributeAxis.class).axis();
        return registerBlock(
            "thruster_array_alignment_" + axis.name().toLowerCase(),
            () -> new BlockBasicMultiblock<>(type, properties -> properties.mapColor(MapColor.COLOR_PINK))
        );
    }

    private static BlockRegistryObject<BlockBasicMultiblock<ThrusterArrayBoosterBlockEntity>, ItemBlockBooster> registerBooster(BlockTypeTile<ThrusterArrayBoosterBlockEntity> type) {
        ITier tier = type.get(AttributeTier.class).tier();
        return registerTieredBlock(
            type,
            "thruster_array_booster_",
            () -> new BlockBasicMultiblock<>(type, properties -> properties.mapColor(tier.getBaseTier().getMapColor())),
            ItemBlockBooster::new
        );
    }

    private static <BLOCK extends Block & IHasDescription, ITEM extends BlockItem> BlockRegistryObject<BLOCK, ITEM> registerTieredBlock(BlockType type, String prefix, Supplier<? extends BLOCK> blockSupplier, BiFunction<BLOCK, Item.Properties, ITEM> itemCreator) {
        ITier tier = type.get(AttributeTier.class).tier();
        return registerTieredBlock(tier, prefix, blockSupplier, itemCreator);
    }

    private static <BLOCK extends Block & IHasDescription, ITEM extends BlockItem> BlockRegistryObject<BLOCK, ITEM> registerTieredBlock(ITier tier, String prefix, Supplier<? extends BLOCK> blockSupplier, BiFunction<BLOCK, Item.Properties, ITEM> itemCreator) {
        return BLOCKS.register(prefix + tier.getBaseTier().getLowerName(), blockSupplier, itemCreator);
    }

    private static <BLOCK extends Block & IHasDescription> BlockRegistryObject<BLOCK, ItemBlockTooltip<BLOCK>> registerBlock(String name, Supplier<? extends BLOCK> blockSupplier) {
        return BLOCKS.register(name, blockSupplier, ItemBlockTooltip::new);
    }
}
