package dev.alyti.fluxnautics.util.placement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import dev.alyti.fluxnautics.util.placement.ghost.GhostBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public interface IPlacementHelper {

    Predicate<ItemStack> getItemPredicate();

    Predicate<BlockState> getStatePredicate();

    PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos, BlockHitResult ray);

    default PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos, BlockHitResult ray, ItemStack heldItem) {
        PlacementOffset offset = getOffset(player, world, state, pos, ray);
        if (heldItem.getItem() instanceof BlockItem blockItem)
            offset = offset.withGhostState(blockItem.getBlock().defaultBlockState());
        return offset;
    }

    default void renderAt(BlockPos pos, BlockState state, BlockHitResult ray, PlacementOffset offset) {
        displayGhost(offset);
    }

    default void displayGhost(PlacementOffset offset) {
        if (!offset.hasGhostState())
            return;
        GhostBlocks.getInstance().showGhostState(this, offset.getTransform().apply(offset.getGhostState()))
                .at(offset.getBlockPos());
    }

    static List<Direction> orderedByDistanceOnlyAxis(BlockPos pos, Vec3 hit, Direction.Axis axis) {
        return orderedByDistance(pos, hit, dir -> dir.getAxis() == axis);
    }

    static List<Direction> orderedByDistanceOnlyAxis(BlockPos pos, Vec3 hit, Direction.Axis axis, Predicate<Direction> includeDirection) {
        return orderedByDistance(pos, hit, ((Predicate<Direction>) dir -> dir.getAxis() == axis).and(includeDirection));
    }

    static List<Direction> orderedByDistanceExceptAxis(BlockPos pos, Vec3 hit, Direction.Axis axis) {
        return orderedByDistance(pos, hit, dir -> dir.getAxis() != axis);
    }

    static List<Direction> orderedByDistanceExceptAxis(BlockPos pos, Vec3 hit, Direction.Axis axis, Predicate<Direction> includeDirection) {
        return orderedByDistance(pos, hit, ((Predicate<Direction>) dir -> dir.getAxis() != axis).and(includeDirection));
    }

    static List<Direction> orderedByDistanceExceptAxis(BlockPos pos, Vec3 hit, Direction.Axis first, Direction.Axis second) {
        return orderedByDistanceExceptAxis(pos, hit, first, d -> d.getAxis() != second);
    }

    static List<Direction> orderedByDistanceExceptAxis(BlockPos pos, Vec3 hit, Direction.Axis first, Direction.Axis second, Predicate<Direction> includeDirection) {
        return orderedByDistanceExceptAxis(pos, hit, first, ((Predicate<Direction>) d -> d.getAxis() != second).and(includeDirection));
    }

    static List<Direction> orderedByDistance(BlockPos pos, Vec3 hit) {
        return orderedByDistance(pos, hit, _$ -> true);
    }

    static List<Direction> orderedByDistance(BlockPos pos, Vec3 hit, Predicate<Direction> includeDirection) {
        List<Direction> directions = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            if (includeDirection.test(dir)) {
                directions.add(dir);
            }
        }
        return orderedByDistance(pos, hit, directions);
    }

    static List<Direction> orderedByDistance(BlockPos pos, Vec3 hit, Collection<Direction> directions) {
        Vec3 centerToHit = hit.subtract(Vec3.atCenterOf(pos));

        record DirDist(Direction dir, double dist) {}

        List<DirDist> distances = new ArrayList<>();
        for (Direction dir : directions) {
            distances.add(new DirDist(dir, Vec3.atLowerCornerOf(dir.getNormal()).distanceTo(centerToHit)));
        }

        distances.sort(Comparator.comparingDouble(DirDist::dist));

        List<Direction> sortedDirections = new ArrayList<>();
        for (DirDist p : distances) {
            sortedDirections.add(p.dir());
        }

        return sortedDirections;
    }

    default boolean matchesItem(ItemStack item) {
        return getItemPredicate().test(item);
    }

    default boolean matchesState(BlockState state) {
        return getStatePredicate().test(state);
    }
}
