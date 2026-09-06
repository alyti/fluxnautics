package dev.alyti.fluxnautics.content.items;

import dev.alyti.fluxnautics.content.blocks.conduit.AbstractConduitHolderBlockEntity;
import dev.alyti.fluxnautics.content.blocks.conduit.TileComponentConduitStrandHolder;
import dev.alyti.fluxnautics.registry.FNDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class ConduitItem extends Item {
    public ConduitItem(Properties properties) {
        super(properties);
    }

    /**
     * Checks if a location is valid for rope to attach to
     *
     * @param level    the level to check in
     * @param blockPos the block to check
     * @return if rope can attach to this block
     */
    public static boolean isValidConduitAttachment(final Level level, final BlockPos blockPos) {
        boolean validLocation = false;
        if (level.getBlockEntity(blockPos) instanceof final AbstractConduitHolderBlockEntity smartBlockEntity) {
            final TileComponentConduitStrandHolder<?> behavior = smartBlockEntity.getStrandHolder();

            if (behavior != null && !behavior.isAttached()) {
                validLocation = true;
            }
        }
        return validLocation;
    }

    public static TileComponentConduitStrandHolder<?> getRopeHolder(final Level level, final BlockPos blockPos) {
        TileComponentConduitStrandHolder<?> holder = null;
        if (level.getBlockEntity(blockPos) instanceof final AbstractConduitHolderBlockEntity smartBlockEntity) {
            final TileComponentConduitStrandHolder<?> behavior = smartBlockEntity.getStrandHolder();

            if (behavior != null) {
                holder = behavior;
            }
        }
        return holder;
    }


    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final BlockPos clickedPos = context.getClickedPos();
        final Level level = context.getLevel();
        final ItemStack heldStack = context.getItemInHand();
        final Player player = context.getPlayer();

        final boolean validLocation = isValidConduitAttachment(level, clickedPos);

        if (player != null && player.isShiftKeyDown()) {
            heldStack.remove(FNDataComponents.CONDUIT_FIRST_CONNECTION);
            return InteractionResult.SUCCESS;
        }

        if (validLocation) {
            if (heldStack.has(FNDataComponents.CONDUIT_FIRST_CONNECTION)) {
                if (heldStack.getCount() < 2 && (player != null && !player.hasInfiniteMaterials())) {
                    player.sendSystemMessage(Component.literal("You need at least two cores to bind the connectors."));
                    return InteractionResult.SUCCESS;
                }

                if (!level.isClientSide) {
                    if (!this.attachConduit(level, heldStack.get(FNDataComponents.CONDUIT_FIRST_CONNECTION), clickedPos, !player.hasInfiniteMaterials())) {
                        heldStack.remove(FNDataComponents.CONDUIT_FIRST_CONNECTION);
                        return InteractionResult.SUCCESS;
                    }
                }

                heldStack.remove(FNDataComponents.CONDUIT_FIRST_CONNECTION);

                if (!player.hasInfiniteMaterials())
                    context.getItemInHand()
                        .shrink(2);

                return InteractionResult.SUCCESS;
            }

            heldStack.set(FNDataComponents.CONDUIT_FIRST_CONNECTION, clickedPos);
            return InteractionResult.SUCCESS;
        }

        return super.useOn(context);
    }


    /**
     * Attaches a rope between two positions
     *
     * @param posA the position of the first block entity, assumed to have a {@link TileComponentConduitStrandHolder}
     * @param posB the position of the second block entity, assumed to have a {@link TileComponentConduitStrandHolder}
     */
    private boolean attachConduit(final Level level, final BlockPos posA, final BlockPos posB, final boolean dropItem) {
        TileComponentConduitStrandHolder<?> ropeHolderA = getRopeHolder(level, posA);
        if (ropeHolderA == null) return false;

        TileComponentConduitStrandHolder<?> ropeHolderB = getRopeHolder(level, posB);
        if (ropeHolderB == null) return false;

//        if (ropeHolderB.getTile() instanceof RopeWinchBlockEntity && !(ropeHolderA.blockEntity instanceof RopeWinchBlockEntity)) {
//            final TileComponentConduitStrandHolder<?> temp = ropeHolderA;
//            ropeHolderA = ropeHolderB;
//            ropeHolderB = temp;
//        }
//
//        if (ropeHolderA.getTile() instanceof RopeWinchBlockEntity && ropeHolderB.blockEntity instanceof RopeWinchBlockEntity) {
//            return false;
//        }

        if (ropeHolderA.createConduit(ropeHolderB, dropItem)) {
            level.playSound(null, posA, SoundEvents.ALLAY_THROW, SoundSource.BLOCKS, 0.5F, 1F);
            level.playSound(null, posB, SoundEvents.ALLAY_THROW, SoundSource.BLOCKS, 0.5F, 1F);
            return true;
        }
        return false;
    }
}
