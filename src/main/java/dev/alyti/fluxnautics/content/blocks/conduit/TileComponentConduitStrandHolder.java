package dev.alyti.fluxnautics.content.blocks.conduit;


import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import dev.alyti.fluxnautics.FluxnauticsConfig;
import dev.alyti.fluxnautics.content.blocks.conduit.strand.client.ClientConduitPoint;
import dev.alyti.fluxnautics.content.blocks.conduit.strand.client.ClientConduitStrand;
import dev.alyti.fluxnautics.content.blocks.conduit.strand.client.ClientLevelConduitManager;
import dev.alyti.fluxnautics.content.blocks.conduit.strand.server.ConduitAttachment;
import dev.alyti.fluxnautics.content.blocks.conduit.strand.server.ConduitAttachmentPoint;
import dev.alyti.fluxnautics.content.blocks.conduit.strand.server.ServerConduitStrand;
import dev.alyti.fluxnautics.content.blocks.conduit.strand.server.ServerLevelConduitManager;
import dev.alyti.fluxnautics.network.packets.conduit.ClientboundConduitDataPacket;
import dev.alyti.fluxnautics.network.packets.conduit.ClientboundConduitStoppedPacket;
import dev.alyti.fluxnautics.registry.FNItems;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.sublevel.system.SubLevelTrackingSystem;
import foundry.veil.api.network.VeilPacketManager;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mekanism.common.tile.component.ITileComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.*;

public class TileComponentConduitStrandHolder<T extends AbstractConduitHolderBlockEntity> implements ITileComponent {
    private final T tile;

    @Nullable
    private UUID attachedConduitID = null;
    private boolean strandOwner = false;

    @Nullable
    private ServerConduitStrand ownedServerStrand;
    private boolean queuedLevelAddition = false;

    @Nullable
    private ClientConduitStrand ownedClientStrand;

    public boolean renderAttached;

    public TileComponentConduitStrandHolder(T tile) {
        this.tile = tile;
        this.tile.addComponent(this);
    }

    @Override
    public String getComponentKey() {
        return "conduitStrand";
    }

    public T getTile() {
        return tile;
    }

    @Override
    public void deserialize(CompoundTag nbt, HolderLookup.Provider provider) {
        deserialize(nbt, provider, false);
    }

    @Override
    public void readFromUpdateTag(CompoundTag updateTag) {
        deserialize(updateTag, null, true);
    }

    public void deserialize(CompoundTag nbt, HolderLookup.Provider provider, boolean clientPacket) {
        this.strandOwner = nbt.getBoolean("OwnStrand");

        if (nbt.contains("HasConduitAttached")) {
            this.attachedConduitID = nbt.getUUID("HasConduitAttached");
        } else {
            this.attachedConduitID = null;
        }

        if (clientPacket) {
            if (!this.strandOwner)
                this.removeClientStrand();
        } else {
            if (nbt.contains("Strand")) {
                final CompoundTag strandNBT = nbt.getCompound("Strand");

                if (this.ownedServerStrand == null) {
                    this.loadServerStrand(strandNBT);
                }
            }
        }
    }

    @Override
    public CompoundTag serialize(HolderLookup.Provider provider) {
        return serialize(new CompoundTag(), provider, false);
    }

    @Override
    public void addToUpdateTag(CompoundTag updateTag) {
        serialize(updateTag, null, true);
    }


    public CompoundTag serialize(CompoundTag nbt, HolderLookup.Provider provider, boolean clientPacket) {
        nbt.putBoolean("OwnStrand", this.strandOwner);

        if (this.attachedConduitID != null) {
            nbt.putUUID("HasConduitAttached", this.attachedConduitID);
        }

        final ServerConduitStrand strand = this.getOwnedStrand();
        if (strand != null && this.strandOwner && !clientPacket) {
            nbt.put("Strand", ServerConduitStrand.CODEC.encodeStart(NbtOps.INSTANCE, strand).getOrThrow());
        }
        return nbt;
    }

    public void tick() {

        if (this.ownedServerStrand != null) {
            if (this.queuedLevelAddition) {
                this.addServerStrand(this.ownedServerStrand);
                this.queuedLevelAddition = false;
            }

            final SubLevelPhysicsSystem system = this.getPhysicsSystem();
            final ServerLevel level = system.getLevel();

            final boolean attachmentsLoaded = this.ownedServerStrand.areAttachmentsLoaded(level);

            if (!this.ownedServerStrand.isActive() && attachmentsLoaded && system.getTicketManager().wouldBeLoaded(level, this.ownedServerStrand)) {
                system.addObject(this.ownedServerStrand);
            }

            if (this.ownedServerStrand.isActive()) {
                this.ownedServerStrand.updatePose();
            }

            this.tickStrandTrackingPlayers();

            this.destroyConduitIfAttachmentBroken();
            this.tile.setChanged();
        }

        if (this.ownedClientStrand != null) {
            ClientLevelConduitManager.getOrCreate(this.getLevel())
                .addStrand(this.ownedClientStrand);
        }
    }

