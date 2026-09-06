package dev.alyti.fluxnautics.content.blocks.antigrav;

import dev.alyti.fluxnautics.FluxnauticsConfig;
import dev.alyti.fluxnautics.content.blocks.spatial_anchor.SpatialAnchorBlockEntity;
import dev.alyti.fluxnautics.registry.FNBlocks;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import mekanism.common.config.value.CachedDoubleValue;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.util.NBTUtils;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;

import java.util.Map;
import java.util.WeakHashMap;

public class AntiGravBlockEntity extends SpatialAnchorBlockEntity {
    private static final Map<ResourceKey<Level>, Map<ServerSubLevel, AntiGravBlockEntity>> mapOfAntigravs = new WeakHashMap<>();
    public static final double DEFAULT_CALIBRATION = 12;
    private static CachedDoubleValue calibrationReference;

    private boolean checkedOtherAntigravs = false;

    protected Vector3d TICK_POINT = new Vector3d();
    protected boolean isDigitalControl;
    protected double digitalControlValue;
    protected int analogControlValue;

    public AntiGravBlockEntity(BlockPos pos, BlockState state) {
        super(FNBlocks.ANTI_GRAV, pos, state);
    }

    @Override
    public void sable$tick(final ServerSubLevel subLevel) {
        if (!checkedOtherAntigravs && this.level != null) {
            checkedOtherAntigravs = true;
            if (!registerAntigravFor(subLevel, this)) {
                BlockPos pos = this.getBlockPos();
                this.level.destroyBlock(pos, false);
                return;
            }
        }
        if (ticker % (SharedConstants.TICKS_PER_SECOND) == 0 && this.level != null) {
            analogControlValue = this.level.getBestNeighborSignal(this.getBlockPos());
        }
        this.energyContainer.updateMassConstant(subLevel.getMassTracker().getMass());
        energyCost = ((Double) (FluxnauticsConfig.common.antiGravJoulesPerMassMult.getOrDefault() * energyContainer.getLastMass() * getControlValue())).longValue();
        this.setActive(this.canFunction());
    }

    @Override
    public void sable$physicsTick(final ServerSubLevel serverSubLevel, final RigidBodyHandle handle, final double timeStep) {
        if (!this.getActive() || !handle.isValid() || this.level == null)
            return;

        MassData massTracker = serverSubLevel.getMassTracker();
        final Pose3d pose = serverSubLevel.logicalPose();
        final Vector3d gravity = DimensionPhysicsData.getGravity(this.level, JOMLConversion.toJOML(this.getBlockPos().getCenter()));
        final Vector3d localGravity = pose.transformNormalInverse(gravity).mul(massTracker.getMass()).mul(timeStep);
        TICK_LINEAR.set(localGravity.negate());
        TICK_LINEAR.fma(-1.0 / getCalibrationValue() * timeStep, gravity);
        TICK_LINEAR.mul(getControlValue());
        handle.applyLinearAndAngularImpulse(TICK_LINEAR, JOMLConversion.ZERO, false);
        matterPos = JOMLConversion.toMojang(TICK_LINEAR.normalize(0.1));
    }

    public static double getCalibrationValue() {
        return calibrationReference == null ? DEFAULT_CALIBRATION : calibrationReference.getOrDefault();
    }

    public static void setConfigReference(CachedDoubleValue multiplierConfigReference) {
        calibrationReference = multiplierConfigReference;
    }

    public static boolean registerAntigravFor(final ServerSubLevel serverSubLevel, final AntiGravBlockEntity antigravBlockEntity) {
        final Map<ServerSubLevel, AntiGravBlockEntity> map = mapOfAntigravs.get(serverSubLevel.getLevel().dimension());
        AntiGravBlockEntity ticket = map != null ? map.get(serverSubLevel) : null;

        if (ticket != null && ticket.isRemoved()) {
            map.remove(serverSubLevel);
            ticket = null;
        }

        if (ticket != null && ticket != antigravBlockEntity) {
            return false;
        }

        if (ticket == null) {
            mapOfAntigravs.computeIfAbsent(serverSubLevel.getLevel().dimension(), x -> new Object2ObjectOpenHashMap<>())
                    .put(serverSubLevel, antigravBlockEntity);
        }
        return true;
    }

    @Override
    public long getEnergyPerMassPerTickMultiplier() {
        return ((Double) (FluxnauticsConfig.common.antiGravJoulesPerMassMult.getOrDefault() * 100L)).longValue();
    }

    public double getControlValue() {
        if (isDigitalControl) {
            return digitalControlValue;
        } else {
            return switch (getControlType()) {
                case DISABLED -> 1.0;
                case HIGH -> analogControlValue / 15.0;
                case LOW -> 1.0 - (analogControlValue / 15.0);
                case PULSE -> (isPowered() && !wasPowered()) ? (analogControlValue / 15.0) : 0;
            };
        }
    }

    @Override
    public boolean shouldFunction() {
        return getControlValue() > 0;
    }

    @ComputerMethod(methodDescription = "Take digital control over antigrav")
    public void takeDigitalControl() {
        this.isDigitalControl = true;
    }

    @ComputerMethod(methodDescription = "Return antigrav control to analog")
    public void releaseDigitalControl() {
        this.isDigitalControl = false;
    }

    @ComputerMethod(methodDescription = "Set how much gravity is negated, 0.0 to 1.0 where 0.0 is full gravity and 1.0 is no gravity.")
    public void setGravity(double value) {
        this.isDigitalControl = true;
        this.digitalControlValue = Math.clamp(value, 0.0, 1.0);
    }

    @NotNull
    @Override
    public CompoundTag getReducedUpdateTag(@NotNull HolderLookup.Provider provider) {
        CompoundTag updateTag = super.getReducedUpdateTag(provider);
        updateTag.putBoolean("digitalControlEnabled", this.isDigitalControl);
        updateTag.putDouble("digitalControlValue", this.digitalControlValue);
        return updateTag;
    }

    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.handleUpdateTag(tag, provider);
        NBTUtils.setBooleanIfPresent(tag, "digitalControlEnabled", (v) -> this.isDigitalControl = v);
        NBTUtils.setDoubleIfPresent(tag, "digitalControlValue", (v) -> this.digitalControlValue = v);
    }
}
