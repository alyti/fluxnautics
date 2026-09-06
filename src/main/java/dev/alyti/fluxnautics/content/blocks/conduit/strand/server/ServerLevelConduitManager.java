package dev.alyti.fluxnautics.content.blocks.conduit.strand.server;


import dev.alyti.fluxnautics.util.WorldAttached;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Contains and manages all ropes for a client level
 */
public class ServerLevelConduitManager {
    private static final WorldAttached<ServerLevelConduitManager> worldAttached = new WorldAttached<>(ServerLevelConduitManager::create);

    private final Level level;
    private final Map<UUID, ServerConduitStrand> ropeStrands = new Object2ObjectOpenHashMap<>();

    public ServerLevelConduitManager(final Level level) {
        this.level = level;
    }

    @Nullable
    public static ServerLevelConduitManager getOrCreate(final Level level) {
        return worldAttached.get(level);
    }

    private static ServerLevelConduitManager create(final LevelAccessor level) {
        if (!(level instanceof final ServerLevel serverLevel)) return null;
        return new ServerLevelConduitManager(serverLevel);
    }

    public void addStrand(final ServerConduitStrand strand) {
        this.ropeStrands.put(strand.getUUID(), strand);
    }

    @Nullable
    public ServerConduitStrand getStrand(final UUID uuid) {
        return this.ropeStrands.get(uuid);
    }

    public void removeStrand(final UUID uuid) {
        this.ropeStrands.remove(uuid);
    }

    public Collection<ServerConduitStrand> getAllStrands() {
        return this.ropeStrands.values();
    }

    public void physicsTick(final SubLevelPhysicsSystem physicsSystem, final double timeStep) {
        final ServerLevel level = physicsSystem.getLevel();

        for (final ServerConduitStrand strand : this.ropeStrands.values()) {
            if (!strand.isActive()) {
                continue;
            }

            if (!strand.isOwnerLoaded(level) || !strand.areAttachmentsLoaded(level)) {
                physicsSystem.removeObject(strand);
                continue;
            }

            strand.prePhysicsTick(physicsSystem, level, timeStep);
        }
    }
}
