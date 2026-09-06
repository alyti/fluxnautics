package dev.alyti.fluxnautics.client.render;

import dev.alyti.fluxnautics.content.blocks.thruster.IonThrusterBlock;
import dev.alyti.fluxnautics.content.blocks.thruster.ThrusterPlacementHelper;
import dev.alyti.fluxnautics.util.placement.IPlacementHelper;
import dev.alyti.fluxnautics.util.placement.PlacementClient;
import dev.alyti.fluxnautics.util.placement.PlacementOffset;
import dev.alyti.fluxnautics.util.placement.ghost.GhostBlockParams;
import dev.alyti.fluxnautics.util.placement.ghost.GhostBlockRenderer;
import dev.alyti.fluxnautics.util.placement.ghost.GhostBlocks;
import dev.alyti.fluxnautics.util.placement.ghost.PlacementOutliner;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

public final class ThrusterPlacementGhostRenderer {

    public static void register() {
        PlacementClient.registerRenderer(ThrusterPlacementHelper.class, ThrusterPlacementGhostRenderer::render);
    }

    public static void render(IPlacementHelper helper, BlockPos pos, BlockState state, BlockHitResult ray, PlacementOffset offset) {
        if (!offset.hasGhostState()) {
            return;
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        BlockState ghostState = offset.getTransform().apply(offset.getGhostState());
        ThrusterPlacementHelper.ThrusterPlacementSettings settings = ThrusterPlacementHelper.ThrusterPlacementSettings.get(player);
        int predictedGroup = settings.groupAuto()
                ? ThrusterPlacementHelper.ThrusterPlacementSupport.findInheritedGroup(player.level(), offset.getBlockPos(), ghostState.getValue(IonThrusterBlock.FACING))
                : settings.forcedGroup();

        GhostBlockParams params = GhostBlockParams.of(ghostState)
                .at(offset.getBlockPos())
                .breathingAlpha();
        float[] tint = predictedGroup >= 0
                ? ThrusterPlacementHelper.ThrusterPlacementSupport.groupColor(predictedGroup)
                : new float[] {1f, 1f, 1f};
        params.tint(tint[0], tint[1], tint[2]);
        GhostBlocks.getInstance().showGhost(helper, GhostBlockRenderer.transparent(), params, 1);

        AABB cell = new AABB(offset.getBlockPos()).deflate(0.005);
        PlacementOutliner.getInstance().showBox(helper, cell, tint[0], tint[1], tint[2], 0.6f);
    }

    private ThrusterPlacementGhostRenderer() {}
}
