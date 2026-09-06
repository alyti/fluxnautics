package dev.alyti.fluxnautics.client.gui.element.scroll;

import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.scroll.GuiTextScrollList;

import java.util.function.Consumer;

public class GuiGroupScrollList extends GuiTextScrollList {

    private final Consumer<Integer> selectedCb;

    public GuiGroupScrollList(IGuiWrapper gui, int x, int y, int width, int height, Consumer<Integer> selectedCb) {
        super(gui, x, y, width, height);
        this.selectedCb = selectedCb;
    }

    @Override
    public void setSelected(int index) {
        super.setSelected(index);
        selectedCb.accept(index);
    }
}
