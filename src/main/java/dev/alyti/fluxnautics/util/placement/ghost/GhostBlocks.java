package dev.alyti.fluxnautics.util.placement.ghost;

import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

public class GhostBlocks {

    private static final GhostBlocks instance = new GhostBlocks();

    public static GhostBlocks getInstance() {
        return instance;
    }

    public static double getBreathingAlpha() {
        double period = 2500;
        double timer = System.currentTimeMillis() % period;
        double offset = Math.cos((float) ((2d / period) * Math.PI * timer));
        return 0.55d - 0.2d * offset;
    }

    final Map<Object, Entry> ghosts = new HashMap<>();

    public GhostBlockParams showGhostState(Object slot, BlockState state) {
        return showGhostState(slot, state, 1);
    }

    public GhostBlockParams showGhostState(Object slot, BlockState state, int ttl) {
        Entry e = refresh(slot, GhostBlockRenderer.transparent(), GhostBlockParams.of(state), ttl);
        return e.params;
    }

    public GhostBlockParams showGhost(Object slot, GhostBlockRenderer ghost, GhostBlockParams params, int ttl) {
        Entry e = refresh(slot, ghost, params, ttl);
        return e.params;
    }

    private Entry refresh(Object slot, GhostBlockRenderer ghost, GhostBlockParams params, int ttl) {
        Entry e = ghosts.computeIfAbsent(slot, $ -> new Entry(ghost, params, ttl));
        e.ticksToLive = ttl;
        e.params = params;
        e.ghost = ghost;
        return e;
    }

    private GhostBlocks() {}

    public void tickGhosts() {
        ghosts.forEach((slot, entry) -> entry.ticksToLive--);
        ghosts.entrySet().removeIf(e -> !e.getValue().isAlive());
    }

    public void renderAll(com.mojang.blaze3d.vertex.PoseStack ms, net.minecraft.world.phys.Vec3 camera) {
        ghosts.forEach((slot, entry) -> entry.ghost.render(ms, camera, entry.params));
    }

    static class Entry {

        private GhostBlockRenderer ghost;
        private GhostBlockParams params;
        private int ticksToLive;

        public Entry(GhostBlockRenderer ghost, GhostBlockParams params, int ttl) {
            this.ghost = ghost;
            this.params = params;
            this.ticksToLive = ttl;
        }

        public boolean isAlive() {
            return ticksToLive >= 0;
        }
    }
}
