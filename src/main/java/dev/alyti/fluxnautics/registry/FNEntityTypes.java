package dev.alyti.fluxnautics.registry;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.content.entities.gear.EntityMekaRobit;
import mekanism.common.registration.MekanismDeferredHolder;
import mekanism.common.registration.impl.EntityTypeDeferredRegister;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class FNEntityTypes {

    public static final EntityTypeDeferredRegister ENTITY_TYPES = new EntityTypeDeferredRegister(Fluxnautics.MODID);

    public static final MekanismDeferredHolder<EntityType<?>, EntityType<EntityMekaRobit>> MEKA_ROBIT = ENTITY_TYPES.register("meka_robit", () -> EntityType.Builder.of(EntityMekaRobit::new, MobCategory.CREATURE)
        .sized(0.75F, 0.75F)
        .fireImmune()
        .clientTrackingRange(64)
        .eyeHeight(0.37F)
        .noSummon(), EntityMekaRobit::getDefaultAttributes);
}
