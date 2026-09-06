package dev.alyti.fluxnautics.content.blocks.cloak;

import dev.alyti.fluxnautics.client.CloakedSublevelCache;
import dev.alyti.fluxnautics.content.items.ModuleDecloaker;
import dev.alyti.fluxnautics.network.packets.CloakingSyncPacket;
import dev.alyti.fluxnautics.registry.FNModules;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import mekanism.api.gear.IModule;
import mekanism.api.gear.IModuleHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

public class CloakGeneratorEvents {

    private static int lastKnownStateVersion = 0;
    private static int cleanupTimer = 0;

    private static final Map<UUID, Boolean> PLAYER_DECLOAKER_STATE = new HashMap<>();

    @SubscribeEvent
    public static void onServerPostTick(ServerTickEvent.Post event) {
        if (++cleanupTimer >= 100) {
            cleanupTimer = 0;
            CloakGeneratorBlockEntity.cleanupOrphanedLinks();
        }

        int currentVersion = CloakGeneratorBlockEntity.mapOfChildSublevelsToRootSublevel.hashCode();
        boolean globalMapChanged = currentVersion != lastKnownStateVersion;
        if (globalMapChanged) {
            lastKnownStateVersion = currentVersion;
        }

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            boolean hasDecloaker = checkHasDecloaker(player);
            boolean lastKnownState = PLAYER_DECLOAKER_STATE.getOrDefault(player.getUUID(), false);

            if (hasDecloaker != lastKnownState || globalMapChanged) {
                PLAYER_DECLOAKER_STATE.put(player.getUUID(), hasDecloaker);
                PacketDistributor.sendToPlayer(player, new CloakingSyncPacket(serverToClientCloakingMap(player)));
            }
        }
    }

    private static boolean checkHasDecloaker(ServerPlayer player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        IModule<ModuleDecloaker> decloakerModule = IModuleHelper.INSTANCE.getIfEnabled(head, FNModules.DECLOAKER);
        return decloakerModule != null;
    }

    private static HashMap<ResourceKey<Level>, Set<UUID>> serverToClientCloakingMap(ServerPlayer serverPlayer) {
        boolean hasDecloaker = PLAYER_DECLOAKER_STATE.getOrDefault(serverPlayer.getUUID(), false);

        HashMap<ResourceKey<Level>, Set<UUID>> result = new HashMap<>();

        for (Map.Entry<ResourceKey<Level>, Map<ServerSubLevel, Set<CloakGeneratorBlockEntity.CloakingTicket>>> levelEntry : CloakGeneratorBlockEntity.mapOfChildSublevelsToRootSublevel.entrySet()) {
            Set<UUID> cloakedIds = new HashSet<>();

            for (Map.Entry<ServerSubLevel, Set<CloakGeneratorBlockEntity.CloakingTicket>> kv : levelEntry.getValue().entrySet()) {
                Set<CloakGeneratorBlockEntity.CloakingTicket> tickets = kv.getValue();
                if (tickets.isEmpty()) continue;

                boolean isIgnored = false;
                if (hasDecloaker) {
                    isIgnored = tickets.stream().allMatch(ticket ->
                            ticket.blockEntities().stream().allMatch(be ->
                                    Objects.equals(be.getOwnerUUID(), serverPlayer.getUUID())
                            )
                    );
                }

                if (!isIgnored) {
                    cloakedIds.add(kv.getKey().getUniqueId());
                }
            }

            if (!cloakedIds.isEmpty()) {
                result.put(levelEntry.getKey(), cloakedIds);
            }
        }

        return result;
    }


    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof Level level) {
            CloakGeneratorBlockEntity.clearDimension(level.dimension());
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity().level().isClientSide()) {
            CloakedSublevelCache.clear();
        } else {
            PLAYER_DECLOAKER_STATE.remove(event.getEntity().getUUID());
        }
    }
}