package dev.alyti.fluxnautics.registry;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.api.datamaps.ForcefieldAbsorption;
import dev.alyti.fluxnautics.api.datamaps.IFNDataMapTypes;
import mekanism.common.registration.impl.DataMapTypeRegister;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;


public class FNDataMapTypes implements IFNDataMapTypes {

    public static final DataMapTypeRegister REGISTER = new DataMapTypeRegister(Fluxnautics.MODID);

    private static final DataMapType<EntityType<?>, ForcefieldAbsorption> FORCEFIELD_ABSORPTION = REGISTER.registerSimple(ForcefieldAbsorption.ID, Registries.ENTITY_TYPE, ForcefieldAbsorption.CODEC);

    @Override
    public DataMapType<EntityType<?>, ForcefieldAbsorption> forcefieldAbsorption() {
        return FORCEFIELD_ABSORPTION;
    }

    @Nullable
    public <TYPE, DATA> DATA getData(RegistryAccess registryAccess, ResourceKey<? extends Registry<? extends TYPE>> registryName, Holder<TYPE> holder, DataMapType<TYPE, DATA> type) {
        if (holder.kind() == Holder.Kind.REFERENCE) {
            //Reference holders can query data map values
            return holder.getData(type);
        }
        Optional<Registry<TYPE>> registry = registryAccess.registry(registryName);
        //noinspection OptionalIsPresent - Capturing lambda
        if (registry.isPresent()) {
            return registry.get().wrapAsHolder(holder.value()).getData(type);
        }
        return null;
    }
}
