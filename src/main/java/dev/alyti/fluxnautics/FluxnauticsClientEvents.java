package dev.alyti.fluxnautics;

import dev.alyti.fluxnautics.api.content.tooltip.IHoverTooltip;
import dev.alyti.fluxnautics.api.content.tooltip.TooltipManager;
import dev.alyti.fluxnautics.api.interaction.HoldInteractionManager;
import dev.alyti.fluxnautics.api.interaction.HoldTipManager;
import dev.alyti.fluxnautics.client.render.field.FieldRenderer;
import dev.alyti.fluxnautics.registry.FNInteractions;
import dev.alyti.fluxnautics.util.FNDistUtil;
import dev.ryanhcode.sable.util.SableDistUtil;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import dev.alyti.fluxnautics.api.interaction.InteractCallback.Result;
import dev.alyti.fluxnautics.api.interaction.InteractCallback;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = Fluxnautics.MODID, value = Dist.CLIENT)
public class FluxnauticsClientEvents {

    /**
     * Called from loader specific implementation of events. Used to call separate onInput inside of client handlers. Can be cancelled.
     *
     * @param input     The button being pressed. Use {@link org.lwjgl.glfw.GLFW#GLFW_KEY_SPACE GLFW} keys to determine what button is being pressed
     * @param modifiers The modifier key that is being applied to this action.
     * @param action    The action of this key. Use {@link org.lwjgl.glfw.GLFW#GLFW_RELEASE GLFW} Action keys.
     */
    public static Result onBeforeMouseInput(final InteractCallback.Input input, final int modifiers, final int action) {
        final Minecraft mc = Minecraft.getInstance();
        final InteractCallback.KeyMappings mappings = InteractCallback.KeyMappings.getMappings();

        if (mc.screen != null) {
            return Result.empty();
        }

        for (final InteractCallback interactCallback : FNInteractions.CLICK_INTERACTION_ENTRIES) {
            final Result returnEvent = InteractCallback.filterInteract(interactCallback, input, modifiers, action, mappings);

            if (!Result.empty().equals(returnEvent)) {
                return returnEvent;
            }
        }

        return Result.empty();
    }


    /**
     * Called from loader specific implementation of events. Used to call onMouseMove inside of client handlers. Can be cancelled.
     *
     * @param yaw    The resulting change in yaw
     * @param pitch  The modifier key that is being applied to this action.
     */
    public static Result onMouseMove(final double yaw, final double pitch) {
        final Minecraft mc = Minecraft.getInstance();

        if (mc.screen != null) {
            return Result.empty();
        }

        for (final InteractCallback interactCallback : FNInteractions.CLICK_INTERACTION_ENTRIES) {
            final Result returnEvent = interactCallback.onMouseMove(yaw, pitch);
            if (!Result.empty().equals(returnEvent)) {
                return returnEvent;
            }
        }

        return Result.empty();
    }


    @SubscribeEvent
    public static void onAfterMouseInput(final InputEvent.MouseButton.Post event) {
    }

    @SubscribeEvent
    public static void playerInteractRightClickBlock(final PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().isLocalPlayer()) {
            if (HoldInteractionManager.isActive()) {
                event.setCancellationResult(InteractionResult.FAIL);
                event.setCanceled(true);
                return;
            }
        }

//        if (event.getItemStack().is(SimItems.HONEY_GLUE)) {
//            event.setUseBlock(TriState.FALSE);
//            if (event.getLevel().isClientSide) {
//                FNInteractions.HONEY_GLUE_MANAGER.selectPos(event.getPos(), event.getEntity(), event.getItemStack());
//            }
//            event.setCancellationResult(InteractionResult.SUCCESS);
//            event.setCanceled(true);
//        }
    }

    public static Result onMouseScroll(final double deltaX, final double deltaY) {
        if (Minecraft.getInstance().screen == null) {
            for (final InteractCallback interactCallback : FNInteractions.CLICK_INTERACTION_ENTRIES) {
                final Result result = interactCallback.onScroll(deltaX, deltaY);

                if (!Result.empty().equals(result)) {
                    return result;
                }
            }
        }

        return Result.empty();
    }

//    @SubscribeEvent
//    public static void preClientTick(final ClientTickEvent.Pre event) {
//
//    }


    @SubscribeEvent
    public static void postClientTick(final ClientTickEvent.Post event) {
        FluxnauticsClient.MEKA_ROBIT_CLIENT_HANDLER.tick();
        FluxnauticsClient.MEKA_TOOL_CLIENT_HANDLER.tick();

        if (Minecraft.getInstance().player == null || Minecraft.getInstance().level == null) {
            return;
        }

        final Level level = SableDistUtil.getClientLevel();
        final LocalPlayer player = (LocalPlayer) FNDistUtil.getClientPlayer();

        if (Minecraft.getInstance().screen != null) {
            HoldInteractionManager.stop();
        }

        for (final InteractCallback interactCallback : FNInteractions.CLICK_INTERACTION_ENTRIES) {
            interactCallback.clientTick(level, player);
        }

        HoldInteractionManager.tick(level, player);
        HoldTipManager.tick();
        FieldRenderer.tick();
    }

    @SubscribeEvent
    public static void postRenderGui(final RenderGuiEvent.Post event) {
//        event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }



    @SubscribeEvent
    public static void guiRenderPost(RenderGuiEvent.Post event) {
        final GuiGraphics graphics = event.getGuiGraphics();
        final int width = graphics.guiWidth();
        final int height = graphics.guiHeight();

        HoldInteractionManager.renderOverlay(graphics, width, height);

        boolean active = false;
        Minecraft instance = Minecraft.getInstance();
        if (instance.hitResult != null && instance.hitResult.getType().equals(HitResult.Type.BLOCK)) {
            BlockHitResult blockHitResult = (BlockHitResult) instance.hitResult;
            if (SableDistUtil.getClientLevel().getBlockEntity(blockHitResult.getBlockPos()) instanceof TileEntityMekanism tile) {
                if (tile instanceof IHoverTooltip) {
                    TooltipManager.renderSelected(
                        graphics,
                        tile
                    );
                    active = true;
                }
            }
        }
    }
}
