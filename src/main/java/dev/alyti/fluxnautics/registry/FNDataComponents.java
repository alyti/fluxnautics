package dev.alyti.fluxnautics.registry;

import com.mojang.serialization.Codec;
import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.content.blocks.assembly_terminal.BlueprintConfig;
import dev.alyti.fluxnautics.content.blocks.flux_tether.FluxTetherConfig;
import dev.alyti.fluxnautics.content.blocks.forcefield.ForcefieldBubbleEmitterConfig;
import dev.alyti.fluxnautics.content.blocks.inertia_dampener.InertiaDampenerConfig;
import dev.alyti.fluxnautics.content.blocks.stabilizer.StabilizerConfig;
import dev.alyti.fluxnautics.content.items.drone.MekaRobitItem;
import mekanism.common.registration.MekanismDeferredHolder;
import mekanism.common.registration.impl.DataComponentDeferredRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;

import java.util.UUID;

public class FNDataComponents {

    private FNDataComponents() {
    }

    public static final DataComponentDeferredRegister DATA_COMPONENTS = new DataComponentDeferredRegister(Fluxnautics.MODID);

    public static final MekanismDeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> IS_GLOBAL = DATA_COMPONENTS.registerBoolean("is_global");
    public static final MekanismDeferredHolder<DataComponentType<?>, DataComponentType<Double>> IMPULSE_MULT = registerDouble("impulse_mult");
    public static final MekanismDeferredHolder<DataComponentType<?>, DataComponentType<Double>> IMPULSE_VEC_X = registerDouble("impulse_vec_x");
    public static final MekanismDeferredHolder<DataComponentType<?>, DataComponentType<Double>> IMPULSE_VEC_Y = registerDouble("impulse_vec_y");
    public static final MekanismDeferredHolder<DataComponentType<?>, DataComponentType<Double>> IMPULSE_VEC_Z = registerDouble("impulse_vec_z");

    public static final MekanismDeferredHolder<DataComponentType<?>, DataComponentType<Long>> COOLDOWN_TICKS = DATA_COMPONENTS.simple("cooldown_ticks", builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));
    public static final MekanismDeferredHolder<DataComponentType<?>, DataComponentType<ForcefieldBubbleEmitterConfig>> EMITTER_CONFIG = DATA_COMPONENTS.simple("emitter_config", builder -> builder.persistent(ForcefieldBubbleEmitterConfig.CODEC).networkSynchronized(ForcefieldBubbleEmitterConfig.STREAM_CODEC));
    public static final MekanismDeferredHolder<DataComponentType<?>, DataComponentType<BlueprintConfig>> BLUEPRINT_CONFIG = DATA_COMPONENTS.simple("blueprint_config", builder -> builder.persistent(BlueprintConfig.CODEC).networkSynchronized(BlueprintConfig.STREAM_CODEC));
    public static final MekanismDeferredHolder<DataComponentType<?>, DataComponentType<FluxTetherConfig>> FLUX_TETHER_CONFIG = DATA_COMPONENTS.simple("flux_tether_config", builder -> builder.persistent(FluxTetherConfig.CODEC).networkSynchronized(FluxTetherConfig.STREAM_CODEC));
    public static final MekanismDeferredHolder<DataComponentType<?>, DataComponentType<InertiaDampenerConfig>> INERTIA_DAMPENER_CONFIG = DATA_COMPONENTS.simple("inertia_dampener_config", builder -> builder.persistent(InertiaDampenerConfig.CODEC).networkSynchronized(InertiaDampenerConfig.STREAM_CODEC));
    public static final MekanismDeferredHolder<DataComponentType<?>, DataComponentType<StabilizerConfig>> STABILIZER_CONFIG = DATA_COMPONENTS.simple("stabilizer_config", builder -> builder.persistent(StabilizerConfig.CODEC).networkSynchronized(StabilizerConfig.STREAM_CODEC));

    public static final MekanismDeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> CONDUIT_FIRST_CONNECTION = registerBlockPos("conduit_first_connection");
    public static final MekanismDeferredHolder<DataComponentType<?>, DataComponentType<UUID>> MEKA_ROBIT_BOUND_ENTITY = registerUUID("meka_robit_bound_entity");

    public static final MekanismDeferredHolder<DataComponentType<?>, DataComponentType<MekaRobitItem.DroneMode>> DRONE_MODE = DATA_COMPONENTS.simple("drone_mode",
        builder -> builder.persistent(MekaRobitItem.DroneMode.CODEC)
            .networkSynchronized(MekaRobitItem.DroneMode.STREAM_CODEC)
    );


    public static MekanismDeferredHolder<DataComponentType<?>, DataComponentType<Double>> registerDouble(String name) {
        return DATA_COMPONENTS.simple(name, builder -> builder.persistent(Codec.DOUBLE)
            .networkSynchronized(ByteBufCodecs.DOUBLE));
    }

    public static MekanismDeferredHolder<DataComponentType<?>, DataComponentType<UUID>> registerUUID(String name) {
        return DATA_COMPONENTS.simple(name, builder -> builder.persistent(UUIDUtil.CODEC)
            .networkSynchronized(UUIDUtil.STREAM_CODEC));
    }

    public static MekanismDeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> registerBlockPos(String name) {
        return DATA_COMPONENTS.simple(name, builder -> builder.persistent(BlockPos.CODEC)
            .networkSynchronized(BlockPos.STREAM_CODEC));
    }
}
