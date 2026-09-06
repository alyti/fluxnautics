package dev.alyti.fluxnautics.client.gui.element.custom.group;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

abstract class MiniElement<TYPE> {

    protected final GuiGroupScreen parent;
    protected final Component description;
    protected final int xPos, yPos;
    protected final Supplier<TYPE> getter;
    protected final Consumer<TYPE> setter;

    protected MiniElement(GuiGroupScreen parent, Supplier<TYPE> getter, Consumer<TYPE> setter, Component description, int xPos, int yPos) {
        this.parent = parent;
        this.getter = getter;
        this.setter = setter;
        this.description = description;
        this.xPos = xPos;
        this.yPos = yPos;
    }

    protected abstract void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY);

    protected abstract void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY);

    protected abstract void click(double mouseX, double mouseY);

    protected void release(double mouseX, double mouseY) {
    }

    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
    }

    protected abstract int getNeededHeight();

    protected int getRelativeX() {
        return parent.getRelativeX() + xPos;
    }

    protected int getRelativeY() {
        return parent.getRelativeY() + yPos;
    }

    protected int getX() {
        return parent.getX() + xPos;
    }

    protected int getY() {
        return parent.getY() + yPos;
    }

    protected boolean mouseOver(double mouseX, double mouseY, int relativeX, int relativeY, int width, int height) {
        int x = getX();
        int y = getY();
        return mouseX >= x + relativeX && mouseX < x + relativeX + width && mouseY >= y + relativeY && mouseY < y + relativeY + height;
    }

    protected void setData(TYPE value) {
        setter.accept(value);
    }
}