package dev.alyti.fluxnautics.client.gui;

import dev.alyti.fluxnautics.client.gui.element.GuiLeverWidget;
import dev.alyti.fluxnautics.content.blocks.conduit.ConduitConnectorBlockEntity;
import dev.alyti.fluxnautics.network.packets.PacketFluxGuiInteract;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.network.PacketUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;

public class GuiConduitConnectorScreen extends GuiMekanismTile<ConduitConnectorBlockEntity, MekanismTileContainer<ConduitConnectorBlockEntity>> {

    public GuiConduitConnectorScreen(MekanismTileContainer<ConduitConnectorBlockEntity> container, Inventory inv, Component title) {
        super(container, inv, title);
        dynamicSlots = true;
        imageHeight += 92;
        inventoryLabelY += 92;
        imageWidth = 266;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        AtomicReference<Double> v = new AtomicReference<>((double) 0);

        addElement(new GuiLeverWidget(this, 120, 20, 40, 120,
            tile::getMotorSpeed,
            (val) -> {
                CompoundTag tag = new CompoundTag();
                tag.putDouble("value", val);
                PacketUtils.sendToServer(new PacketFluxGuiInteract(PacketFluxGuiInteract.GuiFluxInteraction.CONDUIT_MOTOR, tile.getBlockPos(), tag));
            }
        ));
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderTitleText(guiGraphics);
        renderInventoryText(guiGraphics);
        super.drawForegroundText(guiGraphics, mouseX, mouseY);
    }
}