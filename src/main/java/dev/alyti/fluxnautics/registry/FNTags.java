package dev.alyti.fluxnautics.registry;

import dev.alyti.fluxnautics.Fluxnautics;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public class FNTags {

    public static class EntityTypes {
        private EntityTypes() {}

        public static final TagKey<EntityType<?>> FORCEFIELD_GLOBAL_BLOCK = tag("forcefield_global_block");
        public static final TagKey<EntityType<?>> FORCEFIELD_GLOBAL_ALLOW = tag("forcefield_global_allow");

        private static TagKey<EntityType<?>> tag(String name) {
            return TagKey.create(Registries.ENTITY_TYPE, Fluxnautics.id(name));
        }
    }
}
