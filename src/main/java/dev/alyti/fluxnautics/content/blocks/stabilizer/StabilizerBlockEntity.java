package dev.alyti.fluxnautics.content.blocks.stabilizer;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JavaOps;
import dev.alyti.fluxnautics.FluxnauticsConfig;
import dev.alyti.fluxnautics.api.content.redstone.IRedstoneSideConfigurable;
import dev.alyti.fluxnautics.api.content.redstone.TileComponentRedstoneControl;
import dev.alyti.fluxnautics.api.content.tooltip.IHoverTooltip;
import dev.alyti.fluxnautics.content.blocks.AbstractMassMachineBlockEntity;
import dev.alyti.fluxnautics.content.blocks.inertia_dampener.InertiaDampenerBlockEntity;
import dev.alyti.fluxnautics.content.blocks.inertia_dampener.InertiaDampenerConfig;
import dev.alyti.fluxnautics.content.blocks.inertia_dampener.InertiaDampenerRedstoneControl;
import dev.alyti.fluxnautics.registry.FNBlocks;
import dev.alyti.fluxnautics.registry.FNDataComponents;
import dev.alyti.fluxnautics.util.NBTHelper;
import dev.alyti.fluxnautics.util.SableUtils;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelReactionWheel;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import mekanism.common.integration.computer.ComputerException;
import mekanism.common.integration.computer.Convertable;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.util.NBTUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.List;
import java.util.Map;


public class StabilizerBlockEntity extends AbstractMassMachineBlockEntity  implements BlockEntitySubLevelReactionWheel, BlockEntitySubLevelActor, IHoverTooltip, IRedstoneSideConfigurable {

    public StabilizerConfig config = StabilizerConfig.makeDefault();

    private final TileComponentRedstoneControl<StabilizerBlockEntity, StabilizerRedstoneControl> redstoneConfig;

    public StabilizerBlockEntity(BlockPos pos, BlockState state) {
        super(FNBlocks.STABILIZER, pos, state);
        this.redstoneConfig = new TileComponentRedstoneControl<>(this, StabilizerRedstoneControl.class, (side) -> true);
    }

    public TileComponentRedstoneControl<StabilizerBlockEntity, StabilizerRedstoneControl> getRedstoneConfig() {
        return this.redstoneConfig;
    }

    @Override
    public double getEnergyPerMassMultiplier() {
        return Math.max(FluxnauticsConfig.common.stabilizerPassiveJoulesPerMassMult.getOrDefault(), 1.0);
    }

    @Override
    public long calcEnergyCost() {
        return Math.round((this.energyContainer.getLastMass() * FluxnauticsConfig.common.stabilizerActiveJoulesPerPnMult.getOrDefault()) + (angularVelocityImpulse.length() * FluxnauticsConfig.common.stabilizerActiveJoulesPerPnMult.getOrDefault()));
    }

    @Override
    public boolean countConnected() {
        return false;
    }

    @Override
    public void onSublevelTick(ServerSubLevel subLevel, boolean active) {
    }

    @Override
    protected boolean onUpdateServer() {
        boolean update = super.onUpdateServer();
        if (this.redstoneConfig.tickServer()) {
            this.setChanged();
            update = true;
        }
        if ((angularTarget.distance(angularTargetLast) > 5 || angularVelocityTarget.distance(angularVelocityTargetLast) > 5 || angularVelocityImpulse.distance(angularVelocityImpulseLast) > 100_000) || ticker % 40 == 0) {
            angularTargetLast.set(angularTarget);
            angularVelocityImpulseLast.set(angularVelocityImpulse);
            angularVelocityTargetLast.set(angularVelocityTarget);
            update = true;
        }

        return update;
    }

    @Override
    protected void onUpdateClient() {
        super.onUpdateClient();
        tickClientAnimation();
    }

