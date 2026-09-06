package dev.alyti.fluxnautics.client.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.alyti.fluxnautics.util.Couple;
import mekanism.common.lib.Color;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.extensions.IGuiGraphicsExtension;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class UIRenderHelper {

    public static final Couple<Color> COLOR_TEXT = Couple.create(
            Color.argb(0xff_eeeeee),
            Color.argb(0xff_a3a3a3)
    );
    public static final Couple<Color> COLOR_TEXT_DARKER = Couple.create(
            Color.argb(0xff_a3a3a3),
            Color.argb(0xff_808080)
    );
    public static final Couple<Color> COLOR_TEXT_ACCENT = Couple.create(
            Color.argb(0xff_ddeeff),
            Color.argb(0xff_a0b0c0)
    );
    public static final Couple<Color> COLOR_TEXT_STRONG_ACCENT = Couple.create(
            Color.argb(0xff_8ab6d6),
            Color.argb(0xff_6e92ab)
    );

    public static final Color COLOR_STREAK = Color.rgb(0x101010);

    /**
     * @param angle   angle in degrees, 0 means fading to the right
     * @param x       x-position of the starting edge middle point
     * @param y       y-position of the starting edge middle point
     * @param breadth total width of the streak
     * @param length  total length of the streak
     */
    public static void streak(GuiGraphics graphics, float angle, int x, int y, int breadth, int length) {
        streak(graphics, angle, x, y, breadth, length, COLOR_STREAK);
    }

    public static void streak(GuiGraphics graphics, float angle, int x, int y, int breadth, int length, Color c) {
        Color c1 = c.alpha(c.af()*0.625f);
        Color c2 = c.alpha(c.af()*0.5f);
        Color c3 = c.alpha(c.af()*0.0625f);
        Color c4 = c.alpha(0f);

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(x, y, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(angle - 90));

        streak(graphics, breadth / 2, length, c1, c2, c3, c4);

        poseStack.popPose();
    }

    private static void streak(GuiGraphics graphics, int width, int height, Color c1, Color c2, Color c3, Color c4) {
        double split1 = .5;
        double split2 = .75;
        graphics.fillGradient(-width, 0, width, (int) (split1 * height), c1.rgb(), c2.rgb());
        graphics.fillGradient(-width, (int) (split1 * height), width, (int) (split2 * height), c2.rgb(), c3.rgb());
        graphics.fillGradient(-width, (int) (split2 * height), width, height, c3.rgb(), c4.rgb());
    }

    /**
     * @see #angledGradient(GuiGraphics, float, int, int, int, float, float, Color, Color)
     */
    public static void angledGradient(GuiGraphics graphics, float angle, int x, int y, float breadth, float length, Couple<Color> c) {
        angledGradient(graphics, angle, x, y, 0, breadth, length, c);
    }

    /**
     * @see #angledGradient(GuiGraphics, float, int, int, int, float, float, Color, Color)
     */
    public static void angledGradient(GuiGraphics graphics, float angle, int x, int y, int z, float breadth, float length, Couple<Color> c) {
        angledGradient(graphics, angle, x, y, z, breadth, length, c.getFirst(), c.getSecond());
    }

    /**
     * @see #angledGradient(GuiGraphics, float, int, int, int, float, float, Color, Color)
     */
    public static void angledGradient(GuiGraphics graphics, float angle, int x, int y, float breadth, float length, Color color1, Color color2) {
        angledGradient(graphics, angle, x, y, 0, breadth, length, color1, color2);
    }

    /**
     * x and y specify the middle point of the starting edge
     *
     * @param angle      the angle of the gradient in degrees; 0° means from left to right
     * @param startColor the color at the starting edge
     * @param endColor   the color at the ending edge
     * @param breadth    the total width of the gradient
     */
    public static void angledGradient(GuiGraphics graphics, float angle, int x, int y, int z, float breadth, float length, Color startColor, Color endColor) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(angle - 90));

        float w = breadth / 2;
        //graphics.fillGradient(-w, 0, w, length, startColor.getRGB(), endColor.getRGB());
        drawGradientRect(poseStack.last().pose(), 0, -w, 0f, w, length, startColor, endColor);

        poseStack.popPose();
    }

    public static void drawGradientRect(Matrix4f mat, int zLevel, float left, float top, float right, float bottom, Color startColor, Color endColor) {
        RenderSystem.enableDepthTest();
        //RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.addVertex(mat, right, top, zLevel).setColor(startColor.r(), startColor.g(), startColor.b(), startColor.a());
        buffer.addVertex(mat, left, top, zLevel).setColor(startColor.r(), startColor.g(), startColor.b(), startColor.a());
        buffer.addVertex(mat, left, bottom, zLevel).setColor(endColor.r(), endColor.g(), endColor.b(), endColor.a());
        buffer.addVertex(mat, right, bottom, zLevel).setColor(endColor.r(), endColor.g(), endColor.b(), endColor.a());
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.disableBlend();
        //RenderSystem.enableTexture();
    }

    /**
     * centered on 0, 0
     *
     * @param arcAngle length of the sector arc
     */
    public static void drawRadialSector(GuiGraphics graphics, float innerRadius, float outerRadius, float startAngle, float arcAngle, Color innerColor, Color outerColor) {
        List<Point2D> innerPoints = getPointsForCircleArc(innerRadius, startAngle, arcAngle);
        List<Point2D> outerPoints = getPointsForCircleArc(outerRadius, startAngle, arcAngle);

        // if arcAngle > 0, start with inner. otherwise start with outer

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        Matrix4f pose = graphics.pose().last().pose();
        Matrix3f n = graphics.pose().last().normal();

        for (int i = 0; i < innerPoints.size(); i++) {
            Point2D point = outerPoints.get(i);
            //builder.addVertex(pose, (float) point.getX(), (float) point.getY(), 0).setColor(innerColor.getRGB()).normal(n, 1, 1, 0);
            builder.addVertex(pose, (float) point.getX(), (float) point.getY(), 0).setColor(outerColor.rgba());

            point = innerPoints.get(i);
            builder.addVertex(pose, (float) point.getX(), (float) point.getY(), 0).setColor(innerColor.rgba());
        }

        BufferUploader.drawWithShader(builder.buildOrThrow());

        RenderSystem.disableBlend();

    }

    private static List<Point2D> getPointsForCircleArc(float radius, float startAngle, float arcAngle) {
        int segmentCount = Math.abs(arcAngle) <= 90 ? 16 : 32;
        List<Point2D> points = new ArrayList<>(segmentCount);


        float theta = (Mth.DEG_TO_RAD * arcAngle) / (float) (segmentCount - 1);
        float t = Mth.DEG_TO_RAD * startAngle;

        for (int i = 0; i < segmentCount; i++) {
            points.add(new Point2D.Float(
                    (float) (radius * Math.cos(t)),
                    (float) (radius * Math.sin(t))
            ));

            t += theta;
        }

        return points;
    }


    //just like AbstractGui#drawTexture, but with a color at every vertex
    public static void drawColoredTexture(GuiGraphics graphics, Color c, int x, int y, int tex_left, int tex_top, int width, int height) {
        drawColoredTexture(graphics, c, x, y, 0, (float) tex_left, (float) tex_top, width, height, 256, 256);
    }

    public static void drawColoredTexture(GuiGraphics graphics, Color c, int x, int y, int z, float tex_left, float tex_top, int width, int height, int sheet_width, int sheet_height) {
        drawColoredTexture(graphics, c, x, x + width, y, y + height, z, width, height, tex_left, tex_top, sheet_width, sheet_height);
    }

    private static void drawColoredTexture(GuiGraphics graphics, Color c, int left, int right, int top, int bot, int z, int tex_width, int tex_height, float tex_left, float tex_top, int sheet_width, int sheet_height) {
        drawTexturedQuad(graphics.pose().last().pose(), c, left, right, top, bot, z, (tex_left + 0.0F) / (float) sheet_width, (tex_left + (float) tex_width) / (float) sheet_width, (tex_top + 0.0F) / (float) sheet_height, (tex_top + (float) tex_height) / (float) sheet_height);
    }

    private static void drawTexturedQuad(Matrix4f m, Color c, int left, int right, int top, int bot, int z, float u1, float u2, float v1, float v2) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        bufferbuilder.addVertex(m, (float) left, (float) bot, (float) z).setColor(c.r(), c.g(), c.b(), c.a()).setUv(u1, v2);
        bufferbuilder.addVertex(m, (float) right, (float) bot, (float) z).setColor(c.r(), c.g(), c.b(), c.a()).setUv(u2, v2);
        bufferbuilder.addVertex(m, (float) right, (float) top, (float) z).setColor(c.r(), c.g(), c.b(), c.a()).setUv(u2, v1);
        bufferbuilder.addVertex(m, (float) left, (float) top, (float) z).setColor(c.r(), c.g(), c.b(), c.a()).setUv(u1, v1);
        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
        RenderSystem.disableBlend();
    }

    public static void flipForGuiRender(PoseStack poseStack) {
        poseStack.mulPose(new Matrix4f().scaling(1, -1, 1));
    }

    @NotNull
    private static ItemStack cachedTooltipStack = ItemStack.EMPTY;

    public static void preItemToolTip(@NotNull ItemStack stack) {
        cachedTooltipStack = stack;
    }

    public static void postItemToolTip() {
        cachedTooltipStack = ItemStack.EMPTY;
    }

    public static void drawHoveringText(GuiGraphics graphics, List<? extends FormattedText> textLines, int mouseX,
                                        int mouseY, int screenWidth, int screenHeight, int maxTextWidth, Font font) {
        drawHoveringText(graphics, textLines, mouseX, mouseY, screenWidth, screenHeight, maxTextWidth,
                IGuiGraphicsExtension.DEFAULT_BACKGROUND_COLOR, IGuiGraphicsExtension.DEFAULT_BORDER_COLOR_START, IGuiGraphicsExtension.DEFAULT_BORDER_COLOR_END,
                font);
    }

    public static void drawHoveringText(GuiGraphics graphics, List<? extends FormattedText> textLines, int mouseX,
                                        int mouseY, int screenWidth, int screenHeight, int maxTextWidth, int backgroundColor, int borderColorStart,
                                        int borderColorEnd, Font font) {
        drawHoveringText(cachedTooltipStack, graphics, textLines, mouseX, mouseY, screenWidth, screenHeight, maxTextWidth,
                backgroundColor, borderColorStart, borderColorEnd, font);
    }

    public static void drawHoveringText(@NotNull final ItemStack stack, GuiGraphics graphics,
                                        List<? extends FormattedText> textLines, int mouseX, int mouseY, int screenWidth, int screenHeight,
                                        int maxTextWidth, Font font) {
        drawHoveringText(stack, graphics, textLines, mouseX, mouseY, screenWidth, screenHeight, maxTextWidth,
                IGuiGraphicsExtension.DEFAULT_BACKGROUND_COLOR, IGuiGraphicsExtension.DEFAULT_BORDER_COLOR_START, IGuiGraphicsExtension.DEFAULT_BORDER_COLOR_END,
                font);
    }

    public static void drawHoveringText(@NotNull final ItemStack stack, GuiGraphics graphics,
                                        List<? extends FormattedText> textLines, int mouseX, int mouseY, int screenWidth, int screenHeight,
                                        int maxTextWidth, int backgroundColor, int borderColorStart, int borderColorEnd, Font font) {
        if (textLines.isEmpty())
            return;

        List<ClientTooltipComponent> list = ClientHooks.gatherTooltipComponents(stack, textLines,
                stack.getTooltipImage(), mouseX, screenWidth, screenHeight, font);
        RenderTooltipEvent.Pre event =
                new RenderTooltipEvent.Pre(stack, graphics, mouseX, mouseY, screenWidth, screenHeight, font, list, null);
        if (NeoForge.EVENT_BUS.post(event).isCanceled())
            return;

        PoseStack pStack = graphics.pose();

        mouseX = event.getX();
        mouseY = event.getY();
        screenWidth = event.getScreenWidth();
        screenHeight = event.getScreenHeight();
        font = event.getFont();

        // RenderSystem.disableRescaleNormal();
        RenderSystem.disableDepthTest();
        int tooltipTextWidth = 0;

        for (FormattedText textLine : textLines) {
            int textLineWidth = font.width(textLine);
            if (textLineWidth > tooltipTextWidth)
                tooltipTextWidth = textLineWidth;
        }

        boolean needsWrap = false;

        int titleLinesCount = 1;
        int tooltipX = mouseX + 12;
        if (tooltipX + tooltipTextWidth + 4 > screenWidth) {
            tooltipX = mouseX - 16 - tooltipTextWidth;
            if (tooltipX < 4) // if the tooltip doesn't fit on the screen
            {
                if (mouseX > screenWidth / 2)
                    tooltipTextWidth = mouseX - 12 - 8;
                else
                    tooltipTextWidth = screenWidth - 16 - mouseX;
                needsWrap = true;
            }
        }

        if (maxTextWidth > 0 && tooltipTextWidth > maxTextWidth) {
            tooltipTextWidth = maxTextWidth;
            needsWrap = true;
        }

        if (needsWrap) {
            int wrappedTooltipWidth = 0;
            List<FormattedText> wrappedTextLines = new ArrayList<>();
            for (int i = 0; i < textLines.size(); i++) {
                FormattedText textLine = textLines.get(i);
                List<FormattedText> wrappedLine = font.getSplitter()
                        .splitLines(textLine, tooltipTextWidth, Style.EMPTY);
                if (i == 0)
                    titleLinesCount = wrappedLine.size();

                for (FormattedText line : wrappedLine) {
                    int lineWidth = font.width(line);
                    if (lineWidth > wrappedTooltipWidth)
                        wrappedTooltipWidth = lineWidth;
                    wrappedTextLines.add(line);
                }
            }
            tooltipTextWidth = wrappedTooltipWidth;
            textLines = wrappedTextLines;

            if (mouseX > screenWidth / 2)
                tooltipX = mouseX - 16 - tooltipTextWidth;
            else
                tooltipX = mouseX + 12;
        }

        int tooltipY = mouseY - 12;
        int tooltipHeight = 8;

        if (textLines.size() > 1) {
            tooltipHeight += (textLines.size() - 1) * 10;
            if (textLines.size() > titleLinesCount)
                tooltipHeight += 2; // gap between title lines and next lines
        }

        if (tooltipY < 4)
            tooltipY = 4;
        else if (tooltipY + tooltipHeight + 4 > screenHeight)
            tooltipY = screenHeight - tooltipHeight - 4;

        final int zLevel = 400;
        RenderTooltipEvent.Color colorEvent = new RenderTooltipEvent.Color(stack, graphics, tooltipX, tooltipY,
                font, backgroundColor, borderColorStart, borderColorEnd, list);
        NeoForge.EVENT_BUS.post(colorEvent);
        backgroundColor = colorEvent.getBackgroundStart();
        borderColorStart = colorEvent.getBorderStart();
        borderColorEnd = colorEvent.getBorderEnd();

        pStack.pushPose();
        Matrix4f mat = pStack.last()
                .pose();
        graphics.fillGradient(tooltipX - 3, tooltipY - 4, tooltipX + tooltipTextWidth + 3,
                tooltipY - 3, zLevel, backgroundColor, backgroundColor);
        graphics.fillGradient(tooltipX - 3, tooltipY + tooltipHeight + 3,
                tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 4, zLevel, backgroundColor, backgroundColor);
        graphics.fillGradient(tooltipX - 3, tooltipY - 3, tooltipX + tooltipTextWidth + 3,
                tooltipY + tooltipHeight + 3, zLevel, backgroundColor, backgroundColor);
        graphics.fillGradient(tooltipX - 4, tooltipY - 3, tooltipX - 3, tooltipY + tooltipHeight + 3,
                zLevel, backgroundColor, backgroundColor);
        graphics.fillGradient(tooltipX + tooltipTextWidth + 3, tooltipY - 3,
                tooltipX + tooltipTextWidth + 4, tooltipY + tooltipHeight + 3, zLevel, backgroundColor, backgroundColor);
        graphics.fillGradient(tooltipX - 3, tooltipY - 3 + 1, tooltipX - 3 + 1,
                tooltipY + tooltipHeight + 3 - 1, zLevel, borderColorStart, borderColorEnd);
        graphics.fillGradient(tooltipX + tooltipTextWidth + 2, tooltipY - 3 + 1,
                tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3 - 1, zLevel, borderColorStart, borderColorEnd);
        graphics.fillGradient(tooltipX - 3, tooltipY - 3, tooltipX + tooltipTextWidth + 3,
                tooltipY - 3 + 1, zLevel, borderColorStart, borderColorStart);
        graphics.fillGradient(tooltipX - 3, tooltipY + tooltipHeight + 2,
                tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3, zLevel, borderColorEnd, borderColorEnd);

        MultiBufferSource.BufferSource renderType = graphics.bufferSource();
        pStack.translate(0.0D, 0.0D, zLevel);

        for (int lineNumber = 0; lineNumber < list.size(); ++lineNumber) {
            ClientTooltipComponent line = list.get(lineNumber);

            if (line != null)
                line.renderText(font, tooltipX, tooltipY, mat, renderType);

            if (lineNumber + 1 == titleLinesCount)
                tooltipY += 2;

            tooltipY += line == null ? 10 : line.getHeight();
        }

        renderType.endBatch();
        pStack.popPose();

        RenderSystem.enableDepthTest();
    }
}