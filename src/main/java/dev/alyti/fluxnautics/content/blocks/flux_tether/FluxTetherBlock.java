package dev.alyti.fluxnautics.content.blocks.flux_tether;

import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import mekanism.common.block.prefab.BlockTileGlass;
import mekanism.common.content.blocktype.BlockTypeTile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.NonNull;

public class FluxTetherBlock extends  BlockTileGlass<FluxTetherBlockEntity, BlockTypeTile<FluxTetherBlockEntity>> implements BlockSubLevelAssemblyListener {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ASSEMBLED = BooleanProperty.create("assembled");
    public static final BooleanProperty POWERED   = BlockStateProperties.POWERED;

    public FluxTetherBlock(BlockTypeTile<FluxTetherBlockEntity> type) {
        super(type);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(ASSEMBLED, false)
            .setValue(POWERED, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(ASSEMBLED).add(POWERED));
    }

    @Override
    protected @NonNull ItemInteractionResult useItemOn(@NonNull ItemStack stack, @NonNull BlockState state, @NonNull Level level, @NonNull BlockPos pos,
                                                       Player player, @NonNull InteractionHand hand, @NonNull BlockHitResult hitResult) {
        if (player.getItemInHand(hand).isEmpty()) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            if (level.getBlockEntity(pos) instanceof FluxTetherBlockEntity be) {
                if (!player.isShiftKeyDown())
                    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                if (!player.mayBuild())
                    return ItemInteractionResult.FAIL;
                be.assembleNextTick = true;
            }
            return ItemInteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    public void beforeMove(ServerLevel originLevel, ServerLevel resultingLevel, BlockState newState,
                           BlockPos oldPos, BlockPos newPos) {
        if (originLevel.getBlockEntity(oldPos) instanceof FluxTetherBlockEntity be) {
            be.beforeAssembly();
        }
    }

    @Override
    public void afterMove(ServerLevel originLevel, ServerLevel resultingLevel, BlockState newState,
                          BlockPos oldPos, BlockPos newPos) {
        if (resultingLevel.getBlockEntity(newPos) instanceof FluxTetherBlockEntity be) {
            be.assembling = false;
            be.onMovedByForeignAssembly();
        }
    }

    @Override
    public void onRemove(BlockState state, @NonNull Level level, @NonNull BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof FluxTetherBlockEntity be) {
                be.dropEphemeralLink();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}