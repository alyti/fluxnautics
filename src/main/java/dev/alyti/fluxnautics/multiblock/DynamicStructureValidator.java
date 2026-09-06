package dev.alyti.fluxnautics.multiblock;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.lib.math.voxel.IShape;
import mekanism.common.lib.multiblock.*;
import mekanism.common.lib.multiblock.FormationProtocol.FormationResult;
import mekanism.common.util.EnumUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.slf4j.Logger;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

public abstract class DynamicStructureValidator<T extends MultiblockData> implements IStructureValidator<T> {

    protected Level world;
    protected MultiblockManager<T> manager;
    protected Structure structure;

    @Override
    public void init(Level world, MultiblockManager<T> manager, Structure structure) {
        this.world = world;
        this.manager = manager;
        this.structure = structure;
    }

    @Override
    public boolean precheck() {
        return true;
    }

    @FunctionalInterface
    public interface FormationLinker<NODE> {
        Set<BlockPos> links(Level level, Long2ObjectMap<ChunkAccess> chunkMap, BlockPos start, NODE node, BlockPos toCheck);
    }

    public static <NODE> int explore(Level level, Long2ObjectMap<ChunkAccess> chunkMap, BlockPos start, NODE node, FormationProtocol.FormationChecker<NODE> checker, FormationLinker<NODE> linker, int maxCount) {
        if (!checker.check(level, chunkMap, start, node, start)) {
            return 0;
        }

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        Queue<BlockPos> openSet = new LinkedList<>();
        Set<BlockPos> traversed = new ObjectOpenHashSet<>();
        openSet.add(start);
        traversed.add(start);
        while (!openSet.isEmpty()) {
            BlockPos ptr = openSet.poll();
            int traversedSize = traversed.size();
            if (traversedSize >= maxCount) {
                return traversedSize;
            }
            for (Direction side : EnumUtils.DIRECTIONS) {
                mutable.setWithOffset(ptr, side);
                if (!traversed.contains(mutable) && checker.check(level, chunkMap, start, node, mutable)) {
                    BlockPos offset = mutable.immutable();
                    openSet.add(offset);
                    traversed.add(offset);
                    Set<BlockPos> links = linker.links(level, chunkMap, start, node, mutable);
                    if (links != null && !links.isEmpty()) {
                        openSet.addAll(links);
                    }
                }
            }
        }
        return traversed.size();
    }

    @Override
    public FormationResult validate(FormationProtocol<T> ctx, Long2ObjectMap<ChunkAccess> chunkMap) {
        int count = explore(world, chunkMap, this.structure.getController().getBlockPos(), ctx, (level, chunks, start, n, pos) -> {
            BlockPos imm = pos.immutable();
            BlockState state = level.getBlockState(pos);
            FormationProtocol.CasingType type = getComponentType(state);
            if (type != FormationProtocol.CasingType.INVALID) {
                if (level.getBlockEntity(imm) instanceof IMultiblock<?> tile && tile.getManager() == manager) {
                    if (type == FormationProtocol.CasingType.VALVE) {
                        n.valves.add(new IValveHandler.ValveData(imm, Attribute.getFacing(state)));
                    }
                    n.locations.add(imm);
                    n.internalLocations.add(imm);
                    UUID uuid = tile.getCacheID();
                    if (uuid != null) {
                        MultiblockCache<T> cache = manager.getCache(uuid);
                        if (cache == null) {
                            tile.resetCache();
                        } else {
                            ctx.idsFound.put(uuid, cache);
                        }
                    }
                    return true;
                }
            }
            return false;
        }, (level, chunks, start, n, pos) -> null, 32 * 32 * 32);
        if (count >= 32 * 32 * 32)
            return FormationResult.fail(Component.literal("Multiblock too large (%s, max %s)".formatted(count, 32 * 32 * 32)));
        return FormationResult.SUCCESS;
    }

    protected abstract FormationProtocol.CasingType getComponentType(BlockState state);

    @Override
    public FormationProtocol.FormationResult postcheck(T structure, Long2ObjectMap<ChunkAccess> chunkMap) {
        return FormationResult.SUCCESS;
    }

    @Override
    public IShape getShape() {
        return null;
    }

}
