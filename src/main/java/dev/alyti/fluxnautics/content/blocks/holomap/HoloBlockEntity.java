package dev.alyti.fluxnautics.content.blocks.holomap;

import dev.alyti.fluxnautics.capabilities.HoloEnergyContainer;
import dev.alyti.fluxnautics.registry.FNBlocks;
import dev.alyti.fluxnautics.util.MathUtils;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.interfaces.IBoundingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class HoloBlockEntity extends TileEntityMekanism implements IBoundingBlock {

    private HoloEnergyContainer energyContainer;

    public HoloBlockEntity(BlockPos pos, BlockState state) {
        super(FNBlocks.HOLO, pos, state);
    }

    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSide(facingSupplier);
        builder.addContainer(energyContainer = HoloEnergyContainer.input(this, listener), RelativeSide.LEFT, RelativeSide.RIGHT, RelativeSide.BOTTOM);
        return builder.build();
    }

    public int getRadius() {
        return 128;
    }

    public float getYRotation() {
        if (!Sable.HELPER.isInPlotGrid(this.level, this.getBlockPos())) {
            return 0;
        }
        SubLevel subLevel = Sable.HELPER.getContaining(this);
        final float closestYRotation = (float) MathUtils.getClosestYaw(subLevel.logicalPose().orientation());
        return (float) (-(closestYRotation / (Math.PI / 2.0f)) * 90);
    }
}