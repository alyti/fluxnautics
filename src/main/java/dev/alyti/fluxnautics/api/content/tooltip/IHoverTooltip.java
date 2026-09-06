package dev.alyti.fluxnautics.api.content.tooltip;

import dev.alyti.fluxnautics.util.Pair;
import mekanism.common.lib.Color;
import net.minecraft.network.chat.Component;

import java.util.List;

public interface IHoverTooltip {
    void addLines(List<Component> list);

    default Color backgroundColor() {
        return Color.argb(0x99000000);
    }

    default Pair<Color, Color> borderGradient() {
        return Pair.of(Color.argb(0xFFFFFFFF), Color.argb(0xFFFFFFFF));
    }
}