    public static final double BASE_TORQUE = 2048.0;
    public static final double MAX_DAMPING_PERCENT = 1.0; // Fixed: was 0.99, causing negative retention at 100%
    private static final double AUTO_KP = 2.4;
    private static final double STRENGTH_SCALE = 0.1;
    private static final double CORRECTION_RAMP = 1e-4;
    private static final double SETTLE_ANG_VEL = 0.05;
    private static final double DISPLAY_SCALE = 30.0;

    @Override
    public void addLines(List<Component> list) {
        list.add(Component.literal("Down Orientation: %s".formatted(downFace.getName())));
        list.add(Component.literal("Angular Velocity:"));
        list.add(Component.literal("  X: %+,.3f pN%s".formatted(this.angularVelocityImpulse.x(), (config.stabilizePitch() ? " (err = %+.3f°)".formatted(Math.toDegrees(angularTarget.x())) : " (inactive)"))).withStyle(ChatFormatting.RED));
        list.add(Component.literal("  Y: %+,.3f pN%s".formatted(this.angularVelocityImpulse.y(), (config.stabilizeYaw() ? " (err = %+.3f°)".formatted(Math.toDegrees(angularTarget.y())) : " (inactive)"))).withStyle(ChatFormatting.GREEN));
        list.add(Component.literal("  Z: %+,.3f pN%s".formatted(this.angularVelocityImpulse.z(), (config.stabilizeRoll() ? " (err = %+.3f°)".formatted(Math.toDegrees(angularTarget.z())) : " (inactive)"))).withStyle(ChatFormatting.BLUE));
    }

    public enum GyroMode {
        AUTO,
        MANUAL
    }

    private final Direction downFace = getBlockState().getValue(StabilizerBlock.FACING);

    private boolean navTargetOverride;

    private final Vector3d wheelMomentum = new Vector3d();
    private final Vector3d delta = new Vector3d();

    private final Vector3d displayAngles = new Vector3d();
    public final Vector3d angularVelocityTarget = new Vector3d();
    public final Vector3d angularVelocityTargetLast = new Vector3d();
    private final Vector3d angularVelocityImpulse = new Vector3d();
    private final Vector3d angularVelocityImpulseLast = new Vector3d();
    private final Vector3d angularTarget = new Vector3d();
    private final Vector3d angularTargetLast = new Vector3d();

    private boolean litNorth;
    private boolean litEast;
    private boolean litSouth;
    private boolean litWest;

    private int signalNorth;
    private int signalEast;
    private int signalSouth;
    private int signalWest;


    private final GyroPrecisionController navPrecisionController = new GyroPrecisionController();

    private void tickClientAnimation() {
        double dt = 1.0 / 20.0;
        displayAngles.x += angularVelocityTarget.x * dt * DISPLAY_SCALE;
        displayAngles.y += angularVelocityTarget.y * dt * DISPLAY_SCALE;
        displayAngles.z += angularVelocityTarget.z * dt * DISPLAY_SCALE;
    }

    public void onNeighborChanged() {
        if (level == null || level.isClientSide) {
            return;
        }
        readRedstone();
        setChanged();
    }

    private void readRedstone() {
        assert level != null;
        BlockPos p = worldPosition;
        signalNorth = level.getSignal(p.relative(Direction.NORTH), Direction.NORTH);
        signalEast = level.getSignal(p.relative(Direction.EAST), Direction.EAST);
        signalSouth = level.getSignal(p.relative(Direction.SOUTH), Direction.SOUTH);
        signalWest = level.getSignal(p.relative(Direction.WEST), Direction.WEST);

        litNorth = signalNorth > 0;
        litEast = signalEast > 0;
        litSouth = signalSouth > 0;
        litWest = signalWest > 0;
    }

    private double computeMaxCorrectionTorque() {
        return BASE_TORQUE * config.forcePercent() * STRENGTH_SCALE * this.energyContainer.getLastMass();
    }

