package dev.alyti.fluxnautics.client.gui.element.custom.group;

import mekanism.client.gui.GuiUtils;
import mekanism.client.gui.element.text.GuiTextField;
import mekanism.client.gui.element.window.GuiColorWindow;
import mekanism.client.render.IFancyFontRenderer.TextAlignment;
import mekanism.common.MekanismLang;
import mekanism.common.lib.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

class ColorSelection extends MiniElement<Integer> {

    private static final int OFFSET_Y = 1;
    private final int OFFSET_X;

    ColorSelection(GuiGroupScreen parent, Supplier<Integer> getter, Consumer<Integer> setter, Component description, int xPos, int yPos) {
        super(parent, getter, setter, description, xPos, yPos);
        this.OFFSET_X = this.parent.getScreenWidth() - 26;
    }

    private Color getColor() {
        return Color.argb(getter.get());
    }

    @Override
    protected int getNeededHeight() {
        return 20;
    }

    @Override
    protected void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int xTarget = getRelativeX() + OFFSET_X;
        int yTarget = getRelativeY() + OFFSET_Y;
        GuiUtils.drawOutline(guiGraphics, xTarget, yTarget, 18, 18, GuiTextField.SCREEN_COLOR.getAsInt());
        guiGraphics.blit(GuiColorWindow.TRANSPARENCY_GRID, xTarget + 1, yTarget + 1, 0, 0, 16, 16);
        GuiUtils.fill(guiGraphics, xTarget + 1, yTarget + 1, 16, 16, getter.get());
    }

    @Override
    protected void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int textColor = parent.screenTextColor();
        parent.drawScaledScrollingString(guiGraphics, description, xPos, yPos, TextAlignment.LEFT, textColor, OFFSET_X, 3, false, 0.8F);
        
        String hex = String.format("%06X", getter.get() & 0xFFFFFF);
        parent.drawScrollingString(guiGraphics, MekanismLang.GENERIC_HEX.translate(hex), xPos, yPos + 11, TextAlignment.LEFT, textColor, OFFSET_X, 3, false);
    }

    @Override
    protected void click(double mouseX, double mouseY) {
        if (mouseOver(mouseX, mouseY, OFFSET_X, OFFSET_Y, 18, 18)) {
            parent.gui().addWindow(new GuiColorWindow(parent.gui(), (parent.getGuiWidth() - 160) / 2, (parent.getGuiHeight() - 120) / 2, false,
                getColor(), color -> setData(color.argb()), null, null, null));
        }
    }
}
