package dev.alyti.fluxnautics.client.gui.element;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.client.util.UIRenderHelper;
import mekanism.common.lib.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public enum GuiTextureElement {
    BLUEPRINT_ICON_FORCES("blueprint", 0, 48, 16, 16, 512, 256),
    BLUEPRINT_ICON_FORCES_SEPARATED("blueprint", 32, 32, 16, 16, 512, 256),
    BLUEPRINT_ICON_FORCES_MERGED("blueprint", 48, 32, 16, 16, 512, 256),
    BLUEPRINT_ICON_MASS("blueprint", 16, 48, 16, 16, 512, 256),
    BLUEPRINT_ICON_COM_TOGGLE("blueprint", 32, 48, 16, 16, 512, 256),
    BLUEPRINT_ICON_COM("blueprint", 48, 48, 16, 16, 512, 256),
    BLUEPRINT_ICON_COM_TINY("blueprint", 48, 0, 16, 16, 512, 256),
    BLUEPRINT_ICON_COM_ARROW("blueprint", 48, 16, 16, 16, 512, 256),
    BLUEPRINT_ICON_ARROW_IN_PAGE_SHADOW("blueprint", 0, 16, 16, 16, 512, 256),
    BLUEPRINT_ICON_ARROW_OUT_PAGE_SHADOW("blueprint", 16, 16, 16, 16, 512, 256),
    BLUEPRINT_ICON_ARROW_IN_PAGE("blueprint", 0, 32, 16, 16, 512, 256),
    BLUEPRINT_ICON_ARROW_OUT_PAGE("blueprint", 16, 32, 16, 16, 512, 256),
    BLUEPRINT_ICON_CONFIG("blueprint",  0, 48, 16, 16, 512, 256),
    BLUEPRINT_ICON_TAB_LEFT_BG("blueprint",  0, 64, 26, 26, 512, 256),
    BLUEPRINT_ICON_BOUNDING_BOX("blueprint",  0, 0, 16, 16, 512, 256),
    BLUEPRINT_ICON_GEAR("blueprint",  16, 0, 16, 16, 512, 256),
    LEVER_HANDLE("blueprint", 32, 64, 32, 16, 512, 256),
    ;


    @NotNull
    public final ResourceLocation location;

    public final int width, height;
    public final int startX, startY;
    public final int texWidth, texHeight;

    GuiTextureElement(final String location, final int width, final int height) {
        this(location, 0, 0, width, height);
    }

    GuiTextureElement(final int startX, final int startY) {
        this("icons", startX * 16, startY * 16, 16, 16);
    }

    GuiTextureElement(final String location, final int startX, final int startY, final int width, final int height) {
        this(Fluxnautics.MODID, location, startX, startY, width, height);
    }

    GuiTextureElement(final String location, final int startX, final int startY, final int width, final int height, final int texWidth, final int texHeight) {
        this(Fluxnautics.MODID, location, startX, startY, width, height, texWidth, texHeight);
    }

    GuiTextureElement(final String namespace, final String location, final int startX, final int startY, final int width, final int height) {
        this(namespace, location, startX, startY, width, height, 256, 256);
    }

    GuiTextureElement(final ResourceLocation location, final int startX, final int startY, final int width, final int height, final int texWidth, final int texHeight) {
        this.location = location;
        this.width = width;
        this.height = height;
        this.startX = startX;
        this.startY = startY;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
    }

    GuiTextureElement(final String namespace, final String location, final int startX, final int startY, final int width, final int height, final int texWidth, final int texHeight) {
        final ResourceLocation loc = ResourceLocation.tryBuild(namespace, "textures/gui/" + location + ".png");
        assert loc != null;

        this.location = loc;
        this.width = width;
        this.height = height;
        this.startX = startX;
        this.startY = startY;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
    }

    public void bind() {
        RenderSystem.setShaderTexture(0, this.location);
    }

    public void render(final GuiGraphics graphics, final int x, final int y) {
        graphics.blit(this.location, x, y, this.startX, this.startY, this.width, this.height, this.texWidth, this.texHeight);
    }

    public void render (final GuiGraphics graphics, final int x, final int y, final int width, final int height) {
        graphics.blit(this.location, x, y, this.startX, this.startY, width, height, this.texWidth, this.texHeight);
    }

    public void render(final GuiGraphics graphics, final int x, final int y, final Color c) {
        this.bind();
        UIRenderHelper.drawColoredTexture(graphics, c, x, y, 0, this.startX, this.startY, this.width, this.height, this.texWidth, this.texHeight);
    }

}