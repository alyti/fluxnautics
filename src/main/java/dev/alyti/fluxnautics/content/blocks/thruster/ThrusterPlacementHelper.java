package dev.alyti.fluxnautics.content.blocks.thruster;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import javax.annotation.Nullable;

import dev.alyti.fluxnautics.network.packets.ThrusterPlacementSyncPacket;
import mekanism.common.lib.multiblock.IMultiblock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import dev.alyti.fluxnautics.util.placement.IPlacementHelper;
import dev.alyti.fluxnautics.util.placement.PlacementOffset;
import mekanism.common.util.WorldUtils;

public class ThrusterPlacementHelper implements IPlacementHelper {

    @Override
    public java.util.function.Predicate<ItemStack> getItemPredicate() {
        return stack -> stack.getItem() instanceof IonThrusterBlockItem;
    }

    @Override
    public java.util.function.Predicate<BlockState> getStatePredicate() {
        return state -> state.getBlock() instanceof IonThrusterBlock;
    }

    @Override
    public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos, BlockHitResult ray) {
        return getOffset(player, world, state, pos, ray, ItemStack.EMPTY);
    }

    @Override
    public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos, BlockHitResult ray,
                                     ItemStack heldItem) {
        IonThrusterBlockEntity thruster = WorldUtils.getTileEntity(IonThrusterBlockEntity.class, world, pos);
        if (thruster == null) {
            return PlacementOffset.fail();
        }

        Direction facing = state.getValue(IonThrusterBlock.FACING);
        ThrusterPlacementSettings settings = ThrusterPlacementSettings.get(player);
        Direction depth = facing.getOpposite();

        PlacementOffset offset;

        List<Direction> wallDirs = IPlacementHelper.orderedByDistanceExceptAxis(pos, ray.getLocation(), facing.getAxis());
        for (Direction wallDir : wallDirs) {
            offset = tryOffset(world, pos.relative(wallDir), facing, player, heldItem, ray, settings);
            if (offset != null) {
                return offset;
            }
        }

        for (Direction wallDir : wallDirs) {
            offset = tryOffset(world, pos.relative(wallDir).relative(depth), facing, player, heldItem, ray, settings);
            if (offset != null) {
                return offset;
            }
        }

        offset = tryOffset(world, pos.relative(depth), facing, player, heldItem, ray, settings);
        if (offset != null) {
            return offset;
        }

        return PlacementOffset.fail();
    }

    @Nullable
    private PlacementOffset tryOffset(Level world, BlockPos newPos, Direction neighborFacing, Player player,
                                      ItemStack heldItem, BlockHitResult ray, ThrusterPlacementSettings settings) {
        if (!world.getBlockState(newPos).canBeReplaced()) {
            return null;
        }
        Direction resolved = ThrusterPlacementSupport.resolveFacing(settings.facing(), player, ray.getDirection(), neighborFacing);
        Function<BlockState, BlockState> transform = s -> s.setValue(IonThrusterBlock.FACING, resolved);
        BlockState ghost = ThrusterPlacementSupport.ghostStateFor(world, newPos, player, InteractionHand.MAIN_HAND,
            heldItem, Vec3.atCenterOf(newPos), ray.getDirection());
        return PlacementOffset.success(newPos, transform).withGhostState(ghost);
    }

    public static final class ThrusterPlacementSupport {
        public static Direction resolveFacing(ThrusterRadials.ThrusterFacingMode mode, Player player, @Nullable Direction clickedFace,
                                              @Nullable Direction neighborFacing) {
            return switch (mode) {
                case AUTO -> neighborFacing != null ? neighborFacing : player.getDirection().getOpposite();
                case FACE_PLAYER -> player.getDirection();
                case FACE_AWAY -> player.getDirection().getOpposite();
                case FACE_CLICK -> clickedFace != null ? clickedFace.getOpposite() : player.getDirection().getOpposite();
            };
        }

        public static int findInheritedGroup(Level level, BlockPos pos, Direction facing) {
            for (Direction direction : Direction.values()) {
                IonThrusterBlockEntity neighbor = WorldUtils.getTileEntity(IonThrusterBlockEntity.class, level, pos.relative(direction));
                if (neighbor == null || neighbor.getBlockState().getValue(IonThrusterBlock.FACING) != facing) {
                    continue;
                }
                return neighbor.group;
            }
            return -1;
        }

        public static float[] groupColor(int groupId) {
            float hue = (groupId * 0.618034f) % 1f;
            if (hue < 0) {
                hue += 1f;
            }
            int rgb = Color.HSBtoRGB(hue, 0.6f, 1.0f);
            return new float[] {(rgb >> 16 & 0xFF) / 255f, (rgb >> 8 & 0xFF) / 255f, (rgb & 0xFF) / 255f};
        }


        @Nullable
        public static IMultiblock<?> findFormedArray(Level level, BlockPos controllerPos) {
            return WorldUtils.getTileEntity(level, controllerPos) instanceof IMultiblock<?> multiblock
                && multiblock.getMultiblock() != null && multiblock.getMultiblock().isFormed() ? multiblock : null;
        }

        private record GroupCacheKey(net.minecraft.resources.ResourceKey<Level> dimension, BlockPos pos) {}

        private static final Map<GroupCacheKey, CachedGroups> GROUP_CACHE = new HashMap<>();
        private static final long GROUP_CACHE_TTL_MS = 1_000;

        private record CachedGroups(List<Integer> groups, long expiresAt) {}

        public static List<Integer> collectArrayGroups(Level level, BlockPos controllerPos) {
            GroupCacheKey key = new GroupCacheKey(level.dimension(), controllerPos);
            synchronized (GROUP_CACHE) {
                CachedGroups cached = GROUP_CACHE.get(key);
                long now = System.currentTimeMillis();
                if (cached != null && cached.expiresAt > now) {
                    return cached.groups;
                }
                List<Integer> groups = new ArrayList<>();
                IMultiblock<?> multiblock = findFormedArray(level, controllerPos);
                if (multiblock != null) {
                    for (BlockPos location : multiblock.getMultiblock().locations) {
                        IonThrusterBlockEntity thruster = WorldUtils.getTileEntity(IonThrusterBlockEntity.class, level, location);
                        if (thruster != null && !groups.contains(thruster.group)) {
                            groups.add(thruster.group);
                        }
                    }
                }
                groups.sort(Comparator.naturalOrder());
                GROUP_CACHE.put(key, new CachedGroups(groups, now + GROUP_CACHE_TTL_MS));
                return groups;
            }
        }

        public static BlockState ghostStateFor(Level level, BlockPos ghostPos, Player player, InteractionHand hand,
                                               ItemStack heldStack, Vec3 hitVec, Direction hitFace) {
            BlockPlaceContext synthetic = new BlockPlaceContext(player, hand, heldStack,
                new BlockHitResult(hitVec, hitFace, ghostPos, false));
            BlockState state = heldStack.getItem() instanceof IonThrusterBlockItem item
                ? item.getBlock().getStateForPlacement(synthetic)
                : null;
            if (state == null) {
                state = heldStack.getItem() instanceof IonThrusterBlockItem item
                    ? item.getBlock().defaultBlockState()
                    : level.getBlockState(ghostPos);
            }
            return state;
        }
    }

    public static record ThrusterPlacementSettings(ThrusterRadials.ThrusterFacingMode facing, ThrusterRadials.ThrusterSnapMode snap, boolean groupAuto,
                                                   int forcedGroup, @Nullable BoundArray bound, List<Integer> knownGroups) {

        public static final ThrusterPlacementSettings DEFAULT = new ThrusterPlacementSettings(ThrusterRadials.ThrusterFacingMode.AUTO,
            ThrusterRadials.ThrusterSnapMode.FREE, true, 0, null, List.of());

        public record BoundArray(ResourceKey<Level> dimension, BlockPos controllerPos) {}

        private static final Map<UUID, ThrusterPlacementSettings> SERVER_SETTINGS = new ConcurrentHashMap<>();

        private static volatile ThrusterPlacementSettings CLIENT_SETTINGS = DEFAULT;

        public static ThrusterPlacementSettings get(@Nullable Player player) {
            if (player == null || player.level().isClientSide) {
                return CLIENT_SETTINGS;
            }
            return SERVER_SETTINGS.getOrDefault(player.getUUID(), DEFAULT);
        }

        public static void setClient(ThrusterPlacementSettings settings) {
            CLIENT_SETTINGS = settings;
        }

        public static ThrusterPlacementSettings updateServer(ServerPlayer player, UnaryOperator<ThrusterPlacementSettings> update) {
            ThrusterPlacementSettings updated = update.apply(get(player)).withRefreshedGroups(player);
            SERVER_SETTINGS.put(player.getUUID(), updated);
            ThrusterPlacementSyncPacket.send(player, updated);
            return updated;
        }

        private ThrusterPlacementSettings withRefreshedGroups(ServerPlayer player) {
            if (bound == null) {
                return knownGroups.isEmpty() ? this : new ThrusterPlacementSettings(facing, snap, groupAuto, forcedGroup, null, List.of());
            }
            return new ThrusterPlacementSettings(facing, snap, groupAuto, forcedGroup, bound,
                ThrusterPlacementSupport.collectArrayGroups(player.serverLevel(), bound.controllerPos()));
        }

        public static void clearServer(UUID playerId) {
            SERVER_SETTINGS.remove(playerId);
        }
        public boolean isBound() {
            return bound != null;
        }
        public int effectiveGroup(int inheritedGroup) {
            return groupAuto ? inheritedGroup : forcedGroup;
        }

        public static net.minecraft.network.chat.Component boundLabel(BoundArray bound) {
            BlockPos pos = bound.controllerPos();
            return Component.translatable("placement.fluxnautics.bound_label", pos.getX(), pos.getY(), pos.getZ())
                .withStyle(ChatFormatting.GRAY);
        }
    }
}
