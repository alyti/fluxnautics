package dev.alyti.fluxnautics.client.gui.element.window;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import dev.alyti.fluxnautics.api.content.redstone.IRedstoneControlMode;
import dev.alyti.fluxnautics.api.content.redstone.IRedstoneSideConfigurable;
import dev.alyti.fluxnautics.api.content.redstone.TileComponentRedstoneControl;
import dev.alyti.fluxnautics.client.gui.element.GuiScrollDropdown;
import dev.alyti.fluxnautics.client.gui.element.button.RedstoneSideButton;
import dev.alyti.fluxnautics.lang.FNLang;
import dev.alyti.fluxnautics.network.packets.PacketSetRedstoneMode;
import mekanism.api.RelativeSide;
import mekanism.api.text.EnumColor;
import mekanism.api.text.TextComponentUtil;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.client.gui.tooltip.TooltipUtils;
import mekanism.common.inventory.container.SelectedWindowData.WindowType;
import mekanism.common.network.PacketUtils;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.util.EnumUtils;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class GuiRedstoneConfiguration<TILE extends TileEntityMekanism & IRedstoneSideConfigurable, MODE extends Enum<MODE> & IRedstoneControlMode<? super TILE>> extends GuiWindow {

    private static final long HOVER_OPEN_DELAY_MS = 450;
    private static final long CLOSE_GRACE_MS = 400;

    private static final int WINDOW_WIDTH = 156;
    private static final int WINDOW_HEIGHT = 170;
    private static final int DROPDOWN_X = 4;
    private static final int DROPDOWN_WIDTH = 148;
    private static final int DROPDOWN_VISIBLE_ROWS = 4;
    private static final int DROPDOWN_HEIGHT = 12 + DROPDOWN_VISIBLE_ROWS * 10 + 1;

    private static final int STRIP_Y = 96;
    private static final int STRIP_HEIGHT = 16;
    private static final int INVERT_X = 4;
    private static final int INVERT_WIDTH = 88;
    private static final int RESET_X = 94;
    private static final int RESET_WIDTH = 58;

    private final Map<RelativeSide, RedstoneSideButton<TILE>> sideButtons = new EnumMap<>(RelativeSide.class);
    private final TILE tile;
    private final TileComponentRedstoneControl<TILE, MODE> config;
    private final GuiScrollDropdown<MODE> dropdown;
    private final MekanismButton resetButton;
    private final InvertButton invertButton;
    @Nullable
    private final Consumer<Boolean> windowStateListener;

    private RelativeSide selected;
    @Nullable
    private RelativeSide revealSide;
    private long hoverStart;
    private long leaveStart = -1;

    public GuiRedstoneConfiguration(IGuiWrapper gui, int x, int y, TILE tile, TileComponentRedstoneControl<TILE, MODE> config,
                                    @Nullable Consumer<Boolean> windowStateListener) {
        super(gui, x, y, WINDOW_WIDTH, WINDOW_HEIGHT, WindowType.UNSPECIFIED);
        this.tile = tile;
        this.config = config;
        this.windowStateListener = windowStateListener;
        interactionStrategy = InteractionStrategy.ALL;
        RelativeSide initial = null;
        for (RelativeSide side : EnumUtils.SIDES) {
            if (config.isSideConfigurable(side)) {
                initial = side;
                break;
            }
        }
        selected = initial == null ? RelativeSide.FRONT : initial;
        addSideButton(RelativeSide.TOP, 67, 24);
        addSideButton(RelativeSide.LEFT, 44, 47);
        addSideButton(RelativeSide.FRONT, 67, 47);
        addSideButton(RelativeSide.RIGHT, 90, 47);
        addSideButton(RelativeSide.BACK, 44, 70);
        addSideButton(RelativeSide.BOTTOM, 67, 70);
        dropdown = addChild(new GuiScrollDropdown<>(gui(), relativeX + DROPDOWN_X, relativeY + WINDOW_HEIGHT - DROPDOWN_HEIGHT,
            DROPDOWN_WIDTH, List.of(config.getModeClass().getEnumConstants()), () -> getMode(selected),
            mode -> mode.shortText(), mode -> List.of(mode.text()), mode -> sendSetMode(selected, mode), DROPDOWN_VISIBLE_ROWS));
        dropdown.visible = false;
        invertButton = addChild(new InvertButton(gui(), relativeX + INVERT_X, relativeY + STRIP_Y, INVERT_WIDTH, STRIP_HEIGHT));
        resetButton = addChild(new MekanismButton(gui(), relativeX + RESET_X, relativeY + STRIP_Y, RESET_WIDTH, STRIP_HEIGHT,
            FNLang.REDSTONE_RESET_ALL.translate(), (element, mouseX, mouseY) -> sendResetAll()));
        resetButton.setTooltip(FNLang.REDSTONE_RESET_HINT);
        if (windowStateListener != null) {
            windowStateListener.accept(true);
        }
    }

    private void addSideButton(RelativeSide side, int xPos, int yPos) {
        RedstoneSideButton<TILE> button = addChild(new RedstoneSideButton<>(this, gui(), relativeX + xPos, relativeY + yPos, side, tile));
        sideButtons.put(side, button);
    }

    @Override
    public void close() {
        super.close();
        if (windowStateListener != null) {
            windowStateListener.accept(false);
        }
    }

    public void select(@Nullable RelativeSide side) {
        if (side != null) {
            this.selected = side;
        }
    }

    private MODE getMode(RelativeSide side) {
        return config.getMode(side);
    }

    private boolean sendSetMode(RelativeSide side, MODE mode) {
        if (config.isDigitalOverride() || !config.isSideConfigurable(side)) {
            return false;
        }
        return PacketUtils.sendToServer(PacketSetRedstoneMode.setMode(tile.getBlockPos(), side, mode.ordinal()));
    }

    private boolean toggleInversion() {
        if (config.isDigitalOverride() || !config.isSideConfigurable(selected)) {
            return false;
        }
        return PacketUtils.sendToServer(PacketSetRedstoneMode.setInverted(tile.getBlockPos(), selected, !config.isInverted(selected)));
    }

    private boolean sendResetAll() {
        if (config.isDigitalOverride()) {
            return false;
        }
        return PacketUtils.sendToServer(PacketSetRedstoneMode.resetAll(tile.getBlockPos()));
    }

    private Component invertLabel() {
        boolean inverted = config.isInverted(selected);
        return TextComponentUtil.build(inverted ? EnumColor.BRIGHT_GREEN : EnumColor.GRAY,
            FNLang.REDSTONE_INVERT.translate(), ": ", inverted ? "ON" : "OFF");
    }

    @Override
    public void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderForeground(guiGraphics, mouseX, mouseY);
        drawTitleText(guiGraphics, FNLang.REDSTONE_CONTROL.translate(), 5);
        if (config.isDigitalOverride()) {
            guiGraphics.drawString(font(), TextComponentUtil.build(EnumColor.RED, FNLang.REDSTONE_OVERRIDE_ACTIVE.translate()),
                relativeX + 5, relativeY + 15, 0xFFFFFF, false);
        }
        updateHoverReveal(mouseX, mouseY);
    }

    private void updateHoverReveal(double mouseX, double mouseY) {
        RelativeSide hovered = getHoveredConfigurableSide(mouseX, mouseY);
        boolean overDropdown = dropdown.visible && dropdown.isMouseOver(mouseX, mouseY);
        if (hovered != null) {
            leaveStart = -1;
            if (hovered != revealSide) {
                revealSide = hovered;
                hoverStart = Util.getMillis();
            } else if (!dropdown.visible && Util.getMillis() - hoverStart >= HOVER_OPEN_DELAY_MS) {
                reveal(hovered);
            }
        } else if (overDropdown) {
            leaveStart = -1;
            revealSide = null;
        } else if (dropdown.visible) {
            if (leaveStart == -1) {
                leaveStart = Util.getMillis();
            } else if (Util.getMillis() - leaveStart >= CLOSE_GRACE_MS) {
                hideDropdown();
            }
        } else {
            revealSide = null;
            leaveStart = -1;
        }
    }

    private void reveal(RelativeSide side) {
        dropdown.setOpen(false);
        select(side);
        dropdown.visible = true;
        dropdown.setOpen(true);
    }

    private void hideDropdown() {
        dropdown.setOpen(false);
        dropdown.visible = false;
        revealSide = null;
        leaveStart = -1;
    }

    @Nullable
    private RelativeSide getHoveredConfigurableSide(double mouseX, double mouseY) {
        if (config.isDigitalOverride()) {
            return null;
        }
        for (Map.Entry<RelativeSide, RedstoneSideButton<TILE>> entry : sideButtons.entrySet()) {
            if (entry.getValue().isMouseOver(mouseX, mouseY) && config.isSideConfigurable(entry.getKey())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private class InvertButton extends MekanismButton {

        private List<Component> lastInfo = Collections.emptyList();
        @Nullable
        private Tooltip lastTooltip;

        private InvertButton(IGuiWrapper gui, int x, int y, int width, int height) {
            super(gui, x, y, width, height, Component.empty(), (element, mouseX, mouseY) -> toggleInversion());
        }

        @Override
        public void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            setMessage(invertLabel());
            super.renderForeground(guiGraphics, mouseX, mouseY);
        }

        @Override
        public void updateTooltip(int mouseX, int mouseY) {
            List<Component> lines = new ArrayList<>(2);
            lines.add(TextComponentUtil.build(EnumColor.WHITE, FNLang.REDSTONE_INVERT.translate(), " (", selected, ")"));
            if (config.isDigitalOverride()) {
                lines.add(TextComponentUtil.build(EnumColor.RED, FNLang.REDSTONE_OVERRIDE_LOCKED.translate()));
            } else {
                lines.add(FNLang.REDSTONE_INVERT_HINT.translate());
            }
            if (!lines.equals(lastInfo)) {
                lastInfo = lines;
                lastTooltip = TooltipUtils.create(lines);
            }
            setTooltip(lastTooltip);
        }
    }
}