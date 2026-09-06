package dev.alyti.fluxnautics.client.gui.element.button;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.alyti.fluxnautics.api.content.redstone.IRedstoneSideConfigurable;
import dev.alyti.fluxnautics.api.content.redstone.TileComponentRedstoneControl;
import dev.alyti.fluxnautics.client.gui.element.window.GuiRedstoneConfiguration;
import dev.alyti.fluxnautics.lang.FNLang;
import dev.alyti.fluxnautics.network.packets.PacketSetRedstoneMode;
import mekanism.api.RelativeSide;
import mekanism.api.text.EnumColor;
import mekanism.api.text.TextComponentUtil;
import mekanism.client.gui.GuiUtils;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.button.BasicColorButton;
import mekanism.client.gui.tooltip.TooltipUtils;
import mekanism.client.render.lib.ScrollIncrementer;
import mekanism.common.network.PacketUtils;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RedstoneSideButton<TILE extends TileEntityMekanism & IRedstoneSideConfigurable> extends BasicColorButton {

    private final GuiRedstoneConfiguration<TILE, ?> window;
    private final TILE tile;
    private final RelativeSide side;
    private final ScrollIncrementer scrollIncrementer = new ScrollIncrementer(true);
    private final ItemStack otherBlockItem;

    private List<Component> lastInfo = Collections.emptyList();
    @Nullable
    private Tooltip lastTooltip;

    public RedstoneSideButton(GuiRedstoneConfiguration<TILE, ?> window, IGuiWrapper gui, int x, int y, RelativeSide side, TILE tile) {
        super(gui, x, y, 22, () -> {
                TileComponentRedstoneControl<?, ?> config = tile.getRedstoneConfig();
                if (!config.isSideConfigurable(side)) {
                    return EnumColor.GRAY;
                }
                return config.getMode(side).color();
            }, (element, mouseX, mouseY) -> ((RedstoneSideButton<?>) element).cycle(!Screen.hasShiftDown()),
            (element, mouseX, mouseY) -> ((RedstoneSideButton<?>) element).cycle(false));
        this.window = window;
        this.tile = tile;
        this.side = side;
        Level tileWorld = tile.getLevel();
        if (tileWorld != null) {
            Direction globalSide = side.getDirection(tile.getDirection());
            BlockPos otherBlockPos = tile.getBlockPos().relative(globalSide);
            BlockState blockOnSide = tileWorld.getBlockState(otherBlockPos);
            if (!blockOnSide.isAir()) {
                otherBlockItem = blockOnSide.getCloneItemStack(new BlockHitResult(otherBlockPos.getCenter().relative(globalSide.getOpposite(), 0.5), globalSide.getOpposite(), otherBlockPos, false), tileWorld, otherBlockPos, Minecraft.getInstance().player);
            } else {
                otherBlockItem = ItemStack.EMPTY;
            }
        } else {
            otherBlockItem = ItemStack.EMPTY;
        }
    }

    private boolean cycle(boolean forward) {
        if (!tile.getRedstoneConfig().isSideConfigurable(side) || tile.getRedstoneConfig().isDigitalOverride()) {
            return false;
        }
        window.select(side);
        return PacketUtils.sendToServer(PacketSetRedstoneMode.cycle(tile.getBlockPos(), side, forward));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (isMouseOver(mouseX, mouseY) && deltaY != 0) {
            int shift = scrollIncrementer.scroll(deltaY);
            if (shift != 0) {
                boolean forward = shift > 0;
                for (int i = 0; i < Math.abs(shift); i++) {
                    cycle(forward);
                }
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public void drawBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.drawBackground(guiGraphics, mouseX, mouseY, partialTicks);
        if (!otherBlockItem.isEmpty()) {
            GuiUtils.renderItem(guiGraphics, otherBlockItem, this.getRelativeX() + 3, this.getRelativeY() + 3, 1, font(), null, true);
        }
    }

    @Override
    public void updateTooltip(int mouseX, int mouseY) {
        List<Component> tooltipLines = new ArrayList<>(5);
        tooltipLines.add(TextComponentUtil.build(side));
        TileComponentRedstoneControl<?, ?> config = tile.getRedstoneConfig();
        if (!config.isSideConfigurable(side)) {
            tooltipLines.add(FNLang.REDSTONE_SIDE_UNAVAILABLE.translate());
        } else {
            tooltipLines.add(config.getMode(side).text());
            if (config.isInverted(side)) {
                tooltipLines.add(TextComponentUtil.build(EnumColor.ORANGE, FNLang.REDSTONE_INVERTED.translate()));
            }
        }
        if (!otherBlockItem.isEmpty()) {
            tooltipLines.add(otherBlockItem.getHoverName());
        }
        if (config.isDigitalOverride()) {
            tooltipLines.add(TextComponentUtil.build(EnumColor.RED, FNLang.REDSTONE_OVERRIDE_LOCKED.translate()));
        }
        tooltipLines.add(FNLang.REDSTONE_CYCLE_HINT.translate());
        if (!tooltipLines.equals(lastInfo)) {
            lastInfo = tooltipLines;
            lastTooltip = TooltipUtils.create(tooltipLines);
        }
        setTooltip(lastTooltip);
    }
}