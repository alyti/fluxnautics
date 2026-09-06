package dev.alyti.fluxnautics.content.blocks.inertia_dampener;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JavaOps;
import dev.alyti.fluxnautics.api.content.redstone.IRedstoneSideConfigurable;
import dev.alyti.fluxnautics.api.content.redstone.TileComponentRedstoneControl;
import dev.alyti.fluxnautics.content.blocks.spatial_anchor.SpatialAnchorBlockEntity;
import dev.alyti.fluxnautics.registry.FNBlocks;
import dev.alyti.fluxnautics.registry.FNDataComponents;
import dev.alyti.fluxnautics.util.NBTHelper;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import mekanism.common.config.value.CachedDoubleValue;
import mekanism.common.integration.computer.ComputerException;
import mekanism.common.integration.computer.Convertable;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableDouble;
import mekanism.common.inventory.container.sync.SyncableLong;
import mekanism.common.util.NBTUtils;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.slf4j.Logger;

import static foundry.veil.api.util.CodecUtil.VECTOR3D_CODEC;

public class InertiaDampenerBlockEntity extends SpatialAnchorBlockEntity implements IRedstoneSideConfigurable {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final String CONFIG_TAG = "inertiaDampenerConfig";

    public static final double DEFAULT_JOULES_PER_KINETIC_UNIT = 2.0;
    public static final double DEFAULT_LINEAR_CALIBRATION = 100;

    private static CachedDoubleValue joulesPerKineticReference;
    private static CachedDoubleValue linearCalibrationReference;

    public static void setConfigReference(final CachedDoubleValue joulesPerKineticUnit,
                                          final CachedDoubleValue linearCalibration) {
        joulesPerKineticReference = joulesPerKineticUnit;
        linearCalibrationReference = linearCalibration;
    }

    public static double getJoulesPerKineticUnit() {
        return joulesPerKineticReference == null ? DEFAULT_JOULES_PER_KINETIC_UNIT : joulesPerKineticReference.getOrDefault();
    }

    public static double getLinearCalibration() {
        return linearCalibrationReference == null ? DEFAULT_LINEAR_CALIBRATION : linearCalibrationReference.getOrDefault();
    }

    private static final Vector3d SCRATCH_WORLD_LINEAR = new Vector3d();
    private static final Vector3d SCRATCH_WORLD_ANGULAR = new Vector3d();
    private static final Vector3d SCRATCH_LOCAL_LINEAR = new Vector3d();

    private static final double WAKE_EPSILON = 1.0E-9;

    private InertiaDampenerConfig config = InertiaDampenerConfig.DEFAULT;


    private boolean configSyncPending;

    private final TileComponentRedstoneControl<InertiaDampenerBlockEntity, InertiaDampenerRedstoneControl> redstoneConfig;

    private double accumulatedWork;
    private double lastTickWork;

    private final Vector3d cachedLinearVelocity = new Vector3d();
    private final Vector3d cachedAngularVelocity = new Vector3d();

    public InertiaDampenerBlockEntity(BlockPos pos, BlockState state) {
        super(FNBlocks.INERTIA_DAMPENER, pos, state);
        this.redstoneConfig = new TileComponentRedstoneControl<>(this, InertiaDampenerRedstoneControl.class, (side) -> true);
    }

    @Override
    public void sable$tick(final ServerSubLevel subLevel) {
        this.lastTickWork = this.accumulatedWork;
        this.accumulatedWork = 0.0;
        this.energyCost = (long) (getJoulesPerKineticUnit() * this.lastTickWork);
        this.energyContainer.updateMassConstant(subLevel.getMassTracker().getMass());
        this.setActive(this.canFunction());
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdate = super.onUpdateServer();
        if (this.redstoneConfig.tickServer()) {
            this.setChanged();
            sendUpdate = true;
        }
        if (this.configSyncPending) {
            this.configSyncPending = false;
            sendUpdate = true;
        }
        return sendUpdate;
    }