    private Vector3d computeManualTorque(double maxTorque) {
        Vector3d torque = new Vector3d();
        if (config.stabilizePitch()) {
            torque.x += (signalNorth - signalSouth) / 15.0 * maxTorque;
        }
        if (config.stabilizeYaw()) {
            torque.y += (signalEast - signalWest) / 15.0 * maxTorque;
        }
        if (config.stabilizeRoll()) {
            torque.z += (signalEast + signalWest - signalNorth - signalSouth) / 60.0 * maxTorque;
        }
        return torque;
    }

    private void applyTorqueAtGyro(ServerSubLevel subLevel, Vector3d localTorque, double maxTorque, double dt) {
        if (localTorque.lengthSquared() < 1e-12) {
            angularVelocityTarget.mul(0.85);
            setChanged();
            return;
        }

        Vector3d torque = new Vector3d(localTorque);
        if (!config.bidirectionalTorque()) {
            torque = enforceOneWayTorque(torque, maxTorque);
        }

        angularVelocityImpulse.set(torque).mul(dt);
        clampVector(angularVelocityImpulse, maxTorque * dt);

        double wheelScale = maxTorque * dt;
        wheelMomentum.add(angularVelocityImpulse.x / wheelScale, angularVelocityImpulse.y / wheelScale, angularVelocityImpulse.z / wheelScale);
        clampVector(wheelMomentum, 1.0);

        subLevel.getOrCreateQueuedForceGroup(
            ForceGroups.PROPULSION.get()
        ).getForceTotal().applyTorqueImpulse(angularVelocityImpulse);

        syncDisplayFromWheel();
        setChanged();
    }

    private Vector3d enforceOneWayTorque(Vector3d torque, double maxTorque) {
        Vector3d allowed = new Vector3d(torque);
        double cap = maxTorque * 0.5;
        for (int i = 0; i < 3; i++) {
            double t = torque.get(i);
            double w = wheelMomentum.get(i) * cap;
            if (Math.abs(t) < 1e-9) {
                continue;
            }
            if (t * w < 0) {
                continue;
            }
            if (t > 0 && w >= cap) {
                allowed.setComponent(i, 0);
            } else if (t < 0 && w <= -cap) {
                allowed.setComponent(i, 0);
            }
        }
        return allowed;
    }

    private void syncDisplayFromWheel() {
        angularVelocityTarget.set(wheelMomentum);
    }

    private static void clampVector(Vector3d v, double max) {
        double len = v.length();
        if (len > max && len > 1e-6) {
            v.mul(max / len);
        }
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle body, double dt) {
        if (!this.getActive() || ticker == 0) return;
        if (subLevel == null || body == null || subLevel.logicalPose() == null) return;
        physicsTickInternal(subLevel, body, dt);
    }

