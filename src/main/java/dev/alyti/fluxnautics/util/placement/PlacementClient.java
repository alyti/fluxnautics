package dev.alyti.fluxnautics.util.placement;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class PlacementClient {

    @Nullable
    static BlockPos target = null;
    @Nullable
    static BlockPos lastTarget = null;
    static int animationTick = 0;

    public static void tick() {
        setTarget(null);
        checkHelpers();

        if (target == null) {
            if (animationTick > 0) {
                animationTick = Math.max(animationTick - 2, 0);
            }
            return;
        }

        if (animationTick < 10) {
            animationTick++;
        }
    }

    @FunctionalInterface
    public interface HelperRenderer {
        void render(IPlacementHelper helper, BlockPos pos, BlockState state, BlockHitResult ray, PlacementOffset offset);
    }

    private static final Map<Class<?>, HelperRenderer> HELPER_RENDERERS = new HashMap<>();

    public static void registerRenderer(Class<? extends IPlacementHelper> helperClass, HelperRenderer renderer) {
        HELPER_RENDERERS.put(helperClass, renderer);
    }

    private static void checkHelpers() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel world = mc.level;

        if (world == null) {
            return;
        }

        if (!(mc.hitResult instanceof BlockHitResult ray)) {
            return;
        }

        if (mc.player == null) {
            return;
        }

        if (mc.player.isShiftKeyDown()) {
            // disable all helpers while sneaking (Create parity — sneak = place without snapping)
            return;
        }

        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack heldItem = mc.player.getItemInHand(hand);

            java.util.List<IPlacementHelper> filteredForHeldItem = helpersForHeldItem(heldItem);
            if (filteredForHeldItem.isEmpty()) {
                continue;
            }

            BlockPos pos = ray.getBlockPos();
            BlockState state = world.getBlockState(pos);

            for (IPlacementHelper helper : filteredForHeldItem) {
                if (!helper.matchesState(state)) {
                    continue;
                }

                PlacementOffset offset = helper.getOffset(mc.player, world, state, pos, ray, heldItem);
                if (offset.isSuccessful()) {
                    HelperRenderer renderer = HELPER_RENDERERS.get(helper.getClass());
                    if (renderer != null) {
                        renderer.render(helper, pos, state, ray, offset);
                    } else {
                        helper.renderAt(pos, state, ray, offset);
                    }
                    setTarget(offset.getBlockPos());
                    // at least one helper activated, no need to check the offhand
                    return;
                }
            }
        }
    }

    private static java.util.List<IPlacementHelper> helpersForHeldItem(ItemStack heldItem) {
        java.util.List<IPlacementHelper> filtered = new java.util.ArrayList<>();
        for (IPlacementHelper helper : PlacementHelpers.getHelpersView()) {
            if (helper.matchesItem(heldItem)) {
                filtered.add(helper);
            }
        }
        return filtered;
    }

    static void setTarget(@Nullable BlockPos newTarget) {
        target = newTarget;

        if (target == null) {
            return;
        }

        if (lastTarget == null) {
            lastTarget = target;
            return;
        }

        if (!lastTarget.equals(target)) {
            lastTarget = target;
        }
    }

    public static float getCurrentAlpha() {
        return Math.min(animationTick / 10f, 1f);
    }

    @Nullable
    public static BlockPos getTarget() {
        return lastTarget;
    }

    private PlacementClient() {}
}
