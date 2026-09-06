package dev.alyti.fluxnautics.content.blocks.cloak;

import dev.alyti.fluxnautics.FluxnauticsConfig;
import dev.alyti.fluxnautics.content.blocks.AbstractMassMachineBlockEntity;
import dev.alyti.fluxnautics.registry.FNBlocks;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class CloakGeneratorBlockEntity extends AbstractMassMachineBlockEntity {

    private static final Map<ResourceKey<Level>, Map<ServerSubLevel, CloakGeneratorBlockEntity.CloakingTicket>> mapOfCloakers = new Object2ObjectOpenHashMap<>();
    public static final Map<ResourceKey<Level>, Map<ServerSubLevel, Set<CloakGeneratorBlockEntity.CloakingTicket>>> mapOfChildSublevelsToRootSublevel = new Object2ObjectOpenHashMap<>();
    private static long stateVersion = 0L;

    public CloakGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(FNBlocks.CLOAK_GENERATOR, pos, state);
    }

    @Override
    public boolean countConnected() {
        return true;
    }

    @Override
    public double getEnergyPerMassMultiplier() {
        return FluxnauticsConfig.common.cloakGeneratorJoulesPerMassMult.getOrDefault();
    }

    @Override
    public long calcEnergyCost() {
        return Math.round(this.energyContainer.getLastMass() * this.getEnergyPerMassMultiplier());
    }

    @Override
    public void onSublevelTick(ServerSubLevel subLevel, boolean active) {
        updateCloakingFor(subLevel, this, active);
    }

    public static long getStateVersion() {
        return stateVersion;
    }

    private static void bumpStateVersion() {
        stateVersion++;
    }

    public static void updateCloakingFor(final ServerSubLevel serverSubLevel, final CloakGeneratorBlockEntity be, boolean keep) {
        final ResourceKey<Level> dimKey = serverSubLevel.getLevel().dimension();
        final Map<ServerSubLevel, CloakingTicket> map = mapOfCloakers.get(dimKey);
        CloakingTicket ticket = map != null ? map.get(serverSubLevel) : null;

        if (ticket != null && !ticket.isValid()) {
            map.remove(serverSubLevel);
            removeTicketFromChildMap(dimKey, serverSubLevel, ticket);
            ticket = null;
            bumpStateVersion();
            if (!keep) return;
        }

        if (!keep && ticket == null) {
            return;
        }

        if (ticket == null) {
            Set<CloakGeneratorBlockEntity> blockEntities = new ObjectOpenHashSet<>();
            ticket = new CloakingTicket(serverSubLevel, blockEntities);
            mapOfCloakers.computeIfAbsent(dimKey, x -> new Object2ObjectOpenHashMap<>())
                    .put(serverSubLevel, ticket);
        }

        if (keep) {
            ticket.blockEntities.add(be);
        } else {
            ticket.blockEntities.remove(be);
        }

        if (ticket.blockEntities.isEmpty()) {
            map.remove(serverSubLevel);
            removeTicketFromChildMap(dimKey, serverSubLevel, ticket);
            bumpStateVersion();
            return;
        }

        final Map<ServerSubLevel, Set<CloakingTicket>> mapSubs = mapOfChildSublevelsToRootSublevel.get(dimKey);
        for (final SubLevel subLevel : SubLevelHelper.getConnectedChain(serverSubLevel)) {
            ServerSubLevel ssb = (ServerSubLevel) subLevel;
            Set<CloakingTicket> tickets = mapSubs != null ? mapSubs.get(ssb) : null;

            if (tickets == null) {
                if (!keep) continue;
                tickets = new ObjectOpenHashSet<>();
                mapOfChildSublevelsToRootSublevel.computeIfAbsent(dimKey, x -> new Object2ObjectOpenHashMap<>()).put(ssb, tickets);
            }

            if (keep) {
                tickets.add(ticket);
            } else {
                tickets.remove(ticket);
                if (tickets.isEmpty()) {
                    mapSubs.remove(ssb);
                }
            }
        }
        bumpStateVersion();
    }

    private static void removeTicketFromChildMap(ResourceKey<Level> dimKey, ServerSubLevel rootLevel, CloakingTicket ticket) {
        Map<ServerSubLevel, Set<CloakingTicket>> mapSubs = mapOfChildSublevelsToRootSublevel.get(dimKey);
        if (mapSubs == null) return;

        for (final SubLevel subLevel : SubLevelHelper.getConnectedChain(rootLevel)) {
            ServerSubLevel ssb = (ServerSubLevel) subLevel;
            Set<CloakingTicket> tickets = mapSubs.get(ssb);
            if (tickets != null) {
                tickets.remove(ticket);
                if (tickets.isEmpty()) {
                    mapSubs.remove(ssb);
                }
            }
        }

        if (mapSubs.isEmpty()) {
            mapOfChildSublevelsToRootSublevel.remove(dimKey);
        }
    }

    public static void clearDimension(ResourceKey<Level> dimKey) {
        if (mapOfCloakers.remove(dimKey) != null || mapOfChildSublevelsToRootSublevel.remove(dimKey) != null) {
            bumpStateVersion();
        }
    }

    public static void cleanupOrphanedLinks() {
        Iterator<Map.Entry<ResourceKey<Level>, Map<ServerSubLevel, CloakingTicket>>> dimIterator = mapOfCloakers.entrySet().iterator();
        while (dimIterator.hasNext()) {
            Map.Entry<ResourceKey<Level>, Map<ServerSubLevel, CloakingTicket>> dimEntry = dimIterator.next();
            Map<ServerSubLevel, CloakingTicket> map = dimEntry.getValue();

            Iterator<Map.Entry<ServerSubLevel, CloakingTicket>> slIterator = map.entrySet().iterator();
            while (slIterator.hasNext()) {
                Map.Entry<ServerSubLevel, CloakingTicket> slEntry = slIterator.next();
                ServerSubLevel sl = slEntry.getKey();
                CloakingTicket ticket = slEntry.getValue();

                boolean shouldRemove = sl.isRemoved() || ticket == null || !ticket.isValid() || ticket.blockEntities().isEmpty();

                if (shouldRemove) {
                    if (ticket != null) {
                        removeTicketFromChildMap(dimEntry.getKey(), sl, ticket);
                    }
                    slIterator.remove();
                }
            }

            if (map.isEmpty()) {
                dimIterator.remove();
            }
        }

        Iterator<Map.Entry<ResourceKey<Level>, Map<ServerSubLevel, Set<CloakingTicket>>>> childDimIterator = mapOfChildSublevelsToRootSublevel.entrySet().iterator();
        while (childDimIterator.hasNext()) {
            Map.Entry<ResourceKey<Level>, Map<ServerSubLevel, Set<CloakingTicket>>> childDimEntry = childDimIterator.next();
            Map<ServerSubLevel, Set<CloakingTicket>> mapSubs = childDimEntry.getValue();

            Map<ServerSubLevel, CloakingTicket> validRootTickets = mapOfCloakers.get(childDimEntry.getKey());

            Iterator<Map.Entry<ServerSubLevel, Set<CloakingTicket>>> slIterator = mapSubs.entrySet().iterator();
            while (slIterator.hasNext()) {
                Map.Entry<ServerSubLevel, Set<CloakingTicket>> slEntry = slIterator.next();
                ServerSubLevel ssb = slEntry.getKey();
                
                if (ssb.isRemoved()) {
                    slIterator.remove();
                    continue;
                }

                Set<CloakingTicket> tickets = slEntry.getValue();
                if (tickets != null) {
                    tickets.removeIf(ticket -> validRootTickets == null || !validRootTickets.containsValue(ticket));

                    if (tickets.isEmpty()) {
                        slIterator.remove();
                    }
                } else {
                    slIterator.remove();
                }
            }

            if (mapSubs.isEmpty()) {
                childDimIterator.remove();
            }
        }
        bumpStateVersion();
    }

    public record CloakingTicket(ServerSubLevel rootLevel, Set<CloakGeneratorBlockEntity> blockEntities) {
        public boolean isValid() {
            for (final CloakGeneratorBlockEntity be : this.blockEntities) {
                if (be.isRemoved() || be.getLevel() != this.rootLevel.getLevel()) {
                    return false;
                }
            }

            return this.rootLevel != null && !this.rootLevel.isRemoved();
        }
    }
}
