package dev.alyti.fluxnautics.registry;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.content.items.ModuleDecloaker;
import dev.alyti.fluxnautics.content.items.drone.ModuleQuantumPullUnit;
import dev.alyti.fluxnautics.content.items.manipulator.ModuleFieldManipulator;
import dev.alyti.fluxnautics.content.items.drone.ModuleSwarmUnit;
import mekanism.api.gear.config.ModuleEnumConfig;
import mekanism.common.registration.impl.ModuleDeferredRegister;
import mekanism.common.registration.impl.ModuleRegistryObject;

public class FNModules {
    private FNModules() {
    }

    public static final ModuleDeferredRegister MODULES = new ModuleDeferredRegister(Fluxnautics.MODID);

    public static final ModuleRegistryObject<ModuleDecloaker> DECLOAKER = MODULES.registerInstanced("decloaker",
        ModuleDecloaker::new, () -> FNItems.MODULE_DECLOAKER, builder -> builder.maxStackSize(1).handlesModeChange()
            .disabledByDefault());

    public static final ModuleRegistryObject<ModuleSwarmUnit> SWARM_UNIT = MODULES.register("swarm_unit",
        ModuleSwarmUnit::new, () -> FNItems.MODULE_SWARM_UNIT, builder -> builder.maxStackSize(8).handlesModeChange()
            .addInstalledCountConfig(
            installed -> ModuleEnumConfig.createBounded(ModuleSwarmUnit.SWARM_MODE, ModuleSwarmUnit.SwarmMode.values()[installed], installed + 1),
            installed -> ModuleEnumConfig.codec(ModuleSwarmUnit.SwarmMode.CODEC, ModuleSwarmUnit.SwarmMode.class, installed + 1),
            installed -> ModuleEnumConfig.streamCodec(ModuleSwarmUnit.SwarmMode.STREAM_CODEC, ModuleSwarmUnit.SwarmMode.class, installed + 1)
            ).addConfig(
                ModuleEnumConfig.create(ModuleSwarmUnit.SWARM_FORMATION, ModuleSwarmUnit.SwarmFormation.ROUND),
                ModuleEnumConfig.codec(ModuleSwarmUnit.SwarmFormation.CODEC),
                ModuleEnumConfig.streamCodec(ModuleSwarmUnit.SwarmFormation.STREAM_CODEC)
            )
    );

    public static final ModuleRegistryObject<ModuleQuantumPullUnit> QUANTUM_PULL_UNIT = MODULES.register("quantum_pull_unit",
        ModuleQuantumPullUnit::new, () -> FNItems.MODULE_QUANTUM_PULL_UNIT, builder -> builder.maxStackSize(4).handlesModeChange()
            .addInstalledCountConfig(
                installed -> ModuleEnumConfig.createBounded(ModuleQuantumPullUnit.RANGE, ModuleQuantumPullUnit.Range.LOW, installed + 1),
                installed -> ModuleEnumConfig.codec(ModuleQuantumPullUnit.Range.CODEC, ModuleQuantumPullUnit.Range.class, installed + 1),
                installed -> ModuleEnumConfig.streamCodec(ModuleQuantumPullUnit.Range.STREAM_CODEC, ModuleQuantumPullUnit.Range.class, installed + 1)
            )
    );

    public static final ModuleRegistryObject<ModuleFieldManipulator> FIELD_MANIPULATOR_UNIT = MODULES.registerInstanced("field_manipulator_unit",
        ModuleFieldManipulator::new, () -> FNItems.MODULE_FIELD_MANIPULATOR_UNIT, builder -> builder.maxStackSize(8).handlesModeChange()
            .disabledByDefault());
}