    @Override
    public void sable$physicsTick(final ServerSubLevel serverSubLevel, final RigidBodyHandle handle, final double timeStep) {
        if (!this.getActive() || !handle.isValid() || this.level == null) {
            return;
        }

        final MassData massTracker = serverSubLevel.getMassTracker();
        if (massTracker.isInvalid()) {
            return;
        }

        handle.getLinearVelocity(SCRATCH_WORLD_LINEAR);
        handle.getAngularVelocity(SCRATCH_WORLD_ANGULAR);
        this.cachedLinearVelocity.set(SCRATCH_WORLD_LINEAR);
        this.cachedAngularVelocity.set(SCRATCH_WORLD_ANGULAR);

        if (!shouldFunction() || this.config.isIdle()) {
            return;
        }

        final Pose3d pose = serverSubLevel.logicalPose();
        final double mass = massTracker.getMass();

        SCRATCH_LOCAL_LINEAR.set(SCRATCH_WORLD_LINEAR);
        pose.transformNormalInverse(SCRATCH_LOCAL_LINEAR);

        final double maxDeltaV = getLinearCalibration() * timeStep;
        final double dvx = axisDelta(this.config.linearX(), SCRATCH_LOCAL_LINEAR.x, 1.0, maxDeltaV);
        final double dvy = axisDelta(this.config.linearY(), SCRATCH_LOCAL_LINEAR.y, 1.0, maxDeltaV);
        final double dvz = axisDelta(this.config.linearZ(), SCRATCH_LOCAL_LINEAR.z, 1.0, maxDeltaV);

        final double linearWork = 0.5 * mass * (
                (SCRATCH_LOCAL_LINEAR.x * SCRATCH_LOCAL_LINEAR.x - (SCRATCH_LOCAL_LINEAR.x + dvx) * (SCRATCH_LOCAL_LINEAR.x + dvx))
                        + (SCRATCH_LOCAL_LINEAR.y * SCRATCH_LOCAL_LINEAR.y - (SCRATCH_LOCAL_LINEAR.y + dvy) * (SCRATCH_LOCAL_LINEAR.y + dvy))
                        + (SCRATCH_LOCAL_LINEAR.z * SCRATCH_LOCAL_LINEAR.z - (SCRATCH_LOCAL_LINEAR.z + dvz) * (SCRATCH_LOCAL_LINEAR.z + dvz))
        );

        TICK_LINEAR.set(dvx * mass, dvy * mass, dvz * mass);


        final boolean wakeUp = TICK_LINEAR.lengthSquared() > WAKE_EPSILON;
        handle.applyLinearAndAngularImpulse(TICK_LINEAR, JOMLConversion.ZERO, wakeUp);

        this.accumulatedWork += linearWork;

        if (TICK_LINEAR.lengthSquared() > 1.0E-12) {
            this.matterPos = JOMLConversion.toMojang(TICK_LINEAR.normalize(0.3));
        } else {
            this.matterPos = Vec3.ZERO;
        }
    }

    private static double axisDelta(final InertiaDampenerConfig.AxisConfig axis, final double velocity, final double control, final double budget) {
        final double target = switch (axis.mode()) {
            case OFF -> 0.0;
            case DAMPEN -> -velocity * axis.strength() * control;
            case BRAKE -> -velocity * control;
        };
        return Math.clamp(target, -budget, budget);
    }

    public TileComponentRedstoneControl<InertiaDampenerBlockEntity, InertiaDampenerRedstoneControl> getRedstoneConfig() {
        return this.redstoneConfig;
    }

    @Override
    public long getEnergyPerMassPerTickMultiplier() {
        return (long) (getJoulesPerKineticUnit() * 100.0);
    }

    @Nullable
    private static InertiaDampenerConfig.DampenerAxis parseAxis(final String axis) {
        final InertiaDampenerConfig.DampenerAxis resolved = InertiaDampenerConfig.DampenerAxis.byName(axis);
        if (resolved == null) {
            LOGGER.warn("Unknown inertia dampener axis: {}", axis);
        }
        return resolved;
    }

