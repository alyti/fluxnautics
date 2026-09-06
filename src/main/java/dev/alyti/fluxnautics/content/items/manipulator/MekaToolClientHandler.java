package dev.alyti.fluxnautics.content.items.manipulator;

import dev.alyti.fluxnautics.FluxnauticsClient;
import dev.alyti.fluxnautics.api.interaction.InteractCallback;
import dev.alyti.fluxnautics.network.packets.manipulator.ServerboundManipulatorDragPacket;
import dev.alyti.fluxnautics.network.packets.manipulator.ServerboundManipulatorStopPacket;
import dev.alyti.fluxnautics.network.packets.manipulator.ServerboundManipulatorThrowPacket;
import dev.alyti.fluxnautics.registry.FNModules;
import dev.alyti.fluxnautics.util.FNDistUtil;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import mekanism.api.gear.IModuleContainer;
import mekanism.api.gear.IModuleHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.AxisAngle4d;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

public class MekaToolClientHandler {

    public enum TargetType { SUBLEVEL, ENTITY }

    private Vector3d tempVec = new Vector3d();
    private Vector3d tempVec2 = new Vector3d();
    private ClientDragSession dragSession = null;

    public static boolean isModuleActive(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (!stack.isEmpty()) {
            IModuleContainer container = IModuleHelper.INSTANCE.getModuleContainer(stack);
            if (container != null) {
                return container.hasEnabled(FNModules.FIELD_MANIPULATOR_UNIT);
            }
        }
        return false;
    }


    private void startDragSubLevel(SubLevel subLevel, BlockPos blockPos, LocalPlayer player) {
        Vector3d localAnchor = JOMLConversion.atCenterOf(blockPos);
        Quaterniond orientation = new Quaterniond(subLevel.logicalPose().orientation());
        double dist = clampDistance(player.getEyePosition().distanceTo(
            JOMLConversion.toMojang(subLevel.logicalPose().transformPosition(localAnchor, tempVec))));
        this.dragSession = new ClientDragSession(subLevel, localAnchor, orientation, dist);
    }

    private void startDragEntity(LivingEntity entity) {
        LocalPlayer player = Minecraft.getInstance().player;
        double dist = clampDistance(player.getEyePosition().distanceTo(entity.position()));
        this.dragSession = new ClientDragSession(entity, dist);
    }

    private void stopDragging() {
        if (this.dragSession == null) return;
        ServerboundManipulatorStopPacket.send();
        this.dragSession = null;
    }

    public void tick() {
        final Player player = FNDistUtil.getClientPlayer();
        if (this.dragSession == null) return;

        if (!isModuleActive(player)) {
            stopDragging();
            return;
        }

        if (this.dragSession.type == TargetType.SUBLEVEL) {
            if (this.dragSession.subLevel.isRemoved()) { stopDragging(); return; }
        } else {
            if (!this.dragSession.entity.isAlive()) { stopDragging(); return; }
        }

        Vec3 goal = player.getLookAngle().scale(this.dragSession.distance);
        Vector3dc globalAnchor = JOMLConversion.toJOML(goal);
        Quaterniondc orientation = this.dragSession.type == TargetType.SUBLEVEL
            ? this.dragSession.orientation : new Quaterniond();
        Vector3dc localAnchor = this.dragSession.type == TargetType.SUBLEVEL
            ? this.dragSession.localAnchor : new Vector3d();

        ServerboundManipulatorDragPacket.send(
            this.dragSession.type,
            this.dragSession.type == TargetType.SUBLEVEL ? this.dragSession.subLevel.getUniqueId() : this.dragSession.entity.getUUID(),
            globalAnchor, localAnchor, orientation);
    }

