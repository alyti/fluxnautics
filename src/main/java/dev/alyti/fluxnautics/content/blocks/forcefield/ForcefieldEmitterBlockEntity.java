package dev.alyti.fluxnautics.content.blocks.forcefield;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JavaOps;
import dev.alyti.fluxnautics.FluxnauticsConfig;
import dev.alyti.fluxnautics.api.datamaps.ForcefieldAbsorption;
import dev.alyti.fluxnautics.api.datamaps.IFNDataMapTypes;
import dev.alyti.fluxnautics.capabilities.MassBasedEnergyContainer;
import dev.alyti.fluxnautics.mixin_interface.mekanism.ILaserAccess;
import dev.alyti.fluxnautics.mixin_interface.minecraft.IClipContextExtension;
import dev.alyti.fluxnautics.registry.FNBlocks;
import dev.alyti.fluxnautics.registry.FNDataComponents;
import dev.alyti.fluxnautics.registry.FNDataMapTypes;
import dev.alyti.fluxnautics.registry.FNTags;
import dev.alyti.fluxnautics.util.NBTHelper;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.security.IBlockSecurityUtils;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.integration.computer.ComputerException;
import mekanism.common.integration.computer.Convertable;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.container.sync.SyncableDouble;
import mekanism.common.inventory.container.sync.SyncableItemStack;
import mekanism.common.inventory.container.sync.SyncableLong;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.lib.inventory.HashedItem;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.interfaces.IBoundingBlock;
import mekanism.common.tile.laser.TileEntityBasicLaser;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import mekanism.common.util.NBTUtils;
import net.minecraft.SharedConstants;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3i;

import java.util.*;

public class ForcefieldEmitterBlockEntity extends TileEntityConfigurableMachine implements BlockEntitySubLevelActor, IBoundingBlock {
    private static final double BOUNCE_MARGIN = 0.05;
    public static final int ROLLING_BUFFER_TICKS = 100; // 5 seconds at 20 tps
    public static final long COOLDOWN_TICKS = 20 * 10;

    final Vector3d globalCenter = new Vector3d();
    final Vector3d localCenter = new Vector3d();
    final Vector3d radii = new Vector3d();
    private final Vector3d invRadii = new Vector3d();

    final Quaterniond globalRotation = new Quaterniond();
    private final Quaterniond inverseRotation = new Quaterniond();

    private AABB globalSearchAABB;
    private final BoundingBox3d globalBoundingBox = new BoundingBox3d();
    private final Vector3i boundSize = new Vector3i();

    private final Quaterniond rotationOther = new Quaterniond();
    private final Vector3d centerOther = new Vector3d();
    private final Vector3i boundSizeOther = new Vector3i();
    private final Vector3d pointOther = new Vector3d();

    private final Vector3d tempVec1 = new Vector3d();
    private final Vector3d tempVec2 = new Vector3d();
    private final Vector3d tempVec3 = new Vector3d();

    public Vector3d clientCenter = new Vector3d();
    public Vector3d clientRadii = new Vector3d();

    // Client-side active impacts (clustered from server for visual sync)
    public final ObjectOpenHashSet<ImpactData> activeImpacts = new ObjectOpenHashSet<>();

    // Server-side raw impacts map for accurate cost calculation. Key is entityID or sublevelID.
    private final Object2ObjectOpenHashMap<ImpactKey, ImpactData> serverImpacts = new Object2ObjectOpenHashMap<>();

    private record ImpactKey(int entityID, UUID sublevelID) {}

    static final long IMPACT_LIFETIME = 2500;
    final ObjectOpenHashSet<SubLevel> connectedSubLevels = new ObjectOpenHashSet<>();
    private double totalMass;
    private boolean impactChanged = false;
    public long lastImpactTime;
    public float scanOffset;
    public long energyCost;
    public long energyUsed;
    public long rollingImpactCost;
    public long rollingImpactCostSum;
    public long rollingImpactThreshold;
    private double clientFieldVolume;
    private long cooldownTicksRemaining;
    private int raycastImpactsThisTick = 0;
    private long lastSableTickTime = 0;
    private long tickNowMillis = 0;
    private long cooldownTicksStart = 0;

    private final ArrayDeque<Long> impactCostHistory;

    public ForcefieldBubbleEmitterConfig config;

    protected MassBasedEnergyContainer<ForcefieldEmitterBlockEntity> energyContainer;
    @WrappingComputerMethod(wrapper = SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper.class, methodNames = "getChargeItem", docPlaceholder = "charge slot")
    EnergyInventorySlot chargeSlot;
    @WrappingComputerMethod(wrapper = SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper.class, methodNames = "getDischargeItem", docPlaceholder = "discharge slot")
    EnergyInventorySlot dischargeSlot;

    @WrappingComputerMethod(wrapper = SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper.class, methodNames = "getFrequencySlotItem", docPlaceholder = "frequency slot")
    InputInventorySlot frequencySlot;

    private HashedItem frequencyItem;

    public ForcefieldEmitterBlockEntity(BlockPos pos, BlockState blockState) {
        super(FNBlocks.FORCEFIELD_EMITTER, pos, blockState);
        configComponent.setupIOConfig(TransmissionType.ITEM, chargeSlot, dischargeSlot, RelativeSide.BOTTOM, true).setCanEject(false);
        configComponent.setupIOConfig(TransmissionType.ENERGY, energyContainer, RelativeSide.BOTTOM);
        ejectorComponent = new TileComponentEjector(this, () -> Long.MAX_VALUE, false);
        ejectorComponent.setOutputData(configComponent, TransmissionType.ENERGY).setCanEject(type -> canFunction());
        this.config = ForcefieldBubbleEmitterConfig.makeDefault(this);
        this.scanOffset = (float) (Math.random() * 1000);
        this.energyCost = this.calculateEnergyCost();
        this.frequencyItem = HashedItem.create(ItemStack.EMPTY);
        this.cooldownTicksRemaining = 0;
        this.clientRadii = new Vector3d(0.0D, 0.0D, 0.0D);
        this.impactCostHistory = new ArrayDeque<>(FluxnauticsConfig.common.forcefieldRollingImpactWindow.getOrDefault());
    }

