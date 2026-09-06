package dev.alyti.fluxnautics.client.gui;

import dev.alyti.fluxnautics.client.gui.element.GuiValueSliderField;
import dev.alyti.fluxnautics.client.gui.element.button.TriStateModeButton;
import dev.alyti.fluxnautics.client.gui.element.window.GuiRedstoneConfigurationTab;
import dev.alyti.fluxnautics.content.blocks.inertia_dampener.InertiaDampenerBlockEntity;
import dev.alyti.fluxnautics.content.blocks.inertia_dampener.InertiaDampenerConfig;
import dev.alyti.fluxnautics.content.blocks.inertia_dampener.InertiaDampenerConfig.AxisConfig;
import dev.alyti.fluxnautics.content.blocks.inertia_dampener.InertiaDampenerConfig.DampenerAxis;
import dev.alyti.fluxnautics.content.blocks.inertia_dampener.InertiaDampenerConfig.DampenerMode;
import dev.alyti.fluxnautics.content.blocks.inertia_dampener.InertiaDampenerRedstoneControl;
import dev.alyti.fluxnautics.mixin.mekanism.client.AccessorGuiConfigurableTile;
import dev.alyti.fluxnautics.network.packets.InertiaDampenerSaveConfigPacket;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.bar.GuiHorizontalPowerBar;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.common.MekanismLang;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.inventory.warning.WarningTracker;
import mekanism.common.util.text.EnergyDisplay;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

public class GuiInertiaDampenerScreen extends GuiConfigurableTile<InertiaDampenerBlockEntity, MekanismTileContainer<InertiaDampenerBlockEntity>> {

    private static final int SLIDER_WIDTH = 168;

    private GuiRedstoneConfigurationTab<InertiaDampenerBlockEntity, InertiaDampenerRedstoneControl> redstoneConfigTab;

    public GuiInertiaDampenerScreen(MekanismTileContainer<InertiaDampenerBlockEntity> container, Inventory inv, Component title) {
        super(container, inv, title);
        dynamicSlots = true;
        imageHeight += 80;
        inventoryLabelY += 80;
        imageWidth = 266;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();

        int rX = 30;
        int rY = 20;

        addElement(new GuiInnerScreen(this, rX, rY, 206, 14, () ->
                List.of(Component.literal("Linear Axes").withStyle(ChatFormatting.AQUA))));

        rY += 20;
        addAxisControls(rX, rY, "X", DampenerAxis.LINEAR_X);
        rY += 30;
        addAxisControls(rX, rY, "Y", DampenerAxis.LINEAR_Y);
        rY += 30;
        addAxisControls(rX, rY, "Z", DampenerAxis.LINEAR_Z);

        addElement(new GuiEnergyTab(this, () -> {
            long using = tile.getLastTickWork() > 0 ? tile.getEnergyCost() : 0L;
            return List.of(MekanismLang.USING.translate(EnergyDisplay.of(using)),
                    MekanismLang.NEEDED.translate(EnergyDisplay.of(tile.getEnergyContainer().getNeeded())));
        }));

        rY += 30;
        addElement(new GuiHorizontalPowerBar(this, tile.getEnergyContainer(), rX + 5, rY, 190))
                .warning(WarningTracker.WarningType.NOT_ENOUGH_ENERGY, () -> tile.getEnergyContainer().getEnergy() < tile.getEnergyCost());

        redstoneConfigTab = addElement(new GuiRedstoneConfigurationTab<>(this, tile, tile.getRedstoneConfig(),
                open -> {
                    var sideTab = ((AccessorGuiConfigurableTile) this).fluxnautics$getSideConfigTab();
                    if (sideTab != null) sideTab.active = !open;
                },
                () -> redstoneConfigTab));
    }

    private void addAxisControls(int x, int y, String axisLabel, DampenerAxis axis) {
        addModeButtons(x, y, axisLabel, axis);

        addElement(new GuiValueSliderField(this, x + 38, y, SLIDER_WIDTH, axisLabel + " Strength", 0.0, 1.0,
                () -> tile.getConfiguration().getAxis(axis).strength(),
                v -> sendPatch(InertiaDampenerConfig.PartialUpdate.ofStrength(axis, v)),
                v -> String.format("%.0f%%", v * 100)));
    }

    private void addModeButtons(int x, int y, String axisLabel, DampenerAxis axis) {
        addElement(new TriStateModeButton<>(this, x, y, modeSupplier(axis), DampenerMode.OFF,
                (element, mouseX, mouseY) -> {
                    sendPatch(InertiaDampenerConfig.PartialUpdate.ofMode(axis, DampenerMode.OFF));
                    return true;
                }, Component.literal(axisLabel + " Mode: OFF (free motion)")));

        addElement(new TriStateModeButton<>(this, x + 12, y, modeSupplier(axis), DampenerMode.DAMPEN,
                (element, mouseX, mouseY) -> {
                    sendPatch(InertiaDampenerConfig.PartialUpdate.ofMode(axis, DampenerMode.DAMPEN));
                    return true;
                }, Component.literal(axisLabel + " Mode: DAMPEN (proportional soft stop)")));

        addElement(new TriStateModeButton<>(this, x + 24, y, modeSupplier(axis), DampenerMode.BRAKE,
                (element, mouseX, mouseY) -> {
                    sendPatch(InertiaDampenerConfig.PartialUpdate.ofMode(axis, DampenerMode.BRAKE));
                    return true;
                }, Component.literal(axisLabel + " Mode: BRAKE (hard stop at calibration limit)")));
    }

    private Supplier<DampenerMode> modeSupplier(DampenerAxis axis) {
        return () -> tile.getConfiguration().getAxis(axis).mode();
    }

    private void sendPatch(InertiaDampenerConfig.PartialUpdate patch) {
        tile.previewConfigurationClient(patch);
        PacketDistributor.sendToServer(new InertiaDampenerSaveConfigPacket(tile.getBlockPos(), patch));
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderTitleText(guiGraphics);
        renderInventoryText(guiGraphics);
        super.drawForegroundText(guiGraphics, mouseX, mouseY);
    }

}