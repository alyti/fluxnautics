package dev.alyti.fluxnautics.api.content.tooltip;


import com.mojang.blaze3d.vertex.PoseStack;
import dev.alyti.fluxnautics.client.util.UIRenderHelper;
import mekanism.common.lib.Color;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.joml.Math;

import java.util.ArrayList;
import java.util.List;

public class TooltipManager {
    private static final int padding = 4;
    private static float tooltipTransparency = 0f;

    static final Color colorBackground = Color.argb(0xaa000000);
    static final Color colorBorderTop  = Color.argb(0xff514151);
    static final Color colorBorderBot  = Color.argb(0xff664499);

    public static void renderSelected(GuiGraphics graphics, TileEntityMekanism tile) {
        Font font = Minecraft.getInstance().font;
        PoseStack stack = graphics.pose();
        List<Component> list = new ArrayList<>();
        list.add(Component.literal(tile.getBlockHolder().getRegisteredName()));
        if (tile instanceof IHoverTooltip tooltip) {
            tooltip.addLines(list);
        }

        int width = padding * 2;
        int height = padding + ((list.size()==1 ? 1 : list.size()-1)*(font.lineHeight+padding));
        for (Component component : list) {
            int w = font.width(component) + padding * 2;
            if (w > width)
                width = w;
        }

        float centerX = graphics.guiWidth()/2f + 10;
        float centerY = graphics.guiHeight()/2f - (height / 2f);
        UIRenderHelper.drawHoveringText(new ItemStack(tile.getBlockHolder().value()), graphics, list, (int) centerX, (int) centerY, graphics.guiWidth(), graphics.guiHeight(), (int) centerX / 3 * 2, colorBackground.argb(), colorBorderTop.argb(), colorBorderBot.argb(), font);
    }

    public static void guiFrame(boolean active) {
        if (active) {
            tooltipTransparency = Math.lerp(tooltipTransparency, 0.75f, 0.2f);
        } else {
            tooltipTransparency = Math.lerp(tooltipTransparency, 0f, 0.2f);
        }
    }
}
