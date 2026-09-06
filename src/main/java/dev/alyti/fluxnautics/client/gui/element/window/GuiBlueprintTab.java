package dev.alyti.fluxnautics.client.gui.element.window;

import dev.alyti.fluxnautics.content.blocks.assembly_terminal.AssemblyTerminalBlockEntity;
import dev.alyti.fluxnautics.lang.FNLang;
import dev.alyti.fluxnautics.network.packets.blueprint.BlueprintDataPacket;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import mekanism.client.SpecialColors;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.tab.window.GuiWindowCreatorTab;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.inventory.container.SelectedWindowData;
import mekanism.common.util.MekanismUtils;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class GuiBlueprintTab <F extends AssemblyTerminalBlockEntity> extends GuiWindowCreatorTab<AssemblyTerminalBlockEntity, GuiBlueprintTab<AssemblyTerminalBlockEntity>> {

    private static final SelectedWindowData WINDOW_DATA = new SelectedWindowData(SelectedWindowData.WindowType.UNSPECIFIED);

    @NotNull
    private final Supplier<BlueprintDataPacket> dataSupplier;

    @NotNull
    private final Consumer<UUID> sublevelConsumer;


    public GuiBlueprintTab(IGuiWrapper gui, AssemblyTerminalBlockEntity tile, Supplier<GuiBlueprintTab<AssemblyTerminalBlockEntity>> elementSupplier, @NonNull Supplier<BlueprintDataPacket> dataSupplier, @NonNull Consumer<UUID> sublevelConsumer) {
        super(MekanismUtils.getResource(MekanismUtils.ResourceType.GUI, "visuals.png"), gui, tile, -26, 62, 26, 18, true, elementSupplier);
        setTooltip(FNLang.EMITTER_CONFIG);
        this.dataSupplier = dataSupplier;
        this.sublevelConsumer = sublevelConsumer;
    }

    @Override
    protected void colorTab(GuiGraphics guiGraphics) {
        MekanismRenderer.color(guiGraphics, SpecialColors.TAB_VISUALS);
    }

    @Override
    protected GuiWindow createWindow(SelectedWindowData windowData) {
        ClientSubLevel sb = Sable.HELPER.getContainingClient(dataSource);
        if (sb == null) {
            return null;
        }
        this.sublevelConsumer.accept(sb.getUniqueId());
        return new GuiBlueprintWindow(gui(), (getGuiWidth() - 156) / 2, 15, dataSource, windowData, sb, dataSupplier);
    }

    @Override
    protected SelectedWindowData getNextWindowData() {
        return WINDOW_DATA;
    }
}