package dev.alyti.fluxnautics.content.blocks.thruster;

import dev.alyti.fluxnautics.FluxnauticsConfig;
import dev.alyti.fluxnautics.content.blocks.stabilizer.StabilizerConfig;
import dev.alyti.fluxnautics.index.ThrusterDriver;
import dev.alyti.fluxnautics.lang.FNLang;
import dev.alyti.fluxnautics.math.LerpedFloat;
import dev.alyti.fluxnautics.multiblock.ThrusterArrayMultiblockData;
import dev.alyti.fluxnautics.registry.FNBlocks;
import dev.alyti.fluxnautics.registry.FNDataComponents;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import dev.ryanhcode.sable.api.block.propeller.BlockEntityPropeller;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import it.unimi.dsi.fastutil.Pair;
import mekanism.api.text.EnumColor;
import mekanism.common.registries.MekanismDataComponents;
import mekanism.common.util.NBTUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

public class IonThrusterBlockEntity extends ThrusterArrayBoosterBlockEntity
    implements BlockEntitySubLevelActor, BlockSubLevelAssemblyListener, BlockEntityPropeller {

    public Direction getFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(IonThrusterBlock.FACING) ? state.getValue(IonThrusterBlock.FACING) : Direction.NORTH;
    }

    protected LerpedFloat intensity = LerpedFloat.linear();

    public int group;

    private final Map<ThrusterDriver, Object> DRIVER_CACHE = new WeakHashMap<>();

    public IonThrusterBlockEntity(BlockPos pos, BlockState state) {
        super(FNBlocks.ION_THRUSTER, pos, state);
    }

    @Override
    protected boolean onUpdateServer() {
        this.intensity.chase(getPowerModifier(), 0.2, LerpedFloat.Chaser.EXP);
        this.intensity.tickChaser();
        return super.onUpdateServer();
    }

    @Override
    public Direction getBlockDirection() {
        return getFacing().getOpposite();
    }

    public BlockPos getRelativePos() {
        return getMultiblock().getRelativePos(getBlockPos());
    }

    public double getPowerModifier() {
        if (level == null) return 0.0;
        ThrusterArrayMultiblockData data = getMultiblock();
        if (data != null && data.isFormed()) {
            if (group < 0 || group >= data.thrusters.groups.size())
                return 0.0;
            return data.thrusters.groups.get(group).thrustLevel;
        }
        return 0.0;
    }

    @Override
    public void afterMove(ServerLevel oldLevel, ServerLevel newLevel, BlockState state,
                          BlockPos oldPos, BlockPos newPos) {
    }

    @Override
    public InteractionResult onSneakRightClick(Player player) {
        if (!isRemote()) {
            ThrusterArrayMultiblockData.ThrusterGroupComponent component = this.getMultiblock().thrusters.bumpThrusterGroup(this.getRelativePos(), false);
            this.group = component.groupId();
            this.markForSave();
            player.displayClientMessage(FNLang.THRUSTER_GROUP_CHANGED.translateColored(EnumColor.GRAY, component), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult onRightClick(Player player) {
        if (!isRemote()) {
            ThrusterArrayMultiblockData.ThrusterGroupComponent component = this.getMultiblock().thrusters.bumpThrusterGroup(this.getRelativePos(), true);
            this.group = component.groupId();
            this.markForSave();
            player.displayClientMessage(FNLang.THRUSTER_GROUP_CHANGED.translateColored(EnumColor.GRAY, component), true);
        }
        return InteractionResult.SUCCESS;
    }


    @Override
    public void writeSustainedData(HolderLookup.Provider provider, CompoundTag dataMap) {
        super.writeSustainedData(provider, dataMap);
        dataMap.putInt("group", group);
    }

    @Override
    public void readSustainedData(HolderLookup.Provider provider, CompoundTag dataMap) {
        super.readSustainedData(provider, dataMap);
        this.group = dataMap.getInt("group");
    }

    @Override
    protected void applyImplicitComponents(@NotNull BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        this.group = input.getOrDefault(MekanismDataComponents.RADIUS, this.group);
    }

    @Override
    protected void collectImplicitComponents(@NotNull DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (this.group != 0)
            builder.set(MekanismDataComponents.RADIUS, this.group);
    }

    @Override
    public CompoundTag getConfigurationData(final HolderLookup.Provider provider, final Player player) {
        final CompoundTag data = super.getConfigurationData(provider, player);
        data.putInt("Group", group);
        return data;
    }

    @Override
    public void setConfigurationData(final HolderLookup.Provider provider, final Player player, final CompoundTag data) {
        super.setConfigurationData(provider, player, data);
        NBTUtils.setIntIfPresent(data, "Group", (g) -> this.group = g);
    }

    Vector3d THRUST_VECTOR = new Vector3d();
    Vector3d THRUST_POSITION = new Vector3d();

    @Override
    public double getAirflow() {
        return 0.0;
    }

    @Override
    public double getThrust() {
        double intensity = this.intensity.getValue();
        // TODO: Somehow intensity ends up NaN, this is just a workaround for that so physics doesn't freak out.
        if (Double.isNaN(intensity)) return 0.0;
        return intensity * FluxnauticsConfig.common.thrusterArrayThrustValueOne.getAsDouble();
    }

    @Override
    public boolean isActive() {
        return getPowerModifier() > 0;
    }


    @Override
    public double getCurrentAirPressure() {
        if (this.getChemicalInTank(0).getAmount() > 0L)
            return 1.0;
        final Level level = this.getLevel();
        if (level == null)
            return 0.0;
        return DimensionPhysicsData.getAirPressure(level, Sable.HELPER.projectOutOfSubLevel(level, JOMLConversion.toJOML(this.getBlockPos().getCenter())));
    }

    @Override
    public void sable$physicsTick(final ServerSubLevel subLevel, final RigidBodyHandle handle, final double timeStep) {
        if (this.isActive()) {
            ThrusterArrayMultiblockData data = this.getMultiblock();
            if (data == null || !data.isFormed() || !data.hasMaster)
                return;
            if (this.group > data.thrusters.groups.size() - 1)
                return;
            ThrusterArrayMultiblockData.GroupData groupData = data.thrusters.groups.get(this.group);
            // groups in merged thrust mode are driven entirely by the master controller's actor:
            // it aggregates the whole group once per game tick (sable$tick) and the individual
            // thrusters bow out here, so neither the driver math nor the force application runs
            // per thruster per substep
            if (groupData.isMergedThrust())
                return;
            applyThrustFor(this, data, groupData, subLevel, handle, timeStep);
        }
    }

    static boolean computeThrustFor(IonThrusterBlockEntity thruster, ThrusterArrayMultiblockData data,
                                    ThrusterArrayMultiblockData.GroupData groupData, ServerSubLevel subLevel,
                                    RigidBodyHandle handle, double timeStep) {
        ThrusterDriver driver = (ThrusterDriver) groupData.getConfig("driver").get();

        Object rawCache = thruster.DRIVER_CACHE.get(driver);
        Object rawDriver = groupData.driverStore.get(driver);

        Pair<ThrusterDriver.ImpulseGenerator, Optional<Object>> result = driver.drive(thruster, data, groupData, data.getRelativePos(thruster.getBlockPos()), rawCache, rawDriver, subLevel);
        result.right().ifPresent(newCache -> thruster.DRIVER_CACHE.put(driver, newCache));
        result.left().impulse(thruster.THRUST_VECTOR, thruster, groupData.realThrustLevel, subLevel, handle, timeStep);
        thruster.THRUST_POSITION.set(JOMLConversion.atCenterOf(thruster.getBlockPos()));
        if (!data.thrusters.alignmentBudget.equals(0,0,0) && Boolean.TRUE.equals(groupData.getConfig("alignment").get())) {
            Vector3dc com = subLevel.getMassTracker().getCenterOfMass();
            // given center of mass, and the alignmentBudget (which is how much X,Y,Z can be changed)
            // we calculate the new THRUST_POSITION to be close to center of mass within the alignment budget
            assert com != null;
            double deltaX = com.x() - thruster.THRUST_POSITION.x;
            double deltaY = com.y() - thruster.THRUST_POSITION.y;
            double deltaZ = com.z() - thruster.THRUST_POSITION.z;

            deltaX = Math.clamp(deltaX, -data.thrusters.alignmentBudget.x(), data.thrusters.alignmentBudget.x());
            deltaY = Math.clamp(deltaY, -data.thrusters.alignmentBudget.y(), data.thrusters.alignmentBudget.y());
            deltaZ = Math.clamp(deltaZ, -data.thrusters.alignmentBudget.z(), data.thrusters.alignmentBudget.z());

            thruster.THRUST_POSITION.add(deltaX, deltaY, deltaZ);
        }
        return true;
    }

    static void applyThrustFor(IonThrusterBlockEntity thruster, ThrusterArrayMultiblockData data,
                               ThrusterArrayMultiblockData.GroupData groupData, ServerSubLevel subLevel,
                               RigidBodyHandle handle, double timeStep) {
        if (!computeThrustFor(thruster, data, groupData, subLevel, handle, timeStep))
            return;
        final QueuedForceGroup forceGroup = subLevel.getOrCreateQueuedForceGroup(ForceGroups.PROPULSION.get());
        forceGroup.applyAndRecordPointForce(new Vector3d(thruster.THRUST_POSITION), new Vector3d(thruster.THRUST_VECTOR));
    }
}
