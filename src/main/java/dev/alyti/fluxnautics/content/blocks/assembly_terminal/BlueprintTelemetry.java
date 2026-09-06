package dev.alyti.fluxnautics.content.blocks.assembly_terminal;

import dev.alyti.fluxnautics.network.packets.blueprint.BlueprintDataPacket;
import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import foundry.veil.api.network.VeilPacketManager;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class BlueprintTelemetry {
    private static final Map<ResourceKey<Level>, Map<ServerSubLevel, BlueprintRecordingTicket>> queuedBlueprintRecordings = new WeakHashMap<>();

    private static final int MAX_FORCES_PER_GROUP = 512;
    private static final int MAX_TOTAL_FORCES = 4096;

    public static void queueBlueprintDataFor(final SubLevel subLevel, final ServerPlayer player) {
        if (!(subLevel instanceof final ServerSubLevel serverSubLevel)) return;
        serverSubLevel.enableIndividualQueuedForcesTracking(true);

        final Map<ServerSubLevel, BlueprintRecordingTicket> map = queuedBlueprintRecordings.get(serverSubLevel.getLevel().dimension());
        BlueprintRecordingTicket ticket = map != null ? map.get(serverSubLevel) : null;

        if (ticket != null && !ticket.isValid()) {
            queuedBlueprintRecordings.remove(serverSubLevel);
            ticket = null;
        }

        if (ticket == null) {
            final List<ServerPlayer> players = new ObjectArrayList<>();
            ticket = new BlueprintRecordingTicket(serverSubLevel, players);
            queuedBlueprintRecordings.computeIfAbsent(serverSubLevel.getLevel().dimension(), x -> new Object2ObjectOpenHashMap<>())
                    .put(serverSubLevel, ticket);
        }

        final List<ServerPlayer> players = ticket.players();
        if (!players.contains(player)) {
            players.add(player);
        }
    }


    public static void postPhysicsTick(final Level level) {
        final Map<ServerSubLevel, BlueprintRecordingTicket> map = queuedBlueprintRecordings.get(level.dimension());
        if (map == null) return;

        final var iter = map.entrySet().iterator();

        while (iter.hasNext()) {
            final var entry = iter.next();
            final ServerSubLevel subLevel = entry.getKey();
            final BlueprintRecordingTicket ticket = entry.getValue();

            if (!ticket.isValid()) {

                iter.remove();

                subLevel.enableIndividualQueuedForcesTracking(false);
                continue;
            }

            final BlueprintDataPacket dataPacket = makeBlueprintDataPacket(ticket.subLevel());

            for (final var player : ticket.players()) {
                VeilPacketManager.player(player).sendPacket(dataPacket);
            }

            subLevel.enableIndividualQueuedForcesTracking(false);
            iter.remove();
        }
    }

    private static BlueprintDataPacket makeBlueprintDataPacket(final ServerSubLevel serverSubLevel) {
        final MassData massTracker = serverSubLevel.getMassTracker();
        final Object2ObjectMap<ForceGroup, List<QueuedForceGroup.PointForce>> sentForces = new Object2ObjectOpenHashMap<>();

        final Object2ObjectMap<ForceGroup, QueuedForceGroup> queuedForceGroups = serverSubLevel.getQueuedForceGroups();

        final ServerLevel level = serverSubLevel.getLevel();
        final SubLevelPhysicsSystem physicsSystem = SubLevelPhysicsSystem.get(level);
        final double timeStep = 1.0 / 20.0 / physicsSystem.getConfig().substepsPerTick;

        if (queuedForceGroups != null) {
            int totalForces = 0;
            for (final Map.Entry<ForceGroup, QueuedForceGroup> entry : queuedForceGroups.entrySet()) {
                final ForceGroup key = entry.getKey();
                final QueuedForceGroup value = entry.getValue();
                final List<QueuedForceGroup.PointForce> pointForces = new ObjectArrayList<>();

                for (final QueuedForceGroup.PointForce pointForce : value.getRecordedPointForces()) {
                    if (pointForces.size() >= MAX_FORCES_PER_GROUP || totalForces >= MAX_TOTAL_FORCES) {
                        break;
                    }
                    final Vector3dc force = new Vector3d(pointForce.force()).div(timeStep);
                    pointForces.add(new QueuedForceGroup.PointForce(pointForce.point(), force));
                    totalForces++;
                }

                if (pointForces.isEmpty()) {
                    continue;
                }

                sentForces.put(key, pointForces);
            }
        }

        final Vector3dc centerOfMass = massTracker.getCenterOfMass();
        final Pose3d pose = serverSubLevel.logicalPose();
        final Vector3d localGravity = pose.transformNormalInverse(DimensionPhysicsData.getGravity(level)).mul(massTracker.getMass());

        sentForces.put(ForceGroups.GRAVITY.get(), List.of(new QueuedForceGroup.PointForce(new Vector3d(centerOfMass), localGravity)));

        return new BlueprintDataPacket(sentForces, massTracker.getMass(), massTracker.getInertiaTensor(), serverSubLevel.latestLinearVelocity, serverSubLevel.latestAngularVelocity);
    }
}
