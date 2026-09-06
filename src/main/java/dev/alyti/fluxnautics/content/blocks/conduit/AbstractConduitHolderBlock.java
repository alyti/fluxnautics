package dev.alyti.fluxnautics.content.blocks.conduit;

import dev.alyti.fluxnautics.content.blocks.conduit.strand.server.ConduitAttachment;
import dev.alyti.fluxnautics.content.blocks.conduit.strand.server.ConduitAttachmentPoint;
import dev.alyti.fluxnautics.content.blocks.conduit.strand.server.ServerConduitStrand;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import dev.ryanhcode.sable.sublevel.SubLevel;
import mekanism.common.block.interfaces.IHasTileEntity;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.BlockTypeTile;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public abstract class AbstractConduitHolderBlock<T extends AbstractConduitHolderBlockEntity, TYPE extends BlockTypeTile<T>> extends BlockTile<T, TYPE> implements BlockSubLevelAssemblyListener, IHasTileEntity<T> {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public AbstractConduitHolderBlock(TYPE type, UnaryOperator<Properties> propertiesModifier) {
        super(type, propertiesModifier);
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

    static <T extends AbstractConduitHolderBlockEntity, TYPE extends BlockTypeTile<T>> ItemInteractionResult yeetConduit(final AbstractConduitHolderBlock<T, TYPE> block, final Level level, final BlockPos pos, final ServerPlayer player) {
        return block.onBlockEntityUseItemOn(level, pos, be -> {
            final TileComponentConduitStrandHolder<?> ropeHolder = block.getHolder(be);

            final ServerConduitStrand strand = ropeHolder.getAttachedStrand();
            if (strand == null) {
                return ItemInteractionResult.FAIL;
            }

            final ConduitAttachment ropeAttachment = strand.getAttachment(ConduitAttachmentPoint.START);
            if (ropeAttachment == null) {
                return ItemInteractionResult.FAIL;
            }
            final BlockPos attachment = ropeAttachment.blockAttachment();

            final BlockEntity blockEntity = level.getBlockEntity(attachment);

            if (!(blockEntity instanceof final AbstractConduitHolderBlockEntity smartBlockEntity)) return ItemInteractionResult.FAIL;

            final TileComponentConduitStrandHolder<?> otherHolder = smartBlockEntity.getStrandHolder();
            if (otherHolder == null) return ItemInteractionResult.FAIL;

            otherHolder.destroyConduit(player, pos.getCenter(), !player.hasInfiniteMaterials());
            return ItemInteractionResult.SUCCESS;
        });
    }

    public TileComponentConduitStrandHolder<?> getHolder(final T blockEntity) {
        return blockEntity.getStrandHolder();
    }

    @Override
    public void afterMove(final ServerLevel originLevel, final ServerLevel serverLevel, final BlockState blockState, final BlockPos oldPos, final BlockPos newPos) {
        final AtomicReference<ServerConduitStrand> ownedStrand = new AtomicReference<>();
        final AtomicReference<UUID> attachedId = new AtomicReference<>();
        this.withBlockEntityDo(originLevel, oldPos, be -> {
            final TileComponentConduitStrandHolder<?> holder = this.getHolder(be);
            ownedStrand.set(holder.getOwnedStrand());
            attachedId.set(holder.getAttachedConduitId());
            holder.detachConduit();
        });
        this.withBlockEntityDo(serverLevel, newPos, be -> {
            final TileComponentConduitStrandHolder<?> holder = this.getHolder(be);

            if (ownedStrand.get() != null) {
                holder.takeOwnedStrand(ownedStrand.get());
            } else if (attachedId.get() != null) {
                holder.restoreAttachedConduitId(attachedId.get());
            }

            final ServerConduitStrand strand = holder.getAttachedStrand();

            if (strand != null) {
                strand.getTrackingPlayers().clear();
                final SubLevel newSubLevel = Sable.HELPER.getContaining(serverLevel, newPos);
                final UUID newSubLevelId = newSubLevel != null ? newSubLevel.getUniqueId() : null;

                final ConduitAttachmentPoint point = holder.ownsConduit() ? ConduitAttachmentPoint.START : ConduitAttachmentPoint.END;
                final ConduitAttachment attachment = new ConduitAttachment(point, newSubLevelId, newPos);

                strand.addAttachment(serverLevel, point, attachment);
            }
        });
    }

    ItemInteractionResult onBlockEntityUseItemOn(BlockGetter world, BlockPos pos, Function<T, ItemInteractionResult> action) {
        return getBlockEntityOptional(world, pos).map(action)
            .orElse(ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION);
    }

    void withBlockEntityDo(BlockGetter world, BlockPos pos, Consumer<T> action) {
        getBlockEntityOptional(world, pos).ifPresent(action);
    }

    Optional<T> getBlockEntityOptional(BlockGetter world, BlockPos pos) {
        return Optional.ofNullable(getBlockEntity(world, pos));
    }


    @Nullable
    @SuppressWarnings("unchecked")
    T getBlockEntity(BlockGetter worldIn, BlockPos pos) {
        return (T) WorldUtils.getTileEntity(worldIn, pos);
    }
}
