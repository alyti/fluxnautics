package dev.alyti.fluxnautics.client.gui.element.window;

import java.util.function.Consumer;
import java.util.function.Supplier;

import dev.alyti.fluxnautics.api.content.redstone.IRedstoneControlMode;
import dev.alyti.fluxnautics.api.content.redstone.IRedstoneSideConfigurable;
import dev.alyti.fluxnautics.api.content.redstone.TileComponentRedstoneControl;
import dev.alyti.fluxnautics.client.gui.element.window.GuiRedstoneConfiguration;
import dev.alyti.fluxnautics.lang.FNLang;
import mekanism.client.SpecialColors;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.tab.window.GuiWindowCreatorTab;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.inventory.container.SelectedWindowData;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

public class GuiRedstoneConfigurationTab<TILE extends TileEntityMekanism & IRedstoneSideConfigurable, MODE extends Enum<MODE> & IRedstoneControlMode<? super TILE>> extends GuiWindowCreatorTab<TILE, GuiRedstoneConfigurationTab<TILE, MODE>> {

    private static final SelectedWindowData WINDOW_DATA = SelectedWindowData.UNSPECIFIED;

    private final TileComponentRedstoneControl<TILE, MODE> config;
    @Nullable
    private final Consumer<Boolean> windowStateListener;

    public GuiRedstoneConfigurationTab(IGuiWrapper gui, TILE tile, TileComponentRedstoneControl<TILE, MODE> config,
                                       @Nullable Consumer<Boolean> windowStateListener, Supplier<GuiRedstoneConfigurationTab<TILE, MODE>> elementSupplier) {
        super(MekanismUtils.getResource(ResourceType.GUI, "redstone_control_disabled.png"), gui, tile, gui.getXSize(), 137, 26, 18, false, elementSupplier);
        setTooltip(FNLang.REDSTONE_SIDES_HINT);
        this.config = config;
        this.windowStateListener = windowStateListener;
    }

    @Override
    protected void colorTab(GuiGraphics guiGraphics) {
        MekanismRenderer.color(guiGraphics, SpecialColors.TAB_REDSTONE_CONTROL);
    }

    @Override
    protected GuiWindow createWindow(SelectedWindowData windowData) {
        return new GuiRedstoneConfiguration<>(gui(), (getGuiWidth() - 156) / 2 + 240, 80, dataSource, config, windowStateListener);
    }

    @Override
    protected SelectedWindowData getNextWindowData() {
        return WINDOW_DATA;
    }
}