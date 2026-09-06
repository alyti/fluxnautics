package dev.alyti.fluxnautics.api.datamaps;

import mekanism.api.MekanismAPI;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import org.jetbrains.annotations.Nullable;

public interface IFNDataMapTypes {
    IFNDataMapTypes INSTANCE = MekanismAPI.getService(IFNDataMapTypes.class);

    @Nullable
    <TYPE, DATA> DATA getData(RegistryAccess registryAccess, ResourceKey<? extends Registry<? extends TYPE>> registryName, Holder<TYPE> holder, DataMapType<TYPE, DATA> type);

    DataMapType<EntityType<?>, ForcefieldAbsorption> forcefieldAbsorption();

    @Nullable
    default ForcefieldAbsorption getForcefieldAbsorption(RegistryAccess registryAccess, Holder<EntityType<?>> holder) {
        return getData(registryAccess, Registries.ENTITY_TYPE, holder, forcefieldAbsorption());
    }
}