    private void tickStrandTrackingPlayers() {
        assert this.ownedServerStrand != null;
        if (!this.ownedServerStrand.isActive()) return;

        final Set<UUID> alreadyTrackingPlayers = this.ownedServerStrand.getTrackingPlayers();

        final Iterator<UUID> iter = alreadyTrackingPlayers.iterator();

        final List<ServerPlayer> players = this.getStrandTrackingPlayers();

        while (iter.hasNext()) {
            final UUID uuid = iter.next();
            final ServerPlayer player = (ServerPlayer) this.getLevel().getPlayerByUUID(uuid);

            if (player == null || !players.contains(player)) {
                iter.remove();
            }
        }

        for (final ServerPlayer player : players) {
            final UUID uuid = player.getUUID();

            if (alreadyTrackingPlayers.add(uuid)) {
                this.ownedServerStrand.updatePose();
                VeilPacketManager.player(player).sendPacket(this.makeUpdatePacket());
            }
        }
    }

    public VeilPacketManager.PacketSink getStrandPacketSink() {
        final List<ServerPlayer> players = this.getStrandTrackingPlayers();
        return packet -> {
            for (final ServerPlayer player : players) {
                player.connection.send(packet);
            }
        };
    }

    public List<ServerPlayer> getStrandTrackingPlayers() {
        final ServerLevel level = (ServerLevel) this.getLevel();
        if (level == null) return List.of();

        final ChunkPos chunk = new ChunkPos(this.tile.getBlockPos());

        return level.getChunkSource().chunkMap.getPlayers(chunk, false);
    }

    @NotNull
    public ClientboundConduitDataPacket makeUpdatePacket() {
        final ServerSubLevelContainer container = (ServerSubLevelContainer) SubLevelContainer.getContainer(this.getLevel());
        assert container != null;

        final SubLevelTrackingSystem trackingSystem = container.trackingSystem();

        //todo can be null from schematics
        final ConduitAttachment startAttachment = this.ownedServerStrand.getAttachment(ConduitAttachmentPoint.START);
        final ConduitAttachment endAttachment = this.ownedServerStrand.getAttachment(ConduitAttachmentPoint.END);

        return new ClientboundConduitDataPacket(
            trackingSystem.getInterpolationTick(),
            this.tile.getBlockPos(),
            this.ownedServerStrand.getUUID(),
            new ObjectArrayList<>(this.ownedServerStrand.getPoints()),
            startAttachment != null ? startAttachment.blockAttachment() : null,
            endAttachment != null ? endAttachment.blockAttachment() : null
        );
    }

    public ClientboundConduitStoppedPacket makeStopPacket() {
        return new ClientboundConduitStoppedPacket(
            this.tile.getBlockPos()
        );
    }

    private void destroyConduitIfAttachmentBroken() {
        final ServerConduitStrand strand = this.ownedServerStrand;
        assert strand != null;

        final ServerLevel level = (ServerLevel) this.getLevel();

        if (!strand.areAttachmentsLoaded(level)) {
            return;
        }

        final ConduitAttachment endAttachment = strand.getAttachment(ConduitAttachmentPoint.END);

        // todo: no nice way to grab a potentially causing player to check for infinite materials :p
        final boolean tileDrops = level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS);

        if (endAttachment == null) {
            this.destroyConduit(null, this.getAttachmentPoint(), tileDrops);
            return;
        }

        final BlockPos blockAttachment = endAttachment.blockAttachment();
        final BlockEntity tile = level.getBlockEntity(blockAttachment);

        if (tile == null) {
            this.destroyConduit(null, blockAttachment.getCenter(), tileDrops);
            return;
        }

        if (!(tile instanceof final AbstractConduitHolderBlockEntity smartTile)) {
            this.destroyConduit(null, blockAttachment.getCenter(), tileDrops);
            return;
        }

        final TileComponentConduitStrandHolder<?> holderBehavior = smartTile.getStrandHolder();