    public InertiaDampenerConfig getConfiguration() {
        return this.config;
    }

    public void applyConfiguration(final InertiaDampenerConfig.PartialUpdate update) {
        final InertiaDampenerConfig applied = update.applyTo(this.config);
        if (!applied.equals(this.config)) {
            this.config = applied;
            this.setChanged();
            this.configSyncPending = true;
        }
    }

    public void previewConfigurationClient(final InertiaDampenerConfig.PartialUpdate update) {
        if (this.level != null && this.level.isClientSide) {
            this.config = update.applyTo(this.config);
        }
    }

    public void setLinearMode(final InertiaDampenerConfig.DampenerMode mode) {
        this.applyConfiguration(InertiaDampenerConfig.PartialUpdate.ofLinearModes(mode));
    }

    public void setLinearStrength(final double strength) {
        this.applyConfiguration(InertiaDampenerConfig.PartialUpdate.ofLinearStrengths(strength));
    }

    public void stepLinearMode() {
        this.setLinearMode(nextMode(this.config.linearX().mode()));
    }

    private static InertiaDampenerConfig.DampenerMode nextMode(final InertiaDampenerConfig.DampenerMode mode) {
        final InertiaDampenerConfig.DampenerMode[] values = InertiaDampenerConfig.DampenerMode.values();
        return values[(mode.ordinal() + 1) % values.length];
    }

