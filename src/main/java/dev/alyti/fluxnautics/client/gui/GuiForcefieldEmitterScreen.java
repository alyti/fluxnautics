package dev.alyti.fluxnautics.client.gui;

import dev.alyti.fluxnautics.FluxnauticsConfig;
import dev.alyti.fluxnautics.capabilities.MassBasedEnergyContainer;
import dev.alyti.fluxnautics.client.gui.element.window.GuiForcefieldEmitterConfigTab;
import dev.alyti.fluxnautics.content.blocks.forcefield.ForcefieldEmitterBlockEntity;
import dev.alyti.fluxnautics.content.blocks.forcefield.ForcefieldEmitterContainer;
import dev.alyti.fluxnautics.lang.FNLang;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.bar.GuiBar;
import mekanism.client.gui.element.bar.GuiDynamicHorizontalRateBar;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.graph.GuiLongGraph;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.common.MekanismLang;
import mekanism.common.inventory.warning.WarningTracker;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.text.EnergyDisplay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GuiForcefieldEmitterScreen extends GuiConfigurableTile<ForcefieldEmitterBlockEntity, ForcefieldEmitterContainer> {


    private GuiForcefieldEmitterConfigTab<ForcefieldEmitterBlockEntity> configTab;
    private GuiLongGraph impactGraph;

    public GuiForcefieldEmitterScreen(ForcefieldEmitterContainer container, Inventory inv, Component title) {
        super(container, inv, title);

        imageHeight += 100;
        inventoryLabelY += 100;
        dynamicSlots = true;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();

        configTab = addElement(new GuiForcefieldEmitterConfigTab<>(this, tile, () -> configTab));

        addRenderableWidget(new GuiInnerScreen(this, 30, 16, 132, 54, () -> List.of(
                FNLang.EMITTER_VOLUME.translate(Math.round(tile.getFieldVolume())),
                FNLang.EMITTER_PER_IMPACT_USAGE.translate(EnergyDisplay.of(tile.getRollingImpactCost()), FluxnauticsConfig.common.forcefieldRollingImpactWindow.get()),
                FNLang.EMITTER_FREQUENCY_ITEM.translate(tile.getFrequencyItemStack().getItem()),
                FNLang.EMITTER_COOLDOWN.translate(ticksToDuration(tile.getCooldownTicks()))
        )).clearFormat());

        addRenderableWidget(new GuiVerticalPowerBar(this, tile.getEnergyContainer(), 164, 16))
            .warning(WarningTracker.WarningType.NOT_ENOUGH_ENERGY, () -> {
                MassBasedEnergyContainer<ForcefieldEmitterBlockEntity> energyContainer = tile.getEnergyContainer();
                return tile.getEnergyCost() > energyContainer.getEnergy();
            });

        addRenderableWidget(new GuiEnergyTab(this, () -> {
            long using = tile.getActive() ? tile.getEnergyUsed() : 0L;
            return List.of(MekanismLang.USING.translate(EnergyDisplay.of(using)),
                    MekanismLang.NEEDED.translate(EnergyDisplay.of(tile.getEnergyContainer().getNeeded())));
        }));
        addRenderableWidget(new GuiDynamicHorizontalRateBar(this, new GuiBar.IBarInfoHandler() {
            @Override
            public Component getTooltip() {
                if (tile.getCooldownTicks() > 0 && tile.getMaxCooldownTicks() > 0) {
                    return MutableComponent.create( ticksToDuration(tile.getCooldownTicks()).getContents() ).append(Component.literal(" / ")).append( ticksToDuration(tile.getMaxCooldownTicks()) );
                }

                return MutableComponent.create(MekanismUtils.getEnergyDisplayShort(tile.getRollingImpactCost()).getContents()).append(Component.literal(" / ")).append(MekanismUtils.getEnergyDisplayShort(tile.getRollingImpactCooldownThreshold()));
            }

            @Override
            public double getLevel() {
                if (tile.getCooldownTicks() > 0 && tile.getMaxCooldownTicks() > 0) {
                    return Math.min(1, (double) tile.getCooldownTicks() / tile.getMaxCooldownTicks());
                }

                return Math.min(1, (double) tile.getRollingImpactCost() / tile.getRollingImpactCooldownThreshold());
            }
        }, 5, 74, imageWidth - 12));
        impactGraph = addRenderableWidget(new GuiLongGraph(this, 5, 86, imageWidth - 12, 80, MekanismUtils::getEnergyDisplayShort));
        impactGraph.setMinScale(tile.getRollingImpactCooldownThreshold());
        impactGraph.addData(1);
    }

    public static Component ticksToDuration(long duration) {
        if (duration < 20)
            return Component.literal("%d ticks".formatted(duration));
        return Component.literal(StringUtil.formatTickDuration((int) duration, 20));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        impactGraph.addData(tile.getRollingImpactCost());
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderTitleText(guiGraphics);
        renderInventoryText(guiGraphics);
        super.drawForegroundText(guiGraphics, mouseX, mouseY);
    }
}