        if (holderBehavior == null) {
            this.destroyConduit(null, blockAttachment.getCenter(), tileDrops);
        }
    }


    /**
     * Attempts to attach a conduit strand to the target holder.
     *
     * @param target the target holder to attach the conduit to
     * @return true if the conduit was successfully created, false if it already exists or the target is invalid
     */
    public boolean createConduit(final TileComponentConduitStrandHolder<?> target, final boolean dropItem) {
        if (target == this)
            return false;

        if (target.attachedConduitID != null) {
            return false;
        }

        final ServerLevel level = (ServerLevel) this.getLevel();

        final Vec3 localConduitStart = this.getAttachmentPoint();
        final Vec3 localConduitTarget = target.getAttachmentPoint();

        final SubLevel subLevelStart = Sable.HELPER.getContaining(level, localConduitStart);
        final SubLevel subLevelTarget = Sable.HELPER.getContaining(level, localConduitTarget);

        final Vec3 conduitStart = Sable.HELPER.projectOutOfSubLevel(level, localConduitStart);
        final Vec3 conduitTarget = Sable.HELPER.projectOutOfSubLevel(level, localConduitTarget);

        final double maxConduitRange = FluxnauticsConfig.common.conduitsMaxLength.getOrDefault();

        if (!conduitTarget.closerThan(conduitStart, maxConduitRange)) {
            return false;
        }

        this.destroyConduit(null, null, dropItem && level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS));

        final double distance = conduitTarget.distanceTo(conduitStart);
        final int oneLongSegments = Mth.floor(distance);
        final int points = Math.max(1, oneLongSegments + 1);

        final Vec3 diff = conduitTarget.subtract(conduitStart).normalize();

        final List<Vector3d> pointList = new ObjectArrayList<>();
        pointList.add(JOMLConversion.toJOML(conduitStart));

        final double shortSegmentLength = distance - oneLongSegments;

        for (int i = 0; i < points; i++) {
            pointList.add(JOMLConversion.toJOML(conduitStart.add(diff.scale(i + shortSegmentLength))));
        }

        final ServerConduitStrand strand = new ServerConduitStrand(UUID.randomUUID(), pointList);

        strand.updateFirstSegmentExtension(shortSegmentLength);
        strand.addAttachment(level, ConduitAttachmentPoint.START, new ConduitAttachment(ConduitAttachmentPoint.START, Optional.ofNullable(subLevelStart).map(SubLevel::getUniqueId).orElse(null), this.tile.getBlockPos()));
        strand.addAttachment(level, ConduitAttachmentPoint.END, new ConduitAttachment(ConduitAttachmentPoint.END, Optional.ofNullable(subLevelTarget).map(SubLevel::getUniqueId).orElse(null), target.tile.getBlockPos()));

        this.strandOwner = true;
        this.attachedConduitID = strand.getUUID();
        target.strandOwner = false;
        target.attachedConduitID = strand.getUUID();

        this.addServerStrand(strand);

        this.tile.markForSave();
        this.tile.sendUpdatePacket();
        target.tile.markForSave();
        target.tile.sendUpdatePacket();

        return true;
    }

    private @Nullable Level getLevel() {
        return this.tile.getLevel();
    }

    public SubLevelPhysicsSystem getPhysicsSystem() {
        return SubLevelContainer.getContainer((ServerLevel) this.getLevel()).physicsSystem();
    }

    /**
     * Block destroyed or Chunk unloaded. Usually invalidates capabilities
     */
    @Override
    public void invalidate() {
        final Level level = this.getLevel();

        if (this.ownedServerStrand != null) {
            this.removeServerStrand(level);
        }

        if (level != null && level.isClientSide) {
            this.removeClientStrand();
        }
    }

    private void removeServerStrand(final Level level) {
        if (this.ownedServerStrand.isActive()) {
            this.ownedServerStrand.updatePose();
        }
        this.getPhysicsSystem().removeObject(this.ownedServerStrand);
        ServerLevelConduitManager.getOrCreate(level)
            .removeStrand(this.ownedServerStrand.getUUID());
        this.ownedServerStrand = null;
    }

    private void addServerStrand(final ServerConduitStrand strand) {
        this.ownedServerStrand = strand;
        ServerLevelConduitManager.getOrCreate(this.getLevel())
            .addStrand(this.ownedServerStrand);
    }

    /**
     * Destroys the current conduit.
     */
    public void destroyConduit(@Nullable final ServerPlayer player, @Nullable final Vec3 conduitDropPos, final boolean returnItem) {
        if (!this.strandOwner || this.ownedServerStrand == null) {
            return;
        }

        final Level level = this.getLevel();
        if (level == null) {
            return;
        }

        final ServerConduitStrand strand = this.getOwnedStrand();

        if (strand != null) {
            final ConduitAttachment target = strand.getAttachment(ConduitAttachmentPoint.END);
            if (target != null) {
                final BlockPos targetBlockPos = target.blockAttachment();

                if (level.getBlockEntity(targetBlockPos) instanceof final AbstractConduitHolderBlockEntity be) {
                    final TileComponentConduitStrandHolder<?> behaviour = be.getStrandHolder();

                    if (behaviour != null) {
                        behaviour.detachConduit();
                        behaviour.tile.markForSave();
                        behaviour.tile.sendUpdatePacket();
                    }
                }
            }

            final List<Vector3d> points = strand.getPoints();
            final Vector3d middlePointPos = new Vector3d(points.get(points.size() / 2));

            if (returnItem) {
                final ItemStack stack = new ItemStack(FNItems.CONDUIT_CORE.get());

                if (player != null) {
                    if (!player.hasInfiniteMaterials() || !player.getInventory().contains(stack)) {
                        player.getInventory().placeItemBackInInventory(stack);
                    }
                } else {
                    if (conduitDropPos != null)
                        middlePointPos.set(conduitDropPos.x, conduitDropPos.y, conduitDropPos.z);

                    level.addFreshEntity(new ItemEntity(level, middlePointPos.x, middlePointPos.y, middlePointPos.z, stack));
                }
            }

            for (final Vector3d position : points) {
                level.playSound(null, position.x, position.y, position.z, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.5F, 1F);

                if (level instanceof final ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, Blocks.CHAIN.defaultBlockState()),
                        position.x, position.y, position.z,
                        10, 0.1, 0.1, 0.1, 0.1
                    );
                }
            }
        }

        if (this.ownedServerStrand != null) {
            this.removeServerStrand(level);
        }

        this.attachedConduitID = null;
        this.strandOwner = false;
        this.tile.markForSave();
        this.tile.sendUpdatePacket();
    }

    public Vec3 getAttachmentPoint() {
        return this.tile.getAttachmentPoint(this.tile.getBlockPos(), this.tile.getBlockState());
    }

    public Vec3 getVisualAttachmentPoint() {
        return this.tile.getVisualAttachmentPoint(this.tile.getBlockPos(), this.tile.getBlockState());
    }


    @Nullable
    public ServerConduitStrand getOwnedStrand() {
        return this.ownedServerStrand;
    }

    @Nullable
    public ServerConduitStrand getAttachedStrand() {
        final ServerLevelConduitManager manager = ServerLevelConduitManager.getOrCreate(this.getLevel());
        return (manager != null && this.attachedConduitID != null) ? manager.getStrand(this.attachedConduitID) : null;
    }

    private void removeClientStrand() {
        if (this.ownedClientStrand != null) {
            ClientLevelConduitManager.getOrCreate(this.getLevel()).removeStrand(this.ownedClientStrand.getUuid());
            this.ownedClientStrand = null;
        }
    }

    public void receiveClientStrand(final int interpolationTick, final List<Vector3d> incomingPoints, final UUID uuid, @Nullable final BlockPos startAttachmentPos, @Nullable final BlockPos endAttachmentPos) {
        if (this.ownedClientStrand == null) {
            this.ownedClientStrand = new ClientConduitStrand(uuid);
        }

        ClientLevelConduitManager.getOrCreate(this.getLevel())
            .addStrand(this.ownedClientStrand);

        final Vec3 startAttachmentPoint = this.getAttachmentPoint(startAttachmentPos);
        final Vec3 endAttachmentPoint = this.getAttachmentPoint(endAttachmentPos);

        if (startAttachmentPoint != null)
            this.ownedClientStrand.startAttachment = startAttachmentPoint;

        if (endAttachmentPoint != null)
            this.ownedClientStrand.endAttachment = endAttachmentPoint;

        final ObjectArrayList<ClientConduitPoint> points = this.ownedClientStrand.getPoints();
        this.ownedClientStrand.setStopped(false);

        while (points.size() < incomingPoints.size()) {
            final Vector3dc position = incomingPoints.get(incomingPoints.size() - points.size() - 1);
            points.addFirst(new ClientConduitPoint(new Vector3d(position), new Vector3d(position), new ObjectArrayList<>()));
        }

        while (points.size() > incomingPoints.size()) {
            points.removeFirst();
        }

        for (int i = 0; i < incomingPoints.size(); i++) {
            final Vector3d incomingPoint = incomingPoints.get(i);
            points.get(i)
                .snapshots()
                .add(new ClientConduitPoint.Snapshot(interpolationTick, incomingPoint));
        }
    }

    private Vec3 getAttachmentPoint(@Nullable final BlockPos attachment) {
        if (attachment == null) {
            return null;
        }

        final BlockEntity tile = this.getLevel().getBlockEntity(attachment);

        if (!(tile instanceof final AbstractConduitHolderBlockEntity smartTile))
            return null;

        final TileComponentConduitStrandHolder<?> holder = smartTile.getStrandHolder();

        if (holder == null)
            return null;

        return holder.getAttachmentPoint();
    }

    private Vec3 getVisualAttachmentPoint(@Nullable final BlockPos attachment) {
        if (attachment == null) {
            return null;
        }

        final BlockEntity tile = this.getLevel().getBlockEntity(attachment);

        if (!(tile instanceof final AbstractConduitHolderBlockEntity smartTile))
            return null;

        final TileComponentConduitStrandHolder<?> holder = smartTile.getStrandHolder();

        if (holder == null)
            return null;

        return holder.getVisualAttachmentPoint();
    }

    /**
     * Loads a strand into the level.
     */
    private void loadServerStrand(final CompoundTag tag) {
        final DataResult<Pair<ServerConduitStrand, Tag>> result = ServerConduitStrand.CODEC.decode(NbtOps.INSTANCE, tag);
        final ServerConduitStrand strand = result.getOrThrow().getFirst();

        this.ownedServerStrand = strand;
        this.queuedLevelAddition = true;
    }

    /**
     * Block destroyed or removed. Requires block to call ITE::onRemove
     */
    @Override
    public void removed() {
        final ServerConduitStrand attachedStrand = this.getAttachedStrand();

        final Level level = this.getLevel();
        if (level != null && level.isClientSide) {
            this.removeClientStrand();
        }

        final boolean tileDrops = level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS);

        if (!this.strandOwner && attachedStrand != null) {
            final ConduitAttachment startAttachment = attachedStrand.getAttachment(ConduitAttachmentPoint.START);
            final BlockPos blockAttachment = startAttachment.blockAttachment();
            final BlockEntity tile = Objects.requireNonNull(level).getBlockEntity(blockAttachment);

            if (tile instanceof final AbstractConduitHolderBlockEntity smartTile) {
                final TileComponentConduitStrandHolder<?> holder = smartTile.getStrandHolder();

                if (holder != null) {
                    holder.destroyConduit(null, this.getAttachmentPoint(), tileDrops);
                    return;
                }
            }
        }

        this.destroyConduit(null, this.getAttachmentPoint(), tileDrops);
    }

    public boolean isAttached() {
        return this.attachedConduitID != null;
    }

    public boolean ownsConduit() {
        return this.strandOwner;
    }

    /**
     * Detaches the holder from the conduit. Be wary of this, as references must not be entirely lost to the manager.
     */
    @ApiStatus.Internal
    public void detachConduit() {
        this.strandOwner = false;
        this.attachedConduitID = null;
        this.ownedServerStrand = null;
    }

    public ClientConduitStrand getClientStrand() {
        return this.ownedClientStrand;
    }

    public void giveFakeClientStrand(final ClientConduitStrand strand) {
        this.strandOwner = true;
        this.ownedClientStrand = strand;
        this.attachedConduitID = strand.getUuid();
    }

    public void giveFakeClientStrand(final UUID conduitUUID) {
        this.attachedConduitID = conduitUUID;
    }

    /**
     * CR-07: re-takes ownership of a strand after an assembly move. Restores the full ownership
     * identity (strand reference, owner flag and attached id) that detachConduit() wiped, so the
     * owner serializes the strand again and the START attachment is re-pinned by afterMove.
     */
    public void takeOwnedStrand(final ServerConduitStrand ownedStrand) {
        this.ownedServerStrand = ownedStrand;
        this.strandOwner = true;
        this.attachedConduitID = ownedStrand.getUUID();
    }

    public UUID getAttachedConduitId() {
        return this.attachedConduitID;
    }

    /**
     * CR-07: restores the attached-id of the non-owner end of a strand after an assembly move,
     * so getAttachedStrand() can resolve it again and afterMove re-pins the END attachment.
     */
    public void restoreAttachedConduitId(final UUID conduitId) {
        this.attachedConduitID = conduitId;
    }

    public void receiveClientStrandStopped() {
        if (this.ownedClientStrand != null) {
            this.ownedClientStrand.setStopped(true);
        }
    }
}
