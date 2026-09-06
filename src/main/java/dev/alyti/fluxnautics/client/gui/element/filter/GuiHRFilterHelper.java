package dev.alyti.fluxnautics.client.gui.element.filter;

import dev.alyti.fluxnautics.client.gui.hr.GuiHarmonicResonanceFilterSelect;
import dev.alyti.fluxnautics.content.blocks.forcefield.ForcefieldHarmonizerBlockEntity;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.window.filter.GuiFilterHelper;

public interface GuiHRFilterHelper extends GuiFilterHelper<ForcefieldHarmonizerBlockEntity> {

    @Override
    default GuiHarmonicResonanceFilterSelect getFilterSelect(IGuiWrapper gui, ForcefieldHarmonizerBlockEntity tile) {
        return new GuiHarmonicResonanceFilterSelect(gui, tile);
    }
}