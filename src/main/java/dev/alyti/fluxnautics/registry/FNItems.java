package dev.alyti.fluxnautics.registry;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.content.items.ConduitItem;
import dev.alyti.fluxnautics.content.items.drone.MekaRobitItem;
import mekanism.api.functions.ConstantPredicates;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.attachments.containers.energy.ComponentBackedNoClampEnergyContainer;
import mekanism.common.attachments.containers.energy.EnergyContainersBuilder;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import mekanism.common.config.MekanismConfig;
import mekanism.common.content.gear.shared.ModuleEnergyUnit;
import mekanism.common.item.ItemModule;
import mekanism.common.registration.impl.ItemDeferredRegister;
import mekanism.common.registration.impl.ItemRegistryObject;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class FNItems {
    public static final ItemDeferredRegister ITEMS = new ItemDeferredRegister(Fluxnautics.MODID);

    public static final ItemRegistryObject<Item> FORCEFIELD_MATRIX = ITEMS.register("forcefield_matrix", Rarity.EPIC);

    public static final ItemRegistryObject<ItemModule> MODULE_DECLOAKER = ITEMS.registerModule(FNModules.DECLOAKER, Rarity.RARE);
    public static final ItemRegistryObject<ItemModule> MODULE_SWARM_UNIT = ITEMS.registerModule(FNModules.SWARM_UNIT, Rarity.EPIC);
    public static final ItemRegistryObject<ItemModule> MODULE_QUANTUM_PULL_UNIT = ITEMS.registerModule(FNModules.QUANTUM_PULL_UNIT, Rarity.RARE);
    public static final ItemRegistryObject<ItemModule> MODULE_FIELD_MANIPULATOR_UNIT = ITEMS.registerModule(FNModules.FIELD_MANIPULATOR_UNIT, Rarity.EPIC);

    public static final ItemRegistryObject<ConduitItem> CONDUIT_CORE = ITEMS.registerItem("conduit_core", ConduitItem::new);
    public static final ItemRegistryObject<MekaRobitItem> MEKA_ROBIT = ITEMS.registerUnburnable("meka_robit", MekaRobitItem::new)
        .addAttachedContainerCapabilities(ContainerType.ENERGY, () -> EnergyContainersBuilder.builder()
            .addContainer((type, attachedTo, containerIndex) -> new ComponentBackedNoClampEnergyContainer(attachedTo, containerIndex, BasicEnergyContainer.manualOnly,
                ConstantPredicates.alwaysTrue(), () -> ModuleEnergyUnit.getChargeRate(attachedTo, MekanismConfig.gear.mekaToolBaseChargeRate),
                () -> ModuleEnergyUnit.getEnergyCapacity(attachedTo, MekanismConfig.gear.mekaToolBaseEnergyCapacity)))
            .build(), MekanismConfig.gear
        );



}