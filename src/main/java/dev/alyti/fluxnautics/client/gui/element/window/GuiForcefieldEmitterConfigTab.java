package dev.alyti.fluxnautics.client.gui.element.window;

import dev.alyti.fluxnautics.content.blocks.forcefield.ForcefieldEmitterBlockEntity;
import dev.alyti.fluxnautics.lang.FNLang;
import mekanism.client.SpecialColors;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.tab.window.GuiWindowCreatorTab;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.inventory.container.SelectedWindowData;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import mekanism.common.util.MekanismUtils;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.Supplier;

public class GuiForcefieldEmitterConfigTab<F extends TileEntityConfigurableMachine> extends GuiWindowCreatorTab<ForcefieldEmitterBlockEntity, GuiForcefieldEmitterConfigTab<ForcefieldEmitterBlockEntity>> {

    private static final SelectedWindowData WINDOW_DATA = new SelectedWindowData(SelectedWindowData.WindowType.UNSPECIFIED);

    public GuiForcefieldEmitterConfigTab(IGuiWrapper gui, ForcefieldEmitterBlockEntity tile, Supplier<GuiForcefieldEmitterConfigTab<ForcefieldEmitterBlockEntity>> elementSupplier) {
        super(MekanismUtils.getResource(MekanismUtils.ResourceType.GUI, "visuals.png"), gui, tile, -26, 62, 26, 18, true, elementSupplier);
        setTooltip(FNLang.EMITTER_CONFIG);
    }

    @Override
    protected void colorTab(GuiGraphics guiGraphics) {
        MekanismRenderer.color(guiGraphics, SpecialColors.TAB_VISUALS);
    }

    @Override
    protected GuiWindow createWindow(SelectedWindowData windowData) {
        return new GuiForcefieldEmitterConfigWindow(gui(), (getGuiWidth() - 156) / 2, 15, dataSource, windowData);
    }

    @Override
    protected SelectedWindowData getNextWindowData() {
        return WINDOW_DATA;
    }
}