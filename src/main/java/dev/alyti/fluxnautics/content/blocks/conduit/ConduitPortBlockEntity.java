package dev.alyti.fluxnautics.content.blocks.conduit;

import com.google.common.collect.Maps;
import dev.alyti.fluxnautics.api.content.tooltip.IHoverTooltip;
import dev.alyti.fluxnautics.registry.FNBlocks;
import dev.alyti.fluxnautics.util.Pair;
import mekanism.api.RelativeSide;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.base.WrenchResult;
import mekanism.common.util.EnumUtils;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.BaseCapability;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.ICapabilityInvalidationListener;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class ConduitPortBlockEntity extends TileEntityMekanism implements IHoverTooltip {

    public static final ModelProperty<CubeSideState[]> SIDE_STATE_PROPERTY = new ModelProperty<>();

    protected boolean handling = false;
    private final Map<Pair<BlockPos, BaseCapability<?, ?>>, CachedCapability> cachedCapabilities = Maps.newHashMap();

    private final Map<RelativeSide, RelativeSide> proxyMap = new EnumMap<>(RelativeSide.class);
    private final Map<RelativeSide, RelativeSide> reverseProxyMap = new EnumMap<>(RelativeSide.class);

    private boolean mapInitialized = false;
    private boolean needsCapabilityInvalidation = false;
    private boolean capabilitiesChanged = false;
    private boolean isHandlingRedstone = false;

    // State tracking for visuals (Server evaluates, Client renders)
    private final boolean[] serverActiveSides = new boolean[EnumUtils.SIDES.length];
    private final boolean[] syncedActiveSides = new boolean[EnumUtils.SIDES.length];
    private final boolean[] clientActiveSides = new boolean[EnumUtils.SIDES.length];

    // Capability tracking
    private final Map<RelativeSide, Set<ResourceLocation>> localCapabilities = new EnumMap<>(RelativeSide.class);
    private final Map<RelativeSide, Set<ResourceLocation>> remoteCapabilities = new EnumMap<>(RelativeSide.class);
    private final Map<RelativeSide, Set<ResourceLocation>> clientLocalCapabilities = new EnumMap<>(RelativeSide.class);
    private final Map<RelativeSide, Set<ResourceLocation>> clientRemoteCapabilities = new EnumMap<>(RelativeSide.class);

    private static class CachedCapability {
        public final Object capability;
        public final ICapabilityInvalidationListener listener;

        public CachedCapability(Object capability, ICapabilityInvalidationListener listener) {
            this.capability = capability;
            this.listener = listener;
        }
    }

    public ConduitPortBlockEntity(BlockPos pos, BlockState state) {
        super(FNBlocks.CONDUIT_PORT, pos, state);
    }

    public Direction getFacing() {
        return getBlockState().getValue(BlockStateProperties.FACING);
    }

    public boolean isOwner() {
        BlockPos conduitPos = getBlockPos().relative(getFacing());
        if (getLevel().getBlockEntity(conduitPos) instanceof ConduitConnectorBlockEntity conduit) {
            return conduit.getStrandHolder().ownsConduit();
        }
        return false;
    }

    public <T, C> T getCapability(BlockCapability<T, C> capability, C context) {
        if (handling) return null;
        if (!(context instanceof Direction side)) return null;
        if (side == getFacing()) return null; // Conduit side

        RelativeSide localRelative = RelativeSide.fromDirections(getFacing(), side);
        RelativeSide remoteRelative = getMappedSide(localRelative);

        if (remoteRelative == null) return null;

        handling = true;
        try {
            ConduitPortBlockEntity remotePort = getRemotePort();
            if (remotePort == null) return null;

            Direction remoteSide = remoteRelative.getDirection(remotePort.getFacing());
            BlockPos targetPos = remotePort.getBlockPos().relative(remoteSide);
            C targetContext = (C) remoteSide.getOpposite();

            return getCapabilityCached(capability, targetPos, targetContext, localRelative, remoteRelative, remotePort);
        } finally {
            handling = false;
        }
    }

    private void initializeDefaultMap() {
        if (mapInitialized) return;
        proxyMap.put(RelativeSide.BACK, RelativeSide.BACK);
        proxyMap.put(RelativeSide.LEFT, RelativeSide.LEFT);
        proxyMap.put(RelativeSide.RIGHT, RelativeSide.RIGHT);
        proxyMap.put(RelativeSide.TOP, RelativeSide.TOP);
        proxyMap.put(RelativeSide.BOTTOM, RelativeSide.BOTTOM);
        mapInitialized = true;
        setChanged();
    }

    public void setProxyMapping(RelativeSide localSide, RelativeSide remoteSide) {
        if (!isOwner()) return;
        if (localSide == RelativeSide.FRONT || remoteSide == RelativeSide.FRONT) return;
        if (!mapInitialized) initializeDefaultMap();

        proxyMap.entrySet().removeIf(entry -> entry.getKey() == localSide || entry.getValue() == remoteSide);
        proxyMap.put(localSide, remoteSide);
        setChanged();

        ConduitPortBlockEntity remotePort = getRemotePort();
        if (remotePort != null) {
            remotePort.reverseProxyMap.clear();
            for (Map.Entry<RelativeSide, RelativeSide> entry : proxyMap.entrySet()) {
                remotePort.reverseProxyMap.put(entry.getValue(), entry.getKey());
            }
            remotePort.setChanged();
        }

        Level level = getLevel();
        if (level != null) {
            level.invalidateCapabilities(getBlockPos());
            level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
            if (remotePort != null) {
                level.invalidateCapabilities(remotePort.getBlockPos());
                level.updateNeighborsAt(remotePort.getBlockPos(), remotePort.getBlockState().getBlock());
            }
        }

        // Drop cached capabilities since the routing has changed
        clearCapabilityCaches();
        if (remotePort != null) {
            remotePort.clearCapabilityCaches();
        }
    }

    public void removeProxyMapping(RelativeSide localSide) {
        if (!isOwner() || localSide == RelativeSide.FRONT || !mapInitialized) return;

        if (proxyMap.remove(localSide) != null) {
            setChanged();

            ConduitPortBlockEntity remotePort = getRemotePort();
            if (remotePort != null) {
                remotePort.reverseProxyMap.clear();
                for (Map.Entry<RelativeSide, RelativeSide> entry : proxyMap.entrySet()) {
                    remotePort.reverseProxyMap.put(entry.getValue(), entry.getKey());
                }
                remotePort.setChanged();
            }

            Level level = getLevel();
            if (level != null) {
                level.invalidateCapabilities(getBlockPos());
                level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
                if (remotePort != null) {
                    level.invalidateCapabilities(remotePort.getBlockPos());
                    level.updateNeighborsAt(remotePort.getBlockPos(), remotePort.getBlockState().getBlock());
                }
            }

            // Drop cached capabilities since the routing has changed
            clearCapabilityCaches();
            if (remotePort != null) {
                remotePort.clearCapabilityCaches();
            }
        }
    }

    private RelativeSide getMappedSide(RelativeSide localSide) {
        if (isOwner()) {
            if (!mapInitialized) {
                initializeDefaultMap();
                needsCapabilityInvalidation = true;
            }
            return proxyMap.get(localSide);
        } else {
            if (reverseProxyMap.isEmpty()) {
                syncReverseMap();
                needsCapabilityInvalidation = true;
            }
            return reverseProxyMap.get(localSide);
        }
    }

    private void syncReverseMap() {
        ConduitPortBlockEntity remotePort = getRemotePort();
        if (remotePort != null && remotePort.isOwner()) {
            reverseProxyMap.clear();
            for (Map.Entry<RelativeSide, RelativeSide> entry : remotePort.proxyMap.entrySet()) {
                reverseProxyMap.put(entry.getValue(), entry.getKey());
            }
            setChanged();
        }
    }

    @Nullable
    public ConduitPortBlockEntity getRemotePort() {
        BlockPos conduitPos = getBlockPos().relative(getFacing());
        if (getLevel().getBlockEntity(conduitPos) instanceof ConduitConnectorBlockEntity localConduit) {
            ConduitConnectorBlockEntity remoteConduit = localConduit.getRemoteConnector();
            if (remoteConduit != null) {
                RelativeSide side = RelativeSide.fromDirections(localConduit.getDirection(), getFacing().getOpposite());
                Direction remoteDir = side.getDirection(remoteConduit.getDirection());
                BlockPos remotePortPos = remoteConduit.getBlockPos().relative(remoteDir);

                if (getLevel().getBlockEntity(remotePortPos) instanceof ConduitPortBlockEntity remotePort) {
                    return remotePort;
                }
            }
        }
        return null;
    }

    @Override
    public void onNeighborChange(Block block, BlockPos neighbor) {
        super.onNeighborChange(block, neighbor);
        Direction facing = this.getBlockState().getValue(BlockStateProperties.FACING);
        BlockPos conduitPos = this.getBlockPos().relative(facing);

        if (neighbor.equals(conduitPos)) {
            if (!(level.getBlockState(neighbor).getBlock() instanceof AbstractConduitHolderBlock)) {
                if (level instanceof Level l && !l.isClientSide) {
                    dropRemotePort();
                    Block.popResource(l, this.getBlockPos(), new ItemStack(this.getBlockHolder().value()));
                    l.removeBlock(this.getBlockPos(), false);
                }
            }
            return;
        }

        // Redstone propagation: Notify the remote port to update its neighbors
        if (level instanceof Level l && !l.isClientSide && !isHandlingRedstone) {
            ConduitPortBlockEntity remotePort = getRemotePort();
            if (remotePort != null && !remotePort.isRemoved()) {
                remotePort.isHandlingRedstone = true;
                l.updateNeighborsAt(remotePort.getBlockPos(), remotePort.getBlockState().getBlock());
                remotePort.isHandlingRedstone = false;
            }
        }
    }

    public void dropRemotePort() {
        ConduitPortBlockEntity remote = getRemotePort();
        if (remote != null && !remote.isRemoved()) {
            Block.popResource(remote.getLevel(), remote.getBlockPos(), new ItemStack(FNBlocks.CONDUIT_PORT.get()));
            remote.getLevel().removeBlock(remote.getBlockPos(), false);
        }
    }

    private boolean isSideActive(RelativeSide side) {
        if (side == RelativeSide.FRONT) return false;
        RelativeSide remoteRelative = getMappedSide(side);
        if (remoteRelative == null) return false;

        ConduitPortBlockEntity remotePort = getRemotePort();
        if (remotePort == null) return false;

        Direction remoteDir = remoteRelative.getDirection(remotePort.getFacing());
        BlockPos targetPos = remotePort.getBlockPos().relative(remoteDir);

        return getLevel() != null && getLevel().getBlockEntity(targetPos) != null;
    }

    /**
     * Proxies redstone signal from the remote port.
     */
    public int getProxyRedstoneSignal(Direction localDir, boolean strong) {
        if (localDir == null || localDir == getFacing()) return 0;

        ConduitPortBlockEntity remotePort = getRemotePort();
        if (remotePort == null || remotePort.getLevel() == null) return 0;

        RelativeSide localRelative = RelativeSide.fromDirections(getFacing(), localDir);
        RelativeSide remoteRelative = getMappedSide(localRelative);
        if (remoteRelative == null) return 0;

        Direction remoteDir = remoteRelative.getDirection(remotePort.getFacing());
        BlockPos sourcePos = remotePort.getBlockPos().relative(remoteDir);
        BlockState sourceState = remotePort.getLevel().getBlockState(sourcePos);

//        Direction sourceToPortDir = remoteDir;
        if (strong) {
            return sourceState.getDirectSignal(remotePort.getLevel(), sourcePos, remoteDir);
        } else {
            return sourceState.getSignal(remotePort.getLevel(), sourcePos, remoteDir);
        }
    }

    @Override
    protected WrenchResult tryWrenchRotate(BlockState state, Player player, ItemStack stack) {
        return WrenchResult.RADIOACTIVE;
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdatePacket = super.onUpdateServer();

        if (needsCapabilityInvalidation) {
            needsCapabilityInvalidation = false;
            Level level = getLevel();
            if (level != null) {
                level.invalidateCapabilities(getBlockPos());
            }
        }

        if (capabilitiesChanged) {
            capabilitiesChanged = false;
            sendUpdatePacket = true;
        }

        // Simple O(1) check per side to see if a block entity exists on the other end
        for (RelativeSide side : EnumUtils.SIDES) {
            boolean active = isSideActive(side);
            if (active != syncedActiveSides[side.ordinal()]) {
                syncedActiveSides[side.ordinal()] = active;
                sendUpdatePacket = true;
            }
        }

        return sendUpdatePacket;
    }

    @NotNull
    @Override
    public CompoundTag getReducedUpdateTag(@NotNull HolderLookup.Provider provider) {
        CompoundTag updateTag = super.getReducedUpdateTag(provider);
        byte[] arr = new byte[EnumUtils.SIDES.length];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = (byte) (syncedActiveSides[i] ? 1 : 0);
        }
        updateTag.putByteArray("activeSides", arr);
        updateTag.put("localCaps", writeCapabilities(localCapabilities));
        updateTag.put("remoteCaps", writeCapabilities(remoteCapabilities));
        return updateTag;
    }

    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        boolean[] oldStates = clientActiveSides.clone();
        super.handleUpdateTag(tag, provider);

        if (tag.contains("activeSides")) {
            byte[] arr = tag.getByteArray("activeSides");
            for (int i = 0; i < arr.length && i < clientActiveSides.length; i++) {
                clientActiveSides[i] = arr[i] == 1;
            }
        }

        if (tag.contains("localCaps")) {
            readCapabilities(clientLocalCapabilities, tag.getCompound("localCaps"));
        } else {
            clientLocalCapabilities.clear();
        }
        if (tag.contains("remoteCaps")) {
            readCapabilities(clientRemoteCapabilities, tag.getCompound("remoteCaps"));
        } else {
            clientRemoteCapabilities.clear();
        }

        for (int i = 0; i < oldStates.length; i++) {
            if (oldStates[i] != clientActiveSides[i]) {
                updateModelData();
                break;
            }
        }
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putBoolean("mapInitialized", mapInitialized);

        if (isOwner() && !proxyMap.isEmpty()) {
            CompoundTag proxyTag = new CompoundTag();
            for (Map.Entry<RelativeSide, RelativeSide> entry : proxyMap.entrySet()) {
                proxyTag.putString(entry.getKey().name(), entry.getValue().name());
            }
            tag.put("proxyMap", proxyTag);
        } else if (!isOwner() && !reverseProxyMap.isEmpty()) {
            CompoundTag reverseProxyTag = new CompoundTag();
            for (Map.Entry<RelativeSide, RelativeSide> entry : reverseProxyMap.entrySet()) {
                reverseProxyTag.putString(entry.getKey().name(), entry.getValue().name());
            }
            tag.put("reverseProxyMap", reverseProxyTag);
        }
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        mapInitialized = tag.getBoolean("mapInitialized");

        proxyMap.clear();
        reverseProxyMap.clear();
        if (tag.contains("proxyMap")) {
            CompoundTag proxyTag = tag.getCompound("proxyMap");
            for (String key : proxyTag.getAllKeys()) {
                try {
                    proxyMap.put(RelativeSide.valueOf(key), RelativeSide.valueOf(proxyTag.getString(key)));
                } catch (IllegalArgumentException ignored) {}
            }
        } else if (tag.contains("reverseProxyMap") || tag.contains("reverseProxyTag")) {
            CompoundTag reverseProxyTag = tag.contains("reverseProxyMap") ? tag.getCompound("reverseProxyMap") : tag.getCompound("reverseProxyTag");
            for (String key : reverseProxyTag.getAllKeys()) {
                try {
                    reverseProxyMap.put(RelativeSide.valueOf(key), RelativeSide.valueOf(reverseProxyTag.getString(key)));
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void clearCapabilityCaches() {
        cachedCapabilities.clear();
        localCapabilities.clear();
        remoteCapabilities.clear();
        capabilitiesChanged = true;
    }

    private <T, C> T getCapabilityCached(BlockCapability<T, C> capability, BlockPos targetPos, C targetContext, RelativeSide localRelative, RelativeSide remoteRelative, ConduitPortBlockEntity remotePort) {
        Pair<BlockPos, BaseCapability<?, ?>> cacheKey = Pair.of(targetPos, capability);
        CachedCapability cached = cachedCapabilities.get(cacheKey);
        if (cached != null) {
            return (T) cached.capability;
        }

        T outerCapability = WorldUtils.getCapability(getLevel(), capability, targetPos, targetContext);

        if (getLevel() instanceof ServerLevel serverLevel) {
            ResourceLocation capName = capability.name();
            ICapabilityInvalidationListener invalidationListener = () -> {
                cachedCapabilities.remove(cacheKey);
                if (outerCapability != null && !this.isRemoved()) {
                    boolean changed = remoteCapabilities.getOrDefault(localRelative, Collections.emptySet()).remove(capName);
                    boolean remoteChanged = false;
                    if (remotePort != null && !remotePort.isRemoved()) {
                        remoteChanged = remotePort.localCapabilities.getOrDefault(remoteRelative, Collections.emptySet()).remove(capName);
                    }
                    if (changed) capabilitiesChanged = true;
                    if (remoteChanged) remotePort.capabilitiesChanged = true;
                }
                if (getLevel() != null && !this.isRemoved()) {
                    getLevel().invalidateCapabilities(getBlockPos());
                }
                return false;
            };

            cachedCapabilities.put(cacheKey, new CachedCapability(outerCapability, invalidationListener));
            serverLevel.registerCapabilityListener(targetPos, invalidationListener);

            if (outerCapability != null) {
                boolean changed = remoteCapabilities.computeIfAbsent(localRelative, s -> new HashSet<>()).add(capName);
                boolean remoteChanged = false;
                if (remotePort != null && !remotePort.isRemoved()) {
                    remoteChanged = remotePort.localCapabilities.computeIfAbsent(remoteRelative, s -> new HashSet<>()).add(capName);
                }
                if (changed) capabilitiesChanged = true;
                if (remoteChanged) remotePort.capabilitiesChanged = true;
            } else {
                boolean changed = remoteCapabilities.getOrDefault(localRelative, Collections.emptySet()).remove(capName);
                boolean remoteChanged = false;
                if (remotePort != null && !remotePort.isRemoved()) {
                    remoteChanged = remotePort.localCapabilities.getOrDefault(remoteRelative, Collections.emptySet()).remove(capName);
                }
                if (changed) capabilitiesChanged = true;
                if (remoteChanged) remotePort.capabilitiesChanged = true;
            }
        }
        return outerCapability;
    }

    private CompoundTag writeCapabilities(Map<RelativeSide, Set<ResourceLocation>> map) {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<RelativeSide, Set<ResourceLocation>> entry : map.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            ListTag list = new ListTag();
            for (ResourceLocation rl : entry.getValue()) {
                list.add(StringTag.valueOf(rl.toString()));
            }
            tag.put(entry.getKey().name(), list);
        }
        return tag;
    }

    private void readCapabilities(Map<RelativeSide, Set<ResourceLocation>> map, CompoundTag tag) {
        map.clear();
        for (String key : tag.getAllKeys()) {
            try {
                RelativeSide side = RelativeSide.valueOf(key);
                ListTag list = tag.getList(key, Tag.TAG_STRING);
                Set<ResourceLocation> set = new HashSet<>();
                for (int i = 0; i < list.size(); i++) {
                    set.add(ResourceLocation.parse(list.getString(i)));
                }
                map.put(side, set);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    @NotNull
    @Override
    public ModelData getModelData() {
        CubeSideState[] sideStates = new CubeSideState[EnumUtils.SIDES.length];
        for (RelativeSide side : EnumUtils.SIDES) {
            sideStates[side.ordinal()] = clientActiveSides[side.ordinal()] ? CubeSideState.ACTIVE_LIT : CubeSideState.INACTIVE_UNLIT;
        }
        return ModelData.of(SIDE_STATE_PROPERTY, sideStates);
    }

    public Map<RelativeSide, Set<ResourceLocation>> getClientLocalCapabilities() {
        return clientLocalCapabilities;
    }

    public Map<RelativeSide, Set<ResourceLocation>> getClientRemoteCapabilities() {
        return clientRemoteCapabilities;
    }

    @Override
    public void addLines(List<Component> list) {
        if (getLevel() == null || !getLevel().isClientSide) return;

        for (RelativeSide side : EnumUtils.SIDES) {
            if (side == RelativeSide.FRONT) continue;
            Set<ResourceLocation> local = clientLocalCapabilities.getOrDefault(side, Collections.emptySet());
            Set<ResourceLocation> remote = clientRemoteCapabilities.getOrDefault(side, Collections.emptySet());
            if (local.isEmpty() && remote.isEmpty()) continue;

            list.add(Component.literal(side.name() + ":"));
            if (!local.isEmpty()) {
                list.add(Component.literal("  Local: " + local.stream().map(ResourceLocation::toString).collect(Collectors.joining(", "))));
            }
            if (!remote.isEmpty()) {
                list.add(Component.literal("  Remote: " + remote.stream().map(ResourceLocation::toString).collect(Collectors.joining(", "))));
            }
        }
    }

    public enum CubeSideState {
        ACTIVE_LIT,
        INACTIVE_UNLIT,
    }
}