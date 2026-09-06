package dev.alyti.fluxnautics.content.blocks.thruster;

import dev.alyti.fluxnautics.lang.FNLang;
import mekanism.api.text.EnumColor;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.prefab.BlockTileGlass;
import mekanism.common.content.blocktype.BlockTypeTile;
import mekanism.common.registries.MekanismDataComponents;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public class IonThrusterBlock extends BlockTileGlass<IonThrusterBlockEntity, BlockTypeTile<IonThrusterBlockEntity>> {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");
    public static final BooleanProperty UP_LEFT = BooleanProperty.create("up_left");
    public static final BooleanProperty UP_RIGHT = BooleanProperty.create("up_right");
    public static final BooleanProperty DOWN_LEFT = BooleanProperty.create("down_left");
    public static final BooleanProperty DOWN_RIGHT = BooleanProperty.create("down_right");
    public static final BooleanProperty FRONT =  BooleanProperty.create("front");


    public IonThrusterBlock(BlockTypeTile<IonThrusterBlockEntity> type) {
        super(type);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH)
            .setValue(UP, false)
            .setValue(DOWN, false)
            .setValue(LEFT, false)
            .setValue(RIGHT, false)
            .setValue(UP_LEFT, false)
            .setValue(UP_RIGHT, false)
            .setValue(DOWN_LEFT, false)
            .setValue(DOWN_RIGHT, false)
            .setValue(FRONT, false));
    }

     @Nullable
     @Override
     public BlockState getStateForPlacement(BlockPlaceContext context) {
         Level level = context.getLevel();
         BlockPos pos = context.getClickedPos();
         Player player = context.getPlayer();
         ThrusterPlacementHelper.ThrusterPlacementSettings settings = ThrusterPlacementHelper.ThrusterPlacementSettings.get(player);

         if (settings.snap().isExtendOnly() && player != null && !player.isShiftKeyDown()
                 && !hasThrusterNeighbor(level, pos)) {
             return null;
         }

          BlockState state = this.defaultBlockState();
         for (Direction direction : Direction.values()) {
             BlockState neighborState = level.getBlockState(pos.relative(direction));
             if (neighborState.is(this)) {
                 state = state.setValue(FACING, neighborState.getValue(FACING));
                 break;
             }
         }

         if (player != null && settings.facing() != ThrusterRadials.ThrusterFacingMode.AUTO) {
             Direction resolved = ThrusterPlacementHelper.ThrusterPlacementSupport.resolveFacing(settings.facing(), player,
                     context.getClickedFace(), null);
             state = state.setValue(FACING, resolved);
         } else {
             state = state.setValue(FACING, context.getClickedFace().getOpposite());
         }

         return updateConnections(state, level, pos);
     }

     @Override
     public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state,
             @Nullable LivingEntity placer, @NotNull ItemStack stack) {
         super.setPlacedBy(level, pos, state, placer, stack);
         if (level.isClientSide) {
             return;
         }
         IonThrusterBlockEntity placed = WorldUtils.getTileEntity(IonThrusterBlockEntity.class, level, pos);
         if (placed == null) {
             return;
         }

         Player player = placer instanceof Player p ? p : null;
         ThrusterPlacementHelper.ThrusterPlacementSettings settings = ThrusterPlacementHelper.ThrusterPlacementSettings.get(player);

         if (stack.has(MekanismDataComponents.RADIUS)) {
             int explicit = stack.get(MekanismDataComponents.RADIUS);
             if (placed.group != explicit) {
                 placed.group = explicit;
                 placed.setChanged();
             }
             if (player != null) {
                 player.displayClientMessage(Component.translatable(
                         "placement.fluxnautics.messages.placed.item", explicit), true);
             }
             return;
         }

         int inherited = -1;
         Direction facing = state.getValue(FACING);
         for (Direction direction : Direction.values()) {
             IonThrusterBlockEntity neighbor = WorldUtils.getTileEntity(IonThrusterBlockEntity.class,
                     level, pos.relative(direction));
             if (neighbor == null || neighbor.getBlockState().getValue(FACING) != facing) {
                 continue;
             }
             inherited = neighbor.group;
             break;
         }

         int resolved = settings.effectiveGroup(Math.max(inherited, 0));
         if (placed.group != resolved) {
             placed.group = resolved;
             placed.setChanged();
         }
         if (player != null) {
             player.displayClientMessage(Component.translatable(
                     settings.groupAuto() && inherited >= 0
                             ? "placement.fluxnautics.messages.placed.auto"
                             : "placement.fluxnautics.messages.placed.forced",
                     resolved), true);
         }
     }

    @Override
    protected @NonNull BlockState updateShape(BlockState state, @NonNull Direction facing, @NonNull BlockState facingState, @NonNull LevelAccessor level, @NonNull BlockPos currentPos, @NonNull BlockPos facingPos) {
        return updateConnections(state, level, currentPos);
    }

    private BlockState updateConnections(BlockState state, LevelAccessor level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        Direction up, down, left, right;

        if (facing.getAxis() == Direction.Axis.Y) {
            up = Direction.NORTH;
            down = Direction.SOUTH;
            left = Direction.WEST;
            right = Direction.EAST;
        } else {
            up = Direction.UP;
            down = Direction.DOWN;
            left = facing.getClockWise();
            right = facing.getCounterClockWise();
        }

        return state.setValue(UP, isThruster(level, pos.relative(up), facing))
            .setValue(DOWN, isThruster(level, pos.relative(down), facing))
            .setValue(LEFT, isThruster(level, pos.relative(left), facing))
            .setValue(RIGHT, isThruster(level, pos.relative(right), facing))
            .setValue(UP_LEFT, isThruster(level, pos.relative(up).relative(left), facing))
            .setValue(UP_RIGHT, isThruster(level, pos.relative(up).relative(right), facing))
            .setValue(DOWN_LEFT, isThruster(level, pos.relative(down).relative(left), facing))
            .setValue(DOWN_RIGHT, isThruster(level, pos.relative(down).relative(right), facing))
            .setValue(FRONT, isThruster(level, pos.relative(facing.getOpposite()), facing))
            ;
    }

     private static boolean hasThrusterNeighbor(Level level, BlockPos pos) {
         for (Direction direction : Direction.values()) {
             if (level.getBlockState(pos.relative(direction)).getBlock() instanceof IonThrusterBlock) {
                 return true;
             }
         }
         return false;
     }

    private boolean isThruster(LevelAccessor level, BlockPos pos, Direction facing) {
        BlockState state = level.getBlockState(pos);
        return state.is(this) && state.getValue(FACING) == facing;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NonNull Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(UP, DOWN, LEFT, RIGHT, UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT, FRONT);
    }

    @NotNull
    @Override
    protected InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
        IonThrusterBlockEntity tile = WorldUtils.getTileEntity(IonThrusterBlockEntity.class, world, pos);
        if (tile == null) {
            return InteractionResult.PASS;
        } else if (world.isClientSide) {
            return InteractionResult.PASS;
        }

        BlockPos relPos = tile.getMultiblock().getRelativePos(pos);
        player.displayClientMessage(FNLang.THRUSTER_META.translateColored(EnumColor.GRAY, String.format("relPos=%s facing=%s group=%d", relPos, Attribute.getFacing(state), tile.group)), true);
        return InteractionResult.PASS;
    }

    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        if (state.getValue(FRONT)) {
            return Shapes.block();
        }
        return super.getShape(state, world, pos, context);
    }
}