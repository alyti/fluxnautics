package dev.alyti.fluxnautics.content.items.drone;

import com.mojang.logging.LogUtils;
import dev.alyti.fluxnautics.FluxnauticsClient;
import dev.alyti.fluxnautics.api.interaction.InteractCallback;
import dev.alyti.fluxnautics.content.entities.gear.EntityMekaRobit;
import dev.alyti.fluxnautics.network.packets.meka_robit.ServerboundDroneAssistPacket;
import dev.alyti.fluxnautics.network.packets.meka_robit.ServerboundDroneDataPacket;
import dev.alyti.fluxnautics.network.packets.meka_robit.ServerboundDroneStartStopPacket;
import dev.alyti.fluxnautics.registry.FNDataComponents;
import dev.alyti.fluxnautics.util.FNDistUtil;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.SubLevel;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class MekaRobitClientHandler {
    public float extension = 0;
    public float previousExtension = 0;
    public float targetExtension = 0;
    public Quaternionf lastCubeOrientation = new Quaternionf();
    private State state = State.PASSIVE;
    private MekaRobitItem.DroneMode mode = MekaRobitItem.DroneMode.PATROL;
    private Pair<Pair<Optional<MekaRobitItem.DroneMode>, Optional<EntityMekaRobit>>, Pair<Optional<MekaRobitItem.DroneMode>, Optional<EntityMekaRobit>>> holding;
    private boolean holdingRemote;
    private int lastSentTick = -1;
    private boolean isAssisting = false;
    private ClientFlightSession flightSession;
    private Input savedInput;
    private Vector3f tempVec1 = new Vector3f();
    private Vector3d tempVec2 = new Vector3d();
    private boolean leftClick = false, rightClick = false;

    private enum State { PASSIVE, OPENING }

    public boolean isInFPV() {
        return flightSession != null && flightSession.isActive();
    }

    public EntityMekaRobit getFPVDrone() {
        return flightSession != null ? flightSession.entity() : null;
    }

    public void enterFPV(final LocalPlayer player, final EntityMekaRobit drone) {
        if (this.isInFPV()) return; // Already flying

        final Minecraft mc = Minecraft.getInstance();

        this.savedInput = player.input;
        player.input = new FrozenInput(mc.options);

        this.flightSession = new ClientFlightSession(player, drone);
        mc.setCameraEntity(drone);

        sendToServer(new ServerboundDroneStartStopPacket(drone.getId(), true));

//        ((LivingEntityStickExtension) drone).sable$setPlotPosition(null);
    }

    public void exitFPV() {
        if (this.flightSession == null) return;

        final Minecraft mc = Minecraft.getInstance();
        final EntityMekaRobit drone = this.flightSession.entity();

        if (mc.player != null) {
            mc.setCameraEntity(mc.player);
        }

        if (mc.player != null && this.savedInput != null) {
            mc.player.input = this.savedInput;
            this.savedInput = null;
        }

        if (drone != null && !drone.isRemoved()) {
            sendToServer(new ServerboundDroneStartStopPacket(drone.getId(), false));
        }

        this.flightSession = null;
    }

    public void onItemUsed(final boolean primary) {
        if (!this.holdingRemote)
            return;

        final Minecraft minecraft = Minecraft.getInstance();
        final LocalPlayer player = minecraft.player;
        if (player == null) return;

        if (this.holding != null) {
            final Optional<MekaRobitItem.DroneMode> mainMode = this.holding.first().first();
            final Optional<MekaRobitItem.DroneMode> offMode  = this.holding.second().first();

            final boolean isManual =
                (mainMode.isPresent() && mainMode.get() == MekaRobitItem.DroneMode.MANUAL) ||
                    (offMode.isPresent()  && offMode.get()  == MekaRobitItem.DroneMode.MANUAL);

            if (isManual) {
                tryEnterFPV(player);
                return;
            }
        }

        final Level level = player.level();
        final InteractionHand hand = InteractionHand.MAIN_HAND;

        final HitResult hit = player.pick(128, 1.0f, false);

        if (!(hit instanceof final BlockHitResult blockHitResult) || blockHitResult.getType() == HitResult.Type.MISS) {
            this.state = State.OPENING;
            return;
        }

        final Vec3 hitLocation = hit.getLocation();
        final SubLevel subLevel = Sable.HELPER.getContainingClient(hitLocation);

        if (subLevel == null) {
            this.state = State.OPENING;
            return;
        }

        spawnParticles(hand, subLevel, hitLocation, level);
    }

    private void tryEnterFPV(final LocalPlayer player) {
        UUID boundId = null;

        for (final InteractionHand hand : InteractionHand.values()) {
            final ItemStack stack = player.getItemInHand(hand);
            final Optional<MekaRobitItem.DroneMode> dm = MekaRobitItem.getDroneMode(stack);
            if (dm.isPresent() && dm.get() == MekaRobitItem.DroneMode.MANUAL) {
                boundId = stack.get(FNDataComponents.MEKA_ROBIT_BOUND_ENTITY.get());
                if (boundId != null) break;
            }
        }

        if (boundId == null) return;

        for (final Entity entity : player.level().getEntitiesOfClass(EntityMekaRobit.class, player.getBoundingBox().inflate(128))) {
            if (entity instanceof final EntityMekaRobit robit && entity.getUUID().equals(boundId)) {
                enterFPV(player, robit);
                return;
            }
        }
        // TODO: Drone not loaded on client (too far / different dimension) – silently fail for now
    }
    public void tick() {
        final Player player = FNDistUtil.getClientPlayer();

        if (player == null) {
            this.reset();
            return;
        }

        this.holding = MekaRobitItem.getBothHandDroneModes(player);
        this.holdingRemote = (this.holding.first().second().isPresent() || this.holding.second().second().isPresent());

        if (!this.holdingRemote) {
            this.state = State.PASSIVE;
            this.leftClick = false;
            this.rightClick = false;
        }

        switch (this.state) {
            case OPENING -> {
                if (this.extension > 0.97) {
                    this.state = State.PASSIVE;
                }
            }
            case PASSIVE -> {
            }
        }

        final boolean isUsing = this.state != State.PASSIVE;
        this.targetExtension = isUsing ? 1.0f : 0.0f;

        this.previousExtension = this.extension;
        this.extension = Mth.lerp(0.25f, this.extension, this.targetExtension);

        final Minecraft mc = Minecraft.getInstance();

        int currentTick = player.tickCount;
        boolean canSendPacket = currentTick != this.lastSentTick;
        if (canSendPacket) {
            this.lastSentTick = currentTick;
        }

        if (this.flightSession != null) {
            if (!this.flightSession.isActive()) {
                exitFPV();
                return;
            }

            final LocalPlayer localPlayer = mc.player;
            final EntityMekaRobit drone = this.flightSession.entity();

            if (localPlayer == null || drone == null || drone.isRemoved() || !drone.isAlive()) {
                exitFPV();
                return;
            }

            if (!isStillValidForFPV(localPlayer, drone)) {
                exitFPV();
                return;
            }

            if (mc.screen != null) {
                exitFPV();
                return;
            }

            float forward = 0f, strafe = 0f, up = 0f;
            if (mc.options.keyUp.isDown())    forward += 1f;
            if (mc.options.keyDown.isDown())  forward -= 1f;
            if (mc.options.keyRight.isDown()) strafe  += 1f;
            if (mc.options.keyLeft.isDown())  strafe  -= 1f;
            if (mc.options.keyJump.isDown())  up      += 1f;
            if (mc.options.keyShift.isDown()) up      -= 1f;

            drone.clientForward = forward;
            drone.clientStrafe  = strafe;
            drone.clientUp      = up;

            if (canSendPacket) {
                sendToServer(new ServerboundDroneDataPacket(
                    drone.getId(),
                    forward, strafe, up,
                    drone.getYRot(), drone.getXRot()
                ));
            }
        }

        boolean isAssistMode = false;
        boolean isDeleteMode = false;
        EntityMekaRobit assistDrone = null;

        if (this.isInFPV()) {
            assistDrone = this.getFPVDrone();
            isAssistMode = true; // FPV acts as assist
        } else if (this.holdingRemote) {
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = player.getItemInHand(hand);
                Optional<MekaRobitItem.DroneMode> dm = MekaRobitItem.getDroneMode(stack);
                if (dm.isPresent() && (dm.get() == MekaRobitItem.DroneMode.ASSIST || dm.get() == MekaRobitItem.DroneMode.DELETE)) {
                    isAssistMode = dm.get() == MekaRobitItem.DroneMode.ASSIST;
                    isDeleteMode = dm.get() == MekaRobitItem.DroneMode.DELETE;
                    assistDrone = MekaRobitItem.getDrone(player.level(), stack).orElse(null);
                    break;
                }
            }
        }

        if (assistDrone != null) {
            if (leftClick || rightClick) {
                this.isAssisting = true;

                // Only send continuous updates 20 times a second
                if (canSendPacket) {
                    Entity camera = this.isInFPV() ? assistDrone : player;
                    HitResult hit = camera.pick(64, 1.0f, false);
                    Vec3 visualPos = hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : camera.getEyePosition().add(camera.getLookAngle().scale(64));
                    JOMLConversion.toJOML(visualPos, tempVec2);
                    tempVec1.set(tempVec2);
                    Entity entityTarget = mc.crosshairPickEntity;

                    if (leftClick) {
                        if (hit.getType() == HitResult.Type.BLOCK) {
                            BlockPos pos = ((BlockHitResult) hit).getBlockPos();
                            sendToServer(new ServerboundDroneAssistPacket(assistDrone.getId(), true, true, isDeleteMode, pos, -1, tempVec1));
                        } else {
                            sendToServer(new ServerboundDroneAssistPacket(assistDrone.getId(), false, false, false, BlockPos.ZERO, -1, tempVec1));
                        }
                    }
//                    else {
//                        if (entityTarget instanceof LivingEntity living) {
//                            sendToServer(new ServerboundDroneAssistPacket(assistDrone.getId(), true, false, isDeleteMode, BlockPos.ZERO, living.getId(), tempVec1));
//                        } else {
//                            sendToServer(new ServerboundDroneAssistPacket(assistDrone.getId(), false, false, false, BlockPos.ZERO, -1, tempVec1));
//                        }
//                    }
                }
            } else {
                if (this.isAssisting) {
                    this.isAssisting = false;
                    JOMLConversion.toJOML(Vec3.ZERO, tempVec2);
                    tempVec1.set(tempVec2);
                    sendToServer(new ServerboundDroneAssistPacket(assistDrone.getId(), false, false, false, BlockPos.ZERO, -1, tempVec1));
                }
            }
        } else {
            if (this.isAssisting) {
                this.isAssisting = false;
            }
        }
    }

    /**
     * Client-side check: the player must still be holding a MekaRobit item
     * whose DroneMode is MANUAL and whose bound UUID matches the drone.
     */
    private static boolean isStillValidForFPV(final Player player, final EntityMekaRobit drone) {
        final UUID droneUuid = drone.getUUID();
        for (final InteractionHand hand : InteractionHand.values()) {
            final ItemStack stack = player.getItemInHand(hand);
            final Optional<MekaRobitItem.DroneMode> dm = MekaRobitItem.getDroneMode(stack);
            if (dm.isPresent() && dm.get() == MekaRobitItem.DroneMode.MANUAL) {
                final UUID boundId = stack.get(FNDataComponents.MEKA_ROBIT_BOUND_ENTITY.get());
                if (boundId != null && boundId.equals(droneUuid)) return true;
            }
        }
        return false;
    }

    private void reset() {
        this.extension = 0;
        this.previousExtension = 0;
        this.targetExtension = 0;
        this.lastCubeOrientation.identity();
        this.state = State.PASSIVE;
        this.holdingRemote = false;
        if (this.flightSession != null) {
            exitFPV();
        }
    }

    private static void sendToServer(final CustomPacketPayload packet) {
        final var conn = Minecraft.getInstance().getConnection();
        if (conn != null) {
            conn.send(packet);
        }
    }

    private static void spawnParticles(final InteractionHand hand, final SubLevel subLevel, final Vec3 hitLocation, final Level level) {
        final RandomSource random = level.getRandom();
        final Supplier<Double> randomSpeed = () -> (random.nextDouble() - .5d) * .2f;

        for (int i = 0; i < 10; i++) {
            level.addParticle(ParticleTypes.END_ROD, hitLocation.x, hitLocation.y, hitLocation.z, randomSpeed.get(), randomSpeed.get(), randomSpeed.get());
        }
    }

    public static class MouseHandlerMekaRobit implements InteractCallback {


        @Override
        public Result onAttack(int modifiers, int action, KeyMapping leftKey) {
            final MekaRobitClientHandler handler = FluxnauticsClient.MEKA_ROBIT_CLIENT_HANDLER;
            if (handler.holdingRemote) {
                if ((handler.holding.first().first().equals(Optional.of(MekaRobitItem.DroneMode.MANUAL)) || handler.holding.second().first().equals(Optional.of(MekaRobitItem.DroneMode.MANUAL))) && !handler.isInFPV() && action == GLFW.GLFW_PRESS) {
                    handler.onItemUsed(true);
                    return new Result(true);
                }

                boolean isBound = false;
                for (InteractionHand hand : InteractionHand.values()) {
                    ItemStack stack = Minecraft.getInstance().player.getItemInHand(hand);
                    if (stack.has(FNDataComponents.MEKA_ROBIT_BOUND_ENTITY.get())) {
                        isBound = true;
                        break;
                    }
                }

                if (isBound) {
                    handler.leftClick = action == GLFW.GLFW_PRESS;
                    return new Result(true);
                } else {
                    return InteractCallback.super.onAttack(modifiers, action, leftKey);
                }
            }

            if (handler.leftClick) handler.leftClick = false;
            return InteractCallback.super.onAttack(modifiers, action, leftKey);
        }

        @Override
        public Result onMouseMove(final double yaw, final double pitch) {
            final MekaRobitClientHandler handler = FluxnauticsClient.MEKA_ROBIT_CLIENT_HANDLER;

            if (handler.isInFPV()) {
                final EntityMekaRobit drone = handler.getFPVDrone();
                if (drone != null) {
                    final Minecraft mc = Minecraft.getInstance();
                    double sensitivity = mc.options.sensitivity().get();
                    double mult = sensitivity * 0.6D + 0.2D;
                    mult = mult * mult * mult * 8.0D;
                    mult *= 0.5D;

                    float dYaw   = (float) (yaw   * mult);
                    float dPitch = (float) (pitch * mult);
                    if (mc.options.invertYMouse().get()) dPitch = -dPitch;

                    float newYaw   = (float) Mth.wrapDegrees(drone.getYRot() + dYaw);
                    float newPitch = Mth.clamp(drone.getXRot() + dPitch, -90.0F, 90.0F);

                    drone.setYRot(newYaw);
                    drone.setXRot(newPitch);
                    drone.yHeadRot   = newYaw;
                    drone.clientYRot = newYaw;
                    drone.clientXRot = newPitch;

                    return new Result(true);
                }
            }
            return InteractCallback.super.onMouseMove(yaw, pitch);
        }

        @Override
        public Result onKeyPress(int keyCode, int scanCode, int modifiers) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                final MekaRobitClientHandler handler = FluxnauticsClient.MEKA_ROBIT_CLIENT_HANDLER;
                if (handler.isInFPV()) {
                    handler.exitFPV();

                    return new Result(true);
                }
            }
            return Result.empty();
        }

        @Override
        public Result onScroll(final double deltaX, final double deltaY) {
            if (FluxnauticsClient.MEKA_ROBIT_CLIENT_HANDLER.isInFPV()) {
                return new Result(true);
            }
            return InteractCallback.super.onScroll(deltaX, deltaY);
        }
    }

    private static class FrozenInput extends Input {
        FrozenInput(final Options options) { super(); }

        @Override
        public void tick(final boolean slowDown, final float sensitivityMultiplier) {
            this.leftImpulse     = 0.0F;
            this.forwardImpulse  = 0.0F;
            this.up              = false;
            this.down            = false;
            this.jumping         = false;
            this.shiftKeyDown    = false;
        }
    }

    public record ClientFlightSession(LocalPlayer player, EntityMekaRobit entity) {
        public boolean isActive() {
            return entity != null
                && entity.isAlive()
                && !entity.isRemoved()
                && player != null
                && player.isAlive();
        }
    }
}