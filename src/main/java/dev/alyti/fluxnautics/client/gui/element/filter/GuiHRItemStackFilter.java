package dev.alyti.fluxnautics.client.gui.element.filter;

import dev.alyti.fluxnautics.content.blocks.forcefield.ForcefieldHarmonizerBlockEntity;
import dev.alyti.fluxnautics.index.HarmonicResonanceItemStackFilter;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.window.filter.GuiItemStackFilter;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

public class GuiHRItemStackFilter extends GuiItemStackFilter<HarmonicResonanceItemStackFilter, ForcefieldHarmonizerBlockEntity> implements GuiHRFilterHelper {

    public static GuiHRItemStackFilter create(IGuiWrapper gui, ForcefieldHarmonizerBlockEntity tile) {
        return new GuiHRItemStackFilter(gui, (gui.getXSize() - 185) / 2, 15, tile, null);
    }

    public static GuiHRItemStackFilter edit(IGuiWrapper gui, ForcefieldHarmonizerBlockEntity tile, HarmonicResonanceItemStackFilter filter) {
        return new GuiHRItemStackFilter(gui, (gui.getXSize() - 185) / 2, 15, tile, filter);
    }

    private GuiHRItemStackFilter(IGuiWrapper gui, int x, int y, ForcefieldHarmonizerBlockEntity tile, @Nullable HarmonicResonanceItemStackFilter origFilter) {
        super(gui, x, y, 185, 90, tile, origFilter);
    }

    @Override
    protected int getLeftButtonX() {
        return relativeX + 29;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    protected HarmonicResonanceItemStackFilter createNewFilter() {
        return new HarmonicResonanceItemStackFilter();
    }

    @Override
    public void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderForeground(guiGraphics, mouseX, mouseY);
    }
}