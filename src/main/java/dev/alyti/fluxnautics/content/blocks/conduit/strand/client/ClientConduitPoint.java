package dev.alyti.fluxnautics.content.blocks.conduit.strand.client;

import it.unimi.dsi.fastutil.objects.ObjectList;
import org.joml.Vector3d;

public record ClientConduitPoint(Vector3d position, Vector3d previousPosition, ObjectList<Snapshot> snapshots) {
    public Vector3d renderPos(final float partialTicks, final Vector3d dest) {
        return dest.set(this.previousPosition).lerp(this.position, partialTicks);
    }

    public record Snapshot(double interpolationTick, Vector3d position) {
    }

}