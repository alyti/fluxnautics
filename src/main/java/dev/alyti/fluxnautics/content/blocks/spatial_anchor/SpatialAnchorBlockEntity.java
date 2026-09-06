package dev.alyti.fluxnautics.content.blocks.spatial_anchor;

import dev.alyti.fluxnautics.FluxnauticsConfig;
import dev.alyti.fluxnautics.capabilities.MassBasedEnergyContainer;
import dev.alyti.fluxnautics.registry.FNBlocks;
import dev.alyti.fluxnautics.util.NBTHelper;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import mekanism.api.*;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.container.sync.SyncableLong;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import mekanism.common.util.NBTUtils;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;

public class SpatialAnchorBlockEntity extends TileEntityConfigurableMachine implements BlockEntitySubLevelActor {
    protected Vector3d TICK_LINEAR = new Vector3d();
    protected Vector3d TICK_ANGULAR = new Vector3d();
    protected Long energyCost = 0L;
    protected Long energyUsed = 0L;
    private float prevScale;
    protected Vec3 matterPos = new Vec3(0, 0, 0);

    private FixedConstraintHandle handle;

    protected MassBasedEnergyContainer<SpatialAnchorBlockEntity> energyContainer;
    @WrappingComputerMethod(wrapper = SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper.class, methodNames = "getChargeItem", docPlaceholder = "charge slot")
    EnergyInventorySlot chargeSlot;
    @WrappingComputerMethod(wrapper = SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper.class, methodNames = "getDischargeItem", docPlaceholder = "discharge slot")
    EnergyInventorySlot dischargeSlot;

    public SpatialAnchorBlockEntity(BlockPos pos, BlockState state) {
        this(FNBlocks.SPATIAL_ANCHOR, pos, state);
    }

