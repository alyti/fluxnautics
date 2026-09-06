package dev.alyti.fluxnautics.client.gui.element.window;

import dev.alyti.fluxnautics.client.gui.element.GuiBlueprintConfigWidget;
import dev.alyti.fluxnautics.client.gui.element.GuiBlueprintWidget;
import dev.alyti.fluxnautics.client.gui.element.GuiTextureElement;
import dev.alyti.fluxnautics.client.gui.element.button.GuiTextureToggleButton;
import dev.alyti.fluxnautics.content.blocks.assembly_terminal.AssemblyTerminalBlockEntity;
import dev.alyti.fluxnautics.network.packets.blueprint.BlueprintDataPacket;
import dev.alyti.fluxnautics.network.packets.blueprint.UpdateBlueprintConfigPacket;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.common.inventory.container.SelectedWindowData;
import mekanism.common.lib.Color;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.function.Supplier;

public class GuiBlueprintWindow extends GuiWindow {
    private static final int OUTER_SIZE = 400;
    private static final int BORDER_SIZE = 10;
    private static final int INNER_SIZE = OUTER_SIZE - (BORDER_SIZE * 2);

    private final AssemblyTerminalBlockEntity tile;
    public final ClientSubLevel subLevel;
    private GuiBlueprintWidget blueprintWidget;
    private GuiBlueprintConfigWidget blueprintConfigWidget;
    private boolean settingsOpen = false;

    public GuiBlueprintWindow(IGuiWrapper gui, int x, int y, AssemblyTerminalBlockEntity tile, SelectedWindowData windowData, ClientSubLevel subLevel, @NonNull Supplier<BlueprintDataPacket> dataSupplier) {
        super(gui, x, y, OUTER_SIZE, OUTER_SIZE, windowData);
        this.tile = tile;
        this.subLevel = subLevel;
        blueprintConfigWidget = addChild(new GuiBlueprintConfigWidget(gui, relativeX + BORDER_SIZE, relativeY + BORDER_SIZE, INNER_SIZE, INNER_SIZE, tile, dataSupplier));
        blueprintConfigWidget.visible = settingsOpen;
        blueprintConfigWidget.active = settingsOpen;
        blueprintWidget = addChild(new GuiBlueprintWidget(gui, relativeX + BORDER_SIZE, relativeY + BORDER_SIZE, INNER_SIZE, INNER_SIZE, subLevel, dataSupplier, tile));
        blueprintWidget.visible = !settingsOpen;
        blueprintWidget.active = !settingsOpen;

        addChild(new GuiTextureToggleButton(gui(), relativeX - 21, relativeY + 8, () -> settingsOpen, (element, mouseX, mouseY) -> {
            settingsOpen = !settingsOpen;

            blueprintWidget.visible = !settingsOpen;
            blueprintConfigWidget.visible = settingsOpen;
            blueprintWidget.active = !settingsOpen;
            blueprintConfigWidget.active = settingsOpen;
            return true;
        }, GuiTextureElement.BLUEPRINT_ICON_CONFIG, null).activeColor(Color.argb(0xAA999999)).renderTab(true));

        addChild(new GuiTextureToggleButton(gui(), relativeX - 21, relativeY + 8 + 30, () -> tile.blueprintConfig.displayBoundingBox(), (element, mouseX, mouseY) -> {
            tile.blueprintConfig.setDisplayBoundingBox(!tile.blueprintConfig.displayBoundingBox());
            sync();
            return true;
        }, GuiTextureElement.BLUEPRINT_ICON_BOUNDING_BOX, null).activeColor(Color.argb(0xAA999999)).renderTab(true));

        addChild(new GuiTextureToggleButton(gui(), relativeX - 21, relativeY + 8 + 30 + 30, () -> tile.blueprintConfig.displayCenterOfMass(), (element, mouseX, mouseY) -> {
            tile.blueprintConfig.setDisplayCenterOfMass(!tile.blueprintConfig.displayCenterOfMass());
            sync();
            return true;
        }, GuiTextureElement.BLUEPRINT_ICON_COM_TOGGLE, null).activeColor(Color.argb(0xAA999999)).renderTab(true));

        addChild(new GuiTextureToggleButton(gui(), relativeX - 21, relativeY + 8 + 30 + 30 + 30, () -> tile.blueprintConfig.mergeForces(), (element, mouseX, mouseY) -> {
            tile.blueprintConfig.setMergeForces(!tile.blueprintConfig.mergeForces());
            sync();
            return true;
        }, GuiTextureElement.BLUEPRINT_ICON_FORCES_SEPARATED, GuiTextureElement.BLUEPRINT_ICON_FORCES_MERGED).renderTab(true));
    }

    @Override
    public void close() {
        blueprintWidget.freeFramebuffers();
        super.close();
    }

    @Override
    protected boolean isValidClickButton(int button) {
        return button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
    }

    private void sync() {
        PacketDistributor.sendToServer(new UpdateBlueprintConfigPacket(tile.getBlockPos(), tile.blueprintConfig));
    }
}
