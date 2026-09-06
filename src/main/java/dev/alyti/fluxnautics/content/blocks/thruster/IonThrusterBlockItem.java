package dev.alyti.fluxnautics.content.blocks.thruster;

import java.util.List;

import javax.annotation.Nullable;

import dev.alyti.fluxnautics.lang.FNLang;
import mekanism.api.text.EnumColor;
import mekanism.client.key.MekanismKeyHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import dev.alyti.fluxnautics.util.placement.IPlacementHelper;
import dev.alyti.fluxnautics.util.placement.PlacementHelpers;
import mekanism.api.radial.RadialData;
import mekanism.api.radial.mode.IRadialMode;
import mekanism.common.item.block.ItemBlockMekanism;
import mekanism.common.item.interfaces.IModeItem;
import mekanism.common.lib.radial.IGenericRadialModeItem;

public class IonThrusterBlockItem extends ItemBlockMekanism<IonThrusterBlock> implements IGenericRadialModeItem, IModeItem {

    private final int placementHelperId;

    public IonThrusterBlockItem(IonThrusterBlock block, Properties properties) {
        super(block, properties);
        placementHelperId = PlacementHelpers.register(new ThrusterPlacementHelper());
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(FNLang.PLACEMENT_TOOLTIP.translateColored(EnumColor.AQUA, MekanismKeyHandler.handModeSwitchKey.getKey().getDisplayName()));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = world.getBlockState(pos);

        IPlacementHelper helper = PlacementHelpers.get(placementHelperId);
        Player player = context.getPlayer();
        BlockHitResult ray = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), pos, true);
        if (helper.matchesState(state) && player != null && !player.isShiftKeyDown()) {
            return helper.getOffset(player, world, state, pos, ray, stack)
                    .placeInWorld(world, this, player, context.getHand(), ray).result();
        }

        return super.onItemUseFirst(stack, context);
    }

    @Nullable
    @Override
    public RadialData<?> getRadialData(ItemStack stack) {
        return ThrusterRadials.ROOT;
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <MODE extends IRadialMode> MODE getMode(ItemStack stack, RadialData<MODE> radialData) {
        ThrusterPlacementHelper.ThrusterPlacementSettings settings = ThrusterPlacementHelper.ThrusterPlacementSettings.get(null);
        Object mode;
        if (radialData == ThrusterRadials.FACING) {
            mode = settings.facing();
        } else if (radialData == ThrusterRadials.SNAP) {
            mode = settings.snap();
        } else if (radialData == ThrusterRadials.GROUPS) {
            mode = settings.groupAuto() ? ThrusterRadials.GroupSlice.AUTO : ThrusterRadials.GroupSlice.of(settings.forcedGroup());
        } else {
            mode = null;
        }
        return (MODE) mode;
    }

    @Override
    public <MODE extends IRadialMode> void setMode(ItemStack stack, Player player, RadialData<MODE> radialData, MODE mode) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return;
        }
        if (radialData == ThrusterRadials.FACING && mode instanceof ThrusterRadials.ThrusterFacingMode facing) {
            ThrusterPlacementHelper.ThrusterPlacementSettings.updateServer(serverPlayer, s -> new ThrusterPlacementHelper.ThrusterPlacementSettings(facing, s.snap(),
                    s.groupAuto(), s.forcedGroup(), s.bound(), s.knownGroups()));
            serverPlayer.displayClientMessage(Component.translatable("placement.fluxnautics.messages.facing",
                    facing.sliceName()), true);
        } else if (radialData == ThrusterRadials.SNAP && mode instanceof ThrusterRadials.ThrusterSnapMode snap) {
            ThrusterPlacementHelper.ThrusterPlacementSettings.updateServer(serverPlayer, s -> new ThrusterPlacementHelper.ThrusterPlacementSettings(s.facing(), snap,
                    s.groupAuto(), s.forcedGroup(), s.bound(), s.knownGroups()));
            serverPlayer.displayClientMessage(Component.translatable("placement.fluxnautics.messages.snap",
                    snap.sliceName()), true);
        } else if (radialData == ThrusterRadials.GROUPS && mode instanceof ThrusterRadials.GroupSlice slice) {
            if (slice.isHint()) {
                serverPlayer.displayClientMessage(Component.translatable("placement.fluxnautics.messages.need_bind"), true);
            } else {
                boolean auto = slice.isAuto();
                int forced = auto ? 0 : slice.groupId();
                ThrusterPlacementHelper.ThrusterPlacementSettings.updateServer(serverPlayer, s -> new ThrusterPlacementHelper.ThrusterPlacementSettings(s.facing(), s.snap(),
                        auto, forced, s.bound(), s.knownGroups()));
                serverPlayer.displayClientMessage(auto
                        ? Component.translatable("placement.fluxnautics.messages.group.auto")
                        : Component.translatable("placement.fluxnautics.messages.group.forced", forced), true);
            }
        } else if (radialData == ThrusterRadials.ROOT && mode instanceof ThrusterRadials.UnbindMode) {
            ThrusterPlacementHelper.ThrusterPlacementSettings.updateServer(serverPlayer,
                    s -> new ThrusterPlacementHelper.ThrusterPlacementSettings(s.facing(), s.snap(), true, 0, null, List.of()));
            serverPlayer.displayClientMessage(Component.translatable("placement.fluxnautics.messages.unbound"), true);
        }
    }

    @Override
    public void changeMode(@NotNull Player player, @NotNull ItemStack stack, int shift, @NotNull DisplayChange displayChange) {
        ThrusterRadials.ThrusterFacingMode next = ThrusterPlacementHelper.ThrusterPlacementSettings.get(player).facing().adjust(shift);
        setMode(stack, player, ThrusterRadials.FACING, next);
        displayChange.sendMessage(player, next, ThrusterRadials.ThrusterFacingMode::sliceName);
    }

    @Nullable
    @Override
    public Component getScrollTextComponent(@NotNull ItemStack stack) {
        return ThrusterPlacementHelper.ThrusterPlacementSettings.get(null).facing().sliceName();
    }
}
