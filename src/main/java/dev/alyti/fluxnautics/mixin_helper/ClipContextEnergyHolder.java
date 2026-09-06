package dev.alyti.fluxnautics.mixin_helper;

public class ClipContextEnergyHolder {
    private static final ThreadLocal<Long> ENERGY = ThreadLocal.withInitial(() -> 0L);

    public static void setEnergy(long energy) {
        ENERGY.set(energy);
    }

    public static long getEnergy() {
        return ENERGY.get();
    }

    public static void clearEnergy() {
        ENERGY.remove();
    }
}