package dev.alyti.fluxnautics.client.gui.element.button;

import dev.alyti.fluxnautics.content.blocks.flux_tether.FluxTetherConfig;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.tooltip.TooltipUtils;
import mekanism.common.registries.MekanismSounds;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class TriStateModeButton<ENUM extends Enum<ENUM>> extends MekanismButton {

    public static final ResourceLocation RADIO = MekanismUtils.getResource(ResourceType.GUI, "radio_button.png");
    public static final int RADIO_SIZE = 8;

    private final Supplier<ENUM> currentModeSupplier;
    private final ENUM buttonMode;
    private final Tooltip tooltip;

    public TriStateModeButton(IGuiWrapper gui, int x, int y,
                              Supplier<ENUM> currentModeSupplier,
                              ENUM buttonMode,
                              @NotNull IClickable onPress, Component tooltip) {
        super(gui, x, y, RADIO_SIZE, RADIO_SIZE, CommonComponents.EMPTY, onPress);
        this.currentModeSupplier = currentModeSupplier;
        this.buttonMode = buttonMode;
        this.tooltip = TooltipUtils.create(tooltip);
        this.clickSound = () -> isActive() ? MekanismSounds.BEEP_OFF.get() : MekanismSounds.BEEP_ON.get();
        this.clickVolume = 1.0F;
    }

    public boolean isActive() {
        return currentModeSupplier.get() == buttonMode;
    }

    @Override
    public void drawBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.drawBackground(guiGraphics, mouseX, mouseY, partialTicks);
        if (isActive()) {
            guiGraphics.blit(RADIO, getButtonX(), getButtonY(), 0, RADIO_SIZE,
                getButtonWidth(), getButtonHeight(), 2 * RADIO_SIZE, 2 * RADIO_SIZE);
        } else {
            int uOffset = checkWindows(mouseX, mouseY, isHoveredOrFocused()) ? RADIO_SIZE : 0;
            guiGraphics.blit(RADIO, getButtonX(), getButtonY(), uOffset, 0,
                getButtonWidth(), getButtonHeight(), 2 * RADIO_SIZE, 2 * RADIO_SIZE);
        }
    }

    @Override
    public void updateTooltip(int mouseX, int mouseY) {
        setTooltip(tooltip);
    }
}