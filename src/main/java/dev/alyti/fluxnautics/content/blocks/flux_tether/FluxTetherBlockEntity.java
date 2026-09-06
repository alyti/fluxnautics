package dev.alyti.fluxnautics.content.blocks.flux_tether;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JavaOps;
import dev.alyti.fluxnautics.FluxnauticsConfig;
import dev.alyti.fluxnautics.api.content.redstone.IRedstoneSideConfigurable;
import dev.alyti.fluxnautics.api.content.redstone.TileComponentRedstoneControl;
import dev.alyti.fluxnautics.api.content.tooltip.IHoverTooltip;
import dev.alyti.fluxnautics.content.blocks.flux_tether.link_block.FluxTetherLinkBlock;
import dev.alyti.fluxnautics.content.blocks.flux_tether.link_block.FluxTetherLinkBlockEntity;
import dev.alyti.fluxnautics.math.LerpedFloat;
import dev.alyti.fluxnautics.registry.FNBlocks;
import dev.alyti.fluxnautics.registry.FNDamageTypes;
import dev.alyti.fluxnautics.registry.FNDataComponents;
import dev.alyti.fluxnautics.registry.FNSounds;
import dev.alyti.fluxnautics.util.FluxAssemblyException;
import dev.alyti.fluxnautics.util.FluxAssemblyHelper;
import dev.alyti.fluxnautics.util.FluxLevelUtil;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintHandle;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import foundry.veil.api.network.VeilPacketManager;
import mekanism.api.*;
import mekanism.common.base.MekFakePlayer;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.config.MekanismConfig;
import mekanism.common.integration.computer.ComputerException;
import mekanism.common.integration.computer.Convertable;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.container.sync.SyncableDouble;
import mekanism.common.inventory.container.sync.SyncableLong;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.item.gear.ItemAtomicDisassembler;
import mekanism.common.lib.math.Pos3D;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.network.to_client.PacketHitBlockEffect;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import mekanism.common.util.NBTUtils;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.*;

public class FluxTetherBlockEntity extends TileEntityConfigurableMachine implements BlockEntitySubLevelActor, IHoverTooltip, IRedstoneSideConfigurable {

    public static double MIN_EXTENSION_BLOCKS = 1.25;
    public static double DISASSEMBLE_THRESHOLD = 4.0;
    public static double DISASSEMBLE_TOLERANCE = 4.0;
    public static double ENERGY_HOLD_FE_PER_SECOND       = 100.0;
    public static double ENERGY_WORK_FE_PER_MASS_BLOCK   = 25;
    public static double ENERGY_WORK_FE_PER_INERTIA_RAD  = 15;

    public static long BEAM_ENTITY_COST = 64_000L;
    public static float  BEAM_ENTITY_DAMAGE = 5.0f;
    public static double BEAM_RADIUS = 0.5;

    public static final long ENERGY_PER_TICK = 100L;

    private enum DisassembleState {
        IDLE,
        RETURNING,
        DISASSEMBLING
    }

    private final TileComponentRedstoneControl<FluxTetherBlockEntity, FluxTetherRedstoneControl> redstoneConfig;
    @Nullable
    private MachineEnergyContainer<FluxTetherBlockEntity> energyContainer;
    @WrappingComputerMethod(wrapper = SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper.class, methodNames = "getChargeItem", docPlaceholder = "charge slot")
    EnergyInventorySlot chargeSlot;
    @WrappingComputerMethod(wrapper = SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper.class, methodNames = "getDischargeItem", docPlaceholder = "discharge slot")
    EnergyInventorySlot dischargeSlot;

    public  boolean assembleNextTick;
    boolean assembling;

    @Nullable private UUID subLevelID;
    @Nullable public BlockPos plateWorldPos;
    @Nullable public BlockPos localPlatePos;
    @Nullable private GenericConstraintHandle handle;

    private long energyUsed;
    private long energyCost;
    private double clientMaxExtension;
    private double clientMinExtension;

    public FluxTetherConfig config = FluxTetherConfig.makeDefault();
    private final LerpedFloat animatedDistance = LerpedFloat.linear();
    private double targetMagneticDistance = 1.0;
    private double linearServoAccumulator = 0.0;
    private double angularServoAccumulator = 0.0;
    private double lastTargetAngleDegrees = 0.0;
    private double targetAngleDegrees     = 0.0;
    private double currentMagneticDistance = 1;
    private int beamTickCounter = 0;
    private BlockPos lastBeamTarget = null;
    private int beamWarmup = 0;
    private double lastBeamCheckDist = 1.0;
    private Pos3D lastBeamFrom;

    private double cachedTotalMass = 10.0, cachedTotalInertia = 10.0;
    private double billedDistance = 1.0, billedAngle = 0.0, energyCarry = 0.0;
    private boolean linearDriving = false, angularDriving = false;

    private DisassembleState disassembleState = DisassembleState.IDLE;

    @Nullable private FluxAssemblyException lastException;

    public FluxTetherBlockEntity(BlockPos pos, BlockState state) {
        super(FNBlocks.FLUX_TETHER, pos, state);
        configComponent.setupIOConfig(TransmissionType.ITEM, chargeSlot, dischargeSlot, RelativeSide.FRONT, true).setCanEject(false);
        configComponent.setupIOConfig(TransmissionType.ENERGY, energyContainer, RelativeSide.FRONT);
        ejectorComponent = new TileComponentEjector(this, () -> Long.MAX_VALUE, false);
        ejectorComponent.setOutputData(configComponent, TransmissionType.ENERGY).setCanEject(type -> canFunction());
        ejectorComponent.setOutputData(configComponent, TransmissionType.ITEM);
        this.redstoneConfig = new TileComponentRedstoneControl<>(this, FluxTetherRedstoneControl.class, (side) -> !side.equals(RelativeSide.FRONT));
        this.animatedDistance.startWithValue(1.0);
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
        builder.addContainer(energyContainer = MachineEnergyContainer.input(this, listener));
        return builder.build();
    }

    public MachineEnergyContainer<?> getEnergyContainer() {
        return energyContainer;
    }

