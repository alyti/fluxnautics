package dev.alyti.fluxnautics.registry;

import dev.alyti.fluxnautics.Fluxnautics;
import mekanism.common.registration.impl.DamageTypeRegister;
import mekanism.common.registration.impl.MekanismDamageType;

public class FNDamageTypes {

    public static final DamageTypeRegister DAMAGE_TYPES = new DamageTypeRegister(Fluxnautics.MODID);

    public static final MekanismDamageType TEMPORAL = DAMAGE_TYPES.create("temporal", 0.25F);
}
