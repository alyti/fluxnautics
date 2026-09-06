package dev.alyti.fluxnautics.mixin.mekanism.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.alyti.fluxnautics.api.content.redstone.IRedstoneSideConfigurable;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.common.tile.base.TileEntityMekanism;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = GuiMekanismTile.class, remap = false)
public abstract class MixinGuiMekanismTile {

    @Shadow
    @Final
    protected TileEntityMekanism tile;

    @ModifyExpressionValue(method = "addGenericTabs()V",
          at = @At(value = "INVOKE",
                target = "Lmekanism/common/tile/base/TileEntityMekanism;supportsRedstone()Z",
                remap = false))
    private boolean fluxnautics$hideVanillaRedstoneControlTab(boolean original) {
        return original && !(tile instanceof IRedstoneSideConfigurable);
    }
}