    @Override
    public TileComponentRedstoneControl<FluxTetherBlockEntity, FluxTetherRedstoneControl> getRedstoneConfig() {
        return redstoneConfig;
    }


    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableLong.create(this::getEnergyUsed, value -> energyUsed = value));
        container.track(SyncableLong.create(this::getEnergyCost, value -> energyCost = value));
        container.track(SyncableDouble.create(this::getMinExtension, value -> clientMinExtension = value));
        container.track(SyncableDouble.create(this::getMaxExtension, value -> clientMaxExtension = value));
    }

    @Override
    public CompoundTag getConfigurationData(HolderLookup.Provider provider, Player player) {
        CompoundTag data = super.getConfigurationData(provider, player);
        data.put("Config", FluxTetherConfig.CODEC.encodeStart(NbtOps.INSTANCE, this.config).getOrThrow());
        this.redstoneConfig.write(data, provider);
        return data;
    }

    @Override
    public void setConfigurationData(HolderLookup.Provider provider, Player player, CompoundTag data) {
        super.setConfigurationData(provider, player, data);
        NBTUtils.setCompoundIfPresent(data, "Config", (CompoundTag config) -> {
            this.config = FluxTetherConfig.CODEC.parse(NbtOps.INSTANCE, config).getOrThrow();
        });
        this.redstoneConfig.read(data, provider);
    }

    @Override
    protected void applyImplicitComponents(@NotNull BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        this.setConfig(input.getOrDefault(FNDataComponents.FLUX_TETHER_CONFIG, FluxTetherConfig.makeDefault()));
    }

    @Override
    protected void collectImplicitComponents(@NotNull DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(FNDataComponents.FLUX_TETHER_CONFIG, this.config);
    }


    public long getStoredEnergy() {
        return this.energyContainer != null ? this.energyContainer.getEnergy() : 0L;
    }

    public long getEnergyUsed() {
        return this.energyUsed;
    }

    public long getEnergyCost() {
        return this.energyCost;
    }

    private long energyCostForTick() {
        double linearCost = 1.0 + Math.max(0.0, config.linearForceBoost() - 1.0) * 2.0;
        double angularCost = 1.0 + Math.max(0.0, config.angularForceBoost() - 1.0) * 2.0;
        return (long) (FluxnauticsConfig.common.fluxTetherEnergyPerTick.getOrDefault() * linearCost * angularCost);
    }

    private boolean tryConsumeEnergy() {
        if (this.energyContainer == null) return false;
        energyCost = energyCostForTick();
        energyUsed = this.energyContainer.extract(energyCost, Action.EXECUTE, AutomationType.INTERNAL);
        return energyUsed >= energyCost;
    }

    private boolean consumeMotionEnergy() {
        if (this.energyContainer == null) return false;
        final double dt = 1.0 / 20.0;
        double cost = 0.0;

        if (this.linearDriving || this.angularDriving
            || this.disassembleState == DisassembleState.RETURNING) {
            cost += FluxnauticsConfig.common.fluxTetherHoldEnergyPerSecond.getOrDefault() * dt
                * (1.0 + Math.max(0.0, config.linearForceBoost()  - 1.0) * 2.0)
                * (1.0 + Math.max(0.0, config.angularForceBoost() - 1.0) * 2.0);
        }

        final double actualDist = this.readActualDistance();
        final double dDist = Math.abs(actualDist - this.billedDistance);
        this.billedDistance = actualDist;
        if (this.linearDriving && dDist > 1.0e-4) {
            cost += FluxnauticsConfig.common.fluxTetherWorkEnergyPerMassBlock.getOrDefault() * this.cachedTotalMass * dDist * config.linearForceBoost();
        }

        if (this.angularDriving) {
            final double actualAngle = this.readActualAngle();
            double dAngle = Math.abs(actualAngle - this.billedAngle);
            if (dAngle > 180.0) dAngle = 360.0 - dAngle;
            this.billedAngle = actualAngle;
            if (dAngle > 1.0e-4) {
                cost += FluxnauticsConfig.common.fluxTetherWorkEnergyPerInertiaRad.getOrDefault() * this.cachedTotalInertia * Math.toRadians(dAngle) * config.angularForceBoost();
            }
        }

        final double billable = cost + this.energyCarry;
        final long whole = (long) billable;
        this.energyCarry = billable - whole;
        if (whole <= 0) return true;
        this.energyCost = whole;
        this.energyUsed = this.energyContainer.extract(whole, Action.EXECUTE, AutomationType.INTERNAL);
        return this.energyUsed >= whole;
    }

    public boolean hasPowerCached() {
        return this.energyContainer == null || this.energyContainer.getEnergy() > 0;
    }

    public void setConfig(FluxTetherConfig config) {
        this.config = config;
        setChanged();

        if (this.level == null || this.level.isClientSide)
            return;
        sendUpdatePacket();
        wakeUpAttached();
    }

    public void wakeUpAttached() {
        if (this.level == null || this.level.isClientSide)
            return;
        final ServerSubLevelContainer container = (ServerSubLevelContainer) SubLevelContainer.getContainer(this.level);
        final SubLevel other = this.getAttachedSubLevel();
        if (container != null && other instanceof ServerSubLevel ssl)
            container.physicsSystem().getPipeline().wakeUp(ssl);
    }

    public void applyPartialUpdate(FluxTetherConfig.PartialUpdate update) {
        update.applyTo(this.config);
        setChanged();
        sendUpdatePacket();
    }

    @Override
    public void onAdded() {
        super.onAdded();
        if (!level.isClientSide && !isAssembled() && plateWorldPos == null) {
            BlockPos platePos = getBlockPos().relative(getDirection());
            if (level.getBlockState(platePos).canBeReplaced()) {
                level.setBlockAndUpdate(platePos, FNBlocks.FLUX_TETHER_LINK.defaultState().setValue(BlockStateProperties.FACING, getDirection()));
            }
            if (level.getBlockEntity(platePos) instanceof FluxTetherLinkBlockEntity plate) {
                plate.setParent(this);
                this.plateWorldPos = platePos;
            }
        }
    }

    public Vector3d getCenter() {
        final BlockState state = this.getBlockState();
        final Direction facing = state.getValue(BlockStateProperties.FACING);
        final Vec3i facingVec = facing.getNormal();
        double scale = 0.5 - 4.0 / 16.0;
        return JOMLConversion.atCenterOf(this.worldPosition)
            .sub(facingVec.getX() * scale,
                facingVec.getY() * scale,
                facingVec.getZ() * scale);
    }

    @Override
    public void addLines(List<Component> list) {
        list.add(Component.literal("Linear: %s -> %,.3f".formatted(config.linearMode(), targetMagneticDistance)));
        list.add(Component.literal("Angular: %s -> %,.3f".formatted(config.angularMode(), targetAngleDegrees)));
        list.add(Component.literal("Boost: L=%,.2f A=%,.2f".formatted(config.linearForceBoost(), config.angularForceBoost())));
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdate = super.onUpdateServer();

        chargeSlot.drainContainer();
        dischargeSlot.fillContainerOrConvert();

        if (redstoneConfig.tickServer()) {
            setChanged();
            wakeUpAttached();
            sendUpdate = true;
        }

        if (this.assembleNextTick) {
            this.assembleNextTick = false;
            if (!this.isAssembled()) this.assemble();
            else                      this.disassemble();
            sendUpdate = true;
        }

        final ServerSubLevel attached = (ServerSubLevel) this.getAttachedSubLevel();

        updateTargetsFromConfig();

        if (this.isAssembled()) {
            double currentAnimated = this.animatedDistance.getValue();
            if (this.currentMagneticDistance != currentAnimated) {
                this.currentMagneticDistance = currentAnimated;
                sendUpdate = true;
            }
        }

        if (this.getSubLevelID() != null) {
            this.checkPersistence(this.getSubLevelID());
        }

        boolean needsEnergy = config.linearEngaged() || config.angularEngaged()
            || this.disassembleState == DisassembleState.RETURNING;
        if (this.isAssembled() && needsEnergy && consumeMotionEnergy()) {
            if (attached != null && this.handle != null) {
                final SubLevel containing = this.getContainingSubLevel();
                final PhysicsPipeline pipeline = ((ServerSubLevelContainer) SubLevelContainer.getContainer(this.level))
                    .physicsSystem().getPipeline();
                if (containing instanceof ServerSubLevel sslA) pipeline.wakeUp(sslA);
                if (attached    instanceof ServerSubLevel sslB) pipeline.wakeUp(sslB);
            }
            sendUpdate = true;
        }

        return sendUpdate;
    }

    private void updateTargetsFromConfig() {
        final double dt = 1.0 / 20.0;

        if (this.disassembleState == DisassembleState.RETURNING) {
            targetMagneticDistance = FluxnauticsConfig.common.fluxTetherDisassembleThreshold.getOrDefault();
        } else {
            switch (config.linearMode()) {
                case FREE -> {
                    targetMagneticDistance = animatedDistance.getValue();
                }
                case STATIC -> {
                    targetMagneticDistance = Math.clamp(config.linearTarget(),
                        getMinExtension(), getMaxExtension());
                }
                case SERVO -> {
                    linearServoAccumulator += config.linearServoRate() * dt;
                    double loop = config.linearServoLoop();
                    double phase = linearServoAccumulator * Math.PI / loop;
                    double sineValue = (loop / 2.0) * (1.0 - Math.cos(phase));
                    targetMagneticDistance = Math.clamp(sineValue,
                        getMinExtension(), getMaxExtension());
                    wakeUpAttached();
                }
            }
        }

        switch (config.angularMode()) {
            case FREE -> {
                targetAngleDegrees = 0.0;
            }
            case STATIC -> {
                targetAngleDegrees = config.angularTarget();
            }
            case SERVO -> {
                angularServoAccumulator += config.angularServoRate() * dt;
                angularServoAccumulator = ((angularServoAccumulator % 360.0) + 360.0) % 360.0;
                if (angularServoAccumulator >= 180.0) {
                    targetAngleDegrees = angularServoAccumulator - 360.0;
                } else {
                    targetAngleDegrees = angularServoAccumulator;
                }
                wakeUpAttached();
            }
        }
    }

    public void assemble() {
        final BlockPos pos        = this.getBlockPos();
        final Direction facing    = this.getBlockState().getValue(BlockStateProperties.FACING);

        if (this.plateWorldPos == null) {
            this.plateWorldPos = pos.relative(facing);
            if (level.getBlockState(this.plateWorldPos).canBeReplaced()) {
                level.setBlockAndUpdate(this.plateWorldPos, FNBlocks.FLUX_TETHER_LINK.defaultState().setValue(BlockStateProperties.FACING, facing));
            }
        }

        final BlockPos toAssemble = this.plateWorldPos;
        final FluxAssemblyHelper.AssemblyResult result;
        try {
            result = FluxAssemblyHelper.assembleFromSingleBlock(this.level, pos, toAssemble, false, false);
            this.lastException = null;
        } catch (FluxAssemblyException e) {
            this.lastException = e;
            this.sendUpdatePacket();
            return;
        }
        this.sendUpdatePacket();

        final BlockState link = FNBlocks.FLUX_TETHER_LINK.defaultState().setValue(BlockStateProperties.FACING, facing);
        final ServerSubLevel assembledSubLevel;
        final BlockPos localPlatePos;

        if (result != null) {
            assembledSubLevel = (ServerSubLevel) result.subLevel();
            localPlatePos = toAssemble.offset(result.offset());
        } else {
            final ServerSubLevelContainer container = (ServerSubLevelContainer) SubLevelContainer.getContainer(this.level);
            final Pose3d pose = new Pose3d();
            pose.position().set(toAssemble.getX() + 0.5, toAssemble.getY() + 0.5, toAssemble.getZ() + 0.5);
            assembledSubLevel = (ServerSubLevel) container.allocateNewSubLevel(pose);
            final LevelPlot plot = assembledSubLevel.getPlot();
            final ChunkPos center = plot.getCenterChunk();
            plot.newEmptyChunk(center);

            final BlockPos plotCenter = plot.getCenterBlock();
            plot.getEmbeddedLevelAccessor().setBlock(plotCenter, link, 3);
            localPlatePos = plotCenter;

            final BlockPos plotAnchor = plot.getCenterBlock();
            final Vector3dc centerOfMass = assembledSubLevel.getMassTracker().getCenterOfMass();
            final Vector3d subLevelCenter = JOMLConversion.atLowerCornerOf(toAssemble);
            if (centerOfMass != null) {
                subLevelCenter.add(centerOfMass.x() - plotAnchor.getX(),
                    centerOfMass.y() - plotAnchor.getY(),
                    centerOfMass.z() - plotAnchor.getZ());
            } else {
                assembledSubLevel.logicalPose().rotationPoint()
                    .set(plotAnchor.getX() + 0.5, plotAnchor.getY() + 0.5, plotAnchor.getZ() + 0.5);
            }
            assembledSubLevel.logicalPose().position().set(subLevelCenter.x, subLevelCenter.y, subLevelCenter.z);
            final SubLevelPhysicsSystem physicsSystem = ((ServerSubLevelContainer) SubLevelContainer.getContainer(level)).physicsSystem();
            final PhysicsPipeline pipeline = physicsSystem.getPipeline();
            final SubLevel containingSubLevel = this.getContainingSubLevel();
            if (containingSubLevel != null) {
                SubLevelAssemblyHelper.kickFromContainingSubLevel((ServerLevel) this.level, physicsSystem, pipeline,
                    assembledSubLevel, containingSubLevel);
                assembledSubLevel.logicalPose().orientation().set(containingSubLevel.logicalPose().orientation());
            }
            pipeline.teleport(assembledSubLevel, assembledSubLevel.logicalPose().position(), assembledSubLevel.logicalPose().orientation());
            assembledSubLevel.updateLastPose();
        }

        this.level.playSound(null, pos, FNSounds.FLUX_TETHER_ASSEMBLE.get(), SoundSource.BLOCKS, 1.0f, 1.0f);

        this.setSubLevelID(assembledSubLevel.getUniqueId());
        this.localPlatePos = localPlatePos;
        this.plateWorldPos = null;
        this.currentMagneticDistance = 1;
        this.animatedDistance.startWithValue(1.0);
        this.disassembleState = DisassembleState.IDLE;

        BlockEntity be = assembledSubLevel.getLevel().getBlockEntity(localPlatePos);
        if (be instanceof FluxTetherLinkBlockEntity plateBE) {
            plateBE.setParent(this);
        }

        this.getLevel().setBlockAndUpdate(pos, this.getBlockState().setValue(FluxTetherBlock.ASSEMBLED, true));
        this.attachConstraints(assembledSubLevel);
    }

    public void disassemble() {
        if (this.isRemoved())
            return;
        if (this.disassembleState != DisassembleState.IDLE)
            return;
        if (this.currentMagneticDistance > FluxnauticsConfig.common.fluxTetherDisassembleThreshold.getOrDefault() + DISASSEMBLE_TOLERANCE) {
            this.disassembleState = DisassembleState.RETURNING;
            return;
        }
        completeDisassemble();
    }

    private void completeDisassemble() {
        if (this.isRemoved())
            return;
        if (this.disassembleState == DisassembleState.DISASSEMBLING)
            return;
        this.disassembleState = DisassembleState.DISASSEMBLING;
        this.removeHandle();
        this.disassembleState = DisassembleState.IDLE;

        final SubLevel subLevel = SubLevelContainer.getContainer(this.level).getSubLevel(this.getSubLevelID());
        if (subLevel != null) {
            BlockPos returnPlatePos = this.getBlockPos().relative(this.getDirection(), 1);

            if (subLevel instanceof ServerSubLevel ssl) {
                this.breakObstructedBlocks(ssl);

                try {
                    final Direction dir = this.getDirection();
                    final Vector3d dirLocal = new Vector3d(dir.step().x(), dir.step().y(), dir.step().z());
                    final Vector3d dirWorld;
                    final SubLevel containingSub = this.getContainingSubLevel();
                    if (containingSub != null) {
                        dirWorld = new Quaterniond(containingSub.logicalPose().orientation()).transform(dirLocal, new Vector3d());
                    } else {
                        dirWorld = new Vector3d(dirLocal);
                    }
                    final double delta = getMinExtension() - this.currentMagneticDistance;
                    final Vector3d offsetWorld = new Vector3d(dirWorld).mul(delta);
                    final Vector3d returnOrigin = new Vector3d(ssl.logicalPose().position()).add(offsetWorld);

                    final PhysicsPipeline pipeline = ((ServerSubLevelContainer) SubLevelContainer.getContainer(level))
                        .physicsSystem().getPipeline();
                    pipeline.teleport(ssl, returnOrigin, ssl.logicalPose().orientation());
                    ssl.updateLastPose();
                } catch (Exception e) {
                    LogUtils.getLogger().error("QuantumLock teleport-back failed at {}, force-removing sublevel {}",
                        this.getBlockPos(), subLevel.getUniqueId(), e);
                    if (!subLevel.isRemoved()) {
                        SubLevelContainer.getContainer(this.level).removeSubLevel(subLevel, SubLevelRemovalReason.REMOVED);
                    }
                }
            }

            if (!subLevel.isRemoved()) {
                try {
                    FluxAssemblyHelper.disassembleSubLevel((ServerLevel) this.level, subLevel, this.localPlatePos,
                        returnPlatePos, Rotation.NONE);
                } catch (Exception e) {
                    LogUtils.getLogger().error("QuantumLock disassemble failed at {}, force-removing sublevel {}",
                        this.getBlockPos(), subLevel.getUniqueId(), e);
                    if (!subLevel.isRemoved()) {
                        SubLevelContainer.getContainer(this.level).removeSubLevel(subLevel, SubLevelRemovalReason.REMOVED);
                    }
                }
            }

            this.plateWorldPos = returnPlatePos;
            if (level.getBlockEntity(returnPlatePos) instanceof FluxTetherLinkBlockEntity plate) {
                plate.setParent(this);
            }
            this.level.playSound(null, this.getBlockPos(), FNSounds.FLUX_TETHER_DISASSEMBLE.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        }

        this.getLevel().setBlockAndUpdate(this.getBlockPos(),
            this.getBlockState().setValue(FluxTetherBlock.ASSEMBLED, false));
        this.setSubLevelID(null);
        this.localPlatePos = null;
        this.targetAngleDegrees = 0;
        this.currentMagneticDistance = 1;
        this.animatedDistance.startWithValue(1.0);
        this.disassembleState = DisassembleState.IDLE;
        this.sendUpdatePacket();
    }

    private void breakObstructedBlocks(ServerSubLevel ssl) {
        if (this.localPlatePos == null) return;

        final Direction facing = this.getDirection();
        final BlockPos bearingPos = this.getBlockPos();
        final BlockPos localPlatePos = this.localPlatePos;
        final Level bodyBLevel = ssl.getLevel();
        final LevelPlot plot = ssl.getPlot();

        final int slabDepth = (int) Math.ceil(Math.min(this.currentMagneticDistance, FluxnauticsConfig.common.fluxTetherDisassembleThreshold.getOrDefault()) - 1);
        if (slabDepth <= 0) return;

        final Direction.Axis axis = facing.getAxis();
        final int plateCoord = switch (axis) {
            case X -> localPlatePos.getX();
            case Y -> localPlatePos.getY();
            case Z -> localPlatePos.getZ();
        };
        final int towardBearingStep = switch (axis) {
            case X -> -facing.getStepX();
            case Y -> -facing.getStepY();
            case Z -> -facing.getStepZ();
        };

        for (final PlotChunkHolder chunk : plot.getLoadedChunks()) {
            final BoundingBox3ic chunkBounds = chunk.getBoundingBox();
            if (chunkBounds == null || chunkBounds == BoundingBox3i.EMPTY) continue;

            final int chunkMinX = chunkBounds.minX() + chunk.getPos().getMinBlockX();
            final int chunkMaxX = chunkBounds.maxX() + chunk.getPos().getMinBlockX();
            final int chunkMinY = chunkBounds.minY();
            final int chunkMaxY = chunkBounds.maxY();
            final int chunkMinZ = chunkBounds.minZ() + chunk.getPos().getMinBlockZ();
            final int chunkMaxZ = chunkBounds.maxZ() + chunk.getPos().getMinBlockZ();

            for (int depth = 1; depth <= slabDepth; depth++) {
                final int layerCoord = plateCoord + depth * towardBearingStep;

                if (axis == Direction.Axis.X) {
                    if (layerCoord < chunkMinX || layerCoord > chunkMaxX) continue;
                    for (int y = chunkMinY; y <= chunkMaxY; y++) {
                        for (int z = chunkMinZ; z <= chunkMaxZ; z++) {
                            tryBreakObstructionAt(new BlockPos(layerCoord, y, z), localPlatePos, bearingPos, facing, bodyBLevel);
                        }
                    }
                } else if (axis == Direction.Axis.Y) {
                    if (layerCoord < chunkMinY || layerCoord > chunkMaxY) continue;
                    for (int x = chunkMinX; x <= chunkMaxX; x++) {
                        for (int z = chunkMinZ; z <= chunkMaxZ; z++) {
                            tryBreakObstructionAt(new BlockPos(x, layerCoord, z), localPlatePos, bearingPos, facing, bodyBLevel);
                        }
                    }
                } else {
                    if (layerCoord < chunkMinZ || layerCoord > chunkMaxZ) continue;
                    for (int x = chunkMinX; x <= chunkMaxX; x++) {
                        for (int y = chunkMinY; y <= chunkMaxY; y++) {
                            tryBreakObstructionAt(new BlockPos(x, y, layerCoord), localPlatePos, bearingPos, facing, bodyBLevel);
                        }
                    }
                }
            }
        }
    }

    private void tryBreakObstructionAt(BlockPos localPos, BlockPos localPlatePos, BlockPos bearingPos,
                                       Direction facing, Level bodyBLevel) {
        if (localPos.equals(localPlatePos)) return;

        final BlockState bodyBState = bodyBLevel.getBlockState(localPos);
        if (bodyBState.isAir()) return;

        final BlockPos target = bearingPos.relative(facing).offset(localPos.subtract(localPlatePos));

        final BlockState bodyAState = this.level.getBlockState(target);
        if (!bodyAState.isAir() && !bodyAState.canBeReplaced()) {
            bodyBLevel.destroyBlock(localPos, true);
        }
    }

    private void applyBeamEffects() {
        if (this.handle == null || this.level == null || this.energyContainer == null || this.localPlatePos == null) return;
        final Direction facing = this.getDirection();

        final double currentDist = readActualDistance();
        if (currentDist < 1.5) { this.resetBeam(); return; }

        Pos3D from;
        if (lastBeamFrom != null) {
            from = lastBeamFrom;
            lastBeamFrom = null;
        } else
            from = Pos3D.create(this).centre().translate(facing, 0.501);
        Pos3D to = from.translate(facing, currentDist - 0.75);
        BlockHitResult result = clipLaserInBeamSpace(level, new ClipContext(from, to,
            ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, CollisionContext.empty()));

        Player owner = null;
        if (getOwnerUUID() instanceof UUID id)
            owner = level.getPlayerByUUID(id);

        if (result.getType() == HitResult.Type.BLOCK) {
            final BlockPos target = result.getBlockPos();
            final BlockState state = this.level.getBlockState(target);
            float hardness = state.getDestroySpeed(this.level, target);
            if (!state.isAir() && !state.canBeReplaced()
                && hardness > 0
                && !state.is(FNBlocks.FLUX_TETHER) && !state.is(FNBlocks.FLUX_TETHER_LINK)) {

                final long cost = (long) (hardness * MekanismConfig.general.laserEnergyPerHardness.get());

                if (!target.equals(this.lastBeamTarget)) { this.lastBeamTarget = target; this.beamWarmup = 0; }

                final long extracted = this.energyContainer.extract(cost, Action.EXECUTE, AutomationType.INTERNAL);
                if (extracted >= cost) {
                    this.withFakePlayer((ServerLevel) level, to.x(), to.y(), to.z(), target, state, result.getDirection());
                    this.resetBeam();
                } else {
                    VeilPacketManager.tracking((ServerLevel) level, target).sendPacket(new PacketHitBlockEffect(result));
                }
            } else this.resetBeam();
        } else this.resetBeam();

        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
            getLaserBox(facing, from, to, (float) FluxnauticsConfig.common.fluxTetherBeamRadius.getOrDefault()))) {
            final long beamEntityCost = FluxnauticsConfig.common.fluxTetherBeamEntityCost.getOrDefault();
            final long extracted = this.energyContainer.extract(beamEntityCost, Action.EXECUTE, AutomationType.INTERNAL);
            if (extracted < beamEntityCost) return;
            entity.hurt(owner == null ? FNDamageTypes.TEMPORAL.source(level) :level.damageSources().source(FNDamageTypes.TEMPORAL.key(), owner), (float) FluxnauticsConfig.common.fluxTetherBeamEntityDamage.getOrDefault());
        }
    }

    private void withFakePlayer(ServerLevel level, double x, double y, double z, BlockPos hitPos, BlockState hitState, Direction hitSide) {
        MekFakePlayer dummy = MekFakePlayer.setupFakePlayer(level, x, y, z);
        dummy.setEmulatingUUID(getOwnerUUID());//pretend to be the owner
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, hitPos, hitState, dummy);
        if (!NeoForge.EVENT_BUS.post(event).isCanceled()) {
            if (hitState.getBlock() instanceof TntBlock && hitState.isFlammable(level, hitPos, hitSide)) {
                //Convert TNT that can be lit on fire into a tnt entity
                //Note: We don't mark the fake player as the igniter as then when the tnt explodes if it hits a player
                // there will be a crash as our fake player's level will be null
                hitState.onCaughtFire(level, hitPos, hitSide, null);
                level.removeBlock(hitPos, false);
            } else {
                handleBreakBlock(hitState, level, hitPos, dummy, ItemAtomicDisassembler.fullyChargedStack());
            }
        }
        dummy.cleanupFakePlayer(level);
    }

    protected void handleBreakBlock(BlockState state, ServerLevel level, BlockPos hitPos, Player player, ItemStack tool) {
        for (ItemEntity drop : WorldUtils.getDrops(state, level, hitPos, WorldUtils.getTileEntity(level, hitPos), player, tool, true)) {
            if (!drop.getItem().isEmpty()) {
                level.addFreshEntity(drop);
            }
        }
        breakBlock(state, level, hitPos, tool);
    }

    protected final void breakBlock(BlockState state, ServerLevel level, BlockPos hitPos, ItemStack tool) {
        state.spawnAfterBreak(level, hitPos, tool, false);
        level.removeBlock(hitPos, false);
        level.gameEvent(GameEvent.BLOCK_DESTROY, hitPos, GameEvent.Context.of(null, state));
        level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, hitPos, Block.getId(state));
        level.playSound(null, hitPos, FNSounds.FLUX_TETHER_BEAM_TELEGRAPH.get(), SoundSource.BLOCKS, 0.6f, 0.96f + level.random.nextFloat()*0.58f);
    }

    private void resetBeam() { this.lastBeamTarget = null; this.beamWarmup = 0; }

    private AABB getLaserBox(Direction direction, Vec3 from, Vec3 to, float energyScale) {
        AABB aabb = new AABB(from, to);
        double halfDiameter = energyScale / 2;
        return switch (direction) {
            case DOWN, UP -> aabb.inflate(halfDiameter, 0, halfDiameter);
            case NORTH, SOUTH -> aabb.inflate(halfDiameter, halfDiameter, 0);
            case WEST, EAST -> aabb.inflate(0, halfDiameter, halfDiameter);
        };
    }

    private BlockHitResult clipLaserInBeamSpace(Level level, ClipContext context) {
        BlockHitResult result = level.clip(context);
        if (result.getType() == HitResult.Type.MISS) {
            return result;
        }

        SubLevel beamSubLevel = Sable.HELPER.getContaining(level, context.getFrom());
        SubLevel hitSubLevel = Sable.HELPER.getContaining(level, result.getBlockPos());
        if (beamSubLevel == hitSubLevel) {
            return result;
        }

        Vec3 hitLocation = result.getLocation();
        if (hitSubLevel != null) {
            hitLocation = hitSubLevel.logicalPose().transformPosition(hitLocation);
        }
        if (beamSubLevel != null) {
            hitLocation = beamSubLevel.logicalPose().transformPositionInverse(hitLocation);
        }
        return new BlockHitResult(hitLocation, result.getDirection(), result.getBlockPos(), result.isInside());
    }

    private void validateConstraintHandle() {
        if (this.handle != null && !this.handle.isValid()) this.handle = null;
    }

    public ConstraintJointAxis getFreeLinearAxis() {
        final Direction facing = this.getBlockState().getValue(BlockStateProperties.FACING);
        return switch (facing.getAxis()) {
            case X -> ConstraintJointAxis.LINEAR_X;
            case Y -> ConstraintJointAxis.LINEAR_Y;
            case Z -> ConstraintJointAxis.LINEAR_Z;
        };
    }

    public ConstraintJointAxis getFreeAngularAxis() {
        final Direction facing = this.getBlockState().getValue(BlockStateProperties.FACING);
        return switch (facing.getAxis()) {
            case X -> ConstraintJointAxis.ANGULAR_X;
            case Y -> ConstraintJointAxis.ANGULAR_Y;
            case Z -> ConstraintJointAxis.ANGULAR_Z;
        };
    }

    private double readActualDistance() {
        final SubLevel subLevelB = this.getAttachedSubLevel();
        if (!(subLevelB instanceof ServerSubLevel ssl)) return getMinExtension();
        final Direction facing = this.getBlockState().getValue(BlockStateProperties.FACING);

        Vector3d bodyBPos = new Vector3d(ssl.logicalPose().position());
        Vector3d bearingPos = JOMLConversion.toJOML(this.getBlockPos().getCenter());

        final SubLevel containingSub = this.getContainingSubLevel();
        if (containingSub != null) {
            containingSub.logicalPose().transformPositionInverse(bodyBPos);
        }

        Vector3d facingVec = new Vector3d(facing.step().x(), facing.step().y(), facing.step().z());
        return bodyBPos.sub(bearingPos).dot(facingVec);
    }

    private double readActualAngle() {
        final SubLevel subLevelB = this.getAttachedSubLevel();
        if (!(subLevelB instanceof ServerSubLevel ssl)) return this.billedAngle;
        final SubLevel containingSub = this.getContainingSubLevel();

        final Quaterniond orientationA = new Quaterniond();
        final Quaterniond orientationB = new Quaterniond(ssl.logicalPose().orientation());
        if (containingSub != null) {
            orientationA.set(containingSub.logicalPose().orientation());
        }

        final Quaterniond blockFacing = new Quaterniond(this.getBlockState().getValue(BlockStateProperties.FACING).getRotation());
        final Quaterniond localB = new Quaterniond(orientationA).mul(blockFacing).conjugate()
            .mul(new Quaterniond(orientationB).mul(blockFacing));

        final double d = new Vec3(0.0, 1.0, 0.0).dot(new Vec3(localB.x(), localB.y(), localB.z()));
        return -2.0 * Math.toDegrees(Math.atan2(-d, localB.w()));
    }

    public void updateServoCoefficients(double timeStep) {
        this.validateConstraintHandle();
        final GenericConstraintHandle handle = this.handle;
        if (!this.isAssembled() || handle == null)
            return;

        lastBeamFrom = Pos3D.create(this).centre().translate(this.facingSupplier.get(), 0.501);
        if (++this.beamTickCounter >= getBeamIntervalTicks(timeStep)) {
            this.beamTickCounter = 0;
            this.applyBeamEffects();
        }

        final Direction facing = this.getBlockState().getValue(BlockStateProperties.FACING);
        final ConstraintJointAxis freeLinear = getFreeLinearAxis();
        final ConstraintJointAxis freeAngular = getFreeAngularAxis();

        final SubLevel subLevelA = this.getContainingSubLevel();
        final SubLevel subLevelB = this.getAttachedSubLevel();

        double massA = Double.MAX_VALUE;
        double massB = Double.MAX_VALUE;
        if (subLevelA instanceof ServerSubLevel sslA) {
            massA = sslA.getMassTracker().getMass();
        }
        if (subLevelB instanceof ServerSubLevel sslB) {
            massB = sslB.getMassTracker().getMass();
        }
        final double totalMass = Math.max(10.0,
            subLevelA != null && subLevelB != null
                ? Math.max(massA, massB) : Math.min(massA, massB));

        final Vec3i facingVec3i = facing.getNormal();
        final Vector3dc facingVec = new Vector3d(facingVec3i.getX(), facingVec3i.getY(), facingVec3i.getZ());
        double inertiaA = Double.MAX_VALUE;
        double inertiaB = Double.MAX_VALUE;
        final Vector3d temp = new Vector3d();
        if (subLevelA instanceof ServerSubLevel sslA) {
            inertiaA = sslA.getMassTracker().getInertiaTensor().transform(facingVec, temp).dot(facingVec);
        }
        if (subLevelB instanceof ServerSubLevel sslB) {
            inertiaB = sslB.getMassTracker().getInertiaTensor().transform(facingVec, temp).dot(facingVec);
        }
        final double totalInertia = Math.max(10.0,
            subLevelA != null && subLevelB != null
                ? Math.max(inertiaA, inertiaB) : Math.min(inertiaA, inertiaB));

        final double kP_linear = 1600.0 * totalMass * config.linearForceBoost();
        final double kD_linear =  80.0 * totalMass * config.linearForceBoost();
        final double kP_angular = 1600.0 * totalInertia * config.angularForceBoost();
        final double kD_angular =  80.0 * totalInertia * config.angularForceBoost();

        final double actualDist = readActualDistance();

        double effectiveTarget;
        double kP, kD;
        final boolean powered = this.hasPowerCached();
        boolean capped = false;

        if (this.disassembleState == DisassembleState.RETURNING) {
            effectiveTarget = FluxnauticsConfig.common.fluxTetherDisassembleThreshold.getOrDefault();
            kP = kP_linear;
            kD = kD_linear;
            if (actualDist <= FluxnauticsConfig.common.fluxTetherDisassembleThreshold.getOrDefault() + DISASSEMBLE_TOLERANCE) {
                completeDisassemble();
                return;
            }
        } else if (config.linearMode() == FluxTetherConfig.Mode.FREE) {
            if (actualDist > getMaxExtension()) {
                effectiveTarget = getMaxExtension();
                kP = kP_linear;
                kD = kD_linear;
                capped = true;
            } else if (actualDist < getMinExtension()) {
                effectiveTarget = getMinExtension();
                kP = kP_linear;
                kD = kD_linear;
                capped = true;
            } else {
                effectiveTarget = Math.clamp(this.animatedDistance.getValue(),
                    getMinExtension(), getMaxExtension());
                kP = 0.0;
                kD = 0.09;
            }
            this.animatedDistance.startWithValue(effectiveTarget);
        } else {
            effectiveTarget = Math.clamp(targetMagneticDistance,
                getMinExtension(), getMaxExtension());
            kP = kP_linear;
            kD = kD_linear;
        }
        if (!powered && !capped) { kP = 0.0; }
        this.animatedDistance.chase(effectiveTarget, getExtensionSpeed(), LerpedFloat.Chaser.LINEAR);
        this.animatedDistance.tickChaser();
        double animatedTarget = Math.clamp(this.animatedDistance.getValue(),
            getMinExtension(), getMaxExtension());
        final double linearGoal = (animatedTarget - 1) * facing.getAxisDirection().getStep();
        handle.setMotor(freeLinear, linearGoal, kP, kD, false, 0.0);

        if (config.angularMode() == FluxTetherConfig.Mode.FREE) {
            handle.setMotor(freeAngular, 0.0, 0.0, 0.09, false, 0.0);
        } else {
            double angularGoal = Math.toRadians(targetAngleDegrees);
            if (facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
                angularGoal = -angularGoal;
            }
            handle.setMotor(freeAngular, angularGoal, kP_angular, kD_angular, false, 0.0);
        }

        this.cachedTotalMass = totalMass;
        this.cachedTotalInertia = totalInertia;
        this.linearDriving  = kP > 0.0;
        this.angularDriving = config.angularMode() != FluxTetherConfig.Mode.FREE;
        handle.setContactsEnabled(false);
    }


    public void reattachConstraint(final @Nullable ServerSubLevel plateSubLevel, final boolean updatePlate) {
        if (localPlatePos != null) {
            if (this.handle != null) {
                this.handle.remove();
                this.handle = null;
            }

            if (updatePlate)
                this.associatePlateWithParent();

            final BlockState plateState = this.level.getBlockState(localPlatePos);
            if (!plateState.is(FNBlocks.FLUX_TETHER_LINK))
                return;

            this.attachConstraints(plateSubLevel);
        }
    }

    @Override
    public void setRemoved() {
        if (!this.level.isClientSide)
            this.removeHandle();
        super.setRemoved();
    }

    public void associatePlateWithParent() {
        FluxTetherLinkBlockEntity plate = getPlateBE();
        if (plate != null)
            plate.setParent(this);
    }

    public void onMovedByForeignAssembly() {
        if (!this.isAssembled()) return;
        this.removeHandle();
    }

    private FluxTetherLinkBlockEntity getPlateBE() {
        if (this.isAssembled() && this.getSubLevelID() != null) {
            SubLevel subLevel = this.getAttachedSubLevel();
            if (subLevel instanceof ServerSubLevel ssl && this.localPlatePos != null) {
                BlockEntity be = ssl.getLevel().getBlockEntity(this.localPlatePos);
                if (be instanceof FluxTetherLinkBlockEntity plate) return plate;
            }
        } else if (this.plateWorldPos != null) {
            BlockEntity be = this.level.getBlockEntity(this.plateWorldPos);
            if (be instanceof FluxTetherLinkBlockEntity plate) return plate;
        }
        return null;
    }

    private void attachConstraints(@Nullable ServerSubLevel plateSubLevel) {
        this.removeHandle();
        if (this.localPlatePos == null || plateSubLevel == null)
            return;

        final Direction facing = this.getBlockState().getValue(BlockStateProperties.FACING);

        final ConstraintJointAxis freeLinear = getFreeLinearAxis();
        final ConstraintJointAxis freeAngular = getFreeAngularAxis();

        Set<ConstraintJointAxis> locked = EnumSet.allOf(ConstraintJointAxis.class);
        locked.remove(freeLinear);
        locked.remove(freeAngular);

        final Vector3d anchorPos = JOMLConversion.toJOML(this.getBlockPos().relative(facing).getCenter());
        final Vector3d attachPos = JOMLConversion.toJOML(this.localPlatePos.getCenter());

        GenericConstraintConfiguration constraint = new GenericConstraintConfiguration(
            anchorPos, attachPos,
            new Quaterniond(), new Quaterniond(),
            locked
        );

        final ServerSubLevelContainer container = SubLevelContainer.getContainer((ServerLevel) this.getLevel());
        final ServerSubLevel containingSub = (ServerSubLevel) Sable.HELPER.getContaining(this);
        final PhysicsPipeline pipeline = container.physicsSystem().getPipeline();
        if (containingSub == plateSubLevel) return;
        this.handle = pipeline.addConstraint(containingSub, plateSubLevel, constraint);
        if (this.getPlateBE() instanceof FluxTetherLinkBlockEntity plate) {
            plate.parent = this.getBlockPos();
            plate.parentSubLevelId = containingSub != null ? containingSub.getUniqueId() : null;
        }
        if (this.handle != null) {
            this.billedAngle = this.readActualAngle();
        }
    }

    public void afterMoveToNewSubLevel() {
        this.assembling = false;
        if (!this.isAssembled()) return;
        final SubLevel attached = this.getAttachedSubLevel();
        if (attached instanceof ServerSubLevel ssl) {
            this.associatePlateWithParent();
            this.reattachConstraint(ssl, false);
        } else {
            this.removeHandle();
        }
    }


    private void setTargetAngleFromCurrentOrientation(BlockState attachedState, SubLevel attached) {
        final Quaterniond orientationA      = new Quaterniond();
        final Quaterniond blockOrientationA = new Quaterniond(this.getBlockState().getValue(BlockStateProperties.FACING).getRotation());
        final Quaterniond blockOrientationB = new Quaterniond(attachedState.getValue(BlockStateProperties.FACING).getRotation());
        final Quaterniond orientationB      = new Quaterniond(attached.logicalPose().orientation());
        final SubLevel containing           = this.getContainingSubLevel();
        if (containing != null) orientationA.set(containing.logicalPose().orientation());

        final Quaterniond localB = new Quaterniond(orientationA).mul(blockOrientationA).conjugate()
            .mul(new Quaterniond(orientationB).mul(blockOrientationB));

        final double d = new Vec3(0.0, 1.0, 0.0).dot(new Vec3(localB.x(), localB.y(), localB.z()));
        final double currentAngle = -2.0 * Math.toDegrees(Math.atan2(-d, localB.w()));
        this.targetAngleDegrees     = currentAngle;
        this.lastTargetAngleDegrees = currentAngle;
    }


    private void checkPersistence(final UUID id) {
        if (this.localPlatePos != null && FluxLevelUtil.isAreaActuallyLoaded(this.getLevel(), this.localPlatePos, 1)) {
            if (!this.getLevel().getBlockState(this.localPlatePos).is(FNBlocks.FLUX_TETHER_LINK)) {
                return;
            }
        }

        final SubLevel subLevel = SubLevelContainer.getContainer(this.getLevel()).getSubLevel(id);
        this.validateConstraintHandle();

        if (this.handle == null) {
            this.reattachConstraint((ServerSubLevel) subLevel, true);
        }
    }

    private void removeHandle() {
        if (this.handle != null) {
            this.handle.remove(); this.handle = null;
        }
    }

    public void beforeAssembly() { this.assembling = true; }

    public void dropEphemeralLink() {
        if (!this.level.isClientSide && !this.assembling) {
            this.destroyPlate();
        }
    }

    private void destroyPlate() {
        if (this.isAssembled()) {
            final UUID capturedSubLevelID = this.getSubLevelID();
            final BlockPos capturedLocalPlatePos = this.localPlatePos;
            this.removeHandle();

            this.setSubLevelID(null);
            this.localPlatePos = null;

            if (capturedLocalPlatePos != null && capturedSubLevelID != null) {
                final SubLevel subLevel = SubLevelContainer.getContainer(this.level).getSubLevel(capturedSubLevelID);
                if (subLevel instanceof ServerSubLevel ssl && !subLevel.isRemoved()) {
                    try {
                        final BlockEntity be = ssl.getLevel().getBlockEntity(capturedLocalPlatePos);
                        if (be instanceof FluxTetherLinkBlockEntity plateBE) {
                            plateBE.beforeAssembly();
                        }
                        ssl.getLevel().setBlock(capturedLocalPlatePos, Blocks.AIR.defaultBlockState(), 2);
                    } catch (Exception e) {
                        LogUtils.getLogger().warn("QuantumLock destroyPlate could not remove plate from sublevel at {}",
                            this.getBlockPos(), e);
                    }
                }
            }
        }
        if (this.plateWorldPos != null) {
            if (this.getLevel().getBlockState(this.plateWorldPos).getBlock() instanceof FluxTetherLinkBlock) {
                if (this.getLevel().getBlockEntity(this.plateWorldPos) instanceof FluxTetherLinkBlockEntity plate) {
                    plate.beforeAssembly();
                }
                this.getLevel().setBlock(this.plateWorldPos, Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
        if (this.localPlatePos != null) {
            this.updateServoCoefficients(timeStep);
        }
    }

    @Override
    public @Nullable Iterable<@NotNull SubLevel> sable$getConnectionDependencies() {
        final SubLevel attached = this.getAttachedSubLevel();
        return attached == null ? null : List.of(attached);
    }

    public boolean isAssembled() { return this.getBlockState().getValue(FluxTetherBlock.ASSEMBLED); }

    private @Nullable SubLevel getAttachedSubLevel() {
        return SubLevelContainer.getContainer(this.level).getSubLevel(this.subLevelID);
    }
    private @Nullable SubLevel getContainingSubLevel() { return Sable.HELPER.getContaining(this); }

    public @Nullable UUID    getSubLevelID()              { return this.subLevelID; }
    public void setSubLevelID(@Nullable UUID id)          { this.subLevelID = id; }
    public double getTargetAngleDegrees()                 { return this.targetAngleDegrees; }
    public double getTargetMagneticDistance()             { return this.targetMagneticDistance; }
    public @Nullable FluxAssemblyException getLastException() { return this.lastException; }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putDouble("TargetAngle", this.targetAngleDegrees);
        if (this.subLevelID != null)
            tag.putUUID("SubLevelID", this.subLevelID);
        if (this.plateWorldPos  != null)
            tag.put("PlateWorldPos", NbtUtils.writeBlockPos(this.plateWorldPos));
        if (this.localPlatePos  != null)
            tag.put("LocalPlatePos", NbtUtils.writeBlockPos(this.localPlatePos));
        tag.putDouble("CurrentMagneticDistance", this.currentMagneticDistance);
        tag.putDouble("LinearServoAcc", this.linearServoAccumulator);
        tag.putDouble("AngularServoAcc", this.angularServoAccumulator);
        tag.putInt("DisassembleState", this.disassembleState.ordinal());
        tag.put("Config", FluxTetherConfig.CODEC.encodeStart(NbtOps.INSTANCE, this.config)
            .result().orElse(new CompoundTag()));
        FluxAssemblyException.write(tag, provider, this.lastException);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.targetAngleDegrees = tag.getDouble("TargetAngle");
        if (tag.hasUUID("SubLevelID"))
            this.subLevelID = tag.getUUID("SubLevelID");
        if (tag.contains("PlateWorldPos"))
            this.plateWorldPos = NbtUtils.readBlockPos(tag, "PlateWorldPos").orElse(null);
        if (tag.contains("LocalPlatePos"))
            this.localPlatePos = NbtUtils.readBlockPos(tag, "LocalPlatePos").orElse(null);
        if (tag.contains("CurrentMagneticDistance"))
            this.currentMagneticDistance = tag.getDouble("CurrentMagneticDistance");
        if (tag.contains("LinearServoAcc"))
            this.linearServoAccumulator = tag.getDouble("LinearServoAcc");
        if (tag.contains("AngularServoAcc"))
            this.angularServoAccumulator = tag.getDouble("AngularServoAcc");
        if (tag.contains("DisassembleState")) {
            int ord = tag.getInt("DisassembleState");
            DisassembleState[] states = DisassembleState.values();
            this.disassembleState = (ord >= 0 && ord < states.length) ? states[ord] : DisassembleState.IDLE;
        }
        if (tag.contains("Config")) {
            FluxTetherConfig.CODEC.parse(NbtOps.INSTANCE, tag.get("Config"))
                .result().ifPresent(c -> this.config = c);
        }
        this.animatedDistance.startWithValue(this.currentMagneticDistance);
        this.lastException = FluxAssemblyException.read(tag,  provider);
    }

    @NotNull
    @Override
    public CompoundTag getReducedUpdateTag(@NotNull HolderLookup.Provider provider) {
        CompoundTag updateTag = super.getReducedUpdateTag(provider);
        if (subLevelID != null)
            updateTag.putUUID("subLevelID", subLevelID);
        if (localPlatePos != null)
            updateTag.put("linkPos", NbtUtils.writeBlockPos(localPlatePos));
        updateTag.putDouble("targetDistance", targetMagneticDistance);
        updateTag.put("Config", FluxTetherConfig.CODEC.encodeStart(NbtOps.INSTANCE, this.config)
            .result().orElse(new CompoundTag()));
        return updateTag;
    }

    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.handleUpdateTag(tag, provider);
        NBTUtils.setUUIDIfPresent(tag, "subLevelID", (id) -> this.subLevelID = id);
        NBTUtils.setBlockPosIfPresent(tag, "linkPos", (pos) -> this.localPlatePos = pos);
        NBTUtils.setDoubleIfPresent(tag, "targetDistance", (d) -> this.targetMagneticDistance = d);
        if (tag.contains("Config")) {
            FluxTetherConfig.CODEC.parse(NbtOps.INSTANCE, tag.get("Config"))
                .result().ifPresent(c -> this.config = c);
        }
    }

    @ComputerMethod
    public Convertable<FluxTetherConfig> getQuantumLockConfig() throws ComputerException {
        try {
            return Convertable.of(this.config, (o, x) -> {
                DataResult<Object> result =  FluxTetherConfig.CODEC.encode(x, JavaOps.INSTANCE, null);
                return result.getOrThrow();
            });
        } catch (IllegalStateException e) {
            throw new ComputerException("Invalid config: %s", e);
        }
    }

    @ComputerMethod
    public void setQuantumLockConfigFull(Map<?, ?> config) throws ComputerException {
        try {
            DataResult<Pair<FluxTetherConfig, Object>> result = FluxTetherConfig.CODEC.decode(JavaOps.INSTANCE, config);
            this.setConfig(result.getOrThrow().getFirst());
        } catch (IllegalStateException e) {
            throw new ComputerException("Invalid config: %s", e);
        }
    }

    @ComputerMethod
    public void setQuantumLockConfigPartial(Map<?, ?> config) throws ComputerException {
        try {
            DataResult<Pair<FluxTetherConfig.PartialUpdate, Object>> result = FluxTetherConfig.PartialUpdate.CODEC.decode(JavaOps.INSTANCE, config);
            result.getOrThrow().getFirst().applyTo(this.config);
            this.setChanged();
            this.wakeUpAttached();
            this.sendUpdatePacket();
        } catch (IllegalStateException e) {
            throw new ComputerException("Invalid config: %s", e);
        }
    }

    @ComputerMethod
    public double getMinExtension() {
        if (this.level == null || this.level.isClientSide)
            return clientMinExtension;
        return MIN_EXTENSION_BLOCKS;
    }

    @ComputerMethod
    public double getMaxExtension() {
        if (this.level == null || this.level.isClientSide)
            return clientMaxExtension;

        return 64 + upgradeComponent.getUpgrades(Upgrade.ENERGY) * 32;
    }

    @ComputerMethod
    public double getExtensionSpeed() {
        return 0.05 + upgradeComponent.getUpgrades(Upgrade.SPEED) * 0.2;
    }

    public int getBeamIntervalTicks(double timeStep) {
        double gameTickDuration = 1.0 / this.level.tickRateManager().tickrate();
        int stepsPerGameTick = Math.toIntExact(Math.round(gameTickDuration / timeStep));
        return Math.max(1, 8 - upgradeComponent.getUpgrades(Upgrade.SPEED)) ;
    }

    @ComputerMethod
    public int getBeamIntervalTicks() {
        return getBeamIntervalTicks(0.05);
    }


    @ComputerMethod
    public boolean isDigitalOverride() {
        return getRedstoneConfig().isDigitalOverride();
    }

    @ComputerMethod
    public void setDigitalOverride(boolean override) {
        getRedstoneConfig().setDigitalOverride(override);
    }
}
