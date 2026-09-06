package dev.alyti.fluxnautics.content.blocks.conduit;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.alyti.fluxnautics.content.blocks.conduit.strand.client.ConduitStrandRenderer;
import mekanism.client.render.tileentity.MekanismTileEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.ProfilerFiller;

import java.awt.*;

public class ConduitConnectorRenderer extends MekanismTileEntityRenderer<ConduitConnectorBlockEntity> {
    public ConduitConnectorRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void render(ConduitConnectorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlayLight, ProfilerFiller profiler) {
        ConduitStrandRenderer.render(be, be.getStrandHolder(), partialTicks, ms, buffer);
    }

//    public static Map<ResourceLocation, Float> getAggregatedUsage(ConduitConnectorBlockEntity be) {
//        Map<ResourceLocation, Float> aggregated = new HashMap<>();
//        for (Direction dir : Direction.values()) {
//            if (dir == be.getDirection()) continue; // Skip front
//            BlockPos portPos = be.getBlockPos().relative(dir);
//            if (be.getLevel().getBlockEntity(portPos) instanceof ConduitPortBlockEntity port) {
//                for (Map.Entry<ResourceLocation, Integer> entry : port.get().entrySet()) {
//                    float usage = Math.min(1.0f, entry.getValue() / 50.0f); // Normalize 0-1
//                    aggregated.merge(entry.getKey(), usage, Float::sum);
//                }
//            }
//        }
//        return aggregated;
//    }

    // Color lookup for rendering
    public static int getColorForCapability(ResourceLocation rl) {
        return switch (rl.toString()) {
            case "minecraft:item_handler" -> 0xFFFFFF;
            case "minecraft:fluid_handler" -> 0x4A90E2;
            case "minecraft:energy_storage" -> 0xF5A623;
            case "computercraft:wired_element" -> 0xFF5500;
            default -> Color.HSBtoRGB((rl.hashCode() & 0xFFFF) / 65535.0f, 0.7f, 0.8f);
        };
    }

    @Override
    protected String getProfilerSection() {
        return "";
    }
}
