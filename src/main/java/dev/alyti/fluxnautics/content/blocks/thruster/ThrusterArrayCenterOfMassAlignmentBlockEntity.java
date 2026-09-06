package dev.alyti.fluxnautics.content.blocks.thruster;

import dev.alyti.fluxnautics.attribute.AttributeAxis;
import mekanism.common.block.attribute.Attribute;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;

public class ThrusterArrayCenterOfMassAlignmentBlockEntity extends ThrusterArrayStructuralBlockEntity {
    private Direction.Axis axis;

    public ThrusterArrayCenterOfMassAlignmentBlockEntity(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state);
    }

    public Direction.Axis getAxis() {
        return axis;
    }

    @Override
    protected void presetVariables() {
        super.presetVariables();
        axis = Objects.requireNonNull(Attribute.get(getBlockHolder(), AttributeAxis.class)).axis();
    }
}
