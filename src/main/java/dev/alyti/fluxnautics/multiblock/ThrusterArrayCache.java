package dev.alyti.fluxnautics.multiblock;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import mekanism.common.lib.multiblock.MultiblockCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

public class ThrusterArrayCache extends MultiblockCache<ThrusterArrayMultiblockData> {

    private static final String GROUPS_KEY = "fluxnautics_groups";
    private static final String USER_EDIT_KEY = "lastUserEdit";
    private static final String LAST_EDITED_KEY = "lastEdited";

    private static final String[] RUNTIME_KEYS = {"level", "realLevel", LAST_EDITED_KEY, USER_EDIT_KEY};

    public static boolean DEBUG = false;
    public static boolean VERBOSE_KEYS = false;

    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    private static final int BANK_LIMIT = 32;
    private static final long BANK_TTL_MS = 10 * 60 * 1000L;
    private static final double BANK_MIN_COVERAGE = 0.75;
    private static final long BANK_MIN_INTERVAL_MS = 1000L;

    private static final long SYNC_LOG_MIN_INTERVAL_MS = 500L;

    private static final String[] LOG_NOISE_KEYS = {LAST_EDITED_KEY, "realLevel"};

    private record BankedSnapshot(long stamp, CompoundTag snapshot, LongOpenHashSet worldPositions, boolean edited) {}

    private static final Map<Level, ArrayDeque<BankedSnapshot>> SNAPSHOT_BANK = new WeakHashMap<>();

    @Nullable
    private CompoundTag groupsSnapshot;

    private long lastSyncLogStamp;

    @Override
    public void sync(ThrusterArrayMultiblockData data) {
        super.sync(data);
        CompoundTag fresh = captureSnapshot(data);
        if (this.groupsSnapshot != null && hasUserEdit(this.groupsSnapshot) && !hasUserEdit(fresh)) {
            if (DEBUG) {
                LOGGER.info("[fluxnautics/cache] sync: REFUSED blank overwrite - kept edited snapshot {}", describe(this.groupsSnapshot));
            }
            return;
        }
        CompoundTag previous = this.groupsSnapshot;
        this.groupsSnapshot = fresh;
        if (DEBUG) {
            logSync("sync", previous, fresh);
        }
        bankSnapshot(data, fresh);
    }

