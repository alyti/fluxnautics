package dev.alyti.fluxnautics.datagen.loot;

import dev.alyti.fluxnautics.datagen.loot.table.BaseBlockLootTables;
import dev.alyti.fluxnautics.registry.FNBlocks;
import net.minecraft.core.HolderLookup;

public class BlockLootTables extends BaseBlockLootTables {

    public BlockLootTables(HolderLookup.Provider provider) {
        super(provider);
    }

    @Override
    protected void generate() {
        dropSelfWithContents(FNBlocks.BLOCKS.getPrimaryEntries());
    }
}