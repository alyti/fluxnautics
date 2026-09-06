package dev.alyti.fluxnautics.content.blocks.field;

import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.Machine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class FieldDriveBlock extends BlockTile<FieldDriveBlockEntity, Machine<FieldDriveBlockEntity>> {

    public FieldDriveBlock(Machine<FieldDriveBlockEntity> type) {
        super(type, properties -> properties/*.noOcclusion()*/.strength(20, 10f));
    }

    @Nullable
    @Override
    public FieldDriveBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FieldDriveBlockEntity(pos, state);
    }
}
