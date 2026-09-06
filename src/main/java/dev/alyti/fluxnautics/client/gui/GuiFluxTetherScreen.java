package dev.alyti.fluxnautics.client.gui;

import dev.alyti.fluxnautics.client.gui.element.GuiValueSliderField;
import dev.alyti.fluxnautics.client.gui.element.button.TriStateModeButton;
import dev.alyti.fluxnautics.client.gui.element.window.GuiRedstoneConfigurationTab;
import dev.alyti.fluxnautics.content.blocks.flux_tether.FluxTetherBlockEntity;
import dev.alyti.fluxnautics.content.blocks.flux_tether.FluxTetherConfig;
import dev.alyti.fluxnautics.content.blocks.flux_tether.FluxTetherRedstoneControl;
import dev.alyti.fluxnautics.mixin.mekanism.client.AccessorGuiConfigurableTile;
import dev.alyti.fluxnautics.network.packets.FluxTetherSaveConfigPacket;
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
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;

public class GuiFluxTetherScreen extends GuiConfigurableTile<FluxTetherBlockEntity, MekanismTileContainer<FluxTetherBlockEntity>> {

    public GuiFluxTetherScreen(MekanismTileContainer<FluxTetherBlockEntity> container, Inventory inv, Component title) {
        super(container, inv, title);
        dynamicSlots = true;
        imageHeight += 180;
        inventoryLabelY += 180;
        imageWidth = 266;
    }
    
    private static final int COL_WIDTH = 100, FULL_WIDTH = 204;

    private GuiRedstoneConfigurationTab<FluxTetherBlockEntity, FluxTetherRedstoneControl> redstoneConfigTab;

    @Override
    protected void addGuiElements() {
        super.addGuiElements();

        int rX = 30;
        int rY = 20;

        addElement(new GuiInnerScreen(this, rX, rY, 206, 14, () ->
            List.of(Component.literal("Linear Axis").withStyle(ChatFormatting.AQUA))));

        rY += 16;
        addModeButtons(rX, rY, "Linear",
            () -> tile.config.linearMode(),
            (mode) -> tile.config.setLinearMode(mode));


        int rX2 = rX + 106;
        rY += 16;
        addValueField(rX, rY, COL_WIDTH, "Target", tile::getMinExtension, tile::getMaxExtension,
            () -> tile.config.linearTarget(), (d) -> tile.config.setLinearTarget(d), v -> String.format("%.2f", v));
        addValueField(rX2, rY, COL_WIDTH, "Force Boost", 0.0, 4.0,
            () -> tile.config.linearForceBoost(), (d) -> tile.config.setLinearForceBoost(d), v -> String.format("%.2fx", v));

        rY += 30;
        addValueField(rX, rY, COL_WIDTH, "Servo Rate", 0.0, 20.0,
            () -> tile.config.linearServoRate(), (d) -> tile.config.setLinearServoRate(d), v -> String.format("%.2f", v));
        addValueField(rX2, rY, COL_WIDTH, "Servo Loop", tile::getMinExtension, tile::getMaxExtension,
            () -> tile.config.linearServoLoop(), (d) -> tile.config.setLinearServoLoop(d), v -> String.format("%.2f", v));

        
        rY += 30;
        addElement(new GuiInnerScreen(this, rX, rY, 206, 14, () ->
            List.of(Component.literal("Angular Axis").withStyle(ChatFormatting.AQUA))));

        rY += 16;
        addModeButtons(rX, rY, "Angular",
            () -> tile.config.angularMode(),
            (mode) -> tile.config.setAngularMode(mode));

        rY += 16;

        addValueField(rX, rY, COL_WIDTH, "Target", -180.0, 180.0,
            () -> tile.config.angularTarget(), (d) -> tile.config.setAngularTarget(d), v -> String.format("%.2f°", v));
        addValueField(rX2, rY, COL_WIDTH, "Force Boost", 0.0, 4.0,
            () -> tile.config.angularForceBoost(), (d) -> tile.config.setAngularForceBoost(d), v -> String.format("%.2fx", v));

        rY += 30;
        addValueField(rX, rY, FULL_WIDTH, "Servo Rate", -180.0, 180.0,
            () -> tile.config.angularServoRate(), (d) -> tile.config.setAngularServoRate(d), v -> String.format("%.2f°/s", v));


        addElement(new GuiEnergyTab(this, () -> {
            long using = tile.isAssembled() ? tile.getStoredEnergy() > 0 ? tile.getEnergyCost() : 0L : 0L;
            return List.of(MekanismLang.USING.translate(EnergyDisplay.of(using)),
                MekanismLang.NEEDED.translate(EnergyDisplay.of(tile.getEnergyContainer().getNeeded())));
        }));

        rY += 30;
        addElement(new GuiHorizontalPowerBar(this, tile.getEnergyContainer(), rX + 5, rY, 190))
            .warning(WarningTracker.WarningType.NOT_ENOUGH_ENERGY, () -> tile.getStoredEnergy() < tile.getEnergyCost());

        redstoneConfigTab = addElement(new GuiRedstoneConfigurationTab<>(this, tile, tile.getRedstoneConfig(),
            open -> {
                var sideTab = ((AccessorGuiConfigurableTile) this).fluxnautics$getSideConfigTab();
                if (sideTab != null) sideTab.active = !open;
            },
            () -> redstoneConfigTab));
    }

    private void addValueField(int x, int y, int width, String label, double min, double max,
                               DoubleSupplier getter, DoubleConsumer setter, Function<Double, String> format) {
        addElement(new GuiValueSliderField(this, x, y, width, label, min, max, getter,
            v -> {
                setter.accept(v);
                sync();
            }, format));
    }

    private void addValueField(int x, int y, int width, String label, DoubleSupplier min, DoubleSupplier max,
                               DoubleSupplier getter, DoubleConsumer setter, Function<Double, String> format) {
        addElement(new GuiValueSliderField(this, x, y, width, label, min, max, getter,
            v -> {
                setter.accept(v);
                sync();
            }, format));
    }

    private void addModeButtons(int x, int y, String axisLabel,
                                java.util.function.Supplier<FluxTetherConfig.Mode> modeSupplier,
                                java.util.function.Consumer<FluxTetherConfig.Mode> modeSetter) {
        addElement(new TriStateModeButton<>(this, x, y, modeSupplier, FluxTetherConfig.Mode.FREE,
            (element, mouseX, mouseY) -> {
                modeSetter.accept(FluxTetherConfig.Mode.FREE);
                sync();
                return true;
            }, Component.literal(axisLabel + " Mode: FREE (free motion)")));

        addElement(new TriStateModeButton<>(this, x + 12, y, modeSupplier, FluxTetherConfig.Mode.STATIC,
            (element, mouseX, mouseY) -> {
                modeSetter.accept(FluxTetherConfig.Mode.STATIC);
                sync();
                return true;
            }, Component.literal(axisLabel + " Mode: STATIC (hold target)")));

        addElement(new TriStateModeButton<>(this, x + 24, y, modeSupplier, FluxTetherConfig.Mode.SERVO,
            (element, mouseX, mouseY) -> {
                modeSetter.accept(FluxTetherConfig.Mode.SERVO);
                sync();
                return true;
            }, Component.literal(axisLabel + " Mode: SERVO (continuous)")));
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderTitleText(guiGraphics);
        renderInventoryText(guiGraphics);
        super.drawForegroundText(guiGraphics, mouseX, mouseY);
    }

    private void sync() {
        PacketDistributor.sendToServer(new FluxTetherSaveConfigPacket(tile.getBlockPos(), tile.config));
    }
}
