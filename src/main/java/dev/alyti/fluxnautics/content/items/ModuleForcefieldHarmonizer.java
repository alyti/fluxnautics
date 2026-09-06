package dev.alyti.fluxnautics.content.items;

import dev.alyti.fluxnautics.Fluxnautics;
import mekanism.api.gear.ICustomModule;
import mekanism.api.gear.IModule;
import net.minecraft.resources.ResourceLocation;

public record ModuleForcefieldHarmonizer(Harmony harmony) implements ICustomModule<ModuleForcefieldHarmonizer> {
    public static final ResourceLocation HARMONY = Fluxnautics.id("harmony");


    public ModuleForcefieldHarmonizer(IModule<ModuleForcefieldHarmonizer> module) {
        this(module.<Harmony>getConfigOrThrow(HARMONY).get());
    }

    public class Harmony {

    }
}
