package dev.alyti.fluxnautics.mixin_helper.mounted;

import mekanism.common.tile.TileEntityTeleporter;
import mekanism.common.tile.machine.TileEntityDimensionalStabilizer;
import net.minecraft.world.level.ChunkPos;

import java.util.*;

public final class ChunkTicketPolicy {
    private ChunkTicketPolicy() {
    }

    public static Set<ChunkPos> teleporterChunks(MountedMekanismContext context, TileEntityTeleporter teleporter) {
        return Collections.singleton(new ChunkPos(context.globalBlockPos()));
    }

    public static Set<ChunkPos> dimensionalStabilizerChunks(MountedMekanismContext context, TileEntityDimensionalStabilizer stabilizer) {
        ChunkPos center = new ChunkPos(context.globalBlockPos());
        return new LinkedHashSet<>(selectedDimensionalStabilizerGrid(stabilizer, center));
    }

    private static List<ChunkPos> selectedDimensionalStabilizerGrid(TileEntityDimensionalStabilizer stabilizer, ChunkPos center) {
        List<ChunkPos> chunks = new ArrayList<>();
        for (int x = -TileEntityDimensionalStabilizer.MAX_LOAD_RADIUS; x <= TileEntityDimensionalStabilizer.MAX_LOAD_RADIUS; x++) {
            for (int z = -TileEntityDimensionalStabilizer.MAX_LOAD_RADIUS; z <= TileEntityDimensionalStabilizer.MAX_LOAD_RADIUS; z++) {
                if (stabilizer.isChunkLoadingAt(x + TileEntityDimensionalStabilizer.MAX_LOAD_RADIUS, z + TileEntityDimensionalStabilizer.MAX_LOAD_RADIUS)) {
                    chunks.add(new ChunkPos(center.x + x, center.z + z));
                }
            }
        }
        sortByDistanceTo(chunks, center);
        return chunks;
    }

    private static Set<ChunkPos> trim(LinkedHashSet<ChunkPos> chunks, int maxSize) {
        if (chunks.size() <= maxSize) {
            return chunks;
        }
        LinkedHashSet<ChunkPos> trimmed = new LinkedHashSet<>();
        int added = 0;
        for (ChunkPos chunk : chunks) {
            trimmed.add(chunk);
            added++;
            if (added >= maxSize) {
                break;
            }
        }
        return trimmed;
    }

    private static void sortByDistanceTo(List<ChunkPos> chunks, ChunkPos center) {
        chunks.sort(Comparator
            .comparingInt((ChunkPos chunk) -> Math.abs(chunk.x - center.x) + Math.abs(chunk.z - center.z))
            .thenComparingInt(chunk -> Math.abs(chunk.x - center.x))
            .thenComparingInt(chunk -> Math.abs(chunk.z - center.z)));
    }

}