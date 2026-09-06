package dev.alyti.fluxnautics.content.blocks.conduit;

import dev.alyti.fluxnautics.index.BlockShapes;
import dev.alyti.fluxnautics.registry.FNBlockTypes;
import dev.ryanhcode.sable.api.block.BlockSubLevelCollisionShape;
import mekanism.common.block.attribute.AttributeStateFacing;
import mekanism.common.content.blocktype.BlockTypeTile;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ConduitConnectorBlock extends AbstractConduitHolderBlock<ConduitConnectorBlockEntity, BlockTypeTile<ConduitConnectorBlockEntity>> implements BlockSubLevelCollisionShape {
    public ConduitConnectorBlock() {
        super(FNBlockTypes.CONDUIT_CONNECTOR, properties -> properties.noOcclusion().mapColor(MapColor.COLOR_CYAN).strength(10f));
    }

    @Override
    public VoxelShape getSubLevelCollisionShape(final BlockGetter blockGetter, final BlockState state) {
        AttributeStateFacing attr = type.get(AttributeStateFacing.class);
        int index = attr == null ? 0 : (attr.getDirection(state).ordinal() - (attr.getFacingProperty() == BlockStateProperties.FACING ? 0 : 2));
        return BlockShapes.CONDUIT_CONNECTOR_COLLIDER[index];
    }
}