    @Override
    public boolean isOffsetCapabilityDisabled(@NotNull BlockCapability<?, @Nullable Direction> capability, Direction side, @NotNull Vec3i offset) {
        if (offset.equals(Vec3i.ZERO)) {
            return false;
        }
        return offset.distToCenterSqr(0.5, 0.5, 0.5) != 1.0;
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = InventorySlotHelper.forSideWithConfig(this);
        builder.addSlot(dischargeSlot = EnergyInventorySlot.fillOrConvert(energyContainer, this::getLevel, listener, 8, 35));
        builder.addSlot(chargeSlot = EnergyInventorySlot.drain(energyContainer, listener, 8, 53));
        builder.addSlot(frequencySlot = InputInventorySlot.at((stack) -> {
            if (stack.isEmpty())
                return false;
            frequencyItem = HashedItem.create(stack.copy());
            markForSave();
            return true;
        }, (stack) -> true, listener, 8, 17));
        dischargeSlot.setSlotOverlay(SlotOverlay.MINUS);
        chargeSlot.setSlotOverlay(SlotOverlay.PLUS);
        frequencySlot.setSlotOverlay(SlotOverlay.FORMULA);
        return builder.build();
    }

    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSideWithConfig(this);
        builder.addContainer(energyContainer = new MassBasedEnergyContainer<>(this, listener, this::getEnergyPerMassPerTickMultiplier, this::getEnergyPerTickPassiveMultiplier));
        return builder.build();
    }

    public long getEnergyPerMassPerTickMultiplier() {
        double perMass = FluxnauticsConfig.common.forcefieldJoulePerMassPassiveMult.getOrDefault();
        return (long) (perMass * 100.0);
    }

    public long getEnergyPerTickPassiveMultiplier() {
        double passive = FluxnauticsConfig.common.forcefieldJoulePassiveMult.getOrDefault();
        return (long) (passive * getFieldVolume());
    }

    public MassBasedEnergyContainer<ForcefieldEmitterBlockEntity> getEnergyContainer() {
        return energyContainer;
    }

    @Override
    public void encodeExtraContainerData(RegistryFriendlyByteBuf buffer) {
        super.encodeExtraContainerData(buffer);
        ForcefieldBubbleEmitterConfig.STREAM_CODEC.encode(buffer, config);
    }


    public void setConfig(ForcefieldBubbleEmitterConfig config) {
        boolean dirtyConfig = false;
        if (config.fieldExpansion() != this.config.fieldExpansion()) {
            dirtyConfig = true;
        }
        this.config = config;
        this.config.setAllowEntries(this.config.getAllowEntries().resolveTags(BuiltInRegistries.ENTITY_TYPE));
        this.config.setDenyEntries(this.config.getDenyEntries().resolveTags(BuiltInRegistries.ENTITY_TYPE));

        if (dirtyConfig) {
            energyContainer.updateMaxEnergy();
        }

        this.impactChanged = true;
    }

    @Override
    protected boolean onUpdateServer() {
        tickNowMillis = System.currentTimeMillis();
        boolean sendUpdatePacket = super.onUpdateServer();
        chargeSlot.drainContainer();
        dischargeSlot.fillContainerOrConvert();

        if (this.level instanceof ServerLevel serverLevel && System.currentTimeMillis() - lastSableTickTime > 1000) {
            normalLevelTick();
            if (this.getActive()) {
                handleCollisions(null, serverLevel, 0.05);
            }
        }

        if (!radii.equals(clientRadii)) {
            clientRadii.set(radii);
            sendUpdatePacket = true;
        }

        tempVec1.set(localCenter).add(0.5, 0.5, 0.5);
        if (!tempVec1.equals(clientCenter)) {
            clientCenter.set(tempVec1);
            sendUpdatePacket = true;
        }

        cleanupImpacts();

        if (impactChanged || ticker % 5 == 0) {
            activeImpacts.clear();
            activeImpacts.addAll(getClusteredImpacts());
            sendUpdatePacket = true;
            impactChanged = false;
        }

        long tickImpactCost = 0;
        if (this.getActive()) {
            tickImpactCost = calculateTickImpactCost();
        }

        raycastImpactsThisTick = 0;

        impactCostHistory.addLast(tickImpactCost);
        rollingImpactCostSum += tickImpactCost;
        if (impactCostHistory.size() > FluxnauticsConfig.common.forcefieldRollingImpactWindow.getOrDefault()) {
            rollingImpactCostSum -= impactCostHistory.pollFirst();
        }

        if (rollingImpactCostSum != rollingImpactCost || ticker % 20 == 0) {
            rollingImpactCost = rollingImpactCostSum;
            sendUpdatePacket = true;
        }

        if (energyCost + tickImpactCost > this.energyContainer.getEnergy() && this.getActive()) {
            energyUsed = this.energyContainer.extract(energyCost + tickImpactCost, Action.EXECUTE, AutomationType.INTERNAL);
            this.cooldownTicksRemaining = FluxnauticsConfig.common.forcefieldCooldownTicks.getOrDefault();
            this.cooldownTicksStart = this.cooldownTicksRemaining;
            this.setActive(false);
            this.rollingImpactCostSum = 0;
            this.impactCostHistory.clear();
            return true;
        } else if (this.rollingImpactCost > this.getRollingImpactCooldownThreshold() && this.getActive()) {
            this.cooldownTicksRemaining = this.getRollingImpactCooldownTime();
            this.cooldownTicksStart = this.cooldownTicksRemaining;
            this.setActive(false);
            this.rollingImpactCostSum = 0;
            this.impactCostHistory.clear();
            return true;
        } else if (cooldownTicksRemaining > 0) {
            this.energyUsed = 0;
            cooldownTicksRemaining--;
            return true;
        }
        this.setActive(this.canFunction());

        long energyUsedNow = 0;
        if (this.getActive()) {
            energyUsedNow = this.energyContainer.extract(energyCost + tickImpactCost, Action.EXECUTE, AutomationType.INTERNAL);
        }

        if (energyUsedNow != energyUsed || (ticker % (2 * SharedConstants.TICKS_PER_SECOND) == 0)) {
            energyUsed = energyUsedNow;
            sendUpdatePacket = true;
        }
        return sendUpdatePacket;
    }

    private long calculateTickImpactCost() {
        double perImpactEntity = FluxnauticsConfig.common.forcefieldJoulePerImpactEntity.getOrDefault();
        double perImpactMass = FluxnauticsConfig.common.forcefieldJoulePerImpactMass.getOrDefault();
        double perImpactSpeed = FluxnauticsConfig.common.forcefieldImpactSpeedMult.getOrDefault();
        double totalCost = 0;

        for (ImpactData impact : serverImpacts.values()) {
            double ageSeconds = (tickNowMillis - impact.lastHitTime()) / 1000.0;

            double cost = 0;
            double freshness = Math.max(0.0, 1.0 - ageSeconds);
            if (impact.directCost != 0L) {
                cost = impact.directCost * freshness;
            } else {
                cost = perImpactEntity * freshness;
                cost += impact.mass() * perImpactMass * freshness;
                if (impact.speed > 0 && perImpactSpeed > 0) {
                    cost *= (1.0 + perImpactSpeed * impact.speed);
                }
            }

            totalCost += cost;
        }
        return (long) totalCost;
    }

    @ComputerMethod
    public long getEnergyCost() {
        return energyCost;
    }

    public long calculateEnergyCost() {
        if (this.level == null || this.isRemote())
            return energyCost;
        double passive_mult = FluxnauticsConfig.common.forcefieldJoulePassiveMult.getOrDefault();
        double per_mass_mult = FluxnauticsConfig.common.forcefieldJoulePerMassPassiveMult.getOrDefault();
        double mass = this.energyContainer.getLastMass();
        return (long) (getFieldVolume() * passive_mult + mass * per_mass_mult);
    }

    @ComputerMethod
    public long getEnergyUsed() {
        return energyUsed;
    }

    @ComputerMethod
    public long getRollingImpactCost() {
        return rollingImpactCost;
    }

    @ComputerMethod
    public long getRollingImpactCooldownThreshold() {
        if (this.level == null || this.isRemote())
            return rollingImpactThreshold;

        rollingImpactThreshold = (long) (energyContainer.getMaxEnergy() * FluxnauticsConfig.common.forcefieldRollingImpactThreshold.getOrDefault());
        return rollingImpactThreshold;
    }

    public long getRollingImpactCooldownTime() {
        return (long) (
            FluxnauticsConfig.common.forcefieldCooldownTicks.getOrDefault()
                + (Math.sqrt(this.getFieldVolume())
                * Math.log(this.rollingImpactCostSum) / 10)
                * FluxnauticsConfig.common.forcefieldOverloadCooldownMult.getOrDefault()
        );
    }

    @ComputerMethod
    public double getFieldVolume() {
        if (this.level == null || this.isRemote())
            return clientFieldVolume;
        double a = radii.x;
        double b = radii.y;
        double c = radii.z;
        if (a <= 0 || b <= 0 || c <= 0) {
            return 0.0;
        }
        return (4.0 / 3.0) * Math.PI * a * b * c;
    }

    @ComputerMethod
    public ItemStack getFrequencyItemStack() {
        if (this.frequencyItem == null)
            return ItemStack.EMPTY;
        return this.frequencyItem.createStack(1);
    }

    @ComputerMethod
    public void setFrequencyItemStack(ItemStack stack) {
        this.frequencyItem = HashedItem.create(stack);
        this.markForSave();
    }

    @ComputerMethod
    public long getCooldownTicks() {
        return cooldownTicksRemaining;
    }

    @ComputerMethod
    public long getMaxCooldownTicks() {
        return cooldownTicksStart;
    }

    @Override
    @ComputerMethod
    public boolean canFunction() {
        return super.canFunction() && (this.energyContainer.getEnergy() >= energyCost) && cooldownTicksRemaining <= 0;
    }

    @Override
    public CompoundTag getConfigurationData(HolderLookup.Provider provider, Player player) {
        CompoundTag data = super.getConfigurationData(provider, player);
        data.put("Config", ForcefieldBubbleEmitterConfig.CODEC.encodeStart(NbtOps.INSTANCE, this.config).getOrThrow());

        if (this.frequencyItem != null && !this.frequencyItem.isSameItemSameComponents(ItemStack.EMPTY)) {
            data.put("FI", this.frequencyItem.internalToNBT(provider));
        }
        return data;
    }

    @Override
    public void setConfigurationData(HolderLookup.Provider provider, Player player, CompoundTag data) {
        super.setConfigurationData(provider, player, data);
        NBTUtils.setCompoundIfPresent(data, "Config", (CompoundTag config) -> {
            this.config = ForcefieldBubbleEmitterConfig.CODEC.parse(NbtOps.INSTANCE, config).getOrThrow();
        });
        NBTUtils.setItemStackIfPresent(provider, data, "FI", (stack) -> this.frequencyItem = HashedItem.create(stack));
    }

    @Override
    protected void applyImplicitComponents(@NotNull BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        this.cooldownTicksRemaining = input.getOrDefault(FNDataComponents.COOLDOWN_TICKS, 0L);
        this.setConfig(input.getOrDefault(FNDataComponents.EMITTER_CONFIG, ForcefieldBubbleEmitterConfig.makeDefault(this)));
    }

    @Override
    protected void collectImplicitComponents(@NotNull DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(FNDataComponents.COOLDOWN_TICKS, cooldownTicksRemaining);
        builder.set(FNDataComponents.EMITTER_CONFIG, this.config);
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableLong.create(this::getEnergyUsed, value -> energyUsed = value));
        container.track(SyncableLong.create(this::getEnergyCost, value -> energyCost = value));
        container.track(SyncableLong.create(this::getRollingImpactCost, value -> rollingImpactCost = value));
        container.track(SyncableDouble.create(this::getFieldVolume, value -> clientFieldVolume = value));
        container.track(SyncableItemStack.create(this::getFrequencyItemStack, value -> frequencyItem = HashedItem.create(value)));
        container.track(SyncableLong.create(this::getCooldownTicks, value -> cooldownTicksRemaining = value));
        container.track(SyncableLong.create(this::getRollingImpactCooldownThreshold, value -> rollingImpactThreshold = value));
        container.track(SyncableLong.create(this::getMaxCooldownTicks, value -> cooldownTicksStart = value));
    }

    @NotNull
    @Override
    public CompoundTag getReducedUpdateTag(@NotNull HolderLookup.Provider provider) {
        CompoundTag updateTag = super.getReducedUpdateTag(provider);
        if (this.clientCenter != null) {
            updateTag.put("CenterOfMass", NBTHelper.writeVector3d(this.clientCenter));
        }
        if (this.clientRadii != null) {
            updateTag.put("Radii", NBTHelper.writeVector3d(this.clientRadii));
        }
        if (!this.activeImpacts.isEmpty()) {
            ListTag impactsList = new ListTag();
            for (ImpactData impactData : this.activeImpacts) {
                CompoundTag impactNbt = new CompoundTag();
                impactNbt.put("Pos", NBTHelper.writeVector3d(impactData.pos));
                impactNbt.putLong("Time", impactData.time());
                impactNbt.putLong("LastHit", impactData.lastHitTime());
                impactNbt.putDouble("Energy", impactData.strength);
                impactData.entityID.ifPresent(entityID -> impactNbt.putInt("EID", entityID));
                impactData.sublevelID.ifPresent(sid -> impactNbt.putUUID("SID", sid));
                impactsList.add(impactNbt);
            }
            updateTag.put("Impacts", impactsList);
            updateTag.putLong("LIT", lastImpactTime);
        }
        updateTag.putFloat("SO", scanOffset);
        if (this.config != null) {
            updateTag.put("Config", ForcefieldBubbleEmitterConfig.CODEC.encodeStart(NbtOps.INSTANCE, this.config).getOrThrow());
        }
        return updateTag;
    }

    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.handleUpdateTag(tag, provider);
        NBTUtils.setListIfPresent(tag, "CenterOfMass", Tag.TAG_DOUBLE, list -> NBTHelper.readVector3dc(list, clientCenter));
        NBTUtils.setListIfPresent(tag, "Radii", Tag.TAG_DOUBLE, list -> NBTHelper.readVector3dc(list, clientRadii));
        NBTUtils.setListIfPresent(tag, "Impacts", Tag.TAG_COMPOUND, list -> {
            this.activeImpacts.clear();
            for (Tag value : list) {
                CompoundTag impactNbt = (CompoundTag) value;
                ImpactData impactData = new ImpactData(
                    NBTHelper.readVector3d(impactNbt.getList("Pos", Tag.TAG_DOUBLE)),
                    impactNbt.getLong("Time"),
                    impactNbt.getDouble("Energy"),
                    0.0,
                    0.0,
                    0
                );
                if (impactNbt.contains("LastHit", Tag.TAG_LONG)) {
                    impactData.setLastHitTime(impactNbt.getLong("LastHit"));
                }
                NBTUtils.setIntIfPresent(impactNbt, "EID", impactData::setEntityID);
                NBTUtils.setUUIDIfPresent(impactNbt, "SID", impactData::setSublevelID);
                this.activeImpacts.add(impactData);
            }
        });
        NBTUtils.setFloatIfPresent(tag, "SO", (f) -> this.scanOffset = f);
        NBTUtils.setLongIfPresent(tag, "LIT", (f) -> this.lastImpactTime = f);

        NBTUtils.setCompoundIfPresent(tag, "Config", (CompoundTag config) -> {
            this.config = ForcefieldBubbleEmitterConfig.CODEC.parse(NbtOps.INSTANCE, config).getOrThrow();
        });
        if (this.config == null) {
            this.config = ForcefieldBubbleEmitterConfig.makeDefault(this);
        }
    }

    @Override
    public void writeSustainedData(HolderLookup.Provider provider, CompoundTag dataMap) {
        super.writeSustainedData(provider, dataMap);
        if (this.config != null) {
            dataMap.put("Config", ForcefieldBubbleEmitterConfig.CODEC.encodeStart(NbtOps.INSTANCE, this.config).getOrThrow());
        }
        if (this.frequencyItem != null && !this.frequencyItem.isSameItemSameComponents(ItemStack.EMPTY)) {
            dataMap.put("FI", this.frequencyItem.internalToNBT(provider));
        }
    }

    @Override
    public void readSustainedData(HolderLookup.Provider provider, @NotNull CompoundTag dataMap) {
        super.readSustainedData(provider, dataMap);
        NBTUtils.setCompoundIfPresent(dataMap, "Config", (CompoundTag config) -> {
            this.config = ForcefieldBubbleEmitterConfig.CODEC.parse(NbtOps.INSTANCE, config).getOrThrow();
        });
        if (this.config == null) {
            this.config = ForcefieldBubbleEmitterConfig.makeDefault(this);
        }
        NBTUtils.setItemStackIfPresent(provider, dataMap, "FI", (stack) -> this.frequencyItem = HashedItem.create(stack));
    }

    @Override
    public boolean supportsMode(RedstoneControl mode) {
        return true;
    }

    @Override
    public void sable$physicsTick(final ServerSubLevel serverSubLevel, final RigidBodyHandle handle, final double timeStep) {
        tickNowMillis = System.currentTimeMillis();
        if (!this.getActive() || this.isRemoved())
            return;
        this.handleCollisions(serverSubLevel, serverSubLevel.getLevel(), timeStep);
    }

    @Override
    public void sable$tick(final ServerSubLevel serverSubLevel) {
        tickNowMillis = System.currentTimeMillis();
        lastSableTickTime = tickNowMillis;
        this.energyContainer.updateMassConstant(serverSubLevel.getMassTracker().getMass());
        this.energyCost = calculateEnergyCost();

        if (!this.getActive())
            return;

        ForcefieldRegistry.register(serverSubLevel.getLevel(), this);

        connectedSubLevels.clear();
        totalMass = 0;
        for (SubLevel subSubLevel : SubLevelHelper.getConnectedChain(serverSubLevel)) {
            ServerSubLevel subLevel2 = (ServerSubLevel) subSubLevel;
            totalMass += subLevel2.getMassTracker().getMass();
            connectedSubLevels.add(subSubLevel);
        }

        serverSubLevel.getMassTracker().getCenterOfMass().get(localCenter);
        serverSubLevel.getPlot().getBoundingBox().size(boundSize);
        Pose3d pose = serverSubLevel.logicalPose();
        this.radii.set(boundSize).add(config.fieldExpansion(), config.fieldExpansion(), config.fieldExpansion()).div(2.0f).mul(Math.sqrt(3.0));
        this.invRadii.set(1.0 / radii.x, 1.0 / radii.y, 1.0 / radii.z);
        this.globalCenter.set(pose.position());
        this.globalRotation.set(pose.orientation());
        this.inverseRotation.set(this.globalRotation).invert();

        double[][] corners = {
            {this.radii.x, this.radii.y, this.radii.z}, {-this.radii.x, this.radii.y, this.radii.z},
            {this.radii.x, -this.radii.y, this.radii.z}, {this.radii.x, this.radii.y, -this.radii.z},
            {-this.radii.x, -this.radii.y, this.radii.z}, {-this.radii.x, this.radii.y, -this.radii.z},
            {this.radii.x, -this.radii.y, -this.radii.z}, {-this.radii.x, -this.radii.y, -this.radii.z}
        };

        globalBoundingBox.set(this.globalCenter.x, this.globalCenter.y, this.globalCenter.z,
            this.globalCenter.x + 1, this.globalCenter.y + 1, this.globalCenter.z + 1);
        for (double[] corner : corners) {
            tempVec1.set(corner[0], corner[1], corner[2]);
            this.globalRotation.transform(tempVec1);
            tempVec1.add(this.globalCenter);
            globalBoundingBox.expandTo(tempVec1);
        }

        this.globalSearchAABB = globalBoundingBox.toMojang().inflate(5.0);
    }

    private void normalLevelTick() {
        this.energyCost = calculateEnergyCost();
        if (!this.getActive() || this.isRemoved()) return;

        ForcefieldRegistry.register(this.getLevel(), this);

        connectedSubLevels.clear();
        this.energyContainer.updateMassConstant(1_000_000);
        totalMass = 1_000_000;

        // Default 9x9x9 box centered on the block
        localCenter.set(0, 0, 0);
        boundSize.set(9, 9, 9);

        this.globalCenter.set(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
        this.globalRotation.identity();
        this.inverseRotation.identity();

        updateFieldBounds();
    }

    private void updateFieldBounds() {
        this.radii.set(boundSize).add(config.fieldExpansion(), config.fieldExpansion(), config.fieldExpansion()).div(2.0f).mul(Math.sqrt(3.0));
        this.invRadii.set(1.0 / radii.x, 1.0 / radii.y, 1.0 / radii.z);

        double[][] corners = {
            {this.radii.x, this.radii.y, this.radii.z}, {-this.radii.x, this.radii.y, this.radii.z},
            {this.radii.x, -this.radii.y, this.radii.z}, {this.radii.x, this.radii.y, -this.radii.z},
            {-this.radii.x, -this.radii.y, this.radii.z}, {-this.radii.x, this.radii.y, -this.radii.z},
            {this.radii.x, -this.radii.y, -this.radii.z}, {-this.radii.x, -this.radii.y, -this.radii.z}
        };

        globalBoundingBox.set(this.globalCenter.x, this.globalCenter.y, this.globalCenter.z,
            this.globalCenter.x + 1, this.globalCenter.y + 1, this.globalCenter.z + 1);
        for (double[] corner : corners) {
            tempVec1.set(corner[0], corner[1], corner[2]);
            this.globalRotation.transform(tempVec1);
            tempVec1.add(this.globalCenter);
            globalBoundingBox.expandTo(tempVec1);
        }

        this.globalSearchAABB = globalBoundingBox.toMojang().inflate(5.0);
    }

    private InteractionResult checkEntity(final Entity entity) {
        if (entity instanceof Player p) {
            if (config.allowForeignPlayers())
                return InteractionResult.PASS;
            if (p.isCreative())
                return InteractionResult.PASS;
            if (p.getUUID().equals(this.getOwnerUUID()))
                return InteractionResult.PASS;
            if (IBlockSecurityUtils.INSTANCE.canAccess(p, level, this.worldPosition, this.getBlockState(), this))
                return InteractionResult.PASS;
            return InteractionResult.CONSUME;
        }

        Boolean filterResult = checkFilters(entity);
        if (filterResult != null) {
            return filterResult ? InteractionResult.PASS : InteractionResult.CONSUME;
        }

        return switch (entity) {
            case LivingEntity le -> le.getType().getCategory().isFriendly()
                ? (config.allowPassiveMobs() ? InteractionResult.PASS : InteractionResult.CONSUME)
                : (config.allowHostileMobs() ? InteractionResult.PASS : InteractionResult.CONSUME);
            case ItemEntity ignored -> InteractionResult.PASS;
            case Projectile ignored -> config.allowProjectiles() ? InteractionResult.PASS : InteractionResult.CONSUME;
            case FallingBlockEntity ignored -> config.allowProjectiles() ? InteractionResult.PASS : InteractionResult.CONSUME;
            default -> config.allowEverythingElse() ? InteractionResult.PASS : InteractionResult.CONSUME;
        };
    }

    private @Nullable Boolean checkFilters(Entity entity) {
        EntityType<?> holder = entity.getType();

        if (holder.is(FNTags.EntityTypes.FORCEFIELD_GLOBAL_BLOCK)) {
            return false;
        }
        if (holder.is(FNTags.EntityTypes.FORCEFIELD_GLOBAL_ALLOW)) {
            return true;
        }

        if (!config.getDenyEntries().isEmpty() && config.getDenyEntries().contains(BuiltInRegistries.ENTITY_TYPE, holder)) {
            return false;
        }
        if (!config.getAllowEntries().isEmpty() && config.getAllowEntries().contains(BuiltInRegistries.ENTITY_TYPE, holder)) {
            return true;
        }
        return null;
    }

    private final Vector3d reusablePrevPos = new Vector3d();
    private final Vector3d reusableCurrPos = new Vector3d();
    private final Vector3d reusableVelocity = new Vector3d();
    private final Vector3d reusablePredictedPos = new Vector3d();


    private void handleCollisions(@Nullable ServerSubLevel ownSubLevel, ServerLevel level, double timeStep) {
        List<Entity> nearbyEntities = level.getEntitiesOfClass(Entity.class, this.globalSearchAABB);

        for (Entity entity : nearbyEntities) {
            if (checkEntity(entity) == InteractionResult.PASS)
                continue;

            reusablePrevPos.set(entity.xo, entity.yo, entity.zo);
            reusableCurrPos.set(entity.getX(), entity.getY(), entity.getZ());
            Vec3 velocityVec = entity.getDeltaMovement();
            reusableVelocity.set(velocityVec.x, velocityVec.y, velocityVec.z);
            reusablePredictedPos.set(reusableCurrPos).add(reusableVelocity);

            Vector3d intersection = getEllipsoidIntersection(reusablePrevPos, reusablePredictedPos);
            boolean isInsideNow = isInsideEllipsoid(reusableCurrPos);

            if (intersection != null || isInsideNow) {
                Vector3d hitPoint = isInsideNow ? getClosestSurfacePointFromInside(reusableCurrPos) : intersection;
                handleEntityCollision(level, entity, hitPoint, reusableVelocity);
            }
        }

        if (config.allowForeignSublevels())
            return;
        for (SubLevel otherSubLevel : Sable.HELPER.getAllIntersecting(level, this.globalBoundingBox)) {
            if (ownSubLevel != null && (otherSubLevel.equals(ownSubLevel) || connectedSubLevels.contains(otherSubLevel))) {
                continue;
            }
            if (otherSubLevel instanceof final ServerSubLevel hitServerSubLevel) {
                if (ForcefieldHarmonizerBlockEntity.checkHarmony(hitServerSubLevel, this.frequencyItem.getInternalStack()) == InteractionResult.SUCCESS)
                    continue;
                Pose3d otherPose = hitServerSubLevel.logicalPose();
                centerOther.set(otherPose.position());
                rotationOther.set(otherPose.orientation());
                hitServerSubLevel.getPlot().getBoundingBox().size(boundSizeOther).div(2);

                double[] xs = {-boundSizeOther.x, boundSizeOther.x};
                double[] ys = {-boundSizeOther.y, boundSizeOther.y};
                double[] zs = {-boundSizeOther.z, boundSizeOther.z};

                for (double x : xs) {
                    for (double y : ys) {
                        for (double z : zs) {
                            pointOther.set(x, y, z);
                            rotationOther.transform(pointOther);
                            pointOther.add(centerOther);

                            if (isInsideEllipsoid(pointOther)) {
                                handleSublevelCollision(hitServerSubLevel, otherPose, pointOther, timeStep);
                            }
                        }
                    }
                }

                tempVec1.set(this.globalCenter).sub(centerOther);
                rotationOther.transformInverse(tempVec1);

                double closestX = Math.clamp(tempVec1.x, -boundSizeOther.x, boundSizeOther.x);
                double closestY = Math.clamp(tempVec1.y, -boundSizeOther.y, boundSizeOther.y);
                double closestZ = Math.clamp(tempVec1.z, -boundSizeOther.z, boundSizeOther.z);

                pointOther.set(closestX, closestY, closestZ);
                rotationOther.transform(pointOther);
                pointOther.add(centerOther);

                if (isInsideEllipsoid(pointOther)) {
                    handleSublevelCollision(hitServerSubLevel, otherPose, pointOther, timeStep);
                }
            }
        }
    }

    private static final double MAX_DELTA_V = 8.0;

    private void handleSublevelCollision(ServerSubLevel hitServerSubLevel, Pose3d otherPose, Vector3dc worldHitPoint, final double timeStep) {
        double mass = hitServerSubLevel.getMassTracker().getMass();
        if (mass <= 0) return;

        double finalMult = 1.0;
        if (totalMass >= 0 && mass >= totalMass) {
            finalMult = Math.clamp(totalMass / mass, 0.0, 1.0);
        }
        if (finalMult <= 0.05) {
            return;
        }

        Vector3d worldHitPointSurface = getClosestSurfacePointFromInside(worldHitPoint);
        if (Double.isNaN(worldHitPointSurface.y))
            return;

        Vector3d localUnitHit = toUnitSpace(worldHitPointSurface);
        localUnitHit.normalize();
        tempVec1.set(localUnitHit);
        this.globalRotation.transform(tempVec1);
        tempVec1.normalize();

        tempVec2.set(worldHitPointSurface).sub(worldHitPoint);
        double dist = tempVec2.length();

        if (FluxnauticsConfig.common.forcefieldTeleportThreshold.getOrDefault() > 0.0 && (dist > FluxnauticsConfig.common.forcefieldTeleportThreshold.getOrDefault()) && finalMult == 1.0) {
            final ServerSubLevelContainer container = (ServerSubLevelContainer) SubLevelContainer.getContainer(this.level);
            final SubLevelPhysicsSystem physicsSystem = container.physicsSystem();
            final PhysicsPipeline pipeline = physicsSystem.getPipeline();
            pipeline.teleport(hitServerSubLevel, hitServerSubLevel.logicalPose().position().add(tempVec2), hitServerSubLevel.logicalPose().orientation());
            return;
        }

        double correctionSpeed = Math.min(dist, 0.5);
        tempVec2.normalize().mul(correctionSpeed);

        Vector3d currentVelRaw = hitServerSubLevel.latestLinearVelocity;
        if (currentVelRaw == null) currentVelRaw = new Vector3d();

        double dot = currentVelRaw.dot(tempVec1);
        tempVec3.set(currentVelRaw);
        if (dot < 0) {
            tempVec3.sub(tempVec1.x * dot, tempVec1.y * dot, tempVec1.z * dot);
        }
        tempVec3.add(tempVec2);
        tempVec2.set(tempVec3).sub(currentVelRaw);
        if (tempVec2.lengthSquared() > MAX_DELTA_V * MAX_DELTA_V) {
            tempVec2.normalize().mul(MAX_DELTA_V);
        }

        Vector3d impulse = tempVec2.mul(mass).mul(finalMult);

        otherPose.transformNormalInverse(impulse);

        final ForceTotal forceTotal = hitServerSubLevel.getOrCreateQueuedForceGroup(ForceGroups.PROPULSION.get()).getForceTotal();
        forceTotal.applyImpulseAtPoint(hitServerSubLevel.getMassTracker(), hitServerSubLevel.getMassTracker().getCenterOfMass(), impulse);
        registerImpact(worldHitPoint, 1.08 - finalMult, mass, currentVelRaw.length(), 0, Optional.empty(), Optional.of(hitServerSubLevel.getUniqueId()));
    }

    private void handleEntityCollision(ServerLevel level, Entity entity, Vector3dc intersection, Vector3dc incomingVelocity) {
        double speed = incomingVelocity.length();
        double mass = entity.getBoundingBox().getXsize() * entity.getBoundingBox().getYsize() * entity.getBoundingBox().getZsize();

        ForcefieldAbsorption fa = IFNDataMapTypes.INSTANCE.getForcefieldAbsorption(level.registryAccess(), Holder.direct(entity.getType()));
        long cost = 0;
        if (fa != null) {
            if (fa.percentage()) {
                cost += (long) (this.rollingImpactThreshold * fa.absorption());
            } else {
                cost += (long) fa.absorption();
            }
            this.rollingImpactCostSum += cost;
            entity.kill();
        } else {
            Vector3d localUnitHit = toUnitSpace(intersection);
            localUnitHit.normalize();
            tempVec1.set(localUnitHit);
            this.globalRotation.transform(tempVec1);
            tempVec1.normalize();

            double dot = incomingVelocity.dot(tempVec1);

            if (dot < 0) {
                double reflectedX = incomingVelocity.x() - tempVec1.x * 2 * dot;
                double reflectedY = incomingVelocity.y() - tempVec1.y * 2 * dot;
                double reflectedZ = incomingVelocity.z() - tempVec1.z * 2 * dot;

                double restitution = 0.8;
                entity.setDeltaMovement(reflectedX * restitution, reflectedY * restitution, reflectedZ * restitution);
                entity.fallDistance = 0;
            }

            double dynamicMargin = Math.max(BOUNCE_MARGIN, speed * 0.1);

            entity.teleportTo(
                intersection.x() + tempVec1.x * dynamicMargin,
                intersection.y() + tempVec1.y * dynamicMargin,
                intersection.z() + tempVec1.z * dynamicMargin
            );
        }

        entity.hasImpulse = true;
        registerImpact(intersection, 0.05, mass, speed, cost, Optional.of(entity.getId()), Optional.empty());
    }

    private Vector3d getEllipsoidIntersection(Vector3dc start, Vector3dc end) {
        Vector3d uStart = toUnitSpace(start);
        Vector3d uEnd = toUnitSpace(end);
        Vector3d uDir = new Vector3d(uEnd).sub(uStart);

        double a = uDir.dot(uDir);
        if (a < 1e-10)
            return null;
        double b = 2.0 * uStart.dot(uDir);
        double c = uStart.dot(uStart) - 1.0;

        double discriminant = b * b - 4 * a * c;
        if (discriminant < 0) {
            return null;
        }

        double sqrtDisc = Math.sqrt(discriminant);
        double t1 = (-b - sqrtDisc) / (2 * a);
        double t2 = (-b + sqrtDisc) / (2 * a);

        double minT = Math.min(t1, t2);
        double maxT = Math.max(t1, t2);

        double t = -1;
        if (minT >= 0 && minT <= 1) {
            t = minT;
        } else if (maxT >= 0 && maxT <= 1) {
            t = maxT;
        }

        if (t >= 0) {
            return fromUnitSpace(new Vector3d(uStart).fma(t, uDir));
        }
        return null;
    }

    private Vector3d getClosestSurfacePointFromInside(Vector3dc insidePos) {
        Vector3d uInside = toUnitSpace(insidePos);
        uInside.normalize();
        return fromUnitSpace(uInside);
    }

    private Vector3d toUnitSpace(Vector3dc worldPos) {
        Vector3d result = new Vector3d(worldPos).sub(this.globalCenter);
        this.inverseRotation.transform(result);
        result.mul(this.invRadii.x, this.invRadii.y, this.invRadii.z);
        return result;
    }

    private Vector3d fromUnitSpace(Vector3dc unitPos) {
        Vector3d result = new Vector3d(unitPos).mul(this.radii);
        this.globalRotation.transform(result);
        result.add(this.globalCenter);
        return result;
    }

    private boolean isInsideEllipsoid(Vector3dc point) {
        Vector3d u = toUnitSpace(point);
        return u.dot(u) <= 1.0;
    }

    @Nullable
    public BlockHitResult checkRaycast(Vec3 startVec, Vec3 endVec) {
        if (!this.getActive() || this.isRemoved()) return null;
        if (this.radii.x <= 0 || this.radii.y <= 0 || this.radii.z <= 0) return null;
        if (this.globalSearchAABB == null) return null;

        Vector3d start = new Vector3d(startVec.x, startVec.y, startVec.z);
        Vector3d end = new Vector3d(endVec.x, endVec.y, endVec.z);

        if (isInsideEllipsoid(start)) return null;

        Vector3d intersection = getEllipsoidIntersection(start, end);
        if (intersection == null) return null;

        Vector3d localNormal = toUnitSpace(intersection);
        localNormal.mul(invRadii.x, invRadii.y, invRadii.z);
        globalRotation.transform(localNormal);
        localNormal.normalize();

        Direction direction = Direction.getNearest(
            (float) localNormal.x, (float) localNormal.y, (float) localNormal.z
        );

        return new BlockHitResult(
            new Vec3(intersection.x, intersection.y, intersection.z),
            direction,
            BlockPos.containing(intersection.x, intersection.y, intersection.z),
            false
        );
    }

    @Override
    public void setRemoved() {
        ForcefieldRegistry.unregister(this);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        ForcefieldRegistry.unregister(this);
        super.onChunkUnloaded();
    }

    private void registerImpact(Vector3dc pos, double energy, double mass, double speed, long directCost, Optional<Integer> entityID, Optional<UUID> sublevelID) {
        long now = tickNowMillis;
        int eid = entityID.orElse(-1);
        UUID sid = sublevelID.orElse(null);
        ImpactKey key = new ImpactKey(eid, sid);

        ImpactData imp = serverImpacts.get(key);
        if (imp == null) {
            imp = new ImpactData(new Vector3d(pos), now, energy, mass, speed, directCost, entityID, sublevelID);
            serverImpacts.put(key, imp);
        } else {
            imp.refresh(pos, energy, mass, speed, directCost, now);
        }
        lastImpactTime = now;
        impactChanged = true;
    }

    private void cleanupImpacts() {
        long now = tickNowMillis;
        serverImpacts.values().removeIf(impactData -> now - impactData.lastHitTime() > IMPACT_LIFETIME);
    }

    /**
     * Groups raw server impacts into spatial clusters to reduce visual sync bandwidth.
     */
    private List<ImpactData> getClusteredImpacts() {
        if (serverImpacts.isEmpty()) return Collections.emptyList();

        Long2ObjectOpenHashMap<List<ImpactData>> buckets = new Long2ObjectOpenHashMap<>();
        for (ImpactData imp : serverImpacts.values()) {
            long x = (long) Math.floor(imp.pos().x / 4.0);
            long y = (long) Math.floor(imp.pos().y / 4.0);
            long z = (long) Math.floor(imp.pos().z / 4.0);
            long key = (x & 0xFFFFF) | ((y & 0xFFFFF) << 20) | ((z & 0xFFFFF) << 40);
            buckets.computeIfAbsent(key, k -> new ArrayList<>()).add(imp);
        }

        List<ImpactData> clusters = new ArrayList<>(buckets.size());
        for (List<ImpactData> bucket : buckets.values()) {
            double sx = 0, sy = 0, sz = 0;
            double energy = 0, mass = 0, speed = 0;
            long latestHit = 0, directCost = 0;
            long earliestTime = Long.MAX_VALUE;
            for (ImpactData imp : bucket) {
                sx += imp.pos().x;
                sy += imp.pos().y;
                sz += imp.pos().z;
                energy += imp.energy();
                mass += imp.mass();
                speed = Math.max(speed, imp.speed);
                directCost += imp.directCost;
                latestHit = Math.max(latestHit, imp.lastHitTime());
                earliestTime = Math.min(earliestTime, imp.time());
            }
            int count = bucket.size();
            ImpactData cluster = new ImpactData(
                new Vector3d(sx / count, sy / count, sz / count),
                earliestTime,
                energy,
                mass,
                speed,
                directCost
            );
            cluster.setLastHitTime(latestHit);
            clusters.add(cluster);
        }
        return clusters;
    }

    public void applyRaycastCost(ClipContext clipContext, BlockHitResult hit) {
        double perRaycastMult = FluxnauticsConfig.common.forcefieldJoulePerRaycast.get();
        long cost = 0;
        if (clipContext instanceof IClipContextExtension ext) {
            cost = ext.fluxnautics$getEnergy();
        }

        if (cost == 0L) {
            Vec3 from = clipContext.getFrom();
            BlockEntity be = level.getBlockEntity(BlockPos.containing(from.subtract(0.5, 0.5, 0.5)));
            cost = costFromBE(be);

            if (cost == 0L) {
                Position from1 = from.subtract(-3,-3,-3);
                Position from2 = from.add(3,-3,3);
                ServerSubLevel sourceSubLevel = null;

                for (SubLevel sbl : Sable.HELPER.getAllIntersecting(this.level, new BoundingBox3d(from1, from2))) {
                    if (sbl instanceof ServerSubLevel) {
                        sourceSubLevel = (ServerSubLevel) sbl;
                        break;
                    }
                }

                if (sourceSubLevel != null) {
                    Vec3 sblPos = sourceSubLevel.logicalPose().transformPositionInverse(from);
                    be = level.getBlockEntity(BlockPos.containing(sblPos.subtract(0.5, 0.5, 0.5)));
                    cost = costFromBE(be);
                }
            }

        }

        if (cost != 0L) {
            this.rollingImpactCostSum += (long) (cost * FluxnauticsConfig.common.forcefieldLaserDirectMult.getOrDefault());
        }

        registerImpact(JOMLConversion.toJOML(hit.getLocation()), 0.1, perRaycastMult, 0, cost * 2, Optional.of(++raycastImpactsThisTick), Optional.empty());
    }

    private long costFromBE(BlockEntity be) {
        if (be == null) {
            return 0;
        } else if (be instanceof TileEntityBasicLaser laser) {
            return Math.min(laser.getEnergyContainer().getEnergy(), ((ILaserAccess) laser).fluxnautics$toFire());
        }
        return 0;
    }

    public static final class ImpactData {
        private Vector3d pos;
        private final long creationTime;
        private long lastHitTime;
        private double strength;
        private double mass;
        private double speed;
        private long directCost;

        private Optional<Integer> entityID;
        private Optional<UUID> sublevelID;

        public ImpactData(Vector3d pos, long time, double strength, double mass, double speed, long directCost) {
            this(pos, time, strength, mass, speed, directCost, Optional.empty(), Optional.empty());
        }

        public ImpactData(Vector3d pos, long time, double strength, double mass, double speed, long directCost, Optional<Integer> entityID, Optional<UUID> sublevelID) {
            this.pos = pos;
            this.creationTime = time;
            this.lastHitTime = time;
            this.strength = strength;
            this.mass = mass;
            this.speed = speed;
            this.directCost = directCost;
            this.entityID = entityID;
            this.sublevelID = sublevelID;
        }

        public Vector3d pos() {
            return pos;
        }

        public long time() {
            return creationTime;
        }

        public long lastHitTime() {
            return lastHitTime;
        }

        void setLastHitTime(long lastHitTime) {
            this.lastHitTime = lastHitTime;
        }

        public double energy() {
            return strength;
        }

        public double mass() {
            return mass;
        }

        public Optional<Integer> entityID() {
            return entityID;
        }

        public void setEntityID(int entityID) {
            this.entityID = Optional.of(entityID);
        }

        public Optional<UUID> sublevelID() {
            return sublevelID;
        }

        public void setSublevelID(UUID sublevelID) {
            this.sublevelID = Optional.of(sublevelID);
        }

        public void refresh(Vector3dc newPos, double newEnergy, double newMass, double newSpeed, long now, long dc) {
            this.pos.set(newPos);
            this.lastHitTime = now;
            this.strength = newEnergy;
            this.mass = newMass;
            this.speed = newSpeed;
            this.directCost = dc;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (ImpactData) obj;
            return Objects.equals(this.entityID, that.entityID) &&
                Objects.equals(this.sublevelID, that.sublevelID);
        }

        @Override
        public int hashCode() {
            return Objects.hash(entityID, sublevelID);
        }

        @Override
        public String toString() {
            return "ImpactData[" +
                "pos=" + pos + ", " +
                "creationTime=" + creationTime + ", " +
                "lastHitTime=" + lastHitTime + ", " +
                "strength=" + strength + ", " +
                "mass=" + mass + ", " +
                "speed=" + speed + ", " +
                "directCost=" + directCost + ", " +
                "entityID=" + entityID + ", " +
                "sublevelID=" + sublevelID + ']';
        }
    }

    @ComputerMethod
    public Convertable<ForcefieldBubbleEmitterConfig> getEmitterConfig() throws ComputerException {
        try {
            return Convertable.of(this.config, (o, x) -> {
                DataResult<Object> result =  ForcefieldBubbleEmitterConfig.CODEC.encode(x, JavaOps.INSTANCE, null);
                return result.getOrThrow();
            });
        } catch (IllegalStateException e) {
            throw new ComputerException("Invalid config: %s", e);
        }
    }

    @ComputerMethod
    public void setEmitterConfigFull(Map<?, ?> config) throws ComputerException {
        try {
            DataResult<Pair<ForcefieldBubbleEmitterConfig, Object>> result = ForcefieldBubbleEmitterConfig.CODEC.decode(JavaOps.INSTANCE, config);
            this.config = result.getOrThrow().getFirst();
            this.impactChanged = true;
        } catch (IllegalStateException e) {
            throw new ComputerException("Invalid config: %s", e);
        }
    }

    @ComputerMethod
    public void setEmitterConfigPartial(Map<?, ?> config) throws ComputerException {
        try {
            DataResult<Pair<ForcefieldBubbleEmitterConfig.PartialUpdate, Object>> result = ForcefieldBubbleEmitterConfig.PartialUpdate.CODEC.decode(JavaOps.INSTANCE, config);
            result.getOrThrow().getFirst().applyTo(this.config);
            this.impactChanged = true;
        } catch (IllegalStateException e) {
            throw new ComputerException("Invalid config: %s", e);
        }
    }
}