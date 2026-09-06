package dev.alyti.fluxnautics.util;

import mekanism.api.gear.IModule;
import mekanism.api.gear.IModuleContainer;
import mekanism.api.gear.IModuleHelper;
import mekanism.api.gear.ModuleData;
import mekanism.common.content.gear.ModuleContainer;
import mekanism.common.content.gear.ModuleHelper;
import mekanism.common.util.StorageUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

public class FNModuleUtils {
    public static ItemStack maxModules(Holder<Item> itemStack, HolderLookup.Provider provider) {
        ItemStack stack = StorageUtils.getFilledEnergyVariant(itemStack);
        Set<ModuleData<?>> supportedModules = IModuleHelper.INSTANCE.getSupported(itemStack);

        for (ModuleData<?> moduleData : supportedModules) {
            ModuleContainer container = ModuleHelper.get().getModuleContainer(stack);
            if (container != null && container.canInstall(stack, Holder.direct(moduleData))) {
                container.addModule(provider, stack, Holder.direct(moduleData), moduleData.getMaxStackSize());
            }
        }

        return StorageUtils.getFilledEnergyVariant(stack);
    }
}
