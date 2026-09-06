package dev.alyti.fluxnautics.content.blocks.thruster;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.multiblock.DynamicTileEntityMultiblock;
import dev.alyti.fluxnautics.multiblock.ThrusterArrayMultiblockData;
import dev.alyti.fluxnautics.registry.FNBlocks;
import mekanism.api.IConfigurable;
import mekanism.api.IContentsListener;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.dynamic.SyncMapper;
import mekanism.common.lib.multiblock.MultiblockManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class ThrusterArrayStructuralBlockEntity extends DynamicTileEntityMultiblock<ThrusterArrayMultiblockData> implements IConfigurable {

    public ThrusterArrayStructuralBlockEntity(BlockPos pos, BlockState state) {
        this(FNBlocks.THRUSTER_ARRAY_STRUCTURAL, pos, state);
    }

    public ThrusterArrayStructuralBlockEntity(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state);
        delaySupplier = NO_DELAY;
    }

//    @Override
//    public void addLines(List<Component> list) {
//        ThrusterArrayMultiblockData data = this.getMultiblock();
//        if (!data.isFormed()) {
//            list.add(Component.literal("Not formed, invalid multiblock.").withStyle(ChatFormatting.RED).withStyle(ChatFormatting.ITALIC));
//            return;
//        }
//        list.add(Component.literal("Multiblock Info:").withStyle(ChatFormatting.GOLD).withStyle(ChatFormatting.UNDERLINE));
//        list.add(Component.literal("Array Size: %d x %d x %d".formatted(data.width(), data.height(), data.length())));
//        list.add(Component.literal("Maximum thrust multiplier: %f".formatted(data.thrusters.thrustBudget)));
//        list.add(Component.literal("Alignment budget: %f x %f x %f".formatted(data.thrusters.alignmentBudget.x, data.thrusters.alignmentBudget.y, data.thrusters.alignmentBudget.z)));
//        list.add(Component.literal("Thruster groups:"));
//        int id = 0;
//        for (ThrusterArrayMultiblockData.GroupData groupData : data.thrusters.groups) {
//            list.add(Component.literal(" - %s (%d), %d thrusters".formatted(groupData.getName(), id++, groupData.thrusters.size())));
//        }
//    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        SyncMapper.INSTANCE.setup(container, ThrusterArrayMultiblockData.SyncableThrusterData.class, () -> this.getMultiblock().thrusters);
    }

    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        return side -> getMultiblock().getEnergyContainers(side);
    }

    @NotNull
    @Override
    public IChemicalTankHolder getInitialChemicalTanks(IContentsListener listener) {
        return side -> getMultiblock().getChemicalTanks(side);
    }

    @Override
    public boolean persists(ContainerType<?, ?, ?> type) {
        return false;
    }

    @Override
    protected boolean onUpdateServer(ThrusterArrayMultiblockData multiblock) {
        boolean needsPacket = super.onUpdateServer(multiblock);
        setActive(multiblock.isFormed());
        return needsPacket;
    }

    @Override
    public ThrusterArrayMultiblockData createMultiblock() {
        return new ThrusterArrayMultiblockData(this);
    }

    @Override
    public MultiblockManager<ThrusterArrayMultiblockData> getManager() {
        return Fluxnautics.thrusterArrayManager;
    }

    @Override
    public boolean canBeMaster() {
        return false;
    }

}