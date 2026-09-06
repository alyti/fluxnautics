package dev.alyti.fluxnautics.client.gui;

import dev.alyti.fluxnautics.FluxnauticsConfig;
import dev.alyti.fluxnautics.client.gui.element.GuiTextureElement;
import dev.alyti.fluxnautics.client.gui.element.GuiValueSliderField;
import dev.alyti.fluxnautics.client.gui.element.button.GuiTextureToggleButton;
import dev.alyti.fluxnautics.content.blocks.stabilizer.StabilizerBlockEntity;
import dev.alyti.fluxnautics.lang.FNLang;
import dev.alyti.fluxnautics.network.packets.StabilizerSaveConfigPacket;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.GuiSlider;
import mekanism.client.gui.element.bar.GuiHorizontalPowerBar;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.common.MekanismLang;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.inventory.warning.WarningTracker;
import mekanism.common.lib.Color;
import mekanism.common.util.text.EnergyDisplay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GuiStabilizerScreen extends GuiConfigurableTile<StabilizerBlockEntity, MekanismTileContainer<StabilizerBlockEntity>> {
    public GuiStabilizerScreen(MekanismTileContainer<StabilizerBlockEntity> container, Inventory inv, Component title) {
        super(container, inv, title);
        dynamicSlots = true;
        imageHeight += 120;
        inventoryLabelY += 120;
        imageWidth = 266;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();

        int rX = 30;
        int rY = 20;

        addElement(new GuiValueSliderField(this, rX, rY, 100, FNLang.STABILIZER_FORCE_PERCENT.translate(), 0.0, 2.0,
            () -> tile.config.forcePercent(),
            v -> { tile.config.setForcePercent(v); sync(); },
            v -> String.format("%3.2f", v)));

        rX += 106;

        addElement(new GuiValueSliderField(this, rX, rY, 100, FNLang.STABILIZER_DAMPENING_PERCENT.translate(), 0.0, 0.99,
            () -> tile.config.dampingPercent(),
            v -> { tile.config.setDampingPercent(v); sync(); },
            v -> String.format("%3.2f", v)));

        rX -= 106;

        int sW = 206;
        rY += 28;


        addElement(new GuiValueSliderField(this, rX, rY, sW, FNLang.STABILIZER_TARGET_PITCH_DEGREES.translate(), -90.0, 90.0,
            () -> tile.config.targetPitchDeg(),
            v -> { tile.config.setTargetPitchDeg(v); sync(); },
            v -> String.format("%3.2f", v)));

//        addElement(new GuiInnerScreen(this, rX, rY, sW, 16, () -> {
//            List<Component> list = new ArrayList<>();
//            list.add(FNLang.STABILIZER_TARGET_PITCH_DEGREES.translate(String.format("%.2f", tile.config.targetPitchDeg())));
//            return list;
//        }));
//        GuiSlider targetPitchSlider = addElement(new GuiSlider(this, rX, rY + 12, sW-2, value -> {
//            tile.config.setTargetPitchDeg(180.0 * value - 90.0);
//            sync();
//        }));
//        targetPitchSlider.setValue((90.0 + tile.config.targetPitchDeg()) / 180.0);

        addElement(new GuiTextureToggleButton(this, rX + sW - 16, rY, () -> tile.config.stabilizePitch(), ((element, mouseX, mouseY) -> {
            tile.config.setStabilizePitch(!tile.config.stabilizePitch());
            sync();
            return true;
        }), GuiTextureElement.BLUEPRINT_ICON_GEAR, null).activeColor(Color.rgb(0xFF0000)).setTooltip(FNLang.STABILIZER_TOGGLE_PITCH));


        rY += 28;


        addElement(new GuiValueSliderField(this, rX, rY, sW, FNLang.STABILIZER_TARGET_YAW_DEGREES.translate(), -180.0, 180.0,
            () -> tile.config.targetYawDeg(),
            v -> { tile.config.setTargetYawDeg(v); sync(); },
            v -> String.format("%3.2f", v)));

//        addElement(new GuiInnerScreen(this, rX, rY, sW, 16, () -> {
//            List<Component> list = new ArrayList<>();
//            list.add(FNLang.STABILIZER_TARGET_YAW_DEGREES.translate(String.format("%.2f", tile.config.targetYawDeg())));
//            return list;
//        }));
//        GuiSlider targetYawSlider = addElement(new GuiSlider(this, rX, rY + 12, sW-2, value -> {
//            tile.config.setTargetYawDeg(360.0 * value - 180.0);
//            sync();
//        }));
//        targetYawSlider.setValue((180.0 + tile.config.targetYawDeg()) / 360.0);

        addElement(new GuiTextureToggleButton(this, rX + sW - 16, rY, () -> tile.config.stabilizeYaw(), ((element, mouseX, mouseY) -> {
            tile.config.setStabilizeYaw(!tile.config.stabilizeYaw());
            sync();
            return true;
        }), GuiTextureElement.BLUEPRINT_ICON_GEAR, null).activeColor(Color.rgb(0x00FF00)).setTooltip(FNLang.STABILIZER_TOGGLE_YAW));

        rY += 28;

        addElement(new GuiValueSliderField(this, rX, rY, sW, FNLang.STABILIZER_TARGET_ROLL_DEGREES.translate(), -90.0, 90.0,
            () -> tile.config.targetRollDeg(),
            v -> { tile.config.setTargetRollDeg(v); sync(); },
            v -> String.format("%3.2f", v)));

//        addElement(new GuiInnerScreen(this, rX, rY, sW, 16, () -> {
//            List<Component> list = new ArrayList<>();
//            list.add(FNLang.STABILIZER_TARGET_ROLL_DEGREES.translate(String.format("%.2f", tile.config.targetRollDeg())));
//            return list;
//        }));
//        GuiSlider targetRollSlider = addElement(new GuiSlider(this, rX, rY + 12, sW-2, value -> {
//            tile.config.setTargetRollDeg(180.0 * value - 90.0);
//            sync();
//        }));
//        targetRollSlider.setValue((90.0 + tile.config.targetRollDeg()) / 180.0);

        addElement(new GuiTextureToggleButton(this, rX + sW - 16, rY, () -> tile.config.stabilizeRoll(), ((element, mouseX, mouseY) -> {
            tile.config.setStabilizeRoll(!tile.config.stabilizeRoll());
            sync();
            return true;
        }), GuiTextureElement.BLUEPRINT_ICON_GEAR, null).activeColor(Color.rgb(0x0000FF)).setTooltip(FNLang.STABILIZER_TOGGLE_ROLL));

        rY += 28;

        addElement(new GuiValueSliderField(this, rX, rY, sW, FNLang.STABILIZER_ACCEPT_ERR_DEGREES.translate(), 0.0, 45.0,
            () -> tile.config.acceptAngleDeg(),
            v -> { tile.config.setAcceptAngleDeg(v); sync(); },
            v -> String.format("%3.2f", v)));

//        addElement(new GuiInnerScreen(this, rX, rY, sW, 16, () -> {
//            List<Component> list = new ArrayList<>();
//            list.add(FNLang.STABILIZER_ACCEPT_ERR_DEGREES.translate(String.format("%.2f", tile.config.acceptAngleDeg())));
//            return list;
//        }));
//        GuiSlider acceptAngleSlider = addElement(new GuiSlider(this, rX, rY + 12, sW-2, value -> {
//            tile.config.setAcceptAngleDeg(45.0 * value);
//            sync();
//        }));
//        acceptAngleSlider.setValue(tile.config.acceptAngleDeg() / 45.0);

        addElement(new GuiTextureToggleButton(this, rX + sW - 16, rY, () -> tile.config.bidirectionalTorque(), ((element, mouseX, mouseY) -> {
            tile.config.setBidirectionalTorque(!tile.config.bidirectionalTorque());
            sync();
            return true;
        }), GuiTextureElement.BLUEPRINT_ICON_GEAR, null).activeColor(Color.rgb(0xFFFF00)).setTooltip(FNLang.STABILIZER_TOGGLE_BIDIRECTIONAL_TORQUE));

        addRenderableWidget(new GuiEnergyTab(this, () -> {
            long using = tile.getActive() ? tile.getEnergyUsed() : 0L;
            return List.of(MekanismLang.USING.translate(EnergyDisplay.of(using)),
                MekanismLang.NEEDED.translate(EnergyDisplay.of(tile.getEnergyContainer().getNeeded())));
        }));

        rY += 28;
        addRenderableWidget(new GuiHorizontalPowerBar(this, tile.getEnergyContainer(), rX + 5, rY, 190))
            .warning(WarningTracker.WarningType.NOT_ENOUGH_ENERGY, () -> tile.getEnergyCost() > tile.getEnergyContainer().getEnergy());
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderTitleText(guiGraphics);
        renderInventoryText(guiGraphics);
        super.drawForegroundText(guiGraphics, mouseX, mouseY);
    }

    private void sync() {
        PacketDistributor.sendToServer(new StabilizerSaveConfigPacket(tile.getBlockPos(), tile.config));
    }

    @Override
    public void containerTick() {
        super.containerTick();
    }
}