package dev.alyti.fluxnautics.content.blocks.thruster;

import dev.alyti.fluxnautics.index.BoosterTier;
import mekanism.common.block.attribute.Attribute;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ThrusterArrayBoosterBlockEntity extends ThrusterArrayStructuralBlockEntity {

    private BoosterTier tier;

    public ThrusterArrayBoosterBlockEntity(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state);
    }

    public BoosterTier getTier() {
        return tier;
    }

    @Override
    protected void presetVariables() {
        super.presetVariables();
        tier = Attribute.getTier(getBlockHolder(), BoosterTier.class);
    }
}