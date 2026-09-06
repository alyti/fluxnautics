package dev.alyti.fluxnautics.client.gui.element;

import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.GuiScalableElement;
import mekanism.client.gui.tooltip.TooltipUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public class GuiLeverWidget extends GuiScalableElement {

    private final DoubleSupplier getter;
    private final DoubleConsumer setter;

    private double value;

    public GuiLeverWidget(IGuiWrapper gui, int x, int y, int width, int height, DoubleSupplier getter, DoubleConsumer setter) {
        super(GuiInnerScreen.SCREEN, gui, x, y, width, height, 32, 32);
        this.active = true;
        this.getter = getter;
        this.setter = setter;
        this.setValue(getter.getAsDouble());
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
        this.setTooltip(TooltipUtils.create(Component.literal("Motor Speed: %.2f%%".formatted(value * 100))));
    }

    @Override
    public void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderForeground(guiGraphics, mouseX, mouseY);

        if (!isDragging()) {
            value = getter.getAsDouble();
        }

        double val = getValue(); // -1.0 to 1.0

        int slitX = relativeX + width / 2 - 4;
        int slitY = relativeY;
        int slitHeight = height;

        guiGraphics.fill(slitX -2, slitY + 2, slitX+10, slitY + height - 2, 0xFF000000);

        int leverCenterY = relativeY + height / 2 + (int)(val * (height / 2.0 - 4));

        int trackCenterY = relativeY + height / 2;
        int topY = Math.min(leverCenterY, trackCenterY);
        int bottomY = Math.max(leverCenterY, trackCenterY);

        if (bottomY > topY) {
            int trackHeight = bottomY - topY;
            for (int i = 0; i < trackHeight; i++) {
                int py = topY + i;
                float dist = (float)Math.abs(py - trackCenterY) / Math.abs(leverCenterY - trackCenterY);
                int c = (int)(20 + 130 * dist); // Black (20) to Metallic (150)
                int color = (c << 16) | (c << 8) | c | 0xFF000000;
                guiGraphics.fill(slitX, py, slitX + 8, py + 1, color);
            }
        }

        int leverX = relativeX + width / 2 - 16;
        int leverY = leverCenterY - 4;
        GuiTextureElement.LEVER_HANDLE.render(guiGraphics, leverX, leverY);
    }

    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        super.onClick(mouseX, mouseY, button);
        set(mouseX, mouseY);
        setDragging(true);
    }

    @Override
    public void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        super.onDrag(mouseX, mouseY, deltaX, deltaY);
        if (isDragging()) {
            set(mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        double shift = -Math.signum(deltaY) * 0.05;
        double newVal = Mth.clamp(value + shift, -1.0, 1.0);
        if (!Mth.equal(newVal, value)) {
            setValue(newVal);
            setter.accept(value);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int key, int i, int j) {
        if (super.keyPressed(key, i, j)) {
            return true;
        }
        double shift;
        if (isPreviousButton(key) && value > -1.0) {
            shift = -0.01;
        } else if (isNextButton(key) && value < 1.0) {
            shift = 0.01;
        } else {
            return false;
        }
        value = Mth.clamp(value + shift, -1.0, 1.0);
        setter.accept(value);
        return true;
    }

    private void set(double mouseX, double mouseY) {
        double oldValue = value;
        double rawValue = ((mouseY - getY()) / height) * 2.0 - 1.0;
        double snappedValue = Math.round(rawValue / 0.025) * 0.025;
        setValue(Mth.clamp(snappedValue, -1.0, 1.0));
        if (!Mth.equal(value, oldValue)) {
            setter.accept(value);
        }
    }

    private boolean isPreviousButton(int key) {
        return key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_LEFT;
    }

    private boolean isNextButton(int key) {
        return key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_RIGHT;
    }
}