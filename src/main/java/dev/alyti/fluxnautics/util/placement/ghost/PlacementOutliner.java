package dev.alyti.fluxnautics.util.placement.ghost;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import dev.alyti.fluxnautics.util.placement.PlacementClient;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.util.SublevelRenderOffsetHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class PlacementOutliner {

    private static final class BoxEntry {

        final AABB box;
        final float red, green, blue, alpha;
        int ticksToLive;

        BoxEntry(AABB box, float red, float green, float blue, float alpha, int ticksToLive) {
            this.box = box;
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.alpha = alpha;
            this.ticksToLive = ticksToLive;
        }
    }

    private static final PlacementOutliner instance = new PlacementOutliner();

    public static PlacementOutliner getInstance() {
        return instance;
    }

    private final Map<Object, BoxEntry> boxes = new HashMap<>();

    private PlacementOutliner() {}

    public void showBox(Object slot, AABB box, float red, float green, float blue, float alpha) {
        boxes.put(slot, new BoxEntry(box, red, green, blue, alpha, 1));
    }

    public void tick() {
        boxes.forEach((slot, entry) -> entry.ticksToLive--);
        boxes.entrySet().removeIf(e -> e.getValue().ticksToLive < 0);
    }

    public void render(PoseStack ms, Vec3 camera) {
        Minecraft mc = Minecraft.getInstance();
        VertexConsumer consumer = mc.renderBuffers().bufferSource().getBuffer(RenderType.lines());
        float fade = PlacementClient.getCurrentAlpha();
        boxes.forEach((slot, entry) -> {
            float a = entry.alpha * fade;
            if (a <= 0.02f) {
                return;
            }
            ms.pushPose();
            AABB box = entry.box.move(SublevelRenderOffsetHelper.translation(entry.box.getCenter()).scale(-1.0));
            SublevelRenderOffsetHelper.posePlotToProjected(Sable.HELPER.getContainingClient(entry.box.getCenter()), ms);
            ms.translate(-camera.x, -camera.y, -camera.z);
            LevelRenderer.renderLineBox(ms, consumer, box, entry.red, entry.green, entry.blue, a);
            ms.popPose();
        });
    }
}
