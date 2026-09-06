package dev.alyti.fluxnautics.content.items;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.FluxnauticsConfig;
import dev.alyti.fluxnautics.lang.FNLang;
import mekanism.api.gear.ICustomModule;
import mekanism.api.gear.IModule;
import mekanism.api.gear.IModuleContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

public class ModuleDecloaker implements ICustomModule<ModuleDecloaker> {
    public static final ResourceLocation HARMONY = Fluxnautics.id("harmony");

    @Override
    public void tickServer(@NonNull IModule<ModuleDecloaker> module, IModuleContainer moduleContainer, ItemStack stack, Player player) {
        module.useEnergy(player, stack, FluxnauticsConfig.common.cloakGeneratorDecloakerModuleUsage.get());
    }

    @Override
    public boolean canChangeModeWhenDisabled(IModule<ModuleDecloaker> module) {
        return true;
    }

    @Override
    public void changeMode(@NonNull IModule<ModuleDecloaker> module, Player player, IModuleContainer moduleContainer, ItemStack stack, int shift, boolean displayChangeMessage) {
        module.toggleEnabled(moduleContainer, stack, player, FNLang.MODULE_DECLOAKER.translate());
    }
}