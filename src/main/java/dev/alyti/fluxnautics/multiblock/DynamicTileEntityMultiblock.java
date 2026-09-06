package dev.alyti.fluxnautics.multiblock;

import mekanism.common.lib.multiblock.MultiblockData;
import mekanism.common.tile.prefab.TileEntityMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public abstract class DynamicTileEntityMultiblock<T extends MultiblockData> extends TileEntityMultiblock<T> {
    public DynamicTileEntityMultiblock(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state);
    }
}

