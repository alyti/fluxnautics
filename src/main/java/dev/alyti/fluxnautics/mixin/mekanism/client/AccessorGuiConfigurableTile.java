package dev.alyti.fluxnautics.mixin.mekanism.client;

import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.tab.window.GuiSideConfigurationTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes GuiConfigurableTile's private sideConfigTab so the screen can deactivate it while
 * the per-side redstone configuration window is open (prevents the DataType side-config
 * window from stacking alongside it).
 */
@Mixin(value = GuiConfigurableTile.class, remap = false)
public interface AccessorGuiConfigurableTile {

    @Accessor(value = "sideConfigTab", remap = false)
    GuiSideConfigurationTab<?> fluxnautics$getSideConfigTab();
}