    private static boolean isRotateHeld() {
        long handle = Minecraft.getInstance().getWindow().getWindow();
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_TAB) == GLFW.GLFW_PRESS;
    }

    private static double clampDistance(double d) {
        return Math.clamp(d, 2.0, ModuleFieldManipulator.PICK_RANGE);
    }

    public static final class ClientDragSession {
        final TargetType type;
        final SubLevel subLevel;
        final Vector3dc localAnchor;
        final Quaterniond orientation;
        final LivingEntity entity;
        double distance;

        private ClientDragSession(SubLevel subLevel, Vector3dc localAnchor, Quaterniond orientation, double distance) {
            this.type = TargetType.SUBLEVEL; this.subLevel = subLevel;
            this.localAnchor = localAnchor; this.orientation = orientation;
            this.entity = null; this.distance = distance;
        }
        private ClientDragSession(LivingEntity entity, double distance) {
            this.type = TargetType.ENTITY; this.entity = entity;
            this.subLevel = null; this.localAnchor = null; this.orientation = null;
            this.distance = distance;
        }
    }

    public static class MouseHandler implements InteractCallback {

        @Override
        public Result onAttack(int modifiers, int action, KeyMapping leftKey) {
            MekaToolClientHandler handler = FluxnauticsClient.MEKA_TOOL_CLIENT_HANDLER;
            LocalPlayer player = Minecraft.getInstance().player;

            if (!isModuleActive(player)) {
                if (handler.dragSession != null) handler.stopDragging();
                return InteractCallback.super.onAttack(modifiers, action, leftKey);
            }

            if (action != GLFW.GLFW_PRESS) return InteractCallback.super.onAttack(modifiers, action, leftKey);

            MekaToolClientHandler.TargetType targetType;
            UUID targetId;

            if (handler.dragSession != null) {
                targetType = handler.dragSession.type;
                targetId = targetType == TargetType.SUBLEVEL ? handler.dragSession.subLevel.getUniqueId() : handler.dragSession.entity.getUUID();

                Vec3 look = player.getLookAngle();
                Vec3 impulse = look.scale(ModuleFieldManipulator.THROW_SPEED);
                ServerboundManipulatorThrowPacket.send(targetType, targetId, JOMLConversion.toJOML(impulse));

                handler.stopDragging();
                return new Result(true);
            } else {
                HitResult crosshair = Minecraft.getInstance().hitResult;
                if (crosshair instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le && le.isAlive() && le != player) {
                    targetType = TargetType.ENTITY;
                    targetId = le.getUUID();
                } else {
                    HitResult pick = player.pick(ModuleFieldManipulator.PICK_RANGE, 1.0f, false);
                    if (!(pick instanceof BlockHitResult bhr) || bhr.getType() == HitResult.Type.MISS) return InteractCallback.super.onAttack(modifiers, action, leftKey);
                    Vec3 hitLocation = pick.getLocation();
                    ClientSubLevel subLevel = Sable.HELPER.getContainingClient(hitLocation);
                    if (subLevel == null) return InteractCallback.super.onAttack(modifiers, action, leftKey);

                    targetType = TargetType.SUBLEVEL;
                    targetId = subLevel.getUniqueId();
                }

                Vec3 look = player.getLookAngle();
                Vec3 impulse = look.scale(ModuleFieldManipulator.THROW_SPEED);
                ServerboundManipulatorThrowPacket.send(targetType, targetId, JOMLConversion.toJOML(impulse));
                return new Result(true);
            }
        }

        @Override
        public Result onUse(int modifiers, int action, KeyMapping rightKey) {
            MekaToolClientHandler handler = FluxnauticsClient.MEKA_TOOL_CLIENT_HANDLER;
            LocalPlayer player = Minecraft.getInstance().player;

            assert player != null;
            if (!isModuleActive( player )) {
                if (handler.dragSession != null) handler.stopDragging();
                return InteractCallback.super.onUse(modifiers, action, rightKey);
            }
            if (action != GLFW.GLFW_PRESS) return InteractCallback.super.onUse(modifiers, action, rightKey);

            if (handler.dragSession != null) {
                handler.stopDragging();
                return new Result(true);
            }

            HitResult crosshair = Minecraft.getInstance().hitResult;
            if (crosshair instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le
                && le.isAlive() && le != player) {
                handler.startDragEntity(le);
                return new Result(true);
            }

            HitResult pick = player.pick(ModuleFieldManipulator.PICK_RANGE, 1.0f, false);
            if (!(pick instanceof BlockHitResult bhr) || bhr.getType() == HitResult.Type.MISS) return new Result(true);
            Vec3 hitLocation = pick.getLocation();
            ClientSubLevel subLevel = Sable.HELPER.getContainingClient(hitLocation);
            if (subLevel == null) return new Result(true);
            handler.startDragSubLevel(subLevel, bhr.getBlockPos(), player);
            return new Result(true);
        }

        @Override
        public Result onMouseMove(double yaw, double pitch) {
            MekaToolClientHandler handler = FluxnauticsClient.MEKA_TOOL_CLIENT_HANDLER;
            LocalPlayer player = Minecraft.getInstance().player;

            assert player != null;
            if (!isModuleActive( player )) {
                if (handler.dragSession != null) handler.stopDragging();
                return InteractCallback.super.onMouseMove(yaw, pitch);
            }

            if (handler.dragSession == null || handler.dragSession.type != TargetType.SUBLEVEL)
                return InteractCallback.super.onMouseMove(yaw, pitch);
            if (!isRotateHeld()) return InteractCallback.super.onMouseMove(yaw, pitch);

            Vec3 axis = player.calculateViewVector(0.0f, player.getYRot() - 90.0f);
            Quaterniond orientation = handler.dragSession.orientation;
            final double sens = 1.0;
            double yawChange = Math.toRadians(yaw) * sens;
            orientation.rotateLocalY(yawChange);
            orientation.premul(new Quaterniond(new AxisAngle4d(Math.toRadians(-pitch) * sens, axis.x, axis.y, axis.z)));
            return new Result(true);
        }

        @Override
        public Result onScroll(double deltaX, double deltaY) {
            MekaToolClientHandler handler = FluxnauticsClient.MEKA_TOOL_CLIENT_HANDLER;
            LocalPlayer player = Minecraft.getInstance().player;

            assert player != null;
            if (!isModuleActive( player )) {
                if (handler.dragSession != null) handler.stopDragging();
                return InteractCallback.super.onScroll(deltaX, deltaY);
            }
            if ( handler.dragSession == null) return InteractCallback.super.onScroll(deltaX, deltaY);

            double current = handler.dragSession.distance;
            boolean sprint = Minecraft.getInstance().options.keySprint.isDown();
            double mult = Mth.clamp(Math.pow(current / 10.0, 0.5), 1.0, 5) * (sprint ? 4 : 1);
            handler.dragSession.distance = clampDistance(current + deltaY * 0.5 * mult);
            return new Result(true);
        }
    }
}