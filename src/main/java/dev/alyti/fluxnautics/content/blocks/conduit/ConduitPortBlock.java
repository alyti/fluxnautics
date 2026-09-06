package dev.alyti.fluxnautics.content.blocks.conduit;

import dev.alyti.fluxnautics.registry.FNBlockTypes;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.Machine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;

public class ConduitPortBlock extends BlockTile.BlockTileModel<ConduitPortBlockEntity, Machine<ConduitPortBlockEntity>>{
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public ConduitPortBlock() {
        super(FNBlockTypes.CONDUIT_PORT, Properties.of().mapColor(MapColor.METAL).strength(3.5F).noOcclusion());
//        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof ConduitPortBlockEntity port) {
                port.setRemoved();
                port.dropRemotePort(); // Tell the remote port to drop itself
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

}