    private void physicsTickInternal(ServerSubLevel subLevel, RigidBodyHandle body, double dt) {
        if (level == null || level.isClientSide) {
            return;
        }
        readRedstone();
        double maxCorrection = computeMaxCorrectionTorque();
        Quaterniond orientation = subLevel.logicalPose().orientation();
        Vector3d localAngVel = orientation.transformInverse(body.getAngularVelocity(new Vector3d()), new Vector3d());

        ServerSubLevel root = SableUtils.resolveRootAssembly(subLevel);
        GyroTargetAngles eulerTarget = resolveEulerTarget();
        Quaterniond qTarget = resolveTargetOrientation(orientation, eulerTarget);

        double effDamping = config.dampingPercent();
        double forceMult = 1.0;
        Vector3d correctionTorque = null;

        if (config.forcePercent() > 0) {
            if (config.mode() == GyroMode.MANUAL) {
                correctionTorque = computeManualTorque(maxCorrection);
            } else if (config.autoStabilize()) {
                GyroPrecisionController.GyroNavTorque navTorque = GyroPrecisionController.computeTowardOrientation(
                    (Vector3d) GyroPrecisionController.getBodyRotationError(orientation, qTarget, angularTarget),
                    localAngVel,
                    config.stabilizePitch(),
                    config.stabilizeYaw(),
                    config.stabilizeRoll(),
                    config.acceptAngleDeg()
                );

                boolean zeroTolerance = config.acceptAngleDeg() == 0.0;
                GyroPrecisionController.PrecisionState precision = zeroTolerance
                    ? navPrecisionController.update(navTorque.headingErrorRad())
                    : GyroPrecisionController.PrecisionState.inactive();

                if (zeroTolerance && navTorque.headingErrorRad() > GyroPrecisionController.PRECISION_ZONE_RAD) {
                    navPrecisionController.reset();
                } else if (!zeroTolerance) {
                    navPrecisionController.reset();
                }

                if (precision.active()) {
                    effDamping = precision.dampingPercent();
                    forceMult = precision.forceMultiplier();
                }

                if (navTorque.localTorque().lengthSquared() > 1e-12) {
                    correctionTorque = new Vector3d(navTorque.localTorque()).mul(forceMult * maxCorrection);
                }
            }
            if (correctionTorque != null && correctionTorque.lengthSquared() > 1e-12) {
                applyTorqueAtGyro(subLevel, correctionTorque, maxCorrection * forceMult, dt);
            }
        } else {
            navPrecisionController.reset();
        }

        applyRotationalDamping(subLevel, body, orientation, effDamping);
    }

    private GyroTargetAngles resolveEulerTarget() {
        if (navTargetOverride) {
            navTargetOverride = false;
            setChanged();
        }
        return new GyroTargetAngles(config.targetPitchDeg(), config.targetYawDeg(), config.targetRollDeg());
    }

    private void syncNavTargetsToDisplay(GyroTargetAngles nav) {
        double p = Math.clamp(nav.pitchDeg(), -90.0, 90.0);
        double y = wrapDegrees(nav.yawDeg());
        double r = Math.clamp(nav.rollDeg(), -90.0, 90.0);
        if (p != config.targetPitchDeg() || y != config.targetYawDeg() || r != config.targetRollDeg()) {
            config.setTargetPitchDeg(p);
            config.setTargetYawDeg(y);
            config.setTargetRollDeg(r);
            setChanged();
        }
    }

    private Quaterniond resolveTargetOrientation(
        Quaterniond currentOrientation,
        GyroTargetAngles eulerTarget
    ) {
        if (eulerTarget.pitchDeg() == 0.0
            && eulerTarget.yawDeg() == 0.0
            && eulerTarget.rollDeg() == 0.0) {
            return levelDownFaceOrientation(currentOrientation);
        }
        return GyroPrecisionController.orientationFromLevelAttitude(eulerTarget);
    }

    private Quaterniond levelDownFaceOrientation(Quaterniond currentOrientation) {
        Vector3d localDown = localDownFaceDirection();
        Vector3d currentDown = currentOrientation.transform((Vector3dc) localDown, new Vector3d()).normalize();
        Vector3d worldDown = new Vector3d(0, -1, 0);
        Quaterniond qDelta = GyroPrecisionController.rotationBetween(currentDown, worldDown);
        return qDelta.mul(currentOrientation, new Quaterniond());
    }

    private static double wrapDegrees(double degrees) {
        double wrapped = degrees % 360.0;
        if (wrapped > 180.0) {
            wrapped -= 360.0;
        } else if (wrapped < -180.0) {
            wrapped += 360.0;
        }
        return wrapped;
    }

    private Vector3d localDownFaceDirection() {
        return JOMLConversion.atLowerCornerOf(downFace.getNormal());
    }

