package dev.alyti.fluxnautics.content.blocks.forcefield;

import net.minecraft.world.level.Level;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ForcefieldRegistry {
    private static final ConcurrentHashMap<Level, Set<ForcefieldEmitterBlockEntity>> ACTIVE_FIELDS = new ConcurrentHashMap<>();

    public static void register(Level level, ForcefieldEmitterBlockEntity emitter) {
        if (level == null || emitter == null) return;
        ACTIVE_FIELDS.computeIfAbsent(level, k -> ConcurrentHashMap.newKeySet()).add(emitter);
    }

    public static void unregister(ForcefieldEmitterBlockEntity emitter) {
        if (emitter == null) return;
        ACTIVE_FIELDS.values().forEach(set -> set.remove(emitter));
    }

    public static Set<ForcefieldEmitterBlockEntity> getActiveFields(Level level) {
        return ACTIVE_FIELDS.getOrDefault(level, Set.of());
    }
}