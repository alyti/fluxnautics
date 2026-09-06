package dev.alyti.fluxnautics.content.blocks.daemon_protocol;

import dev.alyti.fluxnautics.FluxnauticsConfig;
import dev.alyti.fluxnautics.content.entities.gear.EntityMekaRobit;
import dev.alyti.fluxnautics.index.ExtraSlotOverlays;
import dev.alyti.fluxnautics.registry.FNBlocks;
import dev.alyti.fluxnautics.registry.FNDataComponents;
import dev.alyti.fluxnautics.registry.FNEntityTypes;
import dev.alyti.fluxnautics.registry.FNItems;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.security.ISecurityUtils;
import mekanism.api.security.SecurityMode;
import mekanism.common.Mekanism;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.container.sync.SyncableLong;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import mekanism.common.util.EnumUtils;
import mekanism.common.util.NBTUtils;
import mekanism.common.util.StorageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public class DaemonProtocolBlockEntity extends TileEntityConfigurableMachine implements EntityMekaRobit.IMekaRobitDock {


    private MachineEnergyContainer<DaemonProtocolBlockEntity> energyContainer;

    @WrappingComputerMethod(wrapper = SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper.class, methodNames = "getDroneItem", docPlaceholder = "drone slot")
    BasicInventorySlot droneSlot;
    @WrappingComputerMethod(wrapper = SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper.class, methodNames = "getChargeItem", docPlaceholder = "charge slot")
    EnergyInventorySlot chargeSlot;
    @WrappingComputerMethod(wrapper = SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper.class, methodNames = "getDischargeItem", docPlaceholder = "discharge slot")
    EnergyInventorySlot dischargeSlot;

    private long usedEnergy = 0L;
    public EntityMekaRobit ownedEntity;

    public DaemonProtocolBlockEntity(BlockPos pos, BlockState state) {
        this(FNBlocks.DAEMON_PROTOCOL, pos, state);
    }

    public DaemonProtocolBlockEntity(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state);
        configComponent.setupIOConfig(TransmissionType.ITEM, chargeSlot, dischargeSlot, RelativeSide.FRONT, true).setCanEject(false);
        configComponent.setupIOConfig(TransmissionType.ITEM, droneSlot, RelativeSide.BOTTOM, true).setCanEject(true);
        configComponent.setupIOConfig(TransmissionType.ENERGY, energyContainer, RelativeSide.FRONT);
        ejectorComponent = new TileComponentEjector(this, () -> Long.MAX_VALUE, false);
        ejectorComponent.setOutputData(configComponent, TransmissionType.ENERGY).setCanEject(type -> canFunction());
        ejectorComponent.setOutputData(configComponent, TransmissionType.ITEM);
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = InventorySlotHelper.forSideWithConfig(this);
        builder.addSlot(dischargeSlot = EnergyInventorySlot.fillOrConvert(energyContainer, this::getLevel, listener, 17, 35));
        builder.addSlot(chargeSlot = EnergyInventorySlot.drain(energyContainer, listener, 143, 35));
        builder.addSlot(droneSlot = BasicInventorySlot.at((stack) -> stack.is(FNItems.MEKA_ROBIT), listener, 143, 50));
        dischargeSlot.setSlotOverlay(SlotOverlay.MINUS);
        chargeSlot.setSlotOverlay(SlotOverlay.PLUS);
        droneSlot.setSlotOverlay(ExtraSlotOverlays.MEKA_ROBIT_OVERLAY);
        return builder.build();
    }

    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSideWithConfig(this);
        builder.addContainer(energyContainer = MachineEnergyContainer.input(this, listener));
        return builder.build();
    }

    public static final int DEFAULT_PATROL_RANGE = 32;
    public static final int DEFAULT_PATROL_WAIT_TICKS = 10;
    public static final double DEFAULT_RECALL_DISTANCE = 64.0;

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdatePacket = super.onUpdateServer();
        chargeSlot.drainContainer();
        dischargeSlot.fillContainerOrConvert();
        if (level == null)
            return false;

        long clientEnergyUsed = 0L;
        if (canFunction() && !droneSlot.isEmpty()) {
            ItemStack stack = droneSlot.getStack();
            UUID boundId = stack.get(FNDataComponents.MEKA_ROBIT_BOUND_ENTITY);
            boolean droneBound = boundId != null;
            IEnergyContainer itemEnergy = StorageUtils.getEnergyContainer(stack, 0);
            if (itemEnergy != null) {
                long needed = itemEnergy.getNeeded();
                long canGive = energyContainer.extract(needed, Action.SIMULATE, AutomationType.INTERNAL);
                if (canGive >= 0) {
                    clientEnergyUsed = energyContainer.extract(canGive, Action.EXECUTE, AutomationType.INTERNAL);
                    itemEnergy.insert(clientEnergyUsed, Action.EXECUTE, AutomationType.MANUAL);
                }
            }
            if (!droneBound) {
                long energyPerTick = energyContainer.getEnergyPerTick();
                if (energyContainer.extract(energyPerTick, Action.SIMULATE, AutomationType.INTERNAL) == energyPerTick) {
                    clientEnergyUsed = energyContainer.extract(energyPerTick, Action.EXECUTE, AutomationType.INTERNAL);
                    EntityMekaRobit robit = FNEntityTypes.MEKA_ROBIT.get().create(level);
                    if (robit == null)
                        return false;

                    BlockPos pos = getBlockPos();
                    if (getOwnerUUID() != null)
                        robit.setOwnerUUID(getOwnerUUID());
                    robit.setHome(new GlobalPos(this.level.dimension(), this.getBlockPos()));
                    robit.moveTo(pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 0.0F, 0.0F);
                    stack.set(FNDataComponents.MEKA_ROBIT_BOUND_ENTITY.get(), robit.getUUID());
                    level.addFreshEntity(robit);
                    ownedEntity = robit;
                    sendUpdatePacket = true;
                }
            } else {
                Entity entity = ((ServerLevel) level).getEntity(boundId);
                if (entity instanceof EntityMekaRobit drone) {
                    BlockPos pos = getBlockPos();
                    double recallDistance = FluxnauticsConfig.common.daemonProtocolRecallDistance.getOrDefault();
                    double distanceSq = drone.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                    if (distanceSq > recallDistance * recallDistance) {
                        drone.teleportTo(pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5);
                    }
                } else {
                    stack.remove(FNDataComponents.MEKA_ROBIT_BOUND_ENTITY.get());
                    ownedEntity = null;
                    sendUpdatePacket = true;
                }
            }
        }
        usedEnergy = clientEnergyUsed;
        return sendUpdatePacket;
    }

    public MachineEnergyContainer<DaemonProtocolBlockEntity> getEnergyContainer() {
        return energyContainer;
    }

    public long usedEnergy() {
        return usedEnergy;
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableLong.create(this::usedEnergy, value -> usedEnergy = value));
    }

    @Override
    public ItemStack getDroneStack() {
        return this.droneSlot.getStack();
    }

    @Override
    public EntityMekaRobit.PatrolConfig getPatrolConfig() {
        return new EntityMekaRobit.PatrolConfig(false, null,
                FluxnauticsConfig.common.daemonProtocolPatrolRange.get(),
                FluxnauticsConfig.common.daemonProtocolPatrolWaitTicks.get());
    }

    @Override
    public boolean isEnemy(LivingEntity entity) {
        if (entity instanceof Player player)
            return !ISecurityUtils.INSTANCE.canAccessObject(player, this);
        return false;
    }

    @Override
    public GlobalPos getWanderCenter() {
        if (this.level == null)
            return null;
        return new GlobalPos(this.level.dimension(), this.getBlockPos());
    }


    @NotNull
    @Override
    public CompoundTag getReducedUpdateTag(@NotNull HolderLookup.Provider provider) {
        CompoundTag updateTag = super.getReducedUpdateTag(provider);
        if (ownedEntity != null)
            updateTag.putInt("entity", ownedEntity.getId());

        return updateTag;
    }

    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.handleUpdateTag(tag, provider);
        NBTUtils.setIntIfPresent(tag, "entity", (id) -> {
            this.ownedEntity = null;
            if (level.getEntity(id) instanceof EntityMekaRobit drone)
                this.ownedEntity = drone;
        });
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
    }
}
