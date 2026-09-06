package dev.alyti.fluxnautics.api.render;

import com.mojang.blaze3d.vertex.PoseStack;
import foundry.veil.Veil;
import foundry.veil.api.compat.IrisCompat;
import mekanism.client.render.MekanismRenderer;
import mekanism.client.render.item.MekanismISTER;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.model.data.ModelData;

public abstract class MachineISTER<T extends BlockEntity, P> extends MekanismISTER {

    private final IMachineFX<T, P> fx;

    private double frozenTime;

    protected MachineISTER(IMachineFX<T, P> fx) {
        this.fx = fx;
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {}

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {
        P params = this.fx.captureForItem(stack, context, this.clock());
        if (params == null) {
            return;
        }

        pose.pushPose();
        this.renderBase(stack, context, pose, buffers, light, overlay);
        pose.popPose();

        if (Veil.IRIS && IrisCompat.INSTANCE.areShadersLoaded())
            return;
        IMachineFX.draw(this.fx, params, pose, buffers, light, overlay);
    }

    protected void renderBase(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                              MultiBufferSource buffers, int light, int overlay) {
        if (stack.getItem() instanceof BlockItem blockItem) {
            BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
            dispatcher.renderSingleBlock(blockItem.getBlock().defaultBlockState(), pose, buffers, light, overlay, ModelData.EMPTY, (RenderType)null);
        }
    }

    protected double clock() {
        Minecraft mc = Minecraft.getInstance();
        boolean runningNormally = MekanismRenderer.isRunningNormally();
        if (!runningNormally) {
            return this.frozenTime;
        }
        double time = mc.levelRenderer.getTicks() + mc.getTimer().getGameTimeDeltaPartialTick(true);
        this.frozenTime = time;
        return time;
    }
}