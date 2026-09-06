package dev.alyti.fluxnautics.client.gui.element;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import mekanism.client.gui.GuiUtils;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiElement;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.GuiTexturedElement;
import mekanism.client.gui.tooltip.TooltipUtils;
import mekanism.common.registries.MekanismSounds;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fork of Mekanism's GuiDropdown with the two tweaks an 18-entry redstone mode enum needs:
 * <ul>
 * <li>The open list is capped at {@code maxVisibleRows} rows and pages with the scroll wheel, instead of growing
 * one row per option (18 options would make the open list ~193px tall, overflowing the config window).</li>
 * <li>Names/tooltips are supplied as functions, so the enum does not have to implement Mekanism's
 * {@code GuiComponents.IDropdownEnum} (keeps the enum pure common-side code).</li>
 * </ul>
 * Interaction is identical to GuiDropdown: click the header to toggle, drag down and release on a row to select.
 * When the list has more options than visible rows it scrolls with the wheel and draws a small thumb on its right
 * edge. On open it starts scrolled to roughly center the current selection.
 */
public class GuiScrollDropdown<TYPE> extends GuiTexturedElement {

    private static final int ELEMENT_HEIGHT = 12;
    private static final int ROW_HEIGHT = 10;
    public static final int DEFAULT_MAX_VISIBLE_ROWS = 6;

    private final List<TYPE> options;
    private final Function<TYPE, Component> shortNames;
    private final Function<TYPE, List<Component>> tooltips;
    private final Supplier<TYPE> curType;
    private final Consumer<TYPE> handler;
    private final int maxVisibleRows;
    private final Tooltip[] cachedTooltips;

    @Nullable
    private ScreenRectangle cachedTooltipRect;

    private long msOpened;
    private boolean isOpen;
    private int scrollOffset;

    public GuiScrollDropdown(IGuiWrapper gui, int x, int y, int width, List<TYPE> options, Supplier<TYPE> curType,
          Function<TYPE, Component> shortNames, Function<TYPE, List<Component>> tooltips, Consumer<TYPE> handler) {
        this(gui, x, y, width, options, curType, shortNames, tooltips, handler, DEFAULT_MAX_VISIBLE_ROWS);
    }

