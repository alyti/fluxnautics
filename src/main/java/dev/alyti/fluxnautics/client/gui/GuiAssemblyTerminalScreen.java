package dev.alyti.fluxnautics.client.gui;

import dev.alyti.fluxnautics.client.gui.element.window.GuiBlueprintTab;
import dev.alyti.fluxnautics.content.blocks.assembly_terminal.AssemblyTerminalBlockEntity;
import dev.alyti.fluxnautics.network.packets.blueprint.BlueprintDataPacket;
import dev.alyti.fluxnautics.network.packets.blueprint.RequestBlueprintDataPacket;
import dev.ryanhcode.sable.Sable;
import foundry.veil.api.network.VeilPacketManager;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class GuiAssemblyTerminalScreen extends GuiMekanismTile<AssemblyTerminalBlockEntity, MekanismTileContainer<AssemblyTerminalBlockEntity>> {

    private GuiBlueprintTab<AssemblyTerminalBlockEntity> blueprintTab;

    @Nullable
    private BlueprintDataPacket blueprintServerData = null;
    private UUID sublevelID = null;
    private int ticksWithoutUpdate = 0;
    public static int UPDATE_REQUEST_INTERVAL = 10;

    public GuiAssemblyTerminalScreen(MekanismTileContainer<AssemblyTerminalBlockEntity> container, Inventory inv, Component title) {
        super(container, inv, title);
        dynamicSlots = true;
        imageHeight += 32;
        imageWidth = 266;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        blueprintTab = addElement(new GuiBlueprintTab<>(this, tile, () -> blueprintTab, () -> blueprintServerData, (sb) -> sublevelID = sb));
        blueprintTab.visible = Sable.HELPER.getContainingClient(tile) != null;
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderTitleText(guiGraphics);
        renderInventoryText(guiGraphics);
        super.drawForegroundText(guiGraphics, mouseX, mouseY);
    }

    public void updateBlueprintData(final BlueprintDataPacket data) {
        this.blueprintServerData = data;
    }

    @Override
    public void containerTick() {
        super.containerTick();

        if (sublevelID != null) {
            if (this.ticksWithoutUpdate++ > UPDATE_REQUEST_INTERVAL) {
                this.ticksWithoutUpdate = 0;
                VeilPacketManager.server().sendPacket(new RequestBlueprintDataPacket(this.sublevelID));
            }
        }
    }
}