    public SpatialAnchorBlockEntity(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state);
        configComponent.setupIOConfig(TransmissionType.ITEM, chargeSlot, dischargeSlot, RelativeSide.FRONT, true).setCanEject(false);
        configComponent.setupIOConfig(TransmissionType.ENERGY, energyContainer, RelativeSide.FRONT);
        ejectorComponent = new TileComponentEjector(this, () -> Long.MAX_VALUE, false);
        ejectorComponent.setOutputData(configComponent, TransmissionType.ENERGY).setCanEject(type -> canFunction());
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = InventorySlotHelper.forSideWithConfig(this);
        builder.addSlot(dischargeSlot = EnergyInventorySlot.fillOrConvert(energyContainer, this::getLevel, listener, 8, 15));
        builder.addSlot(chargeSlot = EnergyInventorySlot.drain(energyContainer, listener, 8, 35));
        dischargeSlot.setSlotOverlay(SlotOverlay.MINUS);
        chargeSlot.setSlotOverlay(SlotOverlay.PLUS);
        return builder.build();
    }


    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSideWithConfig(this);
        builder.addContainer(energyContainer = new MassBasedEnergyContainer<>(this, listener, this::getEnergyPerMassPerTickMultiplier));
        return builder.build();
    }

    public MachineEnergyContainer<?> getEnergyContainer() {
        return energyContainer;
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdatePacket = super.onUpdateServer();
        chargeSlot.drainContainer();
        dischargeSlot.fillContainerOrConvert();
        long energyUsedNow = 0;
        this.setActive(this.canFunction());
        if (this.getActive()) {
            energyUsedNow = this.energyContainer.extract(energyCost, Action.EXECUTE, AutomationType.INTERNAL);
        }
        if (energyUsedNow != energyUsed || (ticker % (2 * SharedConstants.TICKS_PER_SECOND) == 0)) {
            energyUsed = energyUsedNow;
            sendUpdatePacket = true;
        }
        return sendUpdatePacket;
    }

    public boolean shouldFunction() {
        return super.canFunction();
    }

    @Override
    public boolean canFunction() {
        return shouldFunction() && (this.energyContainer.getEnergy() >= energyCost);
    }

    public long getEnergyPerMassPerTickMultiplier() {
        return ((Double) (FluxnauticsConfig.common.spatialAnchorJoulesPerMassMult.getOrDefault() * 500L)).longValue();
    }

    @Override
    public boolean supportsMode(RedstoneControl mode) {
        return true;
    }

    @Override
    public void sable$tick(final ServerSubLevel subLevel) {
        if (ticker % (2 * SharedConstants.TICKS_PER_SECOND) == 0) {
            double totalMass = 0;// massTracker.getMass();
            for (SubLevel subSubLevel : SubLevelHelper.getConnectedChain(subLevel)) {
                ServerSubLevel subLevel2 = (ServerSubLevel) subSubLevel;
                totalMass += subLevel2.getMassTracker().getMass();
            }
            this.energyContainer.updateMassConstant(totalMass);
        }

        if (this.getActive()) {
            if (handle == null) {
                toggleLock();
            } else {
                handle.getJointImpulses(TICK_LINEAR, TICK_ANGULAR);
                // final MassData massTracker = subLevel.getMassTracker();
                energyCost = ((Double) TICK_LINEAR.mul(energyContainer.getLastMass() * FluxnauticsConfig.common.spatialAnchorJoulesPerMassMult.getAsDouble()).length()).longValue();
                matterPos = JOMLConversion.toMojang(TICK_LINEAR.normalize(0.1).mul(-1));
            }
        } else {
            if (handle != null) {
                TICK_LINEAR.set(1);
                energyCost = ((Double) TICK_LINEAR.mul(energyContainer.getLastMass() * FluxnauticsConfig.common.spatialAnchorJoulesPerMassMult.getAsDouble()).length()).longValue();
                toggleLock();
            }
        }
    }

    @NotNull
    @Override
    public CompoundTag getReducedUpdateTag(@NotNull HolderLookup.Provider provider) {
        CompoundTag updateTag = super.getReducedUpdateTag(provider);
        updateTag.putFloat(SerializationConstants.SCALE, prevScale);
        updateTag.put("matterPos", NBTHelper.writeVec3(matterPos));
        return updateTag;
    }

    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.handleUpdateTag(tag, provider);
        NBTUtils.setFloatIfPresent(tag, SerializationConstants.SCALE, scale -> prevScale = scale);
        NBTUtils.setListIfPresent(tag, "matterPos", Tag.TAG_DOUBLE, matterPosList -> matterPos = NBTHelper.readVec3(matterPosList));
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableLong.create(this::getEnergyUsed, value -> energyUsed = value));
        container.track(SyncableLong.create(this::getEnergyCost, value -> energyCost = value));
    }

    public Vec3 getMatterPos() {
        return matterPos;
    }

    public float getEnergyScale() {
        return prevScale;
    }

    @ComputerMethod
    public long getEnergyUsed() {
        return energyUsed;
    }

    @ComputerMethod
    public long getEnergyCost() {
        return energyCost;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        this.remove();
    }

    public void remove() {
        if (this.level != null && !this.level.isClientSide && this.handle != null) {
            toggleLock();
        }
    }

    public void toggleLock() {
        if (handle != null) {
            handle.remove();
            handle = null;
            return;
        }
        final ServerSubLevelContainer container = (ServerSubLevelContainer) SubLevelContainer.getContainer(this.level);
        final ServerSubLevel subLevel = (ServerSubLevel) Sable.HELPER.getContaining(this);
        handle = addConstraint(container, subLevel);
    }


    private static FixedConstraintHandle addConstraint(final ServerSubLevelContainer container, final ServerSubLevel subLevel) {
        final SubLevelPhysicsSystem physicsSystem = container.physicsSystem();
        final PhysicsPipeline pipeline = physicsSystem.getPipeline();

        return pipeline.addConstraint(null, subLevel, new FixedConstraintConfiguration(
                subLevel.logicalPose().position(),
                subLevel.logicalPose().rotationPoint(),
                subLevel.logicalPose().orientation()
        ));
    }
}