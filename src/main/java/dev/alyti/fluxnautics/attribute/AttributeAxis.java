package dev.alyti.fluxnautics.attribute;

import mekanism.common.block.attribute.Attribute;
import net.minecraft.core.Direction;

public record AttributeAxis(Direction.Axis axis) implements Attribute {
}
