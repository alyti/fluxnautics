package dev.alyti.fluxnautics.multiblock;


import com.mojang.logging.LogUtils;
import dev.alyti.fluxnautics.content.blocks.thruster.IonThrusterBlockEntity;
import dev.alyti.fluxnautics.content.blocks.thruster.ThrusterArrayBoosterBlockEntity;
import dev.alyti.fluxnautics.content.blocks.thruster.ThrusterArrayCenterOfMassAlignmentBlockEntity;
import dev.alyti.fluxnautics.content.blocks.thruster.ThrusterArraySubControllerBlockEntity;
import dev.alyti.fluxnautics.registry.FNBlockTypes;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import mekanism.common.content.blocktype.BlockType;
import mekanism.common.lib.math.voxel.IShape;
import mekanism.common.lib.math.voxel.VoxelCuboid;
import mekanism.common.lib.multiblock.FormationProtocol;
import mekanism.common.lib.multiblock.FormationProtocol.CasingType;
import mekanism.common.lib.multiblock.IValveHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.joml.Vector3d;

public class ThrusterArrayValidator extends DynamicStructureValidator<ThrusterArrayMultiblockData> {
    @Override
    protected CasingType getComponentType(BlockState state) {
        Block block = state.getBlock();
        if (BlockType.is(block, FNBlockTypes.THRUSTER_ARRAY_CONTROLLER)) {
            return CasingType.OTHER;
        } else if (BlockType.is(block, FNBlockTypes.THRUSTER_ARRAY_STRUCTURAL)) {
            return CasingType.FRAME;
        } else if (
            BlockType.is(block, FNBlockTypes.ION_THRUSTER) ||
                BlockType.is(block, FNBlockTypes.THRUSTER_ARRAY_SUBCONTROLLER) ||
                BlockType.is(block, FNBlockTypes.THRUSTER_ARRAY_BOOSTER_BASIC, FNBlockTypes.THRUSTER_ARRAY_BOOSTER_ADVANCED, FNBlockTypes.THRUSTER_ARRAY_BOOSTER_ELITE, FNBlockTypes.THRUSTER_ARRAY_BOOSTER_ULTIMATE, FNBlockTypes.THRUSTER_ARRAY_BOOSTER_CREATIVE) ||
                BlockType.is(block, FNBlockTypes.THRUSTER_ARRAY_ALIGNMENT_X, FNBlockTypes.THRUSTER_ARRAY_ALIGNMENT_Y, FNBlockTypes.THRUSTER_ARRAY_ALIGNMENT_Z)
        ) {
            return CasingType.VALVE;
        }
        return CasingType.INVALID;
    }

    @Override
    public FormationProtocol.FormationResult postcheck(ThrusterArrayMultiblockData mb, Long2ObjectMap<ChunkAccess> chunkMap) {
        mb.origin = structure.getController().getBlockPos();
        mb.thrusters.thrusterMap.clear();
        BlockPos.MutableBlockPos valvePos = new BlockPos.MutableBlockPos();
        BlockPos min = null;
        BlockPos max = null;
        for (BlockPos pos : mb.locations) {
            if (min == null) {
                min = pos;
                max = pos;
            } else {
                min = new BlockPos(Math.min(min.getX(), pos.getX()), Math.min(min.getY(), pos.getY()), Math.min(min.getZ(), pos.getZ()));
                max = new BlockPos(Math.max(max.getX(), pos.getX()), Math.max(max.getY(), pos.getY()), Math.max(max.getZ(), pos.getZ()));
            }
        }
        double totalThrustBudget = 0.0;
        Vector3d alignmentBudget = new Vector3d();
        for (IValveHandler.ValveData valve : mb.valves) {
            valvePos.set(valve.location);
            if (mb.internalLocations.contains(valvePos)) {
                BlockEntity be = this.world.getBlockEntity(valve.location);
                switch (be) {
                    case IonThrusterBlockEntity ion -> {
                        mb.addThruster(valve.location.subtract(mb.origin).immutable(), valve.side, ion.group);
                    }
                    case ThrusterArrayBoosterBlockEntity booster -> {
                        totalThrustBudget += booster.getTier().getThrustBudget();
                    }
                    case ThrusterArrayCenterOfMassAlignmentBlockEntity com -> {
                        switch (com.getAxis()) {
                            case X ->
                                alignmentBudget.x += 1;
                            case Y ->
                                alignmentBudget.y += 1;
                            case Z ->
                                alignmentBudget.z += 1;
                        }
                    }
                    case ThrusterArraySubControllerBlockEntity  subController -> {
                        // TODO: Anything we want from subcontrollers?
                    }
                    case null -> {
                        LogUtils.getLogger().warn("Failed to find a BlockEntity for {}", valve.location);
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + be);
                }
            }
        }
        if (min != null && max != null) {
            mb.setShape(new VoxelCuboid(min, max));
        }
        mb.thrusters.thrustBudget = totalThrustBudget;
        mb.thrusters.alignmentBudget = alignmentBudget;
        mb.renderLocation = mb.origin;
        mb.markDirty();
        return FormationProtocol.FormationResult.SUCCESS;
    }

    @Override
    public IShape getShape() {
        return structure.getMultiblockData() instanceof ThrusterArrayMultiblockData mb ? mb.getShape() : null;
    }
}