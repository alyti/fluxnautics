package dev.alyti.fluxnautics.content.blocks.stabilizer;

import dev.alyti.fluxnautics.api.content.redstone.IRedstoneControlMode;
import mekanism.api.text.EnumColor;
import net.minecraft.network.chat.Component;

public enum StabilizerRedstoneControl implements IRedstoneControlMode<StabilizerBlockEntity> {
    // TODO: Add controls :D
    NONE(EnumColor.DARK_GRAY, "None", "None") {
        @Override
        public void drive(StabilizerBlockEntity tile, int signal) {
        }
    },
    ;

    private final EnumColor color;
    private final Component text;
    private final Component shortText;

    StabilizerRedstoneControl(EnumColor color, String text, String shortText) {
        this.color = color;
        this.text = Component.literal(text);
        this.shortText = Component.literal(shortText);
    }

    @Override
    public EnumColor color() {
        return color;
    }

    @Override
    public Component text() {
        return text;
    }

    @Override
    public Component shortText() {
        return shortText;
    }

    private static double scale(int signal, double toMin, double toMax) {
        return toMin + (signal / 15.0) * (toMax - toMin);
    }
}