    private void applyRotationalDamping(ServerSubLevel subLevel, RigidBodyHandle body, Quaterniond orientation, double dampingOverride) {
        double activeDamping = Math.min(dampingOverride >= 0 ? dampingOverride : config.dampingPercent(), MAX_DAMPING_PERCENT);
        if (activeDamping <= 0) {
            return;
        }

        double spinRetention = Math.max(0.0, 1.0 - (activeDamping / MAX_DAMPING_PERCENT));

        Vector3d worldOmega = body.getAngularVelocity(new Vector3d());
        Vector3d localOmega = orientation.transformInverse(worldOmega, new Vector3d());
        Vector3d newLocal = new Vector3d(localOmega);

        if (config.stabilizePitch()) {
            newLocal.x = dampAngularSpeed(localOmega.x, spinRetention);
        }
        if (config.stabilizeYaw()) {
            newLocal.y = dampAngularSpeed(localOmega.y, spinRetention);
        }
        if (config.stabilizeRoll()) {
            newLocal.z = dampAngularSpeed(localOmega.z, spinRetention);
        }

        if (localOmega.distanceSquared(newLocal) < 1e-14) {
            return;
        }

        Vector3d newWorld = orientation.transform(newLocal, new Vector3d());
        newWorld.sub(worldOmega, delta);
        body.addLinearAndAngularVelocity(JOMLConversion.ZERO, delta);

        if (config.stabilizePitch()) {
            wheelMomentum.x *= localOmega.x != 0 ? newLocal.x / localOmega.x : spinRetention;
        }
        if (config.stabilizeYaw()) {
            wheelMomentum.y *= localOmega.y != 0 ? newLocal.y / localOmega.y : spinRetention;
        }
        if (config.stabilizeRoll()) {
            wheelMomentum.z *= localOmega.z != 0 ? newLocal.z / localOmega.z : spinRetention;
        }
        syncDisplayFromWheel();
        setChanged();
    }

    private static double dampAngularSpeed(double omega, double spinRetention) {
        if (Math.abs(omega) < SETTLE_ANG_VEL && spinRetention <= 0.05) {
            return 0.0;
        }
        return omega * spinRetention;
    }

    @Override
    public void sable$getAngularVelocity(Vector3d dest) {
        dest.zero();
    }

    public float getDisplayAngleX(float partialTick) {
        return (float) Math.toDegrees(displayAngles.x % (Math.PI * 2));
    }

    public float getDisplayAngleY(float partialTick) {
        return (float) Math.toDegrees(displayAngles.y % (Math.PI * 2));
    }

    public float getDisplayAngleZ(float partialTick) {
        return (float) Math.toDegrees(displayAngles.z % (Math.PI * 2));
    }

    public boolean isLit(Direction side) {
        return switch (side) {
            case NORTH -> litNorth;
            case EAST -> litEast;
            case SOUTH -> litSouth;
            case WEST -> litWest;
            default -> false;
        };
    }

    public GyroMode getMode() { return config.mode(); }

    @Override
    protected void applyImplicitComponents(@NotNull BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        this.config = input.getOrDefault(FNDataComponents.STABILIZER_CONFIG, StabilizerConfig.makeDefault());
    }

    @Override
    protected void collectImplicitComponents(@NotNull DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(FNDataComponents.STABILIZER_CONFIG, this.config);
    }

    @Override
    public CompoundTag getConfigurationData(final HolderLookup.Provider provider, final Player player) {
        final CompoundTag data = super.getConfigurationData(provider, player);
        data.put("Config", StabilizerConfig.CODEC.encodeStart(NbtOps.INSTANCE, this.config).getOrThrow());
        this.redstoneConfig.write(data, provider);
        return data;
    }

    @Override
    public void setConfigurationData(final HolderLookup.Provider provider, final Player player, final CompoundTag data) {
        super.setConfigurationData(provider, player, data);
        NBTUtils.setCompoundIfPresent(data, "Config", (CompoundTag config) -> {
            this.config = StabilizerConfig.CODEC.parse(NbtOps.INSTANCE, config).getOrThrow();
        });
        this.redstoneConfig.read(data, provider);
    }

