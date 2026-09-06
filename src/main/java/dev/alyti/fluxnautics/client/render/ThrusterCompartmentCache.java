package dev.alyti.fluxnautics.client.render;

import dev.alyti.fluxnautics.config.group.GroupConfig;
import dev.alyti.fluxnautics.content.blocks.thruster.IonThrusterBlock;
import dev.alyti.fluxnautics.index.ThrusterDriver;
import dev.alyti.fluxnautics.index.ThrusterDriverHelper;
import dev.alyti.fluxnautics.multiblock.ThrusterArrayMultiblockData;
import dev.alyti.fluxnautics.multiblock.ThrusterArrayMultiblockData.GroupData;
import dev.alyti.fluxnautics.multiblock.ThrusterArrayMultiblockData.ThrusterData;
import dev.alyti.fluxnautics.registry.FNBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;

public final class ThrusterCompartmentCache {

    private static final WeakHashMap<ThrusterArrayMultiblockData, Cache> CLIENT_CACHE = new WeakHashMap<>();

    private ThrusterCompartmentCache() {
    }

    public static List<Compartment> get(ThrusterArrayMultiblockData data) {
        if (data == null || !data.isFormed()) {
            return List.of();
        }
        Cache cache = CLIENT_CACHE.get(data);
        int topology = topologyHash(data);
        if (cache == null || cache.topologyHash != topology) {
            cache = build(data, topology);
            CLIENT_CACHE.put(data, cache);
        }
        return cache.compartments;
    }

    private static int topologyHash(ThrusterArrayMultiblockData data) {
        int h = 1;
        for (Map.Entry<BlockPos, ThrusterData> e : data.thrusters.thrusterMap.entrySet()) {
            h = 31 * h + e.getKey().hashCode();
            ThrusterData t = e.getValue();
            h = 31 * h + t.groupId;
            h = 31 * h + (t.blockState == null ? 0 : t.blockState.hashCode());
        }
        return h;
    }

    private static Cache build(ThrusterArrayMultiblockData data, int topologyHash) {
        Map<BucketKey, List<ThrusterData>> buckets = new LinkedHashMap<>();
        for (ThrusterData t : data.thrusters.thrusterMap.values()) {
            if (t.blockState == null || !t.blockState.is(FNBlocks.ION_THRUSTER)) continue;
            if (t.blockState.getValue(IonThrusterBlock.FRONT)) continue;
            if (t.groupId < 0) continue;
            buckets.computeIfAbsent(new BucketKey(t.groupId, t.side), k -> new ArrayList<>()).add(t);
        }

        List<Compartment> out = new ArrayList<>();
        for (Map.Entry<BucketKey, List<ThrusterData>> bucket : buckets.entrySet()) {
            int groupId = bucket.getKey().groupId();
            Direction side = bucket.getKey().side();
            Direction facing = side.getOpposite();
            Direction[] plane = planeDirs(facing);

            Map<BlockPos, ThrusterData> byPos = new HashMap<>();
            for (ThrusterData t : bucket.getValue()) {
                byPos.put(t.pos, t);
            }
            Set<BlockPos> visited = new HashSet<>();
            for (BlockPos start : byPos.keySet()) {
                if (visited.contains(start)) continue;
                List<ThrusterData> comp = new ArrayList<>();
                ArrayDeque<BlockPos> bfs = new ArrayDeque<>();
                visited.add(start);
                bfs.add(start);
                while (!bfs.isEmpty()) {
                    BlockPos p = bfs.poll();
                    comp.add(byPos.get(p));
                    for (Direction d : plane) {
                        BlockPos n = p.relative(d);
                        if (visited.add(n) && byPos.containsKey(n)) {
                            bfs.add(n);
                        }
                    }
                }
                out.add(buildCompartment(data, groupId, side, facing, plane, comp));
            }
        }

        out.sort(Comparator.comparingInt((Compartment c) -> c.groupId).thenComparing(c -> c.anchor.pos));
        return new Cache(topologyHash, List.copyOf(out));
    }

    private static int coord(BlockPos delta, Direction axis) {
        return delta.getX() * axis.getStepX() + delta.getY() * axis.getStepY() + delta.getZ() * axis.getStepZ();
    }

