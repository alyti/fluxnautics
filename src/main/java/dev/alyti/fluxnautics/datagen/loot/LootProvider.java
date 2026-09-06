package dev.alyti.fluxnautics.datagen.loot;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LootProvider extends BaseLootProvider {

    public LootProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider) {
        super(output, List.of(
                new SubProviderEntry(BlockLootTables::new, LootContextParamSets.BLOCK)
        ), provider);
    }
}