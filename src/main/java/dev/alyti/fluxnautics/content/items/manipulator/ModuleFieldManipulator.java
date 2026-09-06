package dev.alyti.fluxnautics.content.items.manipulator;

import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.gear.ICustomModule;

@NothingNullByDefault
public class ModuleFieldManipulator implements ICustomModule<ModuleFieldManipulator> {

    public static final long DRAG_TICK_COST    = 250L;
    public static final double DRAG_SIZE_MULT   = 60.0;
    public static final long THROW_BASE_COST   = 2_000L;
    public static final double THROW_SIZE_MULT  = 500.0;
    public static final double MAX_BOUNDED_SIZE = 48.0;
    public static final double THROW_SPEED      = 2.8;
    public static final double PICK_RANGE       = 32.0;

    public static long dragCost(double size) {
        return (long) (DRAG_TICK_COST + Math.min(size, MAX_BOUNDED_SIZE) * DRAG_SIZE_MULT);
    }

    public static long throwCost(double size) {
        return (long) (THROW_BASE_COST + Math.min(size, MAX_BOUNDED_SIZE) * THROW_SIZE_MULT);
    }
}