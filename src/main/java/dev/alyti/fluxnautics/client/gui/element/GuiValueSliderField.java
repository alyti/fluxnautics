package dev.alyti.fluxnautics.client.gui.element;

import java.util.Collections;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mojang.blaze3d.vertex.PoseStack;
import mekanism.client.gui.GuiUtils;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class GuiValueSliderField extends GuiInnerScreen {

    private static final ResourceLocation SLIDER = MekanismUtils.getResource(ResourceType.GUI, "smooth_slider.png");
    private static final Pattern NUMBER = Pattern.compile("-?\\d+(?:\\.\\d+)?");

    public static final int SCREEN_ROW = 14;
    public static final int SLIDER_ROW = 12;
    private static final int TRACK_COLOR = 0xFF555555;
    private static final float TEXT_SCALE = 0.8F;

    private final DoubleSupplier valueSupplier;
    private final DoubleConsumer setter;
    private final Component label;
    private final Function<Double, String> valueFormat;
    private final DoubleSupplier minSupplier;
    private final DoubleSupplier maxSupplier;
    private final EditBox editBox;
    private boolean editing;
    private boolean updatingText;
    private boolean commitPending;

    public GuiValueSliderField(IGuiWrapper gui, int x, int y, int width, String label, double min, double max,
                               DoubleSupplier valueSupplier, DoubleConsumer setter, Function<Double, String> valueFormat) {
        this(gui, x, y, width, Component.literal(label), min, max, valueSupplier, setter, valueFormat);
    }

    public GuiValueSliderField(IGuiWrapper gui, int x, int y, int width, Component label, double min, double max,
                               DoubleSupplier valueSupplier, DoubleConsumer setter, Function<Double, String> valueFormat) {
        this(gui, x, y, width, label, () -> min, () -> max, valueSupplier, setter, valueFormat);
    }

    public GuiValueSliderField(IGuiWrapper gui, int x, int y, int width, String label, DoubleSupplier minSupplier,
                               DoubleSupplier maxSupplier, DoubleSupplier valueSupplier, DoubleConsumer setter,
                               Function<Double, String> valueFormat) {
        this(gui, x, y, width, Component.literal(label), minSupplier, maxSupplier, valueSupplier, setter, valueFormat);
    }

    public GuiValueSliderField(IGuiWrapper gui, int x, int y, int width, Component label, DoubleSupplier minSupplier,
                               DoubleSupplier maxSupplier, DoubleSupplier valueSupplier, DoubleConsumer setter,
                               Function<Double, String> valueFormat) {
        super(gui, x, y, width, SCREEN_ROW + SLIDER_ROW);
        this.label = label;
        this.minSupplier = minSupplier;
        this.maxSupplier = maxSupplier;
        this.valueSupplier = valueSupplier;
        this.setter = setter;
        this.valueFormat = valueFormat;
        padding(3).spacing(0).textScale(TEXT_SCALE);

        editBox = new EditBox(font(), getX() + 2, getY() + 1, Math.round((width - 4) / TEXT_SCALE), SCREEN_ROW - 2, Component.empty());
        editBox.setBordered(false);
        editBox.setMaxLength(16);
        editBox.setTextColor(screenTextColor());
        editBox.setTextColorUneditable(screenTextColor());
        editBox.setVisible(false);
        editBox.setResponder(s -> onTextEdited());
        tooltip(() -> List.of(Component.literal("Click to type a precise value")));
    }

    private double lowerBound() {
        return Math.min(minSupplier.getAsDouble(), maxSupplier.getAsDouble());
    }

    private double upperBound() {
        return Math.max(minSupplier.getAsDouble(), maxSupplier.getAsDouble());
    }

    private double toSlider(double value) {
        double span = upperBound() - lowerBound();
        return span <= 0.0 ? 0.0 : Mth.clamp((value - lowerBound()) / span, 0.0, 1.0);
    }

    private double fromSlider(double slider) {
        return Mth.clamp(lowerBound() + (upperBound() - lowerBound()) * slider, lowerBound(), upperBound());
    }

    public boolean isEditing() {
        return editing;
    }

    public double getValue() {
        return valueSupplier.getAsDouble();
    }

    private void updateEditText(double value) {
        updatingText = true;
        editBox.setValue(valueFormat.apply(value));
        updatingText = false;
    }

    private void onTextEdited() {
        if (updatingText) {
            return;
        }
        Double parsed = parseLeadingNumber(editBox.getValue());
        if (parsed != null) {
            setter.accept(Mth.clamp(parsed, lowerBound(), upperBound()));
        }
    }

    private static Double parseLeadingNumber(String text) {
        Matcher matcher = NUMBER.matcher(text);
        return matcher.find() ? Double.parseDouble(matcher.group()) : null;
    }

    private void beginEdit(double mouseX, double mouseY, int button) {
        if (!editing) {
            editing = true;
            updateEditText(getValue());
        }
        editBox.setVisible(true);
        editBox.setFocused(true);
        double scaledX = mouseX;
        if (TEXT_SCALE != 1.0F && scaledX > editBox.getX()) {
            scaledX = editBox.getX() + (scaledX - editBox.getX()) / TEXT_SCALE;
        }
        editBox.mouseClicked(scaledX, mouseY, button);
    }

    public void commitEdit() {
        if (!editing) {
            return;
        }
        editing = false;
        editBox.setFocused(false);
        editBox.setVisible(false);
    }

    @Override
    protected List<Component> getRenderStrings() {
        if (editing) {
            return Collections.emptyList();
        }
        return List.of(label.copy()
            .append(": ")
            .append(Component.literal(valueFormat.apply(getValue())).withStyle(ChatFormatting.AQUA)));
    }

    @Override
    public void renderBackgroundOverlay(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderBackgroundOverlay(guiGraphics, mouseX, mouseY);
        GuiUtils.fill(guiGraphics, relativeX + 2, relativeY + SCREEN_ROW + 3, width - 4, 6, TRACK_COLOR);
        int posX = (int) (toSlider(getValue()) * (width - 6));
        guiGraphics.blit(SLIDER, relativeX + posX, relativeY + SCREEN_ROW, 0, 0, 7, 12, 12, 12);
        if (commitPending) {
            commitPending = false;
            commitEdit();                  // focus loss survived a frame -> it was real
        }
    }

    @Override
    public void drawBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.drawBackground(guiGraphics, mouseX, mouseY, partialTicks);
        if (editing) {
            PoseStack pose = guiGraphics.pose();
            pose.pushPose();
            pose.translate(-getGuiLeft(), -getGuiTop(), 0);
            float reverse = (1 - TEXT_SCALE) / TEXT_SCALE;
            pose.scale(TEXT_SCALE, TEXT_SCALE, TEXT_SCALE);
            pose.translate(editBox.getX() * reverse, (editBox.getY() + 4) * reverse, 0);
            editBox.render(guiGraphics, mouseX, mouseY, partialTicks);
            pose.popPose();
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        super.onClick(mouseX, mouseY, button);
        if (mouseY >= getY() + SCREEN_ROW) {
            dragTo(mouseX);
            setDragging(true);
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            beginEdit(mouseX, mouseY, button);
        }
    }

    @Override
    public void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        super.onDrag(mouseX, mouseY, deltaX, deltaY);
        if (isDragging() && mouseY >= getY() + SCREEN_ROW) {
            dragTo(mouseX);
        }
    }

    private void dragTo(double mouseX) {
        double value = fromSlider(Mth.clamp((mouseX - getX() - 2) / (width - 6), 0.0, 1.0));
        setter.accept(value);
        if (editing) {
            updateEditText(value);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editing) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) {
                commitEdit();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN) {
                double shift = keyCode == GLFW.GLFW_KEY_UP ? 0.05 : -0.05;
                double value = fromSlider(Mth.clamp(toSlider(getValue()) + shift, 0.0, 1.0));
                setter.accept(value);
                updateEditText(value);
                return true;
            }
            return editBox.keyPressed(keyCode, scanCode, modifiers);
        }
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (!isFocused()) {
            return false;
        }
        double shift;
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_UP) {
            shift = -0.01;
        } else if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_DOWN) {
            shift = 0.01;
        } else {
            return false;
        }
        setter.accept(fromSlider(Mth.clamp(toSlider(getValue()) + shift, 0.0, 1.0)));
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (super.mouseScrolled(mouseX, mouseY, deltaX, deltaY)) {
            return true;
        }
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        double shift = Mth.clamp(-deltaY, -1.0, 1.0) * 0.05;
        double value = fromSlider(Mth.clamp(toSlider(getValue()) + shift, 0.0, 1.0));
        if (!Mth.equal(value, getValue())) {
            setter.accept(value);
            if (editing) {
                updateEditText(value);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char c, int keyCode) {
        if (editing) {
            if (Character.isDigit(c) || c == '.' || c == '-') {
                return editBox.charTyped(c, keyCode);
            }
            if (c == ',') {
                return editBox.charTyped('.', keyCode);
            }
            return false;
        }
        return super.charTyped(c, keyCode);
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused && editing) {
            commitPending = true;      // maybe just the re-focus cycle; resolve next frame
        } else if (focused) {
            commitPending = false;     // same-dispatch refocus -> harmless, keep editing
        }
    }

    @Override
    public void resize(int prevLeft, int prevTop, int left, int top) {
        super.resize(prevLeft, prevTop, left, top);
        editBox.setPosition(editBox.getX() - prevLeft + left, editBox.getY() - prevTop + top);
    }
}