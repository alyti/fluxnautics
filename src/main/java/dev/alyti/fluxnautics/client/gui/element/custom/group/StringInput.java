package dev.alyti.fluxnautics.client.gui.element.custom.group;

import dev.alyti.fluxnautics.config.group.GroupStringConfig;
import mekanism.client.render.IFancyFontRenderer.TextAlignment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

class StringInput extends MiniElement<String> {

    private static final int OFFSET_Y = 1;
    private final int OFFSET_X;
    private final GroupStringConfig stringConfig;

    StringInput(GuiGroupScreen parent, GroupStringConfig stringConfig, Supplier<String> getter, Consumer<String> setter, Component description, int xPos, int yPos) {
        super(parent, getter, setter, description, xPos, yPos);
        this.stringConfig = stringConfig;
        this.OFFSET_X = this.parent.getScreenWidth() - 26;
    }

    @Override
    protected int getNeededHeight() {
        return 20;
    }

    @Override
    protected void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    protected void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int textColor = parent.screenTextColor();
        parent.drawScaledScrollingString(guiGraphics, description, xPos, yPos, TextAlignment.LEFT, textColor, OFFSET_X, 3, false, 0.8F);

        String value = getter.get();
        if (value.isEmpty()) {
            value = "---";
        }
        parent.drawScrollingString(guiGraphics, Component.literal(value), xPos, yPos + 11, TextAlignment.LEFT, textColor, OFFSET_X, 3, false);
    }

    @Override
    protected void click(double mouseX, double mouseY) {
        if (mouseOver(mouseX, mouseY, 0, 11, OFFSET_X, 8)) {
            parent.startRenaming(parent.currentGroupId, stringConfig, getter.get());
        }
    }
}