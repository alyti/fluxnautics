package dev.alyti.fluxnautics.api.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface IMachineFX<T extends BlockEntity, P> {
    P capture(T be, float partialTick);
    P captureForItem(ItemStack stack, ItemDisplayContext context, double time);

    void render(P params, PoseStack pose, MultiBufferSource buffers, int light, int overlay);

    static <T extends BlockEntity, P> void draw(IMachineFX<T, P> fx, P params, PoseStack pose,
                                                MultiBufferSource buffers, int light, int overlay) {
        if (params == null) {
            return;
        }
        pose.pushPose();
        fx.render(params, pose, buffers, light, overlay);
        pose.popPose();
    }
}