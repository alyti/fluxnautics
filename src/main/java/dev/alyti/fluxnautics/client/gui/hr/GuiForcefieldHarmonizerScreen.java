package dev.alyti.fluxnautics.client.gui.hr;

import dev.alyti.fluxnautics.content.blocks.forcefield.ForcefieldHarmonizerBlockEntity;
import mekanism.client.gui.element.bar.GuiHorizontalPowerBar;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.client.gui.element.tab.window.GuiSideConfigurationTab;
import mekanism.client.gui.element.tab.window.GuiTransporterConfigTab;
import mekanism.common.MekanismLang;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.util.text.EnergyDisplay;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class GuiForcefieldHarmonizerScreen extends GuiHarmonicResonanceFilterHandler<ForcefieldHarmonizerBlockEntity> {

    private GuiSideConfigurationTab<ForcefieldHarmonizerBlockEntity> sideConfigTab;
    private GuiTransporterConfigTab<ForcefieldHarmonizerBlockEntity> transporterConfigTab;

    public GuiForcefieldHarmonizerScreen(MekanismTileContainer<ForcefieldHarmonizerBlockEntity> container, Inventory inv, Component title) {
        super(container, inv, title);
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();

        sideConfigTab = addRenderableWidget(new GuiSideConfigurationTab<>(this, tile, () -> sideConfigTab));
        transporterConfigTab = addRenderableWidget(new GuiTransporterConfigTab<>(this, tile, () -> transporterConfigTab));

        addRenderableWidget(new GuiHorizontalPowerBar(this, tile.getEnergyContainer(), 10, 122, 215));
        addRenderableWidget(new GuiEnergyTab(this, () -> {
            long using = tile.getActive() ? tile.getEnergyContainer().getEnergyPerTick() : 0L;
            return List.of(MekanismLang.USING.translate(EnergyDisplay.of(using)),
                    MekanismLang.NEEDED.translate(EnergyDisplay.of(tile.getEnergyContainer().getNeeded())));
        }));
    }
}