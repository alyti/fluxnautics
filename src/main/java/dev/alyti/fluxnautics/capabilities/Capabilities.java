package dev.alyti.fluxnautics.capabilities;

import dev.alyti.fluxnautics.Fluxnautics;
import mekanism.api.lasers.ILaserDissipation;
import net.neoforged.neoforge.capabilities.EntityCapability;

public class Capabilities {
    public static final EntityCapability<ILaserDissipation, Void> LASER_DISSIPATION_ENTITY = EntityCapability.createVoid(Fluxnautics.id("laser_dissipation"), ILaserDissipation.class);
}
