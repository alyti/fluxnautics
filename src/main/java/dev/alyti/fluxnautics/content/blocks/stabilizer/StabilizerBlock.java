package dev.alyti.fluxnautics.content.blocks.stabilizer;

import mekanism.common.block.prefab.BlockTileGlass;
import mekanism.common.content.blocktype.BlockTypeTile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public class StabilizerBlock extends BlockTileGlass<StabilizerBlockEntity, BlockTypeTile<StabilizerBlockEntity>> {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public StabilizerBlock(BlockTypeTile<StabilizerBlockEntity> type) {
        super(type);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction normal = context.getNearestLookingDirection();
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            normal = normal.getOpposite();
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        for (Direction direction : Direction.values()) {
            BlockState neighborState = level.getBlockState(pos.relative(direction));
            if (neighborState.is(this)) {
                normal = neighborState.getValue(FACING);
                break;
            }
        }

        return this.defaultBlockState().setValue(FACING, normal);
    }
}