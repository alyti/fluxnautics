package dev.alyti.fluxnautics.client.gui;

import dev.alyti.fluxnautics.client.gui.element.custom.group.GuiGroupScreen;
import dev.alyti.fluxnautics.client.gui.element.scroll.GuiGroupScrollList;
import dev.alyti.fluxnautics.content.blocks.thruster.ThrusterArrayControllerBlockEntity;
import dev.alyti.fluxnautics.multiblock.ThrusterArrayMultiblockData;
import dev.alyti.fluxnautics.network.packets.PacketUpdateGroupSettings;
import dev.alyti.fluxnautics.util.SFColors;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mekanism.api.chemical.IChemicalTank;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.element.GuiElementHolder;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiChemicalGauge;
import mekanism.client.gui.element.gauge.GuiEnergyGauge;
import mekanism.client.gui.element.gauge.GuiTankGauge;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.common.MekanismLang;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.util.text.EnergyDisplay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class GuiThrusterArrayScreen extends GuiMekanismTile<ThrusterArrayControllerBlockEntity, MekanismTileContainer<ThrusterArrayControllerBlockEntity>> {

    private GuiGroupScrollList scrollList;
    private GuiGroupScreen groupScreen;
    private final List<ThrusterArrayMultiblockData.GroupData> currentGroups = new ObjectArrayList<>();
    private List<Integer> groupIds;

    public GuiThrusterArrayScreen(MekanismTileContainer<ThrusterArrayControllerBlockEntity> container, Inventory inv, Component title) {
        super(container, inv, title);
        dynamicSlots = true;
        imageHeight += 32;
        imageWidth = 266;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        addRenderableWidget(new GuiEnergyTab(this, () -> List.of(MekanismLang.USING.translate(EnergyDisplay.of(tile.getMultiblock().getEnergyUsed())),
            MekanismLang.NEEDED.translate(EnergyDisplay.of(tile.getMultiblock().energyContainer.getNeeded())))));
        addRenderableWidget(new GuiEnergyGauge(new GuiEnergyGauge.IEnergyInfoHandler() {
            @Override
            public long getEnergy() {
                return tile.getMultiblock().energyContainer.getEnergy();
            }

            @Override
            public long getMaxEnergy() {
                return tile.getMultiblock().energyContainer.getMaxEnergy();
            }
        }, GaugeType.SMALL_MED, this, 5, 7, 16, 46));

        addRenderableWidget(new GuiChemicalGauge(new GuiTankGauge.ITankInfoHandler<>() {
            @Override
            public IChemicalTank getTank() {
                return tile.getMultiblock().chemicalTank;
            }

            @Override
            public int getTankIndex() {
                return 0;
            }
        }, GaugeType.SMALL_MED, this, 5, 7 + 46 + 8, 16, 46));

        addRenderableWidget(new GuiElementHolder(this, 24, 7, 116, 100));
        scrollList = addRenderableWidget(new GuiGroupScrollList(this, 24, 7, 116, 100 - 17, this::onGroupSelected));
        getGroups();

        groupScreen = addRenderableWidget(new GuiGroupScreen(this, 144, 7));
        groupScreen.setGroupId(0);

        addRenderableWidget(new MekanismButton(this, 24, 7 + 100 - 17, 116, 16, Component.translatable("gui.fluxnautics.button.add_group"), (element, mouseX, mouseY) -> {
            onAddGroup();
            return true;
        }));
    }

    private void onGroupSelected(int index) {
        if (groupIds != null && index >= 0 && index < groupIds.size()) {
            groupScreen.setGroupId(index);
        }
    }

    public void selectGroup(int index) {
        if (scrollList != null) {
            scrollList.setSelected(index);
        }
        if (groupScreen != null) {
            groupScreen.setGroupId(index);
        }
    }

    private void onAddGroup() {
        ThrusterArrayMultiblockData multiblock = tile.getMultiblock();
        int nextId = multiblock.thrusters.groups.size();
        ThrusterArrayMultiblockData.GroupData group = multiblock.thrusters.getOrCreateGroup(nextId);
        group.colorConfig.set(SFColors.MEDIA_OURPLE);
        CompoundTag tag = new CompoundTag();
        tag.putInt("color", SFColors.MEDIA_OURPLE);
        PacketDistributor.sendToServer(new PacketUpdateGroupSettings(tile.getBlockPos(), nextId, "color", tag));
        getGroups();
        selectGroup(currentGroups.size() - 1);
    }

    public void getGroups() {
        ThrusterArrayMultiblockData multiblock = tile.getMultiblock();
        currentGroups.clear();
        currentGroups.addAll(multiblock.thrusters.groups);
        groupIds = IntStream.range(0, currentGroups.size()).collect(IntArrayList::new, IntArrayList::add, IntArrayList::addAll);
        List<String> groupNames = new ArrayList<>();
        for (int id : groupIds) {
            groupNames.add(String.format("%s (%d)", currentGroups.get(id).getName(), id));
        }
        scrollList.setText(groupNames);
    }


    @Override
    protected void drawForegroundText(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        drawTitleText(guiGraphics, playerInventoryTitle, titleTextColor());
        super.drawForegroundText(guiGraphics, mouseX, mouseY);
    }
}