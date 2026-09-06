package dev.alyti.fluxnautics.content.blocks.flux_tether.link_block;

import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import mekanism.common.block.prefab.BlockTileGlass;
import mekanism.common.content.blocktype.BlockTypeTile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import org.jspecify.annotations.NonNull;

import javax.annotation.ParametersAreNonnullByDefault;

public class FluxTetherLinkBlock extends BlockTileGlass<FluxTetherLinkBlockEntity, BlockTypeTile<FluxTetherLinkBlockEntity>> implements BlockSubLevelAssemblyListener {

    public static final VoxelShape PLATE_SHAPE = Shapes.block();

    public FluxTetherLinkBlock(BlockTypeTile<FluxTetherLinkBlockEntity> type) {
        super(type);
    }

    @Override
    @ParametersAreNonnullByDefault
    protected @NonNull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return PLATE_SHAPE;
    }

    @Override
    @ParametersAreNonnullByDefault
    protected @NonNull VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return PLATE_SHAPE;
    }

    @Override
    @ParametersAreNonnullByDefault
    protected @NonNull VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return PLATE_SHAPE;
    }

    @Override
    public void beforeMove(ServerLevel originLevel, ServerLevel resultingLevel, BlockState newState,
                           BlockPos oldPos, BlockPos newPos) {
        if (originLevel.getBlockEntity(oldPos) instanceof FluxTetherLinkBlockEntity be)
            be.beforeAssembly();
    }

    @Override
    public void afterMove(ServerLevel originLevel, ServerLevel resultingLevel, BlockState newState,
                          BlockPos oldPos, BlockPos newPos) {
        if (resultingLevel.getBlockEntity(newPos) instanceof FluxTetherLinkBlockEntity be) {
            be.fixParentLinkingWhenMoved();
            be.assembling = false;
        }
    }

    @Override
    public void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof FluxTetherLinkBlockEntity be) {
                be.notifyRemovedOutsideAssembly();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}