package dev.alyti.fluxnautics.util.placement;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.client.render.ThrusterPlacementGhostRenderer;
import dev.alyti.fluxnautics.util.placement.ghost.GhostBlocks;
import dev.alyti.fluxnautics.util.placement.ghost.PlacementOutliner;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;


@EventBusSubscriber(modid = Fluxnautics.MODID, value = Dist.CLIENT)
public final class FluxnauticsPlacementClient {

    static {
        ThrusterPlacementGhostRenderer.register();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) {
            return;
        }
        PlacementClient.tick();
        GhostBlocks.getInstance().tickGhosts();
        PlacementOutliner.getInstance().tick();
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }


        PoseStack ms = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();

        GhostBlocks.getInstance().renderAll(ms, camera);
        PlacementOutliner.getInstance().render(ms, camera);

        mc.renderBuffers().bufferSource().endBatch();
    }

    private FluxnauticsPlacementClient() {}
}
