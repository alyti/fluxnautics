package dev.alyti.fluxnautics.client.gui.element.window;

import dev.alyti.fluxnautics.FluxnauticsConfig;
import dev.alyti.fluxnautics.client.gui.element.GuiValueSliderField;
import dev.alyti.fluxnautics.content.blocks.forcefield.ForcefieldEmitterBlockEntity;
import dev.alyti.fluxnautics.content.blocks.inertia_dampener.InertiaDampenerConfig;
import dev.alyti.fluxnautics.lang.FNLang;
import dev.alyti.fluxnautics.network.packets.forcefield.EmitterSaveConfigPacket;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiColorPickerSlot;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.GuiScreenSwitch;
import mekanism.client.gui.element.GuiSlider;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.common.MekanismLang;
import mekanism.common.inventory.container.SelectedWindowData;
import mekanism.common.lib.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class GuiForcefieldEmitterConfigWindow extends GuiWindow {

    private final ForcefieldEmitterBlockEntity tile;

    public GuiForcefieldEmitterConfigWindow(IGuiWrapper gui, int x, int y, ForcefieldEmitterBlockEntity tile, SelectedWindowData windowData) {
        super(gui, x, y, 200, 155, windowData);
        this.tile = tile;
        addChild(new GuiColorPickerSlot(gui, relativeX + 12, relativeY + 24, true, () -> Color.argb(tile.config.passiveColor()), color -> {
            tile.config.setPassiveColor(color.argb());
            sync();
        }));
        addChild(new GuiColorPickerSlot(gui, relativeX + 100, relativeY + 24, true, () -> Color.argb(tile.config.activeColor()), color -> {
            tile.config.setActiveColor(color.argb());
            sync();
        }));
        addChild(new GuiScreenSwitch(gui, relativeX +  32, relativeY + 22, 64, FNLang.EMITTER_PASSIVE_RAINBOW.translate(), () -> tile.config.passiveColorRainbow(), (element, mouseX, mouseY) -> {
            tile.config.setPassiveColorRainbow(!tile.config.passiveColorRainbow());
            sync();
            return true;
        }));
        addChild(new GuiScreenSwitch(gui, relativeX + 122, relativeY + 22,  64, FNLang.EMITTER_ACTIVE_RAINBOW.translate(), () -> tile.config.activeColorRainbow(), (element, mouseX, mouseY) -> {
            tile.config.setActiveColorRainbow(!tile.config.activeColorRainbow());
            sync();
            return true;
        }));

        addChild(new GuiValueSliderField(gui, relativeX + 10, relativeY + 46, 180, FNLang.EMITTER_FIELD_EXPANSION.translate(), 1.0, FluxnauticsConfig.common.forcefieldMaxFieldExpansion.getAsDouble(),
            () -> tile.config.fieldExpansion(),
            v -> { tile.config.setFieldExpansion(v); sync(); },
            v -> String.format("%3.3f", v)));

        addChild(new GuiScreenSwitch(gui, relativeX + 6, relativeY + 75,  90, FNLang.EMITTER_ALLOW_PASSIVE_MOBS.translate(), () -> tile.config.allowPassiveMobs(), (element, mouseX, mouseY) -> {
            tile.config.setAllowPassiveMobs(!tile.config.allowPassiveMobs());
            sync();
            return true;
        }));
        addChild(new GuiScreenSwitch(gui, relativeX + 104, relativeY + 75,  90, FNLang.EMITTER_ALLOW_HOSTILE_MOBS.translate(), () -> tile.config.allowHostileMobs(), (element, mouseX, mouseY) -> {
            tile.config.setAllowHostileMobs(!tile.config.allowHostileMobs());
            sync();
            return true;
        }));

        addChild(new GuiScreenSwitch(gui, relativeX + 6, relativeY + 100,  90, FNLang.EMITTER_ALLOW_PROJECTILES.translate(), () -> tile.config.allowProjectiles(), (element, mouseX, mouseY) -> {
            tile.config.setAllowProjectiles(!tile.config.allowProjectiles());
            sync();
            return true;
        }));
        addChild(new GuiScreenSwitch(gui, relativeX + 104, relativeY + 100,  90, FNLang.EMITTER_ALLOW_FOREIGN_PLAYERS.translate(), () -> tile.config.allowForeignPlayers(), (element, mouseX, mouseY) -> {
            tile.config.setAllowForeignPlayers(!tile.config.allowForeignPlayers());
            sync();
            return true;
        }));

        addChild(new GuiScreenSwitch(gui, relativeX + 6, relativeY + 125,  90, FNLang.EMITTER_ALLOW_FOREIGN_SUBLEVELS.translate(), () -> tile.config.allowForeignSublevels(), (element, mouseX, mouseY) -> {
            tile.config.setAllowForeignSublevels(!tile.config.allowForeignSublevels());
            sync();
            return true;
        }));
        addChild(new GuiScreenSwitch(gui, relativeX + 104, relativeY + 125,  90, FNLang.EMITTER_ALLOW_EVERYTHING_ELSE.translate(), () -> tile.config.allowEverythingElse(), (element, mouseX, mouseY) -> {
            tile.config.setAllowEverythingElse(!tile.config.allowEverythingElse());
            sync();
            return true;
        }));
    }

    private void sync() {
        PacketDistributor.sendToServer(new EmitterSaveConfigPacket(tile.getBlockPos(), tile.config));
    }

    @Override
    public void close() {
        super.close();
    }

    @Override
    public void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderForeground(guiGraphics, mouseX, mouseY);
        drawTitleText(guiGraphics, MekanismLang.CONFIG_TYPE.translate("Forcefield Emitter"), 8);
    }

    @Override
    protected int getTitlePadEnd() {
        return super.getTitlePadEnd() + 18;
    }
}