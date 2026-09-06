package dev.alyti.fluxnautics;

import dev.alyti.fluxnautics.content.blocks.assembly_terminal.BlueprintTelemetry;
import dev.alyti.fluxnautics.content.blocks.conduit.strand.server.ServerConduitTrackingSystem;
import dev.alyti.fluxnautics.content.blocks.conduit.strand.server.ServerLevelConduitManager;
import dev.alyti.fluxnautics.content.items.manipulator.MekaToolServerHandler;
import dev.alyti.fluxnautics.content.items.manipulator.MekaToolSubLevelObserver;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.sublevel.system.SubLevelTrackingSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Objects;

public class FluxnauticsSableCommonEvents {

    public static void onPhysicsTick(final SubLevelPhysicsSystem physicsSystem, final double timeStep) {
        final ServerLevel level = physicsSystem.getLevel();
        Objects.requireNonNull(ServerLevelConduitManager.getOrCreate(level)).physicsTick(physicsSystem, timeStep);
        MekaToolServerHandler.get(level).physicsTick(physicsSystem);
    }


    public static void onPostPhysicsTick(final SubLevelPhysicsSystem physicsSystem, final double timeStep) {
        BlueprintTelemetry.postPhysicsTick(physicsSystem.getLevel());
    }


    public static void onContainerReady(final Level level, final SubLevelContainer subLevelContainer) {
        if (!(subLevelContainer instanceof final ServerSubLevelContainer serverContainer)) {
            return;
        }

        serverContainer.addObserver(new MekaToolSubLevelObserver(serverContainer.getLevel()));

        final SubLevelTrackingSystem trackingSystem = serverContainer.trackingSystem();
        trackingSystem.addTrackingPlugin(new ServerConduitTrackingSystem(serverContainer.getLevel()));
    }
}
