package dev.alyti.fluxnautics.datagen.tag;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.registry.FNBlocks;
import dev.alyti.fluxnautics.registry.FNDamageTypes;
import dev.alyti.fluxnautics.registry.FNEntityTypes;
import mekanism.api.MekanismAPITags;
import mekanism.common.registries.MekanismBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class TagProvider extends BaseTagProvider {

    public TagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Fluxnautics.MODID, existingFileHelper);
    }

    @Override
    protected Collection<? extends DeferredHolder<Block, ?>> getAllBlocks() {
        return FNBlocks.BLOCKS.getPrimaryEntries();
    }

    @Override
    protected void registerTags(HolderLookup.Provider registries) {
        addDamageTypes();
        addEntities();
        addHarvestRequirements();
    }


    private void addEntities() {
        getBuilder(PVI_COMPAT).add(FNEntityTypes.MEKA_ROBIT);

        getBuilder(MekanismAPITags.Entities.RADIATION_IMMUNE).add(FNEntityTypes.MEKA_ROBIT);
        
        getBuilder(EntityTypeTags.POWDER_SNOW_WALKABLE_MOBS).add(FNEntityTypes.MEKA_ROBIT);
        getBuilder(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES).add(FNEntityTypes.MEKA_ROBIT);
        getBuilder(EntityTypeTags.CAN_BREATHE_UNDER_WATER).add(FNEntityTypes.MEKA_ROBIT);
        getBuilder(EntityTypeTags.IGNORES_POISON_AND_REGEN).add(FNEntityTypes.MEKA_ROBIT);
        getBuilder(EntityTypeTags.IMMUNE_TO_INFESTED).add(FNEntityTypes.MEKA_ROBIT);
        getBuilder(EntityTypeTags.IMMUNE_TO_OOZING).add(FNEntityTypes.MEKA_ROBIT);
        getBuilder(EntityTypeTags.FALL_DAMAGE_IMMUNE).add(FNEntityTypes.MEKA_ROBIT);
        getBuilder(EntityTypeTags.CAN_BREATHE_UNDER_WATER).add(FNEntityTypes.MEKA_ROBIT);
        getBuilder(EntityTypeTags.NOT_SCARY_FOR_PUFFERFISH).add(FNEntityTypes.MEKA_ROBIT);
        getBuilder(EntityTypeTags.ILLAGER_FRIENDS).add(FNEntityTypes.MEKA_ROBIT);
        getBuilder(EntityTypeTags.WITHER_FRIENDS).add(FNEntityTypes.MEKA_ROBIT);
    }

    private void addDamageTypes() {
        ResourceKey<DamageType> temporal = FNDamageTypes.TEMPORAL.key();
        getBuilder(DamageTypeTags.ALWAYS_HURTS_ENDER_DRAGONS).add(temporal);
        getBuilder(DamageTypeTags.ALWAYS_KILLS_ARMOR_STANDS).add(temporal);
        getBuilder(DamageTypeTags.AVOIDS_GUARDIAN_THORNS).add(temporal);
        getBuilder(DamageTypeTags.BYPASSES_COOLDOWN).add(temporal);
        getBuilder(DamageTypeTags.IS_PROJECTILE).add(temporal);
        getBuilder(DamageTypeTags.PANIC_CAUSES).add(temporal);
        getBuilder(MekanismAPITags.DamageTypes.MEKASUIT_ALWAYS_SUPPORTED).add(temporal);
    }


    private void addHarvestRequirements() {
        FNBlocks.BLOCKS.getPrimaryEntries().forEach((block) -> {
            addToHarvestTag(BlockTags.MINEABLE_WITH_PICKAXE, block);
        });
//        addToHarvestTag(BlockTags.MINEABLE_WITH_PICKAXE,
//            FNBlocks.ANTI_GRAV,
//            FNBlocks.DAEMON_PROTOCOL,
//            FNBlocks.ASSEMBLY_TERMINAL,
//            FNBlocks.STABILIZER,
//            FNBlocks.CLOAK_GENERATOR,
//            FNBlocks.CONDUIT_CONNECTOR,
//            FNBlocks.CONDUIT_PORT,
//            FNBlocks.FIELD_DRIVE,
//            FNBlocks.FORCEFIELD_EMITTER,
//            FNBlocks.FORCEFIELD_HARMONIZER,
//            FNBlocks.HOLO,
//
//
//        );
    }
}
