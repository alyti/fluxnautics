package dev.alyti.fluxnautics.content.blocks.inertia_dampener;

import dev.alyti.fluxnautics.api.content.redstone.IRedstoneControlMode;
import mekanism.api.text.EnumColor;
import net.minecraft.network.chat.Component;

public enum InertiaDampenerRedstoneControl implements IRedstoneControlMode<InertiaDampenerBlockEntity> {
    NONE(EnumColor.DARK_GRAY, "None", "None") {
        @Override
        public void drive(InertiaDampenerBlockEntity tile, int signal) {
        }
    },

    LINEAR_STRENGTH(EnumColor.BRIGHT_GREEN, "Linear Strength", "Lin Str") {
        @Override
        public void drive(InertiaDampenerBlockEntity tile, int signal) {
            tile.setLinearStrength(scale(signal, 0.0, 1.0));
        }

        @Override
        public boolean drivesAtZeroSignal() {
            return true;
        }
    },

    LINEAR_DAMPEN(EnumColor.ORANGE, "Linear Dampen", "Lin Dmp") {
        @Override
        public void drive(InertiaDampenerBlockEntity tile, int signal) {
        }

        @Override
        public void onRisingEdge(InertiaDampenerBlockEntity tile, int signal) {
            tile.setLinearMode(InertiaDampenerConfig.DampenerMode.DAMPEN);
        }
    },
    LINEAR_BRAKE(EnumColor.DARK_GREEN, "Linear Brake", "Lin Brk") {
        @Override
        public void drive(InertiaDampenerBlockEntity tile, int signal) {
        }

        @Override
        public void onRisingEdge(InertiaDampenerBlockEntity tile, int signal) {
            tile.setLinearMode(InertiaDampenerConfig.DampenerMode.BRAKE);
        }
    },
    LINEAR_RELEASE(EnumColor.GRAY, "Linear Release", "Lin Free") {
        @Override
        public void drive(InertiaDampenerBlockEntity tile, int signal) {
        }

        @Override
        public void onRisingEdge(InertiaDampenerBlockEntity tile, int signal) {
            tile.setLinearMode(InertiaDampenerConfig.DampenerMode.OFF);
        }
    },
    STEP_LINEAR_MODE(EnumColor.RED, "Step Linear Mode", "Step Lin") {
        @Override
        public void drive(InertiaDampenerBlockEntity tile, int signal) {
        }

        @Override
        public void onRisingEdge(InertiaDampenerBlockEntity tile, int signal) {
            tile.stepLinearMode();
        }
    },


    LINEAR_HOLD_POWERED(EnumColor.PURPLE, "Linear Brake While Powered", "Lin Hold") {
        @Override
        public void drive(InertiaDampenerBlockEntity tile, int signal) {
            tile.setLinearMode(signal > 0 ? signal == 15 ? InertiaDampenerConfig.DampenerMode.BRAKE : InertiaDampenerConfig.DampenerMode.DAMPEN : InertiaDampenerConfig.DampenerMode.OFF);
            tile.setLinearStrength(scale(signal, 0.0, 1.0));
        }

        @Override
        public boolean drivesAtZeroSignal() {
            return true;
        }
    },
    ;

    private final EnumColor color;
    private final Component text;
    private final Component shortText;

    InertiaDampenerRedstoneControl(EnumColor color, String text, String shortText) {
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
