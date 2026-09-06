package dev.alyti.fluxnautics.index;

import mekanism.api.tier.BaseTier;
import mekanism.api.tier.ITier;
import mekanism.common.config.value.CachedDoubleValue;
import org.jetbrains.annotations.Nullable;

public enum BoosterTier implements ITier {
    BASIC(BaseTier.BASIC, 4.0),
    ADVANCED(BaseTier.ADVANCED, 8.0),
    ELITE(BaseTier.ELITE, 16.0),
    ULTIMATE(BaseTier.ULTIMATE, 32.0),
    CREATIVE(BaseTier.CREATIVE, 1024.0),
    ;

    private final BaseTier baseTier;
    private final double baseThrustBudget;
    @Nullable
    private CachedDoubleValue thrustBudgetReference;


    BoosterTier(BaseTier baseTier, double thrustBudget) {
        this.baseTier = baseTier;
        this.baseThrustBudget = thrustBudget;
    }

    @Override
    public BaseTier getBaseTier() {
        return baseTier;
    }


    public double getBaseThrustBudget() {
        return baseThrustBudget;
    }

    public double getThrustBudget() {
        return thrustBudgetReference == null ? getBaseThrustBudget() : thrustBudgetReference.getOrDefault();
    }

    /**
     * ONLY CALL THIS FROM CommonConfig. It is used to give the EnergyCubeTier a reference to the actual config value object
     */
    public void setConfigReference(CachedDoubleValue thrustBudgetReference) {
        this.thrustBudgetReference = thrustBudgetReference;
    }
}
