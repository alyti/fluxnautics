package dev.alyti.fluxnautics.content.blocks.thruster;

import dev.alyti.fluxnautics.multiblock.ThrusterArrayMultiblockData;
import dev.alyti.fluxnautics.registry.FNBlocks;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import mekanism.api.IConfigurable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.HashMap;
import java.util.Map;


public class ThrusterArrayControllerBlockEntity extends ThrusterArrayStructuralBlockEntity implements IConfigurable, BlockEntitySubLevelActor {

    static final class MergedGroupForce {
        final Vector3d impulse = new Vector3d();
        final Vector3d torque = new Vector3d();
        final Vector3d point = new Vector3d();
        double weight;
        boolean any;
    }

    private final Map<Integer, MergedGroupForce> mergedForces = new HashMap<>();
    private long mergedForcesTick = Long.MIN_VALUE;
    private final Vector3d torqueScratch = new Vector3d();

    public ThrusterArrayControllerBlockEntity(BlockPos pos, BlockState state) {
        super(FNBlocks.THRUSTER_ARRAY_CONTROLLER, pos, state);
        delaySupplier = NO_DELAY;
    }

    @Override
    protected boolean onUpdateServer(ThrusterArrayMultiblockData multiblock) {
        boolean needsPacket = super.onUpdateServer(multiblock);
        if (multiblock.isFormed() && level != null && !multiblock.thrusters.groups.isEmpty()) {
            ThrusterArrayMultiblockData.GroupData group = multiblock.thrusters.groups.get(0);
            if (group != null && !group.digitalControl) {
                int redstoneLevel = level.getBestNeighborSignal(getBlockPos());
                group.thrustLevel = multiblock.getThrustLevelFromRedstoneLevel(redstoneLevel);
            }
        }
        setActive(multiblock.isFormed());
        return needsPacket;
    }

    @Override
    public InteractionResult openGui(Player player) {
        if (isMaster()) {
            return super.openGui(player);
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean canBeMaster() {
        return true;
    }

    @Override
    public void sable$tick(final ServerSubLevel subLevel) {
        if (!isMaster())
            return;
        ThrusterArrayMultiblockData data = getMultiblock();
        if (data == null || !data.isFormed() || !data.hasMaster)
            return;
        Level level = getLevel();
        if (level == null || data.origin == null || data.thrusters.groups.isEmpty())
            return;
        long now = level.getServer().getTickCount();
        if (now - mergedForcesTick <= 10 && !mergedForces.isEmpty())
            return;
        mergedForcesTick = now;

        SubLevelPhysicsSystem physics = SubLevelPhysicsSystem.get(level);
        if (physics == null)
            return;
        double substepDt = 1.0 / 20.0 / physics.getConfig().substepsPerTick;
        RigidBodyHandle handle = physics.getPhysicsHandle(subLevel);

        for (int i = 0; i < data.thrusters.groups.size(); i++) {
            ThrusterArrayMultiblockData.GroupData groupData = data.thrusters.groups.get(i);
            if (!groupData.isMergedThrust())
                continue;
            MergedGroupForce cache = mergedForces.computeIfAbsent(i, k -> new MergedGroupForce());
            cache.impulse.zero();
            cache.torque.zero();
            cache.point.zero();
            cache.weight = 0.0;
            cache.any = false;
            if (groupData.realThrustLevel <= 0 || groupData.getConfig("driver").get() == null)
                continue;
            Vector3dc com = subLevel.getMassTracker().getCenterOfMass();
            if (com == null)
                continue;
            for (BlockPos relPos : groupData.thrusters) {
                BlockEntity tile = level.getBlockEntity(data.origin.immutable().offset(relPos));
                if (!(tile instanceof IonThrusterBlockEntity thruster))
                    continue;
                if (!IonThrusterBlockEntity.computeThrustFor(thruster, data, groupData, subLevel, handle, substepDt))
                    continue;
                Vector3dc force = thruster.THRUST_VECTOR;
                cache.impulse.add(force);
                torqueScratch.set(thruster.THRUST_POSITION).sub(com).cross(force);
                cache.torque.add(torqueScratch);
                double mag = force.length();
                if (mag > 0.0) {
                    cache.point.add(thruster.THRUST_POSITION.x * mag, thruster.THRUST_POSITION.y * mag, thruster.THRUST_POSITION.z * mag);
                    cache.weight += mag;
                }
            }
            if (cache.weight > 0.0) {
                cache.point.div(cache.weight);
                cache.any = true;
            }
        }
    }

    @Override
    public void sable$physicsTick(final ServerSubLevel subLevel, final RigidBodyHandle handle, final double timeStep) {
        if (!isMaster())
            return;
        ThrusterArrayMultiblockData data = getMultiblock();
        if (data == null || !data.isFormed() || !data.hasMaster)
            return;
        Level level = getLevel();
        if (level == null || data.origin == null)
            return;
        for (int i = 0; i < data.thrusters.groups.size(); i++) {
            ThrusterArrayMultiblockData.GroupData groupData = data.thrusters.groups.get(i);
            if (!groupData.isMergedThrust())
                continue;
            MergedGroupForce cache = mergedForces.get(i);
            if (cache == null || !cache.any)
                continue;
            final QueuedForceGroup forceGroup = subLevel.getOrCreateQueuedForceGroup(ForceGroups.PROPULSION.get());
            forceGroup.getForceTotal().applyLinearAndAngularImpulse(cache.impulse, cache.torque);
            forceGroup.recordPointForce(cache.point, cache.impulse);
        }
    }
}
