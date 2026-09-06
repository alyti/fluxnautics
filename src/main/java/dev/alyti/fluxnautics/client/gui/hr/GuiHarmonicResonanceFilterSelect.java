package dev.alyti.fluxnautics.client.gui.hr;

import dev.alyti.fluxnautics.client.gui.element.filter.GuiHRItemStackFilter;
import dev.alyti.fluxnautics.content.blocks.forcefield.ForcefieldHarmonizerBlockEntity;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.window.filter.GuiFilterSelect;
import org.jetbrains.annotations.NotNull;

public class GuiHarmonicResonanceFilterSelect extends GuiFilterSelect<ForcefieldHarmonizerBlockEntity> {

    public GuiHarmonicResonanceFilterSelect(IGuiWrapper gui, ForcefieldHarmonizerBlockEntity tile) {
        super(gui, tile, 3);
    }

    @NotNull
    @Override
    protected GuiFilterCreator<ForcefieldHarmonizerBlockEntity> getItemStackFilterCreator() {
        return GuiHRItemStackFilter::create;
    }
}