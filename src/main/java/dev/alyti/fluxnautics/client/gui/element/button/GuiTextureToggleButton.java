package dev.alyti.fluxnautics.client.gui.element.button;

import dev.alyti.fluxnautics.client.gui.element.GuiTextureElement;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.common.lib.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.CommonComponents;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

public class GuiTextureToggleButton extends MekanismButton {

    private final BooleanSupplier isOn;

    @NonNull
    private final GuiTextureElement textureOff;
    private final GuiTextureElement textureOn;

    private Color activeColor;
    private boolean renderTab;

    public GuiTextureToggleButton(IGuiWrapper gui, int x, int y, BooleanSupplier toggled, @NotNull IClickable onPress, @NotNull GuiTextureElement textureOff, @Nullable GuiTextureElement textureOn) {
        super(gui, x, y, textureOff.width, textureOff.height, CommonComponents.EMPTY, onPress);
        this.isOn = toggled;
        this.textureOff = textureOff;
        this.textureOn = textureOn;
    }

    public GuiTextureToggleButton activeColor(Color color) {
        activeColor = color;
        return this;
    }

    public GuiTextureToggleButton renderTab(boolean on) {
        this.renderTab = on;
        return this;
    }

    @Override
    public boolean resetColorBeforeRender() {
        return false;
    }

    @Override
    public void drawBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.drawBackground(guiGraphics, mouseX, mouseY, partialTicks);
        if (renderTab) {
            GuiTextureElement.BLUEPRINT_ICON_TAB_LEFT_BG.render(guiGraphics, getButtonX() - 5, getButtonY() - 5);
        }
        Color c = Color.argb(0xffffffff);
        if (isOn.getAsBoolean()) {
            if (this.activeColor != null) {
                c =  this.activeColor;
            }
            if (this.textureOn != null) {
                this.textureOn.render(guiGraphics, getButtonX(), getButtonY(), c);
                return;
            }
        }
        this.textureOff.render(guiGraphics, getButtonX(), getButtonY(), c);
    }
}

