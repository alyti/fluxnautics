package dev.alyti.fluxnautics.client.gui.element.custom.group;

import dev.alyti.fluxnautics.client.gui.GuiThrusterArrayScreen;
import dev.alyti.fluxnautics.config.group.*;
import dev.alyti.fluxnautics.multiblock.ThrusterArrayMultiblockData;
import dev.alyti.fluxnautics.network.packets.PacketUpdateGroupSettings;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mekanism.api.text.IHasTextComponent;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.scroll.GuiScrollList;
import mekanism.client.gui.element.scroll.GuiScrollableElement;
import mekanism.client.gui.element.text.GuiTextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GuiGroupScreen extends GuiScrollableElement {

    private static final int ELEMENT_SPACER = 4;

    protected int currentGroupId = 0;
    private final List<MiniElement<?>> miniElements = new ArrayList<>();
    private int maxElements;
    private final GuiThrusterArrayScreen parentScreen;
    private final mekanism.client.gui.element.button.MekanismButton deleteButton;
    private final GuiTextField renameField;
    private int renamingGroupId = -1;
    private GroupStringConfig renamingConfig;

    public GuiGroupScreen(GuiThrusterArrayScreen gui, int x, int y) {
        super(GuiScrollList.SCROLL_LIST, gui, x, y, 116, 100, 116 - 6, 2, 4, 4, 100 - 4);
        this.parentScreen = gui;
        this.deleteButton = addChild(new mekanism.client.gui.element.button.MekanismButton(gui, x + width - 14, y + 2, 12, 12, Component.literal("x"), (element, mouseX, mouseY) -> {
            onDeleteGroup();
            return true;
        }));
        this.renameField = addChild(new GuiTextField(gui, x + 2, y + 2, width - 16, 12));
        this.renameField.visible = false;
        this.renameField.setMaxLength(20);
        this.renameField.setEnterHandler(() -> {
            if (renamingGroupId != -1 && renamingConfig != null) {
                updateSettings(renamingGroupId, renamingConfig, renameField.getText());
                setGroupId(currentGroupId);
                parentScreen.getGroups();
            }
            stopRenaming();
        });
    }

    public void startRenaming(int groupId, GroupStringConfig config, String current) {
        this.renamingGroupId = groupId;
        this.renamingConfig = config;
        this.renameField.setText(current);
        this.renameField.visible = true;
        this.renameField.setFocused(true);
    }

    public void stopRenaming() {
        this.renameField.visible = false;
        this.renamingGroupId = -1;
        this.renamingConfig = null;
    }

    private void onDeleteGroup() {
        if (currentGroupId > 0) {
            PacketDistributor.sendToServer(new PacketUpdateGroupSettings(parentScreen.getTileEntity().getBlockPos(), currentGroupId, "delete", new CompoundTag()));
            parentScreen.getGroups();
            parentScreen.selectGroup(0);
        }
    }

    public void setGroupId(int groupId) {
        this.currentGroupId = groupId;
        deleteButton.visible = (groupId > 0);
        stopRenaming();
        miniElements.clear();
        ObjectArrayList<ThrusterArrayMultiblockData.GroupData> groups = getMultiblock().thrusters.groups;
        if (groupId >= 0 && groupId < groups.size()) {
            ThrusterArrayMultiblockData.GroupData groupData = groups.get(groupId);
            if (groupData != null) {
                int startY = 13 + ELEMENT_SPACER + 1;
                for (GroupConfig<?, ?> config : groupData.getConfigs()) {
                    if (config instanceof GroupStringConfig && config.getName().equals("name") && groupId == 0) continue;
                    MiniElement<?> element = createMiniElement(groupId, config, startY);
                    if (element != null) {
                        miniElements.add(element);
                        startY += element.getNeededHeight() + ELEMENT_SPACER;
                    }
                }
                maxElements = startY - ELEMENT_SPACER;
            } else {
                maxElements = 0;
            }
        } else {
            maxElements = 0;
        }
    }

    private MiniElement<?> createMiniElement(int groupId, GroupConfig<?, ?> config, int startY) {
        if (config instanceof GroupBooleanConfig booleanConfig) {
            return new BooleanToggle(this,
                booleanConfig::get,
                (value) -> updateSettings(groupId, booleanConfig, value),
                Component.translatable(booleanConfig.getDescription().getTranslationKey()), 2, startY);
        } else if (config instanceof GroupColorConfig colorConfig) {
            return new ColorSelection(this,
                colorConfig::get,
                (value) -> updateSettings(groupId, colorConfig, value),
                Component.translatable(colorConfig.getDescription().getTranslationKey()), 2, startY);
        } else if (config instanceof GroupStringConfig stringConfig) {
            return new StringInput(this,
                stringConfig,
                stringConfig::get,
                (value) -> updateSettings(groupId, stringConfig, value),
                Component.translatable(stringConfig.getDescription().getTranslationKey()), 2, startY);
        } else if (config instanceof GroupEnumConfig<?> enumConfig) {
            return createEnumElement(groupId, enumConfig, startY);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <V extends Enum<V> & IHasTextComponent> MiniElement<V> createEnumElement(int groupId, GroupEnumConfig<?> config, int startY) {
        GroupEnumConfig<V> enumConfig = (GroupEnumConfig<V>) config;
        return new EnumToggle<>(this,
            enumConfig::get,
            (value) -> updateSettings(groupId, enumConfig, value),
            Component.translatable(enumConfig.getDescription().getTranslationKey()),
            2, startY, enumConfig);
    }

    private <V> void updateSettings(int groupId, GroupConfig<V, ?> config, V value) {
        config.set(value);
        CompoundTag tag = new CompoundTag();
        config.write(tag);
        PacketDistributor.sendToServer(new PacketUpdateGroupSettings(parentScreen.getTileEntity().getBlockPos(), groupId, config.getName(), tag));
    }

    protected ThrusterArrayMultiblockData getMultiblock() {
        return parentScreen.getTileEntity().getMultiblock();
    }

    @Override
    protected int getMaxElements() {
        return maxElements;
    }

    @Override
    protected int getFocusedElements() {
        return height - 2;
    }

    @Override
    protected int getScrollElementScaler() {
        return 10;
    }

    int getScreenWidth() {
        return barXShift;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double xDelta, double yDelta) {
        return isMouseOver(mouseX, mouseY) && adjustScroll(yDelta) || super.mouseScrolled(mouseX, mouseY, xDelta, yDelta);
    }

    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        super.onClick(mouseX, mouseY, button);
        double scrolledMouseY = mouseY + getCurrentSelection();
        for (MiniElement<?> element : miniElements) {
            element.click(mouseX, scrolledMouseY);
        }
    }

    @Override
    public void drawBackground(@NotNull GuiGraphics guiGraphics, int mx, int my, float partialTicks) {
        super.drawBackground(guiGraphics, mx, my, partialTicks);
        renderBackgroundTexture(guiGraphics, GuiInnerScreen.SCREEN, GuiInnerScreen.SCREEN_SIZE, GuiInnerScreen.SCREEN_SIZE);
        drawScrollBar(guiGraphics, GuiScrollList.TEXTURE_WIDTH, GuiScrollList.TEXTURE_HEIGHT);

        if (currentGroupId == -1) return;

        guiGraphics.enableScissor(getX() + 1, getY() + 13, getRight() - 1, getBottom() - 1);
        guiGraphics.pose().pushPose();
        int shift = getCurrentSelection();
        guiGraphics.pose().translate(0, -shift, 0);

        int startY = 13 + ELEMENT_SPACER + 1;
        for (MiniElement<?> element : miniElements) {
            if (startY + element.getNeededHeight() > shift + 13 && startY < shift + height) {
                element.renderBackground(guiGraphics, mx, (int) (my + shift));
            }
            startY += element.getNeededHeight() + ELEMENT_SPACER;
        }

        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();
    }

    @Override
    public void renderForeground(GuiGraphics guiGraphics, int mx, int my) {
        super.renderForeground(guiGraphics, mx, my);

        if (currentGroupId == -1) {
            drawCenteredText(guiGraphics, Component.translatable("gui.fluxnautics.no_selection"), 0, 0, width, height, titleTextColor());
            return;
        }

        drawTitle(guiGraphics);

        guiGraphics.enableScissor(getX() + 1, getY() + 13, getRight() - 1, getBottom() - 1);
        guiGraphics.pose().pushPose();
        int shift = getCurrentSelection();
        guiGraphics.pose().translate(0, -shift, 0);

        int startY = 13 + ELEMENT_SPACER + 1;
        for (MiniElement<?> element : miniElements) {
            if (startY + element.getNeededHeight() > shift + 13 && startY < shift + height) {
                element.renderForeground(guiGraphics, mx, (int) (my + shift));
            }
            startY += element.getNeededHeight() + ELEMENT_SPACER;
        }

        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();
    }

    private void drawTitle(GuiGraphics guiGraphics) {
        String name = "Invalid state";
        ObjectArrayList<ThrusterArrayMultiblockData.GroupData> groups = getMultiblock().thrusters.groups;
        if (currentGroupId >= 0 && currentGroupId < groups.size()) {
            ThrusterArrayMultiblockData.GroupData groupData = groups.get(currentGroupId);
            if (groupData != null) {
                name = groupData.getName();
            } else if (currentGroupId == 0) {
                name = "Default";
            }
        }
        drawScaledScrollingString(guiGraphics, Component.literal(name), 0, 2, TextAlignment.CENTER, titleTextColor(), getScreenWidth() - 15, 0, false, 0.8f);
    }

    private void drawCenteredText(GuiGraphics guiGraphics, Component text, int x, int y, int width, int height, int color) {
        int textWidth = Minecraft.getInstance().font.width(text);
        float scale = 1.0f;
        if (textWidth > width - 4) {
            scale = (float) (width - 4) / textWidth;
        }
        drawScaledScrollingString(guiGraphics, text, x + (width - (int)(textWidth * scale)) / 2, y + (height - (int)(8 * scale)) / 2, TextAlignment.LEFT, color, width, 0, false, scale);
    }
}