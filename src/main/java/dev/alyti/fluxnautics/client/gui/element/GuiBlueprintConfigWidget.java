package dev.alyti.fluxnautics.client.gui.element;

import dev.alyti.fluxnautics.content.blocks.assembly_terminal.AssemblyTerminalBlockEntity;
import dev.alyti.fluxnautics.network.packets.blueprint.BlueprintDataPacket;
import dev.alyti.fluxnautics.network.packets.blueprint.UpdateBlueprintConfigPacket;
import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.GuiScreenSwitch;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class GuiBlueprintConfigWidget extends GuiInnerScreen {

    private final AssemblyTerminalBlockEntity tile;
    private Supplier<BlueprintDataPacket> serverDataSupplier;
    private BlueprintDataPacket serverData;

    public GuiBlueprintConfigWidget(IGuiWrapper gui, int x, int y, int width, int height, AssemblyTerminalBlockEntity tile, @NonNull Supplier<BlueprintDataPacket> dataSupplier) {
        super(gui, x, y, width, height);
        this.tile = tile;
        this.serverDataSupplier = dataSupplier;
        this.serverData = dataSupplier.get();
        this.isOverlay = true;

        addForceToggleWidgets(gui, relativeX, relativeY - 25);
    }

    private void addForceToggleWidgets(IGuiWrapper gui, final int diagramX, final int diagramY) {
        final Iterable<ForceGroup> forceGroups = ForceGroups.REGISTRY;

        int i = 0;
        for (final ForceGroup forceGroup : forceGroups) {
            final int yOffset = 24 * (i + 1) + 8;
            final int xOffset = 36;
            final ResourceLocation groupId = ForceGroups.REGISTRY.getKey(forceGroup);
            final int groupColor = (255 << 24) | forceGroup.color();
            final MutableComponent name = MutableComponent.create(forceGroup.name().getContents()).withColor(groupColor);
            final GuiScreenSwitch widget = new GuiScreenSwitch(gui, diagramX + xOffset, diagramY + yOffset, 80, name, () -> tile.blueprintConfig.enabledForceGroups().contains(groupId), (element, mouseX, mouseY) -> {
                if (tile.blueprintConfig.enabledForceGroups().contains(groupId))
                    tile.blueprintConfig.enabledForceGroups().remove(groupId);
                else
                    tile.blueprintConfig.enabledForceGroups().add(groupId);
                sync();
                return true;
            });
            addChild(new GuiInnerScreen(gui, diagramX + xOffset - 32, diagramY + yOffset + 1, 32, 20, () -> {
                List<Component> list = new ArrayList<>();
                if (serverData == null) {
                    return list;
                }
                final List<QueuedForceGroup.PointForce> forces = serverData.forces().get(forceGroup);
                int forceCount = forces != null ? forces.size() : 0;
                list.add(Component.literal(String.format("%03d", forceCount)));
                return list;
            }).textScale(1.22f).centerY());

            this.addChild(widget);
            i++;
        }
    }

    @Override
    public void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderForeground(guiGraphics, mouseX, mouseY);
        this.serverData = this.serverDataSupplier.get();
    }

    private void sync() {
        PacketDistributor.sendToServer(new UpdateBlueprintConfigPacket(tile.getBlockPos(), tile.blueprintConfig));
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!visible) {
            return false;
        }
        return super.isMouseOver(mouseX, mouseY);
    }
}
