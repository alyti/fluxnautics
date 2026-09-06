package dev.alyti.fluxnautics.datagen.registries;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.registry.FNDamageTypes;
import mekanism.common.registration.impl.MekanismDamageType;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;

import java.util.concurrent.CompletableFuture;

public class DatapackRegistryProvider extends BaseDatapackRegistryProvider {

    public DatapackRegistryProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, BUILDER, Fluxnautics.MODID);
    }

    private static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
        .add(Registries.DAMAGE_TYPE, context -> {
            for (MekanismDamageType damageType : FNDamageTypes.DAMAGE_TYPES.damageTypes()) {
                context.register(damageType.key(), damageType.toVanilla());
            }
        });
}