    public GuiScrollDropdown(IGuiWrapper gui, int x, int y, int width, List<TYPE> options, Supplier<TYPE> curType,
          Function<TYPE, Component> shortNames, Function<TYPE, List<Component>> tooltips, Consumer<TYPE> handler, int maxVisibleRows) {
        super(GuiInnerScreen.SCREEN, gui, x, y, width, ELEMENT_HEIGHT);
        this.options = List.copyOf(options);
        this.curType = curType;
        this.shortNames = shortNames;
        this.tooltips = tooltips;
        this.handler = handler;
        this.maxVisibleRows = Math.max(1, maxVisibleRows);
        this.cachedTooltips = new Tooltip[this.options.size()];
        this.active = true;
        this.clickSound = MekanismSounds.BEEP_ON;
        this.clickVolume = 1.0F;
    }

    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        if (!options.isEmpty()) {
            super.onClick(mouseX, mouseY, button);
            setDragging(true);
            setOpen(!isOpen || mouseY > getY() + 11);
        }
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        boolean wasDragging = isDragging();
        super.onRelease(mouseX, mouseY);
        if (wasDragging && isOpen && mouseY > getY() + 11) {
            int index = getHoveredIndex(mouseX, mouseY);
            if (index != -1 && index < options.size()) {
                handler.accept(options.get(index));
            }
            setOpen(false);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (isOpen && deltaY != 0 && visibleRows() < options.size()) {
            int maxOffset = options.size() - visibleRows();
            int next = Math.max(0, Math.min(maxOffset, scrollOffset + (deltaY > 0 ? -1 : 1)));
            if (next != scrollOffset) {
                scrollOffset = next;
                clearTooltip();
                cachedTooltipRect = null;
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderForeground(guiGraphics, mouseX, mouseY);
        drawOptionName(guiGraphics, curType.get(), 2, true);
        if (isOpen) {
            for (int i = 0; i < visibleRows(); i++) {
                int index = scrollOffset + i;
                if (index >= options.size()) {
                    break;
                }
                drawOptionName(guiGraphics, options.get(index), ELEMENT_HEIGHT + 1 + ROW_HEIGHT * i, false);
            }
        }
    }

    @Override
    public void drawBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.drawBackground(guiGraphics, mouseX, mouseY, partialTicks);
        renderBackgroundTexture(guiGraphics, getResource(), GuiInnerScreen.SCREEN_SIZE, GuiInnerScreen.SCREEN_SIZE);

        int localRow = getHoveredRow(mouseX, mouseY);
        if (localRow != -1) {
            GuiUtils.drawOutline(guiGraphics, relativeX + 1, relativeY + ELEMENT_HEIGHT + localRow * ROW_HEIGHT, width - 2, ROW_HEIGHT, screenTextColor());
        }

        if (isOpen && visibleRows() < options.size()) {
            int trackTop = relativeY + ELEMENT_HEIGHT + 1;
            int trackHeight = visibleRows() * ROW_HEIGHT - 2;
            int thumbHeight = Math.max(4, trackHeight * visibleRows() / options.size());
            int maxOffset = options.size() - visibleRows();
            int thumbY = trackTop + (trackHeight - thumbHeight) * scrollOffset / Math.max(1, maxOffset);
            guiGraphics.fill(relativeX + width - 2, thumbY, relativeX + width - 1, thumbY + thumbHeight, screenTextColor());
        }
    }

    @NotNull
    @Override
    protected ScreenRectangle getTooltipRectangle(int mouseX, int mouseY) {
        return cachedTooltipRect == null ? super.getTooltipRectangle(mouseX, mouseY) : cachedTooltipRect;
    }

    @Override
    public void updateTooltip(int mouseX, int mouseY) {
        int localRow = getHoveredRow(mouseX, mouseY);
        if (localRow != -1) {
            int index = scrollOffset + localRow;
            if (index < options.size()) {
                Tooltip text = cachedTooltips[index];
                if (text == null) {
                    text = TooltipUtils.create(tooltips.apply(options.get(index)));
                    cachedTooltips[index] = text;
                }
                cachedTooltipRect = new ScreenRectangle(getX() + 1, getY() + ELEMENT_HEIGHT + localRow * ROW_HEIGHT, width - 2, ROW_HEIGHT);
                setTooltip(text);
                return;
            }
        }
        clearTooltip();
        cachedTooltipRect = null;
    }

    private void drawOptionName(GuiGraphics guiGraphics, TYPE option, int y, boolean alwaysDisplayed) {
        drawScaledScrollingString(guiGraphics, shortNames.apply(option), 0, y, TextAlignment.LEFT, screenTextColor(), getWidth(), ROW_HEIGHT, 3, false, 0.8F,
              alwaysDisplayed ? getTimeOpened() : msOpened);
    }

    /**
     * @return Hovered row relative to the top of the (possibly scrolled) open list, or -1 if the mouse is not over the open list.
     */
    private int getHoveredRow(double mouseX, double mouseY) {
        if (isOpen && mouseX >= getX() && mouseX < getRight() && mouseY >= getY() + 11 && mouseY < getBottom()) {
            return Math.max(0, Math.min(visibleRows() - 1, (int) ((mouseY - getY() - 11) / ROW_HEIGHT)));
        }
        return -1;
    }

    private int getHoveredIndex(double mouseX, double mouseY) {
        int localRow = getHoveredRow(mouseX, mouseY);
        return localRow == -1 ? -1 : scrollOffset + localRow;
    }

    private int visibleRows() {
        return Math.min(maxVisibleRows, options.size());
    }

    public void setOpen(boolean open) {
        if (isOpen != open) {
            isOpen = open;
            if (open) {
                height += visibleRows() * ROW_HEIGHT + 1;
                int index = options.indexOf(curType.get());
                int maxOffset = options.size() - visibleRows();
                scrollOffset = Math.max(0, Math.min(maxOffset, index - (visibleRows() - 1) / 2));
                msOpened = Util.getMillis();
            } else {
                height = ELEMENT_HEIGHT;
                clearTooltip();
                cachedTooltipRect = null;
            }
        }
    }

    @Override
    public void syncFrom(GuiElement element) {
        super.syncFrom(element);
        GuiScrollDropdown<?> old = (GuiScrollDropdown<?>) element;
        if (old.isOpen) {
            isOpen = true;
            height += visibleRows() * ROW_HEIGHT + 1;
            msOpened = old.msOpened;
            scrollOffset = old.scrollOffset;
        }
    }
}