    @Override
    public void addContainerTrackers(final MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableLong.create(this::getEnergyCost, value -> this.energyCost = value));
        container.track(SyncableDouble.create(this::getLastTickWork, value -> this.lastTickWork = value));
    }

    public long getEnergyCost() {
        return this.energyCost;
    }

    @ComputerMethod(methodDescription = "Take digital control over the inertia dampener")
    public void takeDigitalControl() {
        this.redstoneConfig.setDigitalOverride(true);
    }

    @ComputerMethod(methodDescription = "Return inertia dampener control to analog")
    public void releaseDigitalControl() {
        this.redstoneConfig.setDigitalOverride(false);
    }

    @ComputerMethod(methodDescription = "Set the mode of an axis: linearX/Y/Z, mode off/dampen/brake. Returns true on success.")
    public boolean setAxisMode(String axis, String mode) {
        final InertiaDampenerConfig.DampenerAxis resolved = parseAxis(axis);
        if (resolved == null) {
            return false;
        }
        this.applyConfiguration(InertiaDampenerConfig.PartialUpdate.ofMode(resolved, InertiaDampenerConfig.DampenerMode.byName(mode)));
        return true;
    }

    @ComputerMethod(methodDescription = "Get the mode of an axis (off/dampen/brake), or null if the axis is unknown")
    @Nullable
    public String getAxisMode(String axis) {
        final InertiaDampenerConfig.DampenerAxis resolved = InertiaDampenerConfig.DampenerAxis.byName(axis);
        return resolved == null ? null : this.config.getAxis(resolved).mode().name();
    }

    @ComputerMethod(methodDescription = "Set the damping strength of an axis, 0.0 to 1.0 (dampen mode only). Returns true on success.")
    public boolean setAxisStrength(String axis, double strength) {
        final InertiaDampenerConfig.DampenerAxis resolved = parseAxis(axis);
        if (resolved == null) {
            return false;
        }
        this.applyConfiguration(InertiaDampenerConfig.PartialUpdate.ofStrength(resolved, Math.clamp(strength, 0.0, 1.0)));
        return true;
    }

    @ComputerMethod(methodDescription = "Get the damping strength of an axis, or null if the axis is unknown")
    @Nullable
    public Double getAxisStrength(String axis) {
        final InertiaDampenerConfig.DampenerAxis resolved = InertiaDampenerConfig.DampenerAxis.byName(axis);
        return resolved == null ? null : this.config.getAxis(resolved).strength();
    }

    @ComputerMethod(methodDescription = "Get the last measured world-frame linear velocity of the sub-level [m/s] as [x, y, z]")
    public Convertable<Vector3d> getLinearVelocity() throws ComputerException {
        try {
            return Convertable.of(this.cachedLinearVelocity, (o, x) -> {
                DataResult<Object> result =  VECTOR3D_CODEC.encode(x, JavaOps.INSTANCE, null);
                return result.getOrThrow();
            });
        } catch (IllegalStateException e) {
            throw new ComputerException("Invalid config: %s", e);
        }
    }

    public Vector3d getCachedLinearVelocity() {
        return this.cachedLinearVelocity;
    }

    @ComputerMethod(methodDescription = "Get the kinetic energy the dampener removed last tick")
    public double getLastTickWork() {
        return this.lastTickWork;
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);

        InertiaDampenerConfig.CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), this.config)
            .resultOrPartial(error -> LOGGER.error("Failed to encode inertia dampener config for saving: {}", error))
            .ifPresent(encoded -> tag.put(CONFIG_TAG, encoded));
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);

        final Tag encoded = tag.get(CONFIG_TAG);
        if (encoded != null) {
            InertiaDampenerConfig.CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), encoded)
                .resultOrPartial(error -> LOGGER.error("Failed to parse saved inertia dampener config: {}", error))
                .ifPresent(parsed -> this.config = parsed);
        }
    }

    @Override
    protected void applyImplicitComponents(@NotNull BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        this.config = input.getOrDefault(FNDataComponents.INERTIA_DAMPENER_CONFIG, InertiaDampenerConfig.DEFAULT);
    }

    @Override
    protected void collectImplicitComponents(@NotNull DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(FNDataComponents.INERTIA_DAMPENER_CONFIG, this.config);
    }

    @Override
    public CompoundTag getConfigurationData(final HolderLookup.Provider provider, final Player player) {
        final CompoundTag data = super.getConfigurationData(provider, player);
        data.put("Config", InertiaDampenerConfig.CODEC.encodeStart(NbtOps.INSTANCE, this.config).getOrThrow());
        this.redstoneConfig.write(data, provider);
        return data;
    }

    @Override
    public void setConfigurationData(final HolderLookup.Provider provider, final Player player, final CompoundTag data) {
        super.setConfigurationData(provider, player, data);
        NBTUtils.setCompoundIfPresent(data, "Config", (CompoundTag config) -> {
            this.config = InertiaDampenerConfig.CODEC.parse(NbtOps.INSTANCE, config).getOrThrow();
        });
        this.redstoneConfig.read(data, provider);
    }

    @NotNull
    @Override
    public CompoundTag getReducedUpdateTag(@NotNull HolderLookup.Provider provider) {
        CompoundTag updateTag = super.getReducedUpdateTag(provider);
        InertiaDampenerConfig.CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), this.config)
                .resultOrPartial(error -> LOGGER.error("Failed to encode inertia dampener config for sync: {}", error))
                .ifPresent(encoded -> updateTag.put(CONFIG_TAG, encoded));
        if (this.cachedLinearVelocity != null) {
            updateTag.put("linear", NBTHelper.writeVector3d(this.cachedLinearVelocity));
        }
        return updateTag;
    }

    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.handleUpdateTag(tag, provider);
        final Tag encoded = tag.get(CONFIG_TAG);
        if (encoded != null) {
            InertiaDampenerConfig.CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), encoded)
                    .resultOrPartial(error -> LOGGER.error("Failed to parse synced inertia dampener config: {}", error))
                    .ifPresent(parsed -> this.config = parsed);
        }
        NBTUtils.setListIfPresent(tag, "linear", Tag.TAG_DOUBLE, list -> NBTHelper.readVector3dc(list, cachedLinearVelocity));

    }
}
