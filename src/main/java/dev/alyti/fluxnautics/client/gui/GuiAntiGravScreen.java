package dev.alyti.fluxnautics.client.gui;

import dev.alyti.fluxnautics.content.blocks.antigrav.AntiGravBlockEntity;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiEnergyGauge;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.common.MekanismLang;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.util.text.EnergyDisplay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GuiAntiGravScreen extends GuiConfigurableTile<AntiGravBlockEntity, MekanismTileContainer<AntiGravBlockEntity>> {

    public GuiAntiGravScreen(MekanismTileContainer<AntiGravBlockEntity> container, Inventory inv, Component title) {
        super(container, inv, title);
        dynamicSlots = true;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        addRenderableWidget(new GuiEnergyGauge(tile.getEnergyContainer(), GaugeType.WIDE, this, 55, 18));

        addRenderableWidget(new GuiEnergyTab(this, () -> {
            long using = tile.getActive() ? tile.getEnergyUsed() : 0L;
            return List.of(MekanismLang.USING.translate(EnergyDisplay.of(using)),
                    MekanismLang.NEEDED.translate(EnergyDisplay.of(tile.getEnergyContainer().getNeeded())));
        }));
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderTitleText(guiGraphics);
        renderInventoryText(guiGraphics);
        super.drawForegroundText(guiGraphics, mouseX, mouseY);
    }
}