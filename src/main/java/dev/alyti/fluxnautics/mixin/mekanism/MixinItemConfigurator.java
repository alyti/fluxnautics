package dev.alyti.fluxnautics.mixin.mekanism;

import java.util.Locale;

import dev.alyti.fluxnautics.api.content.redstone.IRedstoneControlMode;
import dev.alyti.fluxnautics.api.content.redstone.IRedstoneSideConfigurable;
import dev.alyti.fluxnautics.api.content.redstone.TileComponentRedstoneControl;
import mekanism.api.RelativeSide;
import mekanism.api.security.IBlockSecurityUtils;
import mekanism.api.text.EnumColor;
import mekanism.api.text.TextComponentUtil;
import mekanism.common.item.ItemConfigurator;
import mekanism.common.item.ItemConfigurator.ConfiguratorMode;
import mekanism.common.tile.interfaces.ISideConfiguration;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ItemConfigurator.class, remap = false)
public abstract class MixinItemConfigurator {

    @Inject(method = "useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;",
          at = @At("HEAD"), cancellable = true, remap = false)
    private void fluxnautics$redstoneSideConfiguration(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null || level.isClientSide) {
            return;
        }
        ConfiguratorMode mode = ((ItemConfigurator) (Object) this).getMode(context.getItemInHand());
        if (!mode.isConfigurating()) {
            return;
        }
        BlockPos pos = context.getClickedPos();
        BlockEntity tile = WorldUtils.getTileEntity(level, pos);
        if (!(tile instanceof IRedstoneSideConfigurable configurable)) {
            return;
        }
        if (tile instanceof ISideConfiguration sideConfig && mode.getTransmission() != null
              && sideConfig.getConfig().supports(mode.getTransmission())) {
            return;
        }
        RelativeSide relativeSide = RelativeSide.fromDirections(configurable.getDirection(), context.getClickedFace());
        TileComponentRedstoneControl<?, ?> redstoneConfig = configurable.getRedstoneConfig();
        IRedstoneControlMode<?> current = redstoneConfig.getMode(relativeSide);
        if (!player.isShiftKeyDown()) {
            player.displayClientMessage(TextComponentUtil.build(EnumColor.GRAY, "Redstone mode ", sideLabel(relativeSide),
                  EnumColor.GRAY, ": ", current.color(), current.text()), true);
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }
        if (!IBlockSecurityUtils.INSTANCE.canAccessOrDisplayError(player, level, pos, tile)) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }
        IRedstoneControlMode<?> next = redstoneConfig.cycleMode(relativeSide, 1);
        if (next != current) {
            player.displayClientMessage(TextComponentUtil.build(EnumColor.GRAY, "Redstone mode ", sideLabel(relativeSide),
                  EnumColor.GRAY, " set to ", next.color(), next.text()), true);
        }
        cir.setReturnValue(InteractionResult.SUCCESS);
    }

    private static Component sideLabel(RelativeSide side) {
        String name = side.name().toLowerCase(Locale.ROOT);
        return Component.literal(Character.toUpperCase(name.charAt(0)) + name.substring(1));
    }
}