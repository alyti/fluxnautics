package dev.alyti.fluxnautics.content.blocks.flux_tether;

import dev.alyti.fluxnautics.api.content.redstone.IRedstoneControlMode;
import dev.alyti.fluxnautics.content.blocks.flux_tether.FluxTetherBlockEntity;
import dev.alyti.fluxnautics.content.blocks.flux_tether.FluxTetherConfig;
import mekanism.api.text.EnumColor;
import net.minecraft.network.chat.Component;

/**
 * The Flux Tether's redstone "transmission types". The first constant (NONE) is the default
 * assigned to all sides and is never sampled by the tile component.
 *
 * <p>Drivers only mutate {@code tile.config} through its setters (which clamp). Persistence,
 * physics wake-up and client sync are handled by the tile: tickServer() reports "changed" and
 * onUpdateServer reacts with setChanged + wakeUpAttached + sendUpdate.
 */
public enum FluxTetherRedstoneControl implements IRedstoneControlMode<FluxTetherBlockEntity> {
    NONE(EnumColor.DARK_GRAY, "None", "None") {
        @Override
        public void drive(FluxTetherBlockEntity tile, int signal) {
        }
    },
    LINEAR_TARGET(EnumColor.BRIGHT_GREEN, "Linear Target", "Lin Tgt") {
        @Override
        public void drive(FluxTetherBlockEntity tile, int signal) {
            tile.config.setLinearTarget(scale(signal, tile.getMinExtension(), tile.getMaxExtension()));
        }
    },
    LINEAR_FORCE(EnumColor.AQUA, "Linear Force Boost", "Lin Frc") {
        @Override
        public void drive(FluxTetherBlockEntity tile, int signal) {
            tile.config.setLinearForceBoost(scale(signal, 0.0, 4.0));
        }
    },
    LINEAR_SERVO_RATE(EnumColor.ORANGE, "Linear Servo Rate", "Lin Rt") {
        @Override
        public void drive(FluxTetherBlockEntity tile, int signal) {
            tile.config.setLinearServoRate(scale(signal, 0.0, 20.0));
        }
    },
    LINEAR_SERVO_LOOP(EnumColor.INDIGO, "Linear Servo Loop", "Lin Loop") {
        @Override
        public void drive(FluxTetherBlockEntity tile, int signal) {
            tile.config.setLinearServoLoop(scale(signal, 1.0, 64.0));
        }
    },
    ANGULAR_TARGET(EnumColor.YELLOW, "Angular Target", "Ang Tgt") {
        @Override
        public void drive(FluxTetherBlockEntity tile, int signal) {
            tile.config.setAngularTarget(scale(signal, 0, 180.0));
        }

        @Override
        public boolean drivesAtZeroSignal() {
            return true;
        }
    },
    ANGULAR_SERVO_RATE(EnumColor.DARK_AQUA, "Angular Servo Rate", "Ang Rt") {
        @Override
        public void drive(FluxTetherBlockEntity tile, int signal) {
            tile.config.setAngularServoRate(scale(signal, 0, 180.0));
        }

        @Override
        public boolean drivesAtZeroSignal() {
            return true;
        }
    },
    LINEAR_HOLD(EnumColor.DARK_GREEN, "Linear Hold", "Lin Hold") {
        @Override
        public void drive(FluxTetherBlockEntity tile, int signal) {
        }

        @Override
        public void onRisingEdge(FluxTetherBlockEntity tile, int signal) {
            tile.config.setLinearMode(FluxTetherConfig.Mode.STATIC);
        }
    },
    LINEAR_SERVO_ON(EnumColor.PURPLE, "Linear Servo On", "Lin Srv") {
        @Override
        public void drive(FluxTetherBlockEntity tile, int signal) {
        }

        @Override
        public void onRisingEdge(FluxTetherBlockEntity tile, int signal) {
            tile.config.setLinearMode(FluxTetherConfig.Mode.SERVO);
        }
    },
    LINEAR_RELEASE(EnumColor.GRAY, "Linear Release", "Lin Free") {
        @Override
        public void drive(FluxTetherBlockEntity tile, int signal) {
        }

        @Override
        public void onRisingEdge(FluxTetherBlockEntity tile, int signal) {
            tile.config.setLinearMode(FluxTetherConfig.Mode.FREE);
        }
    },
    ANGULAR_HOLD(EnumColor.PINK, "Angular Hold", "Ang Hold") {
        @Override
        public void drive(FluxTetherBlockEntity tile, int signal) {
        }

        @Override
        public void onRisingEdge(FluxTetherBlockEntity tile, int signal) {
            tile.config.setAngularMode(FluxTetherConfig.Mode.STATIC);
        }
    },
    ANGULAR_SERVO_ON(EnumColor.DARK_BLUE, "Angular Servo On", "Ang Srv") {
        @Override
        public void drive(FluxTetherBlockEntity tile, int signal) {
        }

        @Override
        public void onRisingEdge(FluxTetherBlockEntity tile, int signal) {
            tile.config.setAngularMode(FluxTetherConfig.Mode.SERVO);
        }
    },
    ANGULAR_RELEASE(EnumColor.BLACK, "Angular Release", "Ang Free") {
        @Override
        public void drive(FluxTetherBlockEntity tile, int signal) {
        }

        @Override
        public void onRisingEdge(FluxTetherBlockEntity tile, int signal) {
            tile.config.setAngularMode(FluxTetherConfig.Mode.FREE);
        }
    },
    STEP_LINEAR_MODE(EnumColor.RED, "Step Linear Mode", "Step Lin") {
        @Override
        public void drive(FluxTetherBlockEntity tile, int signal) {
        }

        @Override
        public void onRisingEdge(FluxTetherBlockEntity tile, int signal) {
            tile.config.setLinearMode(nextMode(tile.config.linearMode()));
        }
    },
    STEP_ANGULAR_MODE(EnumColor.DARK_RED, "Step Angular Mode", "Step Ang") {
        @Override
        public void drive(FluxTetherBlockEntity tile, int signal) {
        }

        @Override
        public void onRisingEdge(FluxTetherBlockEntity tile, int signal) {
            tile.config.setAngularMode(nextMode(tile.config.angularMode()));
        }
    },
    STEP_MODES(EnumColor.RED, "Step Both Modes", "Step Both") {
        @Override
        public void drive(FluxTetherBlockEntity tile, int signal) {
        }

        @Override
        public void onRisingEdge(FluxTetherBlockEntity tile, int signal) {
            tile.config.setLinearMode(nextMode(tile.config.linearMode()));
            tile.config.setAngularMode(nextMode(tile.config.angularMode()));
        }
    },
    ENGAGE_BOTH(EnumColor.DARK_GREEN, "Engage Both", "Engage") {
        @Override
        public void drive(FluxTetherBlockEntity tile, int signal) {
        }

        @Override
        public void onRisingEdge(FluxTetherBlockEntity tile, int signal) {
            tile.config.setLinearMode(FluxTetherConfig.Mode.STATIC);
            tile.config.setAngularMode(FluxTetherConfig.Mode.STATIC);
        }
    },
    RELEASE_BOTH(EnumColor.GRAY, "Release Both", "Release") {
        @Override
        public void drive(FluxTetherBlockEntity tile, int signal) {
        }

        @Override
        public void onRisingEdge(FluxTetherBlockEntity tile, int signal) {
            tile.config.setLinearMode(FluxTetherConfig.Mode.FREE);
            tile.config.setAngularMode(FluxTetherConfig.Mode.FREE);
        }
    },
    ;

    private final EnumColor color;
    private final Component text;
    private final Component shortText;

    FluxTetherRedstoneControl(EnumColor color, String text, String shortText) {
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

    private static FluxTetherConfig.Mode nextMode(FluxTetherConfig.Mode mode) {
        FluxTetherConfig.Mode[] values = FluxTetherConfig.Mode.values();
        return values[(mode.ordinal() + 1) % values.length];
    }
}