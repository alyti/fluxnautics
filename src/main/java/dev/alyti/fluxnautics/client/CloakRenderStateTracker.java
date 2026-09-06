package dev.alyti.fluxnautics.client;

import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CloakRenderStateTracker {

    private static final float CLOAK_DURATION_MS = 1500.0f;
    private static final float DECLOAK_DURATION_MS = 1500.0f;
    private static final double PROXIMITY_REVEAL_DISTANCE = 6.0;
    private static final float PROXIMITY_MAX_OPACITY = 0.98f;

    private static final Map<UUID, Long> CLOAK_START_TIMES = new HashMap<>();
    private static final Map<UUID, Long> DECLOAK_START_TIMES = new HashMap<>();

    private CloakRenderStateTracker() {}

    public static void onCloakActivated(UUID sublevelId) {
        CLOAK_START_TIMES.putIfAbsent(sublevelId, System.currentTimeMillis());
        DECLOAK_START_TIMES.remove(sublevelId);
    }

    public static void onCloakDeactivated(UUID sublevelId) {
        if (CLOAK_START_TIMES.containsKey(sublevelId)) {
            DECLOAK_START_TIMES.put(sublevelId, System.currentTimeMillis());
        }
        CLOAK_START_TIMES.remove(sublevelId);
    }

    public static boolean isDecloaking(UUID sublevelId) {
        if (!DECLOAK_START_TIMES.containsKey(sublevelId)) {
            return false;
        }

        long startTime = DECLOAK_START_TIMES.get(sublevelId);
        long elapsed = System.currentTimeMillis() - startTime;

        if (elapsed >= DECLOAK_DURATION_MS) {
            DECLOAK_START_TIMES.remove(sublevelId);
            return false;
        }

        return true;
    }

    public static void clear() {
        CLOAK_START_TIMES.clear();
        DECLOAK_START_TIMES.clear();
    }

    public static float getOpacity(ClientSubLevel subLevel) {
        UUID id = subLevel.getUniqueId();

        Long decloakStart = DECLOAK_START_TIMES.get(id);
        if (decloakStart != null) {
            long elapsed = System.currentTimeMillis() - decloakStart;
            float decloakOpacity = Math.min(1.0f, elapsed / DECLOAK_DURATION_MS);

            if (decloakOpacity >= 1.0f) {
                DECLOAK_START_TIMES.remove(id);
            }

            return Math.max(decloakOpacity, getProximityOpacity(subLevel));
        }

        if (!CloakedSublevelCache.isCloaked(subLevel)) {
            return 1.0f;
        }

        long startTime = CLOAK_START_TIMES.getOrDefault(id, Long.MIN_VALUE);
        long elapsed = System.currentTimeMillis() - startTime;
        float fadeOpacity = 1.0f - Math.min(1.0f, elapsed / CLOAK_DURATION_MS);
        float proximityOpacity = getProximityOpacity(subLevel);
        return Math.max(fadeOpacity, proximityOpacity);
    }

    private static float getProximityOpacity(ClientSubLevel subLevel) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return 0.0f;

        Vec3 playerPos = player.position();
        BoundingBox3dc bounds = subLevel.boundingBox();

        if (bounds.contains(playerPos.x, playerPos.y, playerPos.z)) {
            return 1.0f;
        }

        double distX = Math.max(0, Math.max(bounds.minX() - playerPos.x, playerPos.x - bounds.maxX()));
        double distY = Math.max(0, Math.max(bounds.minY() - playerPos.y, playerPos.y - bounds.maxY()));
        double distZ = Math.max(0, Math.max(bounds.minZ() - playerPos.z, playerPos.z - bounds.maxZ()));

        double distanceSq = (distX * distX) + (distY * distY) + (distZ * distZ);
        double distance = Math.sqrt(distanceSq);

        if (distance < PROXIMITY_REVEAL_DISTANCE) {
            float factor = 1.0f - (float)(distance / PROXIMITY_REVEAL_DISTANCE);
            return factor * PROXIMITY_MAX_OPACITY;
        }

        return 0.0f;
    }
}