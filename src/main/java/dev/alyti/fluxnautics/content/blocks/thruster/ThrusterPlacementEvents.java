package dev.alyti.fluxnautics.content.blocks.thruster;


import dev.alyti.fluxnautics.registry.FNBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import mekanism.common.lib.multiblock.IMultiblock;

@EventBusSubscriber(modid = "fluxnautics")
public final class ThrusterPlacementEvents {
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Level level = event.getLevel();
        BlockState state = level.getBlockState(event.getPos());
        if (!isArrayController(state))
            return;

        ItemStack held = event.getEntity().getItemInHand(event.getHand() != null ? event.getHand() : InteractionHand.MAIN_HAND);
        if (!isThrusterItem(held))
            return;

        event.setCanceled(true);

        if (level.isClientSide || event.getHand() != InteractionHand.MAIN_HAND)
            return;
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;

        bind(player, level, event.getPos());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        ThrusterPlacementHelper.ThrusterPlacementSettings.clearServer(event.getEntity().getUUID());
    }

    private static void bind(ServerPlayer player, Level level, BlockPos controllerPos) {
        IMultiblock<?> multiblock = ThrusterPlacementHelper.ThrusterPlacementSupport.findFormedArray(level, controllerPos);
        if (multiblock == null) {
            player.displayClientMessage(Component.translatable("placement.fluxnautics.messages.bind_failed")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        ThrusterPlacementHelper.ThrusterPlacementSettings.BoundArray newBound = new ThrusterPlacementHelper.ThrusterPlacementSettings.BoundArray(level.dimension(), controllerPos);
        ThrusterPlacementHelper.ThrusterPlacementSettings current = ThrusterPlacementHelper.ThrusterPlacementSettings.get(player);
        if (newBound.equals(current.bound())) {
            ThrusterPlacementHelper.ThrusterPlacementSettings.updateServer(player, s -> s);
            return;
        }

        ThrusterPlacementHelper.ThrusterPlacementSettings.updateServer(player, s -> new ThrusterPlacementHelper.ThrusterPlacementSettings(s.facing(), s.snap(),
                s.groupAuto(), s.forcedGroup(), newBound, java.util.List.of()));
        player.displayClientMessage(Component.translatable("placement.fluxnautics.messages.bound",
                controllerPos.getX(), controllerPos.getY(), controllerPos.getZ())
                .withStyle(ChatFormatting.GREEN), true);
    }

    private static boolean isThrusterItem(ItemStack stack) {
        return stack.is(FNBlocks.ION_THRUSTER.getItemHolder());
    }

    private static boolean isArrayController(BlockState state) {
        return state.is(FNBlocks.THRUSTER_ARRAY_CONTROLLER.get());
    }
}