    @NotNull
    @Override
    public CompoundTag getReducedUpdateTag(@NotNull HolderLookup.Provider provider) {
        CompoundTag updateTag = super.getReducedUpdateTag(provider);
        if (this.angularVelocityImpulse != null) {
            updateTag.put("Impulse", NBTHelper.writeVector3d(this.angularVelocityImpulse));
        }
        if (this.angularVelocityTarget != null) {
            updateTag.put("AVTarget", NBTHelper.writeVector3d(this.angularVelocityTarget));
        }
        if (this.angularTarget != null) {
            updateTag.put("ATarget", NBTHelper.writeVector3d(this.angularTarget));
        }
        if (this.config != null) {
            updateTag.put("Config", StabilizerConfig.CODEC.encodeStart(NbtOps.INSTANCE, this.config).getOrThrow());
        }
        return updateTag;
    }

    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.handleUpdateTag(tag, provider);
        NBTUtils.setListIfPresent(tag, "Impulse", Tag.TAG_DOUBLE, list -> NBTHelper.readVector3dc(list, angularVelocityImpulse));
        NBTUtils.setListIfPresent(tag, "AVTarget", Tag.TAG_DOUBLE, list -> NBTHelper.readVector3dc(list, angularVelocityTarget));
        NBTUtils.setListIfPresent(tag, "ATarget", Tag.TAG_DOUBLE, list -> NBTHelper.readVector3dc(list, angularTarget));

        NBTUtils.setCompoundIfPresent(tag, "Config", (CompoundTag config) -> {
            this.config = StabilizerConfig.CODEC.parse(NbtOps.INSTANCE, config).getOrThrow();
        });
        if (this.config == null) {
            this.config = StabilizerConfig.makeDefault();
        }
    }

    @Override
    public void writeSustainedData(HolderLookup.Provider provider, CompoundTag dataMap) {
        super.writeSustainedData(provider, dataMap);
        if (this.config != null) {
            dataMap.put("Config", StabilizerConfig.CODEC.encodeStart(NbtOps.INSTANCE, this.config).getOrThrow());
        }
    }

    @Override
    public void readSustainedData(HolderLookup.Provider provider, @NotNull CompoundTag dataMap) {
        super.readSustainedData(provider, dataMap);
        NBTUtils.setCompoundIfPresent(dataMap, "Config", (CompoundTag config) -> {
            this.config = StabilizerConfig.CODEC.parse(NbtOps.INSTANCE, config).getOrThrow();
        });
        if (this.config == null) {
            this.config = StabilizerConfig.makeDefault();
        }
    }


    @ComputerMethod
    public Convertable<StabilizerConfig> getStabilizerConfig() throws ComputerException {
        try {
            return Convertable.of(this.config, (o, x) -> {
                DataResult<Object> result =  StabilizerConfig.CODEC.encode(x, JavaOps.INSTANCE, null);
                return result.getOrThrow();
            });
        } catch (IllegalStateException e) {
            throw new ComputerException("Invalid config: %s", e);
        }
    }

    @ComputerMethod
    public void setStabilizerConfigFull(Map<?, ?> config) throws ComputerException {
        try {
            DataResult<Pair<StabilizerConfig, Object>> result = StabilizerConfig.CODEC.decode(JavaOps.INSTANCE, config);
            this.config = result.getOrThrow().getFirst();
            this.setChanged();
        } catch (IllegalStateException e) {
            throw new ComputerException("Invalid config: %s", e);
        }
    }

    @ComputerMethod
    public void setStabilizerConfigPartial(Map<?, ?> config) throws ComputerException {
        try {
            DataResult<Pair<StabilizerConfig.PartialUpdate, Object>> result = StabilizerConfig.PartialUpdate.CODEC.decode(JavaOps.INSTANCE, config);
            result.getOrThrow().getFirst().applyTo(this.config);
            this.setChanged();
        } catch (IllegalStateException e) {
            throw new ComputerException("Invalid config: %s", e);
        }
    }

    public void setConfig(StabilizerConfig config) {
        this.config = config;
        this.setChanged();
    }
}