    private void logSync(String label, @Nullable CompoundTag previous, CompoundTag fresh) {
        if (previous != null && stripNoise(previous).equals(stripNoise(fresh))) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastSyncLogStamp < SYNC_LOG_MIN_INTERVAL_MS) {
            return;
        }
        lastSyncLogStamp = now;
        LOGGER.info("[fluxnautics/cache] {}: snapshot now {}", label, describe(fresh));
    }

    private static CompoundTag stripNoise(CompoundTag snapshot) {
        ListTag groups = snapshot.getList("groups", Tag.TAG_COMPOUND);
        ListTag stripped = new ListTag(groups.size());
        for (int i = 0; i < groups.size(); i++) {
            CompoundTag entry = groups.getCompound(i).copy();
            for (String key : LOG_NOISE_KEYS) {
                entry.remove(key);
            }
            stripped.add(entry);
        }
        CompoundTag out = new CompoundTag();
        out.put("groups", stripped);
        return out;
    }

    public void syncGroupsNow(ThrusterArrayMultiblockData data) {
        CompoundTag fresh = captureSnapshot(data);
        if (this.groupsSnapshot != null && hasUserEdit(this.groupsSnapshot) && !hasUserEdit(fresh)) {
            if (DEBUG) {
                LOGGER.info("[fluxnautics/cache] syncGroupsNow: REFUSED blank overwrite - kept edited snapshot {}", describe(this.groupsSnapshot));
            }
            return;
        }
        this.groupsSnapshot = fresh;
        if (DEBUG) {
            LOGGER.info("[fluxnautics/cache] syncGroupsNow (pre-deform flush): {}", describe(fresh));
        }
        bankSnapshot(data, fresh);
    }

    @Override
    public void apply(HolderLookup.Provider provider, ThrusterArrayMultiblockData data) {
        super.apply(provider, data);
        boolean formationEdited = false;
        for (ThrusterArrayMultiblockData.GroupData group : data.thrusters.groups) {
            if (group.lastUserEdit > 0) {
                formationEdited = true;
                break;
            }
        }
        CompoundTag snapshotTag = this.groupsSnapshot;
        if ((snapshotTag == null || !hasUserEdit(snapshotTag)) && !formationEdited) {
            CompoundTag recovered = recoverFromBank(data);
            if (recovered != null) {
                snapshotTag = recovered;
                this.groupsSnapshot = recovered;
            }
        }
        if (snapshotTag != null) {
            if (DEBUG) {
                LOGGER.info("[fluxnautics/cache] apply: merging {} into a formation that had {} group(s)",
                    describe(snapshotTag), data.thrusters.groups.size());
            }
            ThrusterArrayMultiblockData.SyncableThrusterData snapshot = new ThrusterArrayMultiblockData.SyncableThrusterData();
            snapshot.readGroups(snapshotTag.copy(), provider);
            data.thrusters.applyGroups(snapshot);
        } else if (DEBUG) {
            LOGGER.info("[fluxnautics/cache] apply: cache has NO groups snapshot - formation starts from defaults");
        }
    }

    @Override
    public void merge(MultiblockCache<ThrusterArrayMultiblockData> mergeCache, RejectContents rejectContents) {
        super.merge(mergeCache, rejectContents);
        if (!(mergeCache instanceof ThrusterArrayCache other)) {
            return;
        }
        if (this.groupsSnapshot == null) {
            this.groupsSnapshot = other.groupsSnapshot;
            if (DEBUG) {
                LOGGER.info("[fluxnautics/cache] merge: adopted snapshot from merged cache: {}",
                    other.groupsSnapshot == null ? "none" : describe(other.groupsSnapshot));
            }
            return;
        }
        if (other.groupsSnapshot == null) {
            if (DEBUG) {
                LOGGER.info("[fluxnautics/cache] merge: kept own snapshot (other had none): {}", describe(this.groupsSnapshot));
            }
            return;
        }
        this.groupsSnapshot = reconcileSnapshots(this.groupsSnapshot, other.groupsSnapshot);
        if (DEBUG) {
            LOGGER.info("[fluxnautics/cache] merge: reconciled per-group by edit recency: {}", describe(this.groupsSnapshot));
        }
    }

    @Override
    public void save(HolderLookup.Provider provider, CompoundTag nbtTags) {
        super.save(provider, nbtTags);
        if (this.groupsSnapshot != null) {
            nbtTags.put(GROUPS_KEY, this.groupsSnapshot.copy());
        }
        if (DEBUG) {
            LOGGER.info("[fluxnautics/cache] save: {}", this.groupsSnapshot == null ? "no groups snapshot" : describe(this.groupsSnapshot));
        }
    }

    @Override
    public void load(HolderLookup.Provider provider, CompoundTag nbtTags) {
        super.load(provider, nbtTags);
        if (nbtTags.contains(GROUPS_KEY, Tag.TAG_COMPOUND)) {
            this.groupsSnapshot = nbtTags.getCompound(GROUPS_KEY).copy();
        }
        if (DEBUG) {
            LOGGER.info("[fluxnautics/cache] load: {}", this.groupsSnapshot == null ? "no groups snapshot on disk" : describe(this.groupsSnapshot));
        }
    }

    private CompoundTag captureSnapshot(ThrusterArrayMultiblockData data) {
        CompoundTag fresh = new CompoundTag();
        data.thrusters.writeGroups(fresh);
        stampUserEdits(fresh);
        return fresh;
    }

    private void stampUserEdits(CompoundTag fresh) {
        if (this.groupsSnapshot == null) {
            return;
        }
        ListTag previous = this.groupsSnapshot.getList("groups", Tag.TAG_COMPOUND);
        ListTag current = fresh.getList("groups", Tag.TAG_COMPOUND);
        long now = System.currentTimeMillis();
        for (int i = 0; i < current.size(); i++) {
            CompoundTag entry = current.getCompound(i);
            CompoundTag prev = i < previous.size() ? previous.getCompound(i) : null;
            if (prev == null || configsChanged(prev, entry)) {
                entry.putLong(USER_EDIT_KEY, Math.max(entry.getLong(USER_EDIT_KEY), now));
            }
        }
    }

    private static boolean configsChanged(CompoundTag previousEntry, CompoundTag currentEntry) {
        CompoundTag a = previousEntry.copy();
        CompoundTag b = currentEntry.copy();
        for (String key : RUNTIME_KEYS) {
            a.remove(key);
            b.remove(key);
        }
        return !a.equals(b);
    }

    private static boolean hasUserEdit(CompoundTag groupsSnapshotTag) {
        ListTag list = groupsSnapshotTag.getList("groups", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            if (list.getCompound(i).getLong(USER_EDIT_KEY) > 0) {
                return true;
            }
        }
        return false;
    }

    private static CompoundTag reconcileSnapshots(CompoundTag own, CompoundTag other) {
        ListTag a = own.getList("groups", Tag.TAG_COMPOUND);
        ListTag b = other.getList("groups", Tag.TAG_COMPOUND);
        ListTag merged = new ListTag();
        int count = Math.max(a.size(), b.size());
        for (int i = 0; i < count; i++) {
            CompoundTag ea = i < a.size() ? a.getCompound(i) : null;
            CompoundTag eb = i < b.size() ? b.getCompound(i) : null;
            CompoundTag pick;
            if (ea == null) {
                pick = eb;
            } else if (eb == null) {
                pick = ea;
            } else {
                pick = isNewerEntry(eb, ea) ? eb : ea;
            }
            merged.add(pick.copy());
        }
        CompoundTag out = new CompoundTag();
        out.put("groups", merged);
        return out;
    }

    private static boolean isNewerEntry(CompoundTag candidate, CompoundTag current) {
        long candidateUser = candidate.getLong(USER_EDIT_KEY);
        long currentUser = current.getLong(USER_EDIT_KEY);
        if (candidateUser != currentUser) {
            return candidateUser > currentUser;
        }
        return candidate.getLong(LAST_EDITED_KEY) > current.getLong(LAST_EDITED_KEY);
    }

    private void bankSnapshot(ThrusterArrayMultiblockData data, CompoundTag snapshot) {
        Level level = data.getLevel();
        if (level == null || level.isClientSide() || data.origin == null || data.thrusters.thrusterMap.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        ArrayDeque<BankedSnapshot> bank = SNAPSHOT_BANK.computeIfAbsent(level, l -> new ArrayDeque<>());
        BankedSnapshot last = bank.peekLast();
        boolean edited = hasUserEdit(snapshot);
        if (last != null && last.snapshot.equals(snapshot)) {
            return;
        }
        if (last != null && now - last.stamp < BANK_MIN_INTERVAL_MS && !(edited && !last.edited)) {
            return;
        }
        LongOpenHashSet positions = new LongOpenHashSet(data.thrusters.thrusterMap.size());
        for (BlockPos relative : data.thrusters.thrusterMap.keySet()) {
            positions.add(relative.offset(data.origin).asLong());
        }
        bank.addLast(new BankedSnapshot(now, snapshot.copy(), positions, edited));
        while (bank.size() > BANK_LIMIT) {
            BankedSnapshot oldest = bank.pollFirst();
            if (oldest != null && oldest.edited) {
                for (Iterator<BankedSnapshot> it = bank.iterator(); it.hasNext(); ) {
                    if (!it.next().edited) {
                        it.remove();
                        break;
                    }
                }
            }
        }
        for (Iterator<BankedSnapshot> it = bank.iterator(); it.hasNext(); ) {
            if (now - it.next().stamp > BANK_TTL_MS) {
                it.remove();
            }
        }
        if (DEBUG && edited) {
            LOGGER.info("[fluxnautics/cache] bank: stored edited snapshot ({} thruster positions, bank size {})", positions.size(), bank.size());
        }
    }

    @Nullable
    private CompoundTag recoverFromBank(ThrusterArrayMultiblockData data) {
        Level level = data.getLevel();
        if (level == null || data.origin == null || data.thrusters.thrusterMap.isEmpty()) {
            return null;
        }
        ArrayDeque<BankedSnapshot> bank = SNAPSHOT_BANK.get(level);
        if (bank == null || bank.isEmpty()) {
            return null;
        }
        long now = System.currentTimeMillis();
        LongOpenHashSet formation = new LongOpenHashSet(data.thrusters.thrusterMap.size());
        for (BlockPos relative : data.thrusters.thrusterMap.keySet()) {
            formation.add(relative.offset(data.origin).asLong());
        }
        BankedSnapshot best = null;
        double bestScore = 0.0;
        for (BankedSnapshot candidate : bank) {
            if (now - candidate.stamp > BANK_TTL_MS) {
                continue;
            }
            int hits = 0;
            for (long pos : formation) {
                if (candidate.worldPositions.contains(pos)) {
                    hits++;
                }
            }
            double coverage = (double) hits / formation.size();
            if (coverage < BANK_MIN_COVERAGE) {
                continue;
            }
            boolean better;
            if (best == null) {
                better = true;
            } else if (candidate.edited != best.edited) {
                better = candidate.edited;
            } else if (coverage != bestScore) {
                better = coverage > bestScore;
            } else {
                better = candidate.stamp > best.stamp;
            }
            if (better) {
                best = candidate;
                bestScore = coverage;
            }
        }
        if (best == null) {
            return null;
        }
        if (DEBUG) {
            LOGGER.info("[fluxnautics/cache] bank: recovered snapshot for formation (coverage {}%, {})",
                String.format("%.0f", bestScore * 100.0), describe(best.snapshot));
        }
        return best.snapshot.copy();
    }

    private static String describe(CompoundTag groupsSnapshotTag) {
        ListTag list = groupsSnapshotTag.getList("groups", Tag.TAG_COMPOUND);
        StringBuilder sb = new StringBuilder(list.size() + " group(s)");
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            sb.append(" | [").append(i).append(']')
                .append(" uEdit=").append(entry.getLong(USER_EDIT_KEY))
                .append(" e=").append(entry.getLong(LAST_EDITED_KEY))
                .append(" lvl=").append(entry.getDouble("level"));
            if (VERBOSE_KEYS) {
                sb.append(" keys=").append(entry.getAllKeys());
            }
        }
        return sb.toString();
    }
}
