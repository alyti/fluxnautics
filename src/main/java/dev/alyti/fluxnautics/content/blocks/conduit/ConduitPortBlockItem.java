package dev.alyti.fluxnautics.content.blocks.conduit;

import dev.alyti.fluxnautics.registry.FNBlocks;
import mekanism.api.RelativeSide;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

public class ConduitPortBlockItem extends BlockItem {
    public ConduitPortBlockItem() {
        super(FNBlocks.CONDUIT_PORT.get(), new Properties());
    }

    @Override
    public @NonNull InteractionResult place(BlockPlaceContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (!level.isClientSide) {
            BlockPos conduitPos = context.replacingClickedOnBlock() ? context.getClickedPos() : context.getClickedPos().relative(context.getClickedFace().getOpposite());

            if (!(level.getBlockEntity(conduitPos) instanceof ConduitConnectorBlockEntity conduit)) {
                if (player != null) {
                    player.sendSystemMessage(Component.literal("Must be used on Conduit Connector's side"));
                }
                return InteractionResult.FAIL;
            }

            if (!conduit.getStrandHolder().isAttached()) {
                if (player != null) {
                    player.sendSystemMessage(Component.literal("Conduit is not attached"));
                }
                return InteractionResult.FAIL;
            }

            ConduitConnectorBlockEntity remoteConduit = conduit.getRemoteConnector();
            if (remoteConduit == null) {
                if (player != null) {
                    player.sendSystemMessage(Component.literal("Conduit is not attached 2: Electric Boogaloo"));
                }
                return InteractionResult.FAIL;
            }

            Direction localFace = context.getClickedFace();
            RelativeSide localSide = RelativeSide.fromDirections(conduit.getDirection(), localFace);

            if (localSide == RelativeSide.FRONT) {
                if (player != null) {
                    player.sendSystemMessage(Component.literal("Can't place on the front of the Conduit Connector"));
                }
                return InteractionResult.FAIL; // Can't place on the conduit connection side
            }

            Direction remoteFacing = remoteConduit.getDirection();
            Direction remoteFace = localSide.getDirection(remoteFacing);

            BlockPos localPlacePos = conduitPos.relative(localFace);
            BlockPos remotePlacePos = remoteConduit.getBlockPos().relative(remoteFace);

            BlockPlaceContext localCtx = new BlockPlaceContext(context);
            if (!level.getBlockState(localPlacePos).canBeReplaced(localCtx) ||
                !level.getBlockState(remotePlacePos).canBeReplaced(localCtx)) {
                if (player != null) {
                    player.sendSystemMessage(Component.literal("No empty space on the other end (%s)".formatted(remotePlacePos)));
                }
                return InteractionResult.FAIL;
            }

            if (player != null && !player.hasInfiniteMaterials()) {
                ItemStack stack = context.getItemInHand();
                if (stack.getCount() < 2) {
                    player.sendSystemMessage(Component.literal("Must have at least 2 Conduit Ports"));
                    return InteractionResult.FAIL;
                }
                stack.shrink(2);
            }

            BlockState localState = this.getBlock().defaultBlockState().setValue(ConduitPortBlock.FACING, localFace.getOpposite());
            BlockState remoteState = this.getBlock().defaultBlockState().setValue(ConduitPortBlock.FACING, remoteFace.getOpposite());

            level.setBlock(localPlacePos, localState, 3);
            level.setBlock(remotePlacePos, remoteState, 3);

            level.playSound(player, localPlacePos, SoundEvents.VAULT_OPEN_SHUTTER, SoundSource.BLOCKS);
            level.playSound(player, remotePlacePos, SoundEvents.VAULT_OPEN_SHUTTER, SoundSource.BLOCKS);
        }


        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}