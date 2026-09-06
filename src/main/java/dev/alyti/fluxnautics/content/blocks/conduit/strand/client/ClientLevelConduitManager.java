package dev.alyti.fluxnautics.content.blocks.conduit.strand.client;

import dev.alyti.fluxnautics.util.WorldAttached;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

/**
 * Contains and manages all ropes for a client level
 */
public class ClientLevelConduitManager {

    private static final WorldAttached<ClientLevelConduitManager> worldAttached = new WorldAttached<>(ClientLevelConduitManager::create);

    private final Level level;
    private final Map<UUID, ClientConduitStrand> conduitStrands = new Object2ObjectOpenHashMap<>();

    public ClientLevelConduitManager(final Level level) {
        this.level = level;
    }

    public static ClientLevelConduitManager getOrCreate(final Level level) {
        return worldAttached.get(level);
    }

    private static ClientLevelConduitManager create(final LevelAccessor level) {
        if (!(level instanceof final ClientLevel clientLevel)) return null;
        return new ClientLevelConduitManager(clientLevel);
    }

    public void addStrand(final ClientConduitStrand strand) {
        this.conduitStrands.put(strand.getUuid(), strand);
    }

    @Nullable
    public ClientConduitStrand getStrand(final UUID uuid) {
        return this.conduitStrands.get(uuid);
    }

    public void removeStrand(final UUID uuid) {
        this.conduitStrands.remove(uuid);
    }

    public Iterable<ClientConduitStrand> getAllStrands() {
        return this.conduitStrands.values();
    }

    public void tickInterpolation(final double interpolationTick) {
        for (final ClientConduitStrand strand : this.conduitStrands.values()) {
            strand.tickInterpolation(interpolationTick);
        }
    }
}