    private static Compartment buildCompartment(ThrusterArrayMultiblockData data, int groupId, Direction side, Direction facing, Direction[] plane, List<ThrusterData> comp) {
        ThrusterData anchor = comp.stream().min(Comparator.comparing(t -> t.pos)).orElseThrow();
        Direction right = plane[1];
        Direction up = plane[2];

        Map<BlockPos, int[]> cells = new HashMap<>();
        for (ThrusterData t : comp) {
            BlockPos d = t.pos.subtract(anchor.pos);
            cells.put(t.pos, new int[]{coord(d, right), coord(d, up)});
        }

        record EdgeKey(int side, boolean flared, int c) {}
        Map<EdgeKey, List<int[]>> edges = new HashMap<>();
        for (Map.Entry<BlockPos, int[]> e : cells.entrySet()) {
            BlockPos pos = e.getKey();
            int[] o = e.getValue();
            for (int s = 0; s < 4; s++) {
                ThrusterData n = data.thrusters.thrusterMap.get(pos.relative(plane[s]));
                boolean draw;
                if (n == null) draw = true;
                else if (n.blockState == null || !n.blockState.is(FNBlocks.ION_THRUSTER)) draw = true;
                else if (n.blockState.getValue(IonThrusterBlock.FRONT)) draw = true;
                else draw = n.groupId != groupId || n.side != side;
                if (!draw) continue;
                boolean flared = n != null;
                switch (s) {
                    case 0 -> edges.computeIfAbsent(new EdgeKey(0, flared, o[1]), k -> new ArrayList<>()).add(new int[]{o[0], o[0] + 1});
                    case 1 -> edges.computeIfAbsent(new EdgeKey(1, flared, o[0] + 1), k -> new ArrayList<>()).add(new int[]{o[1], o[1] + 1});
                    case 2 -> edges.computeIfAbsent(new EdgeKey(2, flared, o[1] + 1), k -> new ArrayList<>()).add(new int[]{o[0], o[0] + 1});
                    case 3 -> edges.computeIfAbsent(new EdgeKey(3, flared, o[0]), k -> new ArrayList<>()).add(new int[]{o[1], o[1] + 1});
                }
            }
        }
        List<Wall> walls = new ArrayList<>();
        for (Map.Entry<EdgeKey, List<int[]>> entry : edges.entrySet()) {
            List<int[]> spans = entry.getValue();
            spans.sort(Comparator.comparingInt(a -> a[0]));
            int a = spans.getFirst()[0], b = spans.getFirst()[1];
            for (int i = 1; i < spans.size(); i++) {
                int s0 = spans.get(i)[0], s1 = spans.get(i)[1];
                if (s0 <= b) {
                    b = Math.max(b, s1);
                } else {
                    walls.add(new Wall(entry.getKey().side(), entry.getKey().flared(), a, b, entry.getKey().c()));
                    a = s0;
                    b = s1;
                }
            }
            walls.add(new Wall(entry.getKey().side(), entry.getKey().flared(), a, b, entry.getKey().c()));
        }

        Map<Integer, List<int[]>> rows = new TreeMap<>();
        for (int[] o : cells.values()) {
            rows.computeIfAbsent(o[1], k -> new ArrayList<>()).add(new int[]{o[0], o[0] + 1});
        }
        List<Cap> caps = new ArrayList<>();
        for (Map.Entry<Integer, List<int[]>> row : rows.entrySet()) {
            List<int[]> spans = row.getValue();
            spans.sort(Comparator.comparingInt(a -> a[0]));
            int a = spans.getFirst()[0], b = spans.getFirst()[1];
            for (int i = 1; i < spans.size(); i++) {
                int s0 = spans.get(i)[0], s1 = spans.get(i)[1];
                if (s0 <= b) {
                    b = Math.max(b, s1);
                } else {
                    caps.add(new Cap(a, b, row.getKey()));
                    a = s0;
                    b = s1;
                }
            }
            caps.add(new Cap(a, b, row.getKey()));
        }

        return new Compartment(groupId, side, facing, anchor, List.copyOf(walls), List.copyOf(caps), List.copyOf(comp));
    }

    public static Direction[] planeDirs(Direction facing) {
        if (facing.getAxis() == Direction.Axis.Y) {
            boolean upPos = facing.getAxisDirection().getStep() == 1; // facing UP
            Direction up = upPos ? Direction.NORTH : Direction.SOUTH;
            Direction down = upPos ? Direction.SOUTH : Direction.NORTH;
            return new Direction[]{down, Direction.EAST, up, Direction.WEST};
        }
        return new Direction[]{Direction.DOWN, facing.getCounterClockWise(), Direction.UP, facing.getClockWise()};
    }

    public static Vector3d computeThrust(GroupData groupData, Direction side) {
        Direction facing = side.getOpposite();
        GroupConfig<?, ?> cfg = groupData.getConfig("driver");
        ThrusterDriver driver = cfg == null ? null : (ThrusterDriver) cfg.get();
        if (driver == ThrusterDriver.VECTOR) {
            if (groupData.driverStore.get(ThrusterDriver.VECTOR) instanceof ThrusterDriverHelper.VectorDriverData vector && !vector.auto()) {
                Vector3d manual = vector.manualVectoring(facing);
                if (manual != null) {
                    return manual;
                }
            }
        }
        return new Vector3d(side.getStepX(), side.getStepY(), side.getStepZ());
    }

    public static Vector2f compartmentShift(Compartment compartment, GroupData groupData) {
        Vector3d thrust = computeThrust(groupData, compartment.side);
        Quaternionf toLocal = new Quaternionf()
                .rotationX((float) Math.toRadians(90.0))
                .mul(new Quaternionf(compartment.facing.getRotation()).invert());
        Vector3f exhaust = new Vector3f((float) -thrust.x, (float) -thrust.y, (float) -thrust.z).rotate(toLocal);
        return new Vector2f(Mth.clamp(exhaust.x, -1.0f, 1.0f), Mth.clamp(-exhaust.y, -1.0f, 1.0f));
    }

    private record BucketKey(int groupId, Direction side) {}

    private record Cache(int topologyHash, List<Compartment> compartments) {}

    public static final class Compartment {
        public final int groupId;
        public final Direction side;
        public final Direction facing;
        public final ThrusterData anchor;
        public final List<Wall> walls;
        public final List<Cap> caps;
        public final List<ThrusterData> members;

        Compartment(int groupId, Direction side, Direction facing, ThrusterData anchor, List<Wall> walls, List<Cap> caps, List<ThrusterData> members) {
            this.groupId = groupId;
            this.side = side;
            this.facing = facing;
            this.anchor = anchor;
            this.walls = walls;
            this.caps = caps;
            this.members = members;
        }
    }

    public record Wall(int side, boolean flared, int a, int b, int c) {}

    public record Cap(int minX, int maxX, int y) {}
}