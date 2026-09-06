package dev.alyti.fluxnautics.client;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CloakedSublevelCache {

    public static final Map<ResourceKey<Level>, Set<UUID>> CLOAKED_SUBLEVELS = new Object2ObjectOpenHashMap<>();

    private CloakedSublevelCache() {}

    public static void updateCloakedSublevels(Map<ResourceKey<Level>, Set<UUID>> cloaked) {
        // Trigger activation for newly cloaked sublevels
        for (Map.Entry<ResourceKey<Level>, Set<UUID>> entry : cloaked.entrySet()) {
            Set<UUID> oldSet = CLOAKED_SUBLEVELS.get(entry.getKey());
            if (oldSet == null) {
                entry.getValue().forEach(CloakRenderStateTracker::onCloakActivated);
            } else {
                for (UUID id : entry.getValue()) {
                    if (!oldSet.contains(id)) {
                        CloakRenderStateTracker.onCloakActivated(id);
                    }
                }
            }
        }

        // Trigger deactivation for sublevels that uncloaked
        for (Map.Entry<ResourceKey<Level>, Set<UUID>> entry : CLOAKED_SUBLEVELS.entrySet()) {
            Set<UUID> newSet = cloaked.get(entry.getKey());
            if (newSet == null) {
                entry.getValue().forEach(CloakRenderStateTracker::onCloakDeactivated);
            } else {
                for (UUID id : entry.getValue()) {
                    if (!newSet.contains(id)) {
                        CloakRenderStateTracker.onCloakDeactivated(id);
                    }
                }
            }
        }

        CLOAKED_SUBLEVELS.clear();
        CLOAKED_SUBLEVELS.putAll(cloaked);
    }

    public static void clear() {
        CLOAKED_SUBLEVELS.clear();
        CloakRenderStateTracker.clear();
    }

    public static boolean isEntityCloaked(Entity entity) {
        if (isEmpty()) {
            return false;
        }

        ClientSubLevel subLevel = Sable.HELPER.getContainingClient(entity);
        if (subLevel != null)
            return isCloaked(subLevel);

        double ex = entity.getX();
        double ey = entity.getY();
        double ez = entity.getZ();

        BoundingBox3d entityQueryBounds = new BoundingBox3d(ex, ey, ez, ex, ey, ez).expand(0.5);

        Iterable<SubLevel> intersecting = Sable.HELPER.getAllIntersecting(entity.level(), entityQueryBounds);

        for (SubLevel subLevelIter : intersecting) {
            if (isCloaked(subLevelIter))
                return true;
        }
        return false;
    }

    public static boolean isCloaked(SubLevel subLevel) {
        if (CloakRenderStateTracker.isDecloaking(subLevel.getUniqueId())) {
            return true;
        }

        Set<UUID> cloaked = CLOAKED_SUBLEVELS.get(subLevel.getLevel().dimension());
        if (cloaked == null || !cloaked.contains(subLevel.getUniqueId())) {
            return false;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            Vec3 pos = player.position();
            return !subLevel.boundingBox().contains(pos.x, pos.y, pos.z);
        }

        return true;
    }

    public static boolean isCloaked(ResourceKey<Level> levelKey, UUID sublevelId) {
        Set<UUID> cloaked = CLOAKED_SUBLEVELS.get(levelKey);
        if (cloaked == null || !cloaked.contains(sublevelId)) {
            return false;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && Minecraft.getInstance().level != null) {
            SubLevelContainer container = SubLevelContainer.getContainer(Minecraft.getInstance().level);
            if (container != null) {
                SubLevel sb = container.getSubLevel(sublevelId);
                return sb == null || !sb.boundingBox().contains(player.position().x, player.position().y, player.position().z);
            }
        }

        return true;
    }

    public static boolean isEmpty() {
        return CLOAKED_SUBLEVELS.isEmpty();
    }

    public static Set<UUID> getCloakedForLevel(ResourceKey<Level> levelKey) {
        return CLOAKED_SUBLEVELS.getOrDefault(levelKey, Collections.emptySet());
    }
}