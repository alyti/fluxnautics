package dev.alyti.fluxnautics.content.items.manipulator;

import dev.alyti.fluxnautics.registry.FNModules;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.FreeConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.gear.IModuleContainer;
import mekanism.api.gear.IModuleHelper;
import mekanism.common.util.StorageUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MekaToolServerHandler {

    private static final Map<ServerLevel, MekaToolServerHandler> MANAGERS = new ConcurrentHashMap<>();
    private final ServerLevel level;
    private final Map<UUID, DragSession> activeSessions = new ConcurrentHashMap<>();

    private MekaToolServerHandler(ServerLevel level) {
        this.level = level;
    }

    public static MekaToolServerHandler get(ServerLevel level) {
        return MANAGERS.computeIfAbsent(level, MekaToolServerHandler::new);
    }

    public static boolean isModuleEnabled(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!stack.isEmpty()) {
            IModuleContainer container = IModuleHelper.INSTANCE.getModuleContainer(stack);
            if (container != null) {
                return container.hasEnabled(FNModules.FIELD_MANIPULATOR_UNIT);
            }
        }
        return false;
    }

    private static final double REACH_GRACE = 2.0;
    private static double maxReachSqr() {
        final double reach = ModuleFieldManipulator.PICK_RANGE + REACH_GRACE;
        return reach * reach;
    }

    public void drag(ServerPlayer player, MekaToolClientHandler.TargetType type, UUID targetId,
                     Vector3dc globalAnchor, Vector3dc localAnchor, Quaterniondc orientation) {
        DragSession s = activeSessions.get(player.getUUID());

        if (s == null) {
            if (!isModuleEnabled(player)) return;

            double size;
            if (type == MekaToolClientHandler.TargetType.SUBLEVEL) {
                ServerSubLevelContainer container = SubLevelContainer.getContainer(this.level);
                if (container == null) return;
                ServerSubLevel subLevel = (ServerSubLevel) container.getSubLevel(targetId);
                if (subLevel == null || subLevel.isRemoved()) return;
                var bb = subLevel.boundingBox();
                double px = Mth.clamp(player.getX(), bb.minX(), bb.maxX());
                double py = Mth.clamp(player.getY(), bb.minY(), bb.maxY());
                double pz = Mth.clamp(player.getZ(), bb.minZ(), bb.maxZ());
                if (player.distanceToSqr(px, py, pz) > maxReachSqr()) return;
                size = characteristicSize(subLevel);
                DragSession session = new DragSession(player, subLevel, globalAnchor, localAnchor, orientation, size);
                clampSessionGoal(session);
                activeSessions.put(player.getUUID(), session);
            } else {
                Entity e = this.level.getEntity(targetId);
                if (!(e instanceof LivingEntity le) || !le.isAlive()) return;
                if (le instanceof ServerPlayer || le == player) return;
                if (le.distanceToSqr(player.getX(), player.getY(), player.getZ()) > maxReachSqr()) return;
                size = characteristicSize(le);
                DragSession session = new DragSession(player, le, size);
                clampSessionGoal(session);
                activeSessions.put(player.getUUID(), session);
            }
            return;
        }

        if (!isModuleEnabled(player)) {
            stopDragging(player);
            return;
        }

        s.playerRelativeGoal.set(globalAnchor);
        clampSessionGoal(s);
        if (s.type == MekaToolClientHandler.TargetType.SUBLEVEL) {
            s.plotAnchor.set(localAnchor);
            s.orientation.set(orientation);
        } else {
            if (!drainEnergy(player, ModuleFieldManipulator.dragCost(s.size))) {
                stopDragging(player);
                return;
            }
            holdEntity(s, player);
        }
    }

    private static void clampSessionGoal(DragSession s) {
        if (s.playerRelativeGoal.lengthSquared() > maxReachSqr()) {
            s.playerRelativeGoal.normalize(ModuleFieldManipulator.PICK_RANGE);
        }
    }

    public void throwTarget(ServerPlayer player, MekaToolClientHandler.TargetType targetType, UUID targetID, Vector3dc impulse) {
        if (!isModuleEnabled(player)) return;

        final double maxImpulse = ModuleFieldManipulator.THROW_SPEED * 1.05;
        Vector3d impulseVec = new Vector3d(impulse);
        if (impulseVec.lengthSquared() > maxImpulse * maxImpulse) {
            impulseVec.normalize(maxImpulse);
        }

        DragSession s = activeSessions.remove(player.getUUID());
        double size;
        ServerSubLevelContainer container = SubLevelContainer.getContainer(this.level);
        if (container == null) {
            if (s != null) s.onRemoved();
            return;
        }

        if (s != null) {
            targetType = s.type;
            size = s.size;

            if (!drainEnergy(player, ModuleFieldManipulator.throwCost(size))) return;

            if (targetType == MekaToolClientHandler.TargetType.SUBLEVEL) {
                if (s.constraint != null) { s.constraint.remove(); s.constraint = null; }
                if (!s.subLevel.isRemoved()) {
                    impulseVec.mul(s.subLevel.getMassTracker().getMass());
                    container.physicsSystem().getPhysicsHandle(s.subLevel).applyLinearImpulse(impulseVec);
                }
            } else {
                LivingEntity le = s.entity;
                if (le.isAlive()) {
                    le.setDeltaMovement(impulseVec.x(), impulseVec.y(), impulseVec.z());
                    le.hurtMarked = true;
                    le.resetFallDistance();
                }
            }
            s.onRemoved();
        } else {
            if (targetType == MekaToolClientHandler.TargetType.SUBLEVEL) {
                ServerSubLevel subLevel = (ServerSubLevel) container.getSubLevel(targetID);
                if (subLevel == null || subLevel.isRemoved()) return;

                size = characteristicSize(subLevel);
                if (!drainEnergy(player, ModuleFieldManipulator.throwCost(size))) return;

                impulseVec.mul(subLevel.getMassTracker().getMass());
                container.physicsSystem().getPhysicsHandle(subLevel).applyLinearImpulse(impulseVec);
            } else {
                Entity e = this.level.getEntity(targetID);
                if (!(e instanceof LivingEntity le) || !le.isAlive()) return;

                size = characteristicSize(le);
                if (!drainEnergy(player, ModuleFieldManipulator.throwCost(size))) return;

                le.setDeltaMovement(impulseVec.x(), impulseVec.y(), impulseVec.z());
                le.hurtMarked = true;
                le.resetFallDistance();
            }
        }
    }

    public void stopDragging(ServerPlayer player) {
        DragSession s = activeSessions.remove(player.getUUID());
        if (s != null) s.onRemoved();
    }

    public void tick() {
        Iterator<Map.Entry<UUID, DragSession>> it = activeSessions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, DragSession> e = it.next();
            DragSession s = e.getValue();
            ServerPlayer p = this.level.getServer().getPlayerList().getPlayer(e.getKey());
            if (p == null || !isModuleEnabled(p)) { s.onRemoved(); it.remove(); continue; }
            if (!drainEnergy(p, ModuleFieldManipulator.dragCost(s.size))) { s.onRemoved(); it.remove(); continue; }
            if (s.type == MekaToolClientHandler.TargetType.ENTITY) {
                holdEntity(s, p);   // vanilla entity hold
            }
            if (s.isStale()) { s.onRemoved(); it.remove(); }
        }
    }

    public void physicsTick(SubLevelPhysicsSystem physicsSystem) {
        for (DragSession s : activeSessions.values()) {
            if (s.type != MekaToolClientHandler.TargetType.SUBLEVEL) continue;
            s.physicsTick(physicsSystem);
        }
    }

    private static void holdEntity(DragSession s, ServerPlayer player) {
        if (!s.entity.isAlive()) return;
        double partial = 0.0; // server-side; no partial tick
        double ex = Mth.lerp(partial, player.xOld, player.getX());
        double ey = Mth.lerp(partial, player.yOld, player.getY()) + player.getEyeHeight();
        double ez = Mth.lerp(partial, player.zOld, player.getZ());
        Vec3 look = player.getLookAngle();
        double dist = s.playerRelativeGoal.length(); // distance encoded in goal magnitude
        double tx = ex + look.x * dist, ty = ey + look.y * dist, tz = ez + look.z * dist;
        s.entity.setDeltaMovement(tx - s.entity.getX(), ty - s.entity.getY(), tz - s.entity.getZ());
        s.entity.setPos(tx, ty, tz);
        s.entity.hurtMarked = true;
        s.entity.resetFallDistance();
    }

    private static boolean drainEnergy(ServerPlayer player, long energy) {
        ItemStack stack = player.getMainHandItem();
        IEnergyContainer energyStorage = StorageUtils.getEnergyContainer(stack, 0);
        if (energyStorage != null) {
            int cost = (int) Math.min(Integer.MAX_VALUE, energy);
            if (energyStorage.extract(cost, Action.SIMULATE, AutomationType.MANUAL) >= cost) {
                energyStorage.extract(cost, Action.EXECUTE, AutomationType.MANUAL);
                return true;
            }
        }
        return false;
    }

    private static double characteristicSize(SubLevel subLevel) {
        var bb = subLevel.boundingBox(); // ⚠ use whatever AABB accessor your SubLevel exposes
        return Math.cbrt(bb.volume());
    }

    private static double characteristicSize(LivingEntity e) {
        return Math.sqrt(e.getBbWidth() * e.getBbWidth() + e.getBbHeight() * e.getBbHeight());
    }

    private static final class DragSession {
        final MekaToolClientHandler.TargetType type;
        final ServerPlayer player;

        final ServerSubLevel subLevel;
        final Vector3d plotAnchor = new Vector3d();
        final Vector3d playerRelativeGoal = new Vector3d();
        final Vector3d localGoal = new Vector3d();
        final Quaterniond orientation = new Quaterniond();
        PhysicsConstraintHandle constraint = null;

        final LivingEntity entity;

        final double size;

        DragSession(ServerPlayer player, ServerSubLevel subLevel, Vector3dc globalAnchor,
                    Vector3dc localAnchor, Quaterniondc orientation, double size) {
            this.type = MekaToolClientHandler.TargetType.SUBLEVEL;
            this.player = player; this.subLevel = subLevel; this.size = size;
            this.entity = null;
            this.playerRelativeGoal.set(globalAnchor);
            this.plotAnchor.set(localAnchor);
            this.orientation.set(orientation);
        }

        DragSession(ServerPlayer player, LivingEntity entity, double size) {
            this.type = MekaToolClientHandler.TargetType.ENTITY;
            this.player = player; this.entity = entity; this.size = size;
            this.subLevel = null;
            this.playerRelativeGoal.set(entity.getX() - player.getX(), entity.getY() - player.getY(), entity.getZ() - player.getZ());
        }

        void physicsTick(SubLevelPhysicsSystem physicsSystem) {
            if (subLevel.isRemoved()) return;
            if (constraint != null) { constraint.remove(); constraint = null; }
            attachConstraint(physicsSystem);

            float angStiff = 8000f, angDamp = 200f, linStiff = 6000f, linDamp = 150f;
            for (ConstraintJointAxis a : ConstraintJointAxis.ANGULAR) {
                constraint.setMotor(a, 0.0, angStiff, angDamp, false, 0.0);
            }

            double partial = physicsSystem.getPartialPhysicsTick();
            double ex = Mth.lerp(partial, player.xOld, player.getX());
            double ey = Mth.lerp(partial, player.yOld, player.getY()) + player.getEyeHeight();
            double ez = Mth.lerp(partial, player.zOld, player.getZ());

            localGoal.set(playerRelativeGoal).add(ex, ey, ez);
            orientation.transformInverse(localGoal);

            constraint.setMotor(ConstraintJointAxis.LINEAR_X, localGoal.x(), linStiff, linDamp, false, 0.0);
            constraint.setMotor(ConstraintJointAxis.LINEAR_Y, localGoal.y(), linStiff, linDamp, false, 0.0);
            constraint.setMotor(ConstraintJointAxis.LINEAR_Z, localGoal.z(), linStiff, linDamp, false, 0.0);
        }

        private void attachConstraint(SubLevelPhysicsSystem physicsSystem) {
            PhysicsPipeline pipeline = physicsSystem.getPipeline();
            FreeConstraintConfiguration cfg = new FreeConstraintConfiguration(
                JOMLConversion.ZERO, plotAnchor, orientation);
            constraint = pipeline.addConstraint(null, subLevel, cfg);
        }

        boolean isStale() {
            if (type == MekaToolClientHandler.TargetType.SUBLEVEL) return subLevel.isRemoved();
            return !entity.isAlive();
        }

        void onRemoved() {
            if (constraint != null) { constraint.remove(); constraint = null; }
        }
    }
}