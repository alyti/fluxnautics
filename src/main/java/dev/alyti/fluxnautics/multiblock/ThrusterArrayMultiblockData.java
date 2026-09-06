package dev.alyti.fluxnautics.multiblock;


import dev.alyti.fluxnautics.FluxnauticsConfig;
import dev.alyti.fluxnautics.config.group.*;
import dev.alyti.fluxnautics.content.blocks.thruster.IonThrusterBlockEntity;
import dev.alyti.fluxnautics.index.ThrusterDriver;
import dev.alyti.fluxnautics.index.ThrusterEffect;
import dev.alyti.fluxnautics.index.ThrusterPlumeEffect;
import dev.alyti.fluxnautics.lang.FNLang;
import dev.alyti.fluxnautics.math.LerpedFloat;
import dev.alyti.fluxnautics.registry.FNBlockEntityTypes;
import dev.alyti.fluxnautics.util.NBTHelper;
import dev.alyti.fluxnautics.util.Pair;
import dev.alyti.fluxnautics.util.SFColors;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.SerializationConstants;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.attribute.ChemicalAttributes;
import mekanism.api.datamaps.IMekanismDataMapTypes;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.api.text.EnumColor;
import mekanism.api.text.IHasTextComponent;
import mekanism.common.capabilities.chemical.VariableCapacityChemicalTank;
import mekanism.common.capabilities.energy.VariableCapacityEnergyContainer;
import mekanism.common.integration.computer.ComputerException;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.inventory.container.sync.dynamic.ContainerSync;
import mekanism.common.lib.Color;
import mekanism.common.lib.math.voxel.IShape;
import mekanism.common.lib.math.voxel.VoxelCuboid;
import mekanism.common.lib.multiblock.MultiblockData;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.util.NBTUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.Predicate;


public class ThrusterArrayMultiblockData extends MultiblockData {

    private static final List<RecipeError> TRACKED_ERROR_TYPES = List.of(
        RecipeError.NOT_ENOUGH_INPUT,
        RecipeError.NOT_ENOUGH_OUTPUT_SPACE,
        RecipeError.INPUT_DOESNT_PRODUCE_OUTPUT
    );

    @SuppressWarnings("removal")
    public static final Predicate<ChemicalStack> HAS_FUEL = chemical -> chemical.getData(IMekanismDataMapTypes.INSTANCE.chemicalFuel()) != null
        || chemical.hasLegacy(ChemicalAttributes.Fuel.class);//TODO - 1.22 Remove this legacy check

    @NotNull
    @ContainerSync
    @WrappingComputerMethod(wrapper = SpecialComputerMethodWrapper.ComputerChemicalTankWrapper.class, methodNames = {"getInput", "getInputCapacity", "getInputNeeded",
        "getInputFilledPercentage"}, docPlaceholder = "chemical tank")
    public final IChemicalTank chemicalTank;

    @NotNull
    @ContainerSync
    public final VariableCapacityEnergyContainer energyContainer;


    @ContainerSync(getter = "getChemicalCapacity")
    private long clientMaxChemical = 0L;

    @ContainerSync(getter = "getEnergyCapacity")
    private long clientMaxEnergy = 0L;

    @ContainerSync(getter = "getEnergyUsed")
    private long clientEnergyUsed = 0L;
    private long energyUsedLastTick = 0L;

    @ContainerSync
    private final boolean[] trackedErrors = new boolean[TRACKED_ERROR_TYPES.size()];

    @ContainerSync
    public BlockPos origin;

    public final SyncableThrusterData thrusters = new SyncableThrusterData();

    public ThrusterArrayMultiblockData(TileEntityMekanism tile) {
        super(tile);

        energyContainers.add(energyContainer = VariableCapacityEnergyContainer.input(this::getEnergyCapacity, this));
        chemicalTanks.add(chemicalTank = VariableCapacityChemicalTank.input(this, this::getChemicalCapacity, HAS_FUEL, this));
    }

    @Override
    public void onContentsChanged() {
        super.onContentsChanged();
        markDirty();
    }

    public long getEnergyCapacity() {
        return (getLevel() == null || isRemote())  ? clientMaxEnergy : (4_000_000L * Math.max(1, valves.size() * ((long) thrusters.thrustBudget + 1)));
    }

    public long getChemicalCapacity() {
        return (getLevel() == null || isRemote()) ? clientMaxChemical : (64_000L * Math.max(1, valves.size() * ((long) thrusters.thrustBudget + 1)));
    }

    public long getBoosterCount() {
        return 1;
    }

    @Override
    public boolean tick(Level world) {
        boolean needsPacket = super.tick(world);
        long energyUsed = 0;
//        long energyCost = FluxnauticsConfig.common.thrusterArrayJoulePerThrustLevel.get();
        long chemicalUsed = 0;
        long chemicalCost = FluxnauticsConfig.common.thrusterArrayChemicalsPerThrustLevel.get();
        int groupId = 0;
        for (GroupData group : thrusters.groups) {
//            long groupEnergyCost = energyCost * group.thrusters.size();
            if (group.thrustLevel == 0) {
                if (group.thrustLevel != group.realThrustLevel)
                    needsPacket = true;
                group.realThrustLevel = 0;
                continue;
            }
            ThrusterDriver driver = (ThrusterDriver) group.getConfig("driver").get();
            Object rawDriver = group.driverStore.get(driver);
            long groupEnergyCost = driver.cost(this, group, rawDriver);
            long groupEnergyUsed = this.energyContainer.extract(
                groupEnergyCost,
                Action.EXECUTE,
                AutomationType.INTERNAL
            );
            if (groupEnergyUsed < groupEnergyCost) {
                group.realThrustLevel = (double) groupEnergyUsed / groupEnergyCost;
            } else {
                group.realThrustLevel = group.thrustLevel;
            }
            energyUsed += groupEnergyUsed;

            if (groupEnergyUsed > 0) {
                long groupChemicalCost = chemicalCost * group.thrusters.size();
                long groupChemicalRequested = Math.round(groupChemicalCost * group.thrustLevel);
                long groupChemicalUsed = this.chemicalTank.extract(
                    groupChemicalRequested,
                    Action.EXECUTE,
                    AutomationType.INTERNAL
                ).getAmount();
                chemicalUsed += groupChemicalUsed;
            }
            groupId++;
        }

        if (energyUsed != energyUsedLastTick) {
            needsPacket = true;
        }
        energyUsedLastTick = energyUsed;

        for (ThrusterData data : thrusters.thrusterMap.values()) {
            BlockPos absolutePos = data.pos.immutable().offset(origin);
            BlockState state = world.getBlockState(absolutePos);
            if (!Objects.equals(data.blockState, state)) {
                data.blockState = state;
                needsPacket = true;
            }
        }

        if (!world.isClientSide()) {
            needsPacket |= thrusters.tick();
        }
        if (needsPacket) {
            markDirty();
        }
        return needsPacket;
    }

    @Override
    public void readUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        super.readUpdateTag(tag, provider);
        thrusters.read(tag, provider);
        if (tag.contains(SerializationConstants.POSITION)) {
            origin = NbtUtils.readBlockPos(tag, SerializationConstants.POSITION).orElse(null);
        } else {
            origin = null;
        }
    }

    @Override
    public void writeUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        super.writeUpdateTag(tag, provider);
        thrusters.write(tag);
        if (origin != null) {
            tag.put(SerializationConstants.POSITION, NbtUtils.writeBlockPos(origin));
        }
    }

    public BlockPos getRelativePos(BlockPos pos) {
        if (origin == null) {
            return pos;
        }
        return pos.subtract(origin);
    }

    private IShape shape;

    public boolean setShape(IShape shape) {
        this.shape = shape;
        if (shape instanceof VoxelCuboid) {
            return super.setShape(shape);
        }
        return true;
    }

    public IShape getShape() {
        return shape;
    }

    public void addThruster(BlockPos location, Direction opposite, int group) {
        if (!thrusters.thrusterMap.containsKey(location)) {
            thrusters.thrusterMap.put(location, new ThrusterData(location, opposite, group));
            markDirty();
        }
    }

    /// Convert 0-15 redstone levels to thrustLevel accounting for the thrustBudget.
    public double getThrustLevelFromRedstoneLevel(int redstoneLevel) {
        if (redstoneLevel == 0) {
            return 0.0;
        }
        return Math.clamp((redstoneLevel / 15.0) * (1.0 + this.thrusters.thrustBudget), 0.0, 1.0 + this.thrusters.thrustBudget);
    }

    @ComputerMethod
    public long getEnergyUsed() {
        if (getLevel() == null || isRemote())
            return clientEnergyUsed;

        return energyUsedLastTick;
    }

    private GroupData requireGroup(int group) throws ComputerException {
        if (group < 0 || group >= thrusters.groups.size()) {
            throw new ComputerException("Thruster group out of bounds: %d (0..%d)", group, thrusters.groups.size() - 1);
        }
        return thrusters.groups.get(group);
    }

    @ComputerMethod
    public double getThrustBudget() {
        return this.thrusters.thrustBudget;
    }

    @ComputerMethod(methodDescription = "Set thrust level for a group, 0.0 to (1.0 + thrustBudget)", threadSafe = true)
    public void setGroupThrust(int group, double thrust) throws ComputerException {
        GroupData groupData = requireGroup(group);
        groupData.digitalControl = true;
        groupData.thrustLevel = Math.clamp(thrust, 0.0, 1.0 + thrusters.thrustBudget);
        groupData.markUserEdited();
        markDirty();
    }

    @ComputerMethod
    public void setGroupThrustBulk(Map<?, ?> thrusts) throws ComputerException {
        for (Map.Entry<?, ?> entry : thrusts.entrySet()) {
            if (entry.getKey() instanceof Double groupD && entry.getValue() instanceof Double thrust) {
                int group = groupD.intValue();
                GroupData groupData = requireGroup(group);
                groupData.thrustLevel = Math.clamp(thrust, 0.0, 1.0 + thrusters.thrustBudget);
                groupData.markUserEdited();
            } else {
                throw new ComputerException("Unsupported key (%s) or value (%s) type ", entry.getKey().getClass().getName(), entry.getValue().getClass().getName());
            }
        }
        markDirty();
    }

    @ComputerMethod
    public void setGroupDriverData(Map<?, ?> dataPerGroup) throws ComputerException {
        for (Map.Entry<?, ?> entry : dataPerGroup.entrySet()) {
            if (entry.getKey() instanceof Double groupD) {
                int group = groupD.intValue();
                GroupData groupData = requireGroup(group);
                ThrusterDriver driver = (ThrusterDriver) groupData.getConfig("driver").get();
                try {
                    Object parsed = driver.parseDriver((Map<?, ?>) entry.getValue());
                    groupData.driverStore.put(driver, parsed);
                    groupData.markUserEdited();
                } catch (IllegalStateException e) {
                    throw new ComputerException("Invalid driver config: %s", e);
                }
            } else {
                throw new ComputerException("Unsupported key (%s) type ", entry.getKey().getClass().getName());
            }
        }
        markDirty();
    }

    @ComputerMethod
    public double getGroupThrust(int group) throws ComputerException {
        return requireGroup(group).thrustLevel;
    }

    @ComputerMethod(methodDescription = "Release digital control of the group.")
    public void releaseDigitalControl(int group) throws ComputerException {
        GroupData groupData = requireGroup(group);
        groupData.digitalControl = false;
        groupData.markUserEdited();
        markDirty();
    }

    @ComputerMethod(methodDescription = "Take digital control of the group, must be re released with releaseDigitalControl to resume analog/redstone control.")
    public void takeDigitalControl(int group) throws ComputerException {
        GroupData groupData = requireGroup(group);
        groupData.digitalControl = true;
        groupData.markUserEdited();
        markDirty();
    }

    @ComputerMethod(methodDescription = "Get thruster info, table key is the relative position to the thruster array controller.")
    Map<BlockPos, BlockState> getThrusterMap() throws ComputerException {
        Object2ObjectOpenHashMap<BlockPos, BlockState> blocks = new Object2ObjectOpenHashMap<>();
        for (Map.Entry<BlockPos, ThrusterData> entry : thrusters.thrusterMap.entrySet()) {
            blocks.put(entry.getKey(), entry.getValue().blockState);
        }
        return blocks;
    }

    @ComputerMethod(methodDescription = "Set thruster's group, consider setThrusterGroupsByPredicates for large operations.")
    public void setThrusterGroup(int posX, int posY, int posZ, int group) throws ComputerException {
        BlockPos pos = new BlockPos(posX, posY, posZ);
        if (group < 0 || group > thrusters.groups.size() - 1)
            throw new ComputerException("Thruster group out of bounds");

        if (thrusters.thrusterMap.containsKey(pos)) {
            thrusters.thrusterMap.get(pos).groupId = group;
            Optional<IonThrusterBlockEntity> be = this.getLevel().getBlockEntity(this.origin.immutable().offset(pos), FNBlockEntityTypes.ION_THRUSTER.get());
            be.ifPresent(blockEntity -> {
                blockEntity.group = group;
                blockEntity.markForSave();
            });
            thrusters.rebuildGroupThrusterMapCache();
            this.markDirty();
        } else {
            throw new ComputerException("Thruster not found");
        }
    }

    @ComputerMethod(methodDescription = "Set thruster groups based on predicates, sorted by group, first matching predicate wins.")
    public void setThrusterGroupsByPredicates(Map<?, ?> groupsAndPredicates) throws ComputerException {
        List<Pair<Integer, Predicate<ThrusterContext>>> predicates = new ObjectArrayList<>();

        for (Map.Entry<?, ?> entry : groupsAndPredicates.entrySet()) {
            int group = ((Double) entry.getKey()).intValue();
            if (group < 0 || group >= thrusters.groups.size())
                throw new ComputerException("Thruster group out of bounds: %d (0..%d)", group, thrusters.groups.size() - 1);

            if (entry.getValue() instanceof Map<?, ?> rawPredicateMaybe) {
                Predicate<ThrusterContext> predicate = parsePredicate(rawPredicateMaybe);
                predicates.add(Pair.of(group, predicate));
            } else {
                throw new ComputerException("Expected thruster predicate to be a table.");
            }
        }

        predicates.sort(Comparator.comparingInt(Pair::getFirst));

        for (Map.Entry<BlockPos, ThrusterData> entry : thrusters.thrusterMap.entrySet()) {
            ThrusterContext ctx = new ThrusterContext(entry.getKey(), entry.getValue().side);
            for (Pair<Integer, Predicate<ThrusterContext>> predicate : predicates) {
                if (predicate.getSecond().test(ctx)) {
                    this.thrusters.thrusterMap.get(entry.getKey()).groupId = predicate.getFirst();
                    Optional<IonThrusterBlockEntity> be = this.getLevel().getBlockEntity(this.origin.immutable().offset(entry.getKey()), FNBlockEntityTypes.ION_THRUSTER.get());
                    be.ifPresent(blockEntity -> {
                        blockEntity.group = predicate.getFirst();
                        blockEntity.markForSave();
                    });
                    break;
                }
            }
        }
        this.markDirty();
        this.thrusters.rebuildGroupThrusterMapCache();
    }

    private Predicate<ThrusterContext> parsePredicate(Map<?, ?> raw) {
        List<Predicate<ThrusterContext>> predicates = new ArrayList<>();

        if (raw.containsKey("AND")) {
            Object rawList = raw.get("AND");
            if (rawList instanceof Map<?, ?> list) {
                return ctx -> list.values().stream().allMatch(item -> {
                    if (item instanceof Map<?, ?> m) return parsePredicate(m).test(ctx);
                    return false;
                });
            }
        }

        if (raw.containsKey("OR")) {
            Object rawList = raw.get("OR");
            if (rawList instanceof Map<?, ?> list) {
                return ctx -> list.values().stream().anyMatch(item -> {
                    if (item instanceof Map<?, ?> m) return parsePredicate(m).test(ctx);
                    return false;
                });
            }
        }

        if (raw.containsKey("NOT")) {
            Object rawChild = raw.get("NOT");
            if (rawChild instanceof Map<?, ?> childMap) {
                return parsePredicate(childMap).negate();
            }
        }

        // TODO: Consider adding some other fields, maybe neighbor blockState of the thruster (UP, DOWN, LEFT, RIGHT)?

        if (raw.containsKey("x")) predicates.add(ctx -> ctx.pos().getX() == ((Number) raw.get("x")).intValue());
        if (raw.containsKey("x_min")) predicates.add(ctx -> ctx.pos().getX() >= ((Number) raw.get("x_min")).intValue());
        if (raw.containsKey("x_max")) predicates.add(ctx -> ctx.pos().getX() <= ((Number) raw.get("x_max")).intValue());

        if (raw.containsKey("y")) predicates.add(ctx -> ctx.pos().getY() == ((Number) raw.get("y")).intValue());
        if (raw.containsKey("y_min")) predicates.add(ctx -> ctx.pos().getY() >= ((Number) raw.get("y_min")).intValue());
        if (raw.containsKey("y_max")) predicates.add(ctx -> ctx.pos().getY() <= ((Number) raw.get("y_max")).intValue());

        if (raw.containsKey("z")) predicates.add(ctx -> ctx.pos().getZ() == ((Number) raw.get("z")).intValue());
        if (raw.containsKey("z_min")) predicates.add(ctx -> ctx.pos().getZ() >= ((Number) raw.get("z_min")).intValue());
        if (raw.containsKey("z_max")) predicates.add(ctx -> ctx.pos().getZ() <= ((Number) raw.get("z_max")).intValue());

        if (raw.containsKey("facing")) {
            String facing = raw.get("facing").toString();
            Direction facingDir = Direction.byName(facing);
            if (facingDir != null) {
                predicates.add(ctx -> Objects.equals(ctx.side(), facingDir));
            }
        }

        if (predicates.isEmpty()) return ctx -> true;

        return ctx -> {
            for (Predicate<ThrusterContext> p : predicates) {
                if (!p.test(ctx)) return false;
            }
            return true;
        };
    }

    @ComputerMethod(methodDescription = "Make sure groups up to specified amount exist. ex. `{[1]={ name = \"Group 2\", color = \"FF00FF\"}}`")
    public void ensureGroupsExist(int upToGroups) throws ComputerException {
        for (int group = 0; group < upToGroups; group++) {
            this.thrusters.getOrCreateGroup(group);
        }
        markDirty();
    }

    @ComputerMethod(methodDescription = "Create groups if they don't exist and/or apply configs to them.")
    public void ensureGroupsExistDetailed(Map<?, ?> details) throws ComputerException {
        ArrayList<Pair<Integer, GroupBuilder>> todo = new ArrayList<>(details.size());

        for (Map.Entry<?, ?> e : details.entrySet()) {
            int group = ((Double) e.getKey()).intValue(); // Why are all keys doubles, CC?
            if (group < 0 || group > thrusters.groups.size())
                throw new ComputerException("Thruster group (%d) out of bounds (%d)", group, thrusters.groups.size());
            if (e.getValue() instanceof Map<?,?> map) {
                GroupBuilder meta = GroupBuilder.of(map);
                todo.add(Pair.of(group, meta));
            }
        }
        todo.sort(Comparator.comparingInt(Pair::getFirst));

        for (Pair<Integer, GroupBuilder> p : todo) {
            GroupData groupData = thrusters.getOrCreateGroup(p.getFirst());
            p.getSecond().apply(groupData);
            groupData.markUserEdited();
        }
        markDirty();
    }

    public static class SyncableThrusterData {
        public final Map<BlockPos, ThrusterData> thrusterMap = new Object2ObjectOpenHashMap<>();
        public final ObjectArrayList<GroupData> groups = new ObjectArrayList<>();

        public double thrustBudget = 0.0;
        public Vector3d alignmentBudget = new Vector3d(0.0, 0.0, 0.0);

        public SyncableThrusterData() {
            getOrCreateGroup(0);
            groups.getFirst().nameConfig.set("Default");
        }

        public GroupData getOrCreateGroup(int group) {
            if (group < 0 || group > groups.size()) {
                throw new IllegalArgumentException("Group index out of bounds: " + group + " (0.." + groups.size() + ")");
            }
            if (group < groups.size()) {
                return groups.get(group);
            }
            groups.add(group, new GroupData());
            return groups.get(group);
        }

        public int prevHash;
        private int[] prevGroupHashes = new int[0];

        private boolean tick() {
            for (ThrusterData data : thrusterMap.values()) {
                data.prevLevel = data.level;
                data.level = 0;
            }

            if (prevGroupHashes.length != groups.size()) {
                prevGroupHashes = new int[groups.size()];
            }
            for (int i = 0; i < groups.size(); i++) {
                GroupData group = groups.get(i);
                int hash = group.hashCode();
                if (hash != prevGroupHashes[i]) {
                    prevGroupHashes[i] = hash;
                    group.touch();
                }
            }

            int newHash = hashCode();
            boolean ret = newHash != prevHash;
            prevHash = newHash;
            return ret;
        }

        @Override
        public int hashCode() {
            int result = thrusterMap.hashCode();
            result = 31 * result + groups.hashCode();
            result = 31 * result + Double.hashCode(thrustBudget);
            result  = 31 * result + alignmentBudget.hashCode();
            return result;
        }

        public void write(CompoundTag tags) {
            tags.putDouble("thrustBudget", thrustBudget);
            tags.put("alignmentBudget", NBTHelper.writeVector3d(alignmentBudget));

            ListTag list = new ListTag(thrusterMap.size());
            for (ThrusterData data : thrusterMap.values()) {
                CompoundTag tag = new CompoundTag();
                tag.putInt("group", data.groupId);
                tag.put(SerializationConstants.POSITION, NbtUtils.writeBlockPos(data.pos));
                NBTUtils.writeEnum(tag, SerializationConstants.SIDE, data.side);
                tag.putInt(SerializationConstants.LEVEL, data.prevLevel);
                tag.putFloat("hitDistance", data.hitDistance);
                tag.putFloat("maxDistance", data.throttledThrust);
                if (data.blockState != null) {
                    tag.put("blockState", NbtUtils.writeBlockState(data.blockState));
                }
                list.add(tag);
            }
            tags.put("Thrusters", list);

            writeGroups(tags);
        }

        public void writeGroups(CompoundTag tags) {
            ListTag groupList = new ListTag();
            groups.forEach(data -> {
                CompoundTag entry = new CompoundTag();
                entry.putBoolean("digitalControl", data.digitalControl);
                entry.putDouble("level", data.thrustLevel);
                entry.putDouble("realLevel", data.realThrustLevel);
                entry.putLong("lastEdited", data.lastEdited);
                entry.putLong("lastUserEdit", data.lastUserEdit);
                for (GroupConfig<?, ?> config : data.getConfigs()) {
                    config.write(entry);
                }
                if (!data.driverStore.isEmpty()) {
                    CompoundTag driverTag = new CompoundTag();
                    for (Map.Entry<ThrusterDriver, Object> value : data.driverStore.entrySet()) {
                        Tag driverData = value.getKey().serializeDriver(value.getValue());
                        if (driverData == null) continue;
                        driverTag.put(value.getKey().getSerializedName(), driverData);
                    }
                    entry.put("drivers", driverTag);
                }
                groupList.add(entry);
            });
            tags.put("groups", groupList);
        }

        public void read(CompoundTag tags, HolderLookup.Provider provider) {
            if (tags.contains("thrustBudget")) {
                thrustBudget = tags.getDouble("thrustBudget");
            }
            if (tags.contains("Thrusters", Tag.TAG_LIST)) {
                ListTag list = tags.getList("Thrusters", Tag.TAG_COMPOUND);

                if (list.size() != thrusterMap.size()) {
                    thrusterMap.clear();
                }
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag tag = list.getCompound(i);
                    Optional<BlockPos> pos = NbtUtils.readBlockPos(tag, SerializationConstants.POSITION);
                    if (pos.isPresent() && thrusterMap.containsKey(pos.get())) {
                        thrusterMap.get(pos.get()).read(tag, provider);
                    } else {
                        if (pos.isEmpty()) continue;
                        Direction side = Direction.from3DDataValue(tag.getInt(SerializationConstants.SIDE));
                        ThrusterData data = new ThrusterData(pos.get(), side, tag.getInt("group"));
                        data.read(tag, provider);
                        thrusterMap.put(data.pos, data);
                    }
                }
            }

            if (tags.contains("groups", Tag.TAG_LIST)) {
                readGroups(tags, provider);
            }
            NBTUtils.setListIfPresent(tags, "alignmentBudget", Tag.TAG_DOUBLE, matterPosList -> NBTHelper.readVector3dc(matterPosList, alignmentBudget));

            this.rebuildGroupThrusterMapCache();
        }

        public void readGroups(CompoundTag tags, HolderLookup.Provider provider) {
            ListTag listGroups = tags.getList("groups", Tag.TAG_COMPOUND);
            if (listGroups.size() != groups.size()) {
                groups.clear();
            }
            getOrCreateGroup(0);
            for (int i = 0; i < listGroups.size(); i++) {
                CompoundTag tag = listGroups.getCompound(i);
                if (groups.size() > i) {
                    groups.get(i).read(tag, provider);
                } else {
                    GroupData data = new GroupData();
                    data.read(tag, provider);
                    groups.add(i, data);
                }
            }
        }

        public int bumpGroup(int currentGroup, boolean up) {
            return Math.clamp(currentGroup + (up ? 1 : -1), 0, this.groups.size() -1);
        }

        public ThrusterGroupComponent bumpThrusterGroup(BlockPos relPos, boolean up) {
            ThrusterData thruster = this.thrusterMap.get(relPos);
            if (thruster == null) {
                return new ThrusterGroupComponent(getOrCreateGroup(0), 0);
            }
            int newGroup = bumpGroup(thruster.groupId, up);
            thruster.groupId = newGroup;
            GroupData groupData = this.groups.get(newGroup);
            return new ThrusterGroupComponent(groupData, newGroup);
        }

        public void apply(SyncableThrusterData data, Predicate<BlockPos> isReal) {
            applyGroups(data);

            data.thrusterMap.forEach((pos, thrusterData) -> {
                if (!isReal.test(pos))
                    return;
                ThrusterData ourData = this.thrusterMap.computeIfAbsent(pos, k -> new ThrusterData(thrusterData.pos, thrusterData.side, thrusterData.groupId));
                ourData.throttledThrust = thrusterData.throttledThrust;
                ourData.blockState = thrusterData.blockState;
            });
//            this.thrustBudget = data.thrustBudget;
//            this.alignmentBudget.set(data.alignmentBudget);
            this.rebuildGroupThrusterMapCache();
        }

        public void applyGroups(SyncableThrusterData source) {
            for (int i = 0; i < source.groups.size(); i++) {
                GroupData theirs = source.groups.get(i);
                GroupData ours = getOrCreateGroup(i);
                if (theirs.lastUserEdit > ours.lastUserEdit
                    || (theirs.lastUserEdit == ours.lastUserEdit && theirs.lastEdited > ours.lastEdited)) {
                    ours.thrustLevel = theirs.thrustLevel;
                    ours.realThrustLevel = theirs.realThrustLevel;
                    ours.digitalControl = theirs.digitalControl;
                    ours.lastEdited = theirs.lastEdited;
                    ours.lastUserEdit = theirs.lastUserEdit;
                    for (Map.Entry<String, GroupConfig<?, ?>> e : ours.configMap.entrySet()) {
                        GroupConfig<?, ?> other = theirs.getConfig(e.getKey());
                        if (other != null) {
                            CompoundTag encoded = new CompoundTag();
                            other.write(encoded);
                            e.getValue().read(encoded);
                        }
                    }
                    ours.driverStore.clear();
                    ours.driverStore.putAll(theirs.driverStore);
                }
            }
            this.rebuildGroupThrusterMapCache();
        }

        public void rebuildGroupThrusterMapCache() {
            for (GroupData groupData : this.groups) {
                groupData.thrusters.clear();
            }
            for (ThrusterData thrusterData : this.thrusterMap.values()) {
                if (thrusterData.groupId < 0 || thrusterData.groupId > this.groups.size() - 1)
                    continue;
                this.groups.get(thrusterData.groupId).thrusters.add(thrusterData.pos);
            }
        }
    }

    public static class GroupData {
        public Set<BlockPos> thrusters = new ObjectOpenHashSet<>();

        @ContainerSync
        public double thrustLevel;
        @ContainerSync
        public double realThrustLevel;

        public Map<ThrusterDriver, Object> driverStore = new Object2ObjectOpenHashMap<>();

        @ContainerSync
        public boolean digitalControl;

        @ContainerSync
        public final GroupStringConfig nameConfig;
        @ContainerSync
        public final GroupColorConfig colorConfig = new GroupColorConfig("color", FNLang.GUI_GROUP_NAME, SFColors.MEDIA_OURPLE);
        @ContainerSync
        public final GroupBooleanConfig rainbowConfig = new GroupBooleanConfig("rainbow", FNLang.GUI_GROUP_RAINBOW, false);

        public final GroupBooleanConfig mergedConfig = new GroupBooleanConfig("merged_forces", FNLang.GUI_GROUP_MERGED_FORCES, true);

        private final Map<String, GroupConfig<?, ?>> configMap = new LinkedHashMap<>();

        public LerpedFloat clientIntensity = LerpedFloat.linear();

        public long lastEdited = 0;
        public long lastUserEdit = 0;

        public void touch() {
            lastEdited = Math.max(lastEdited + 1, System.currentTimeMillis());
        }

        public void markUserEdited() {
            touch();
            lastUserEdit = Math.max(lastUserEdit + 1, System.currentTimeMillis());
        }

        public GroupData() {
            this.nameConfig = new GroupStringConfig("name", FNLang.GUI_GROUP_NAME, "");
            addConfig(nameConfig);
            addConfig(colorConfig);
            addConfig(rainbowConfig);
            addConfig(mergedConfig);
            addConfig(new GroupEnumConfig<ThrusterEffect>("effect", FNLang.GUI_GROUP_EFFECT, ThrusterEffect.LIGHTNING));
            addConfig(new GroupEnumConfig<ThrusterPlumeEffect>("plume_effect", FNLang.GUI_GROUP_PLUME_EFFECT, ThrusterPlumeEffect.CLASSIC));
            addConfig(new GroupEnumConfig<ThrusterDriver>("driver", FNLang.GUI_GROUP_DRIVER, ThrusterDriver.DIRECT));
            addConfig(new GroupBooleanConfig("alignment", FNLang.GUI_GROUP_ALIGNMENT, true));
        }

        private void addConfig(GroupConfig<?, ?> config) {
            configMap.put(config.getName(), config);
        }

        public Collection<GroupConfig<?, ?>> getConfigs() {
            return configMap.values();
        }

        public GroupConfig<?, ?> getConfig(String name) {
            return configMap.get(name);
        }

        public String getName() {
            String name = nameConfig.get();
            if (name.isEmpty()) {
                return "Unnamed";
            }
            return name;
        }

        @Override
        public int hashCode() {
            return Objects.hash(thrustLevel, realThrustLevel, configMap, digitalControl, driverStore);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GroupData that = (GroupData) o;
            return thrustLevel == that.thrustLevel && realThrustLevel == that.realThrustLevel
                && digitalControl == that.digitalControl
                && Objects.equals(configMap, that.configMap)
                && Objects.equals(driverStore, that.driverStore);
        }

        public void read(CompoundTag tag, HolderLookup.Provider provider) {
            this.thrustLevel = tag.getDouble("level");
            this.realThrustLevel = tag.getDouble("realLevel");
            this.digitalControl = tag.getBoolean("digitalControl");
            this.lastEdited = tag.getLong("lastEdited");
            this.lastUserEdit = tag.getLong("lastUserEdit");
            for (GroupConfig<?, ?> config : this.getConfigs()) {
                config.read(tag);
            }
            if (tag.contains("drivers")) {
                CompoundTag tags = tag.getCompound("drivers");
                this.driverStore.clear();
                for (ThrusterDriver driver : ThrusterDriver.values()) {
                    if (tags.contains(driver.getSerializedName())) {
                        this.driverStore.put(driver, driver.deserializeDriver(tags.getCompound(driver.getSerializedName())));
                    }
                }
            }
        }

        public boolean isMergedThrust() {
            return mergedConfig.get();
        }
    }

    public static class GroupBuilder {
        public CompoundTag configs;

        public GroupBuilder(CompoundTag tag) {
            this.configs = tag;
        }

        public static GroupBuilder of(Map<?, ?> value) throws ComputerException {
            CompoundTag tag = new CompoundTag();
            for (Map.Entry<?, ?> entry : value.entrySet()) {
                if (entry.getKey() instanceof String key) {
                    switch (key) {
                        case "name": {
                            if (entry.getValue() instanceof String string) {
                                (new GroupStringConfig("name", null, string)).write(tag);
                            }
                            break;
                        }
                        case "color": {
                            if (entry.getValue() instanceof String string && string.length() == 6) {
                                int r = HexFormat.fromHexDigits(string, 0, 2);
                                int g = HexFormat.fromHexDigits(string, 2, 4);
                                int b = HexFormat.fromHexDigits(string, 4, 6);
                                (new GroupColorConfig("color", null, Color.argbi(255, r, g, b).rgb())).write(tag);
                            } else if (entry.getValue() instanceof Double intColor) {
                                (new GroupColorConfig("color", null, intColor.intValue())).write(tag);
                            }
                            break;
                        }
                        case "rainbow": {
                            if (entry.getValue() instanceof Boolean bool) {
                                (new GroupBooleanConfig("rainbow", null, bool)).write(tag);
                            }
                            break;
                        }
                        case "merged_forces": {
                            if (entry.getValue() instanceof Boolean bool) {
                                (new GroupBooleanConfig("merged_forces", null, bool)).write(tag);
                            }
                            break;
                        }
                        case "effect": {
                            if (entry.getValue() instanceof String string) {
                                if (Arrays.stream(ThrusterEffect.values()).map(Enum::name).noneMatch((s) -> s.equals(string)))
                                    throw new ComputerException("Unknown ThrusterEffect: " + string + ", only " + Arrays.toString(ThrusterEffect.values()) + " are supported.");

                                (new GroupEnumConfig<ThrusterEffect>("effect", null, ThrusterEffect.valueOf(string))).write(tag);
                            }
                            break;
                        }
                        case "driver": {
                            if (entry.getValue() instanceof String string) {
                                if (Arrays.stream(ThrusterDriver.values()).map(Enum::name).noneMatch((s) -> s.equals(string)))
                                    throw new ComputerException("Unknown ThrusterDriver: " + string + ", only " + Arrays.toString(ThrusterDriver.values()) + " are supported.");

                                (new GroupEnumConfig<ThrusterDriver>("driver", null, ThrusterDriver.valueOf(string))).write(tag);
                            }
                            break;
                        }
                        default:
                            throw new ComputerException("Unsupported group meta key: " + key);
                    }
                } else {
                    throw new ComputerException("Unsupported group meta key type: " + entry.getKey());
                }
            }
            return new GroupBuilder(tag);
        }

        public void apply(GroupData group) {
            for  (Map.Entry<String, GroupConfig<?, ?>> entry : group.configMap.entrySet()) {
                entry.getValue().read(this.configs);
            }
        }
    }

    public static class ThrusterData {
        public final BlockPos pos;
        public final Direction side;

        public int groupId;
        public int prevLevel;
        private int level;
        public float hitDistance;
        public float throttledThrust;
        public BlockState blockState;

        public ThrusterData(BlockPos pos, Direction side, int groupId) {
            this.pos = pos;
            this.side = side;
            this.groupId = groupId;
        }

        @Override
        public int hashCode() {
            int result = 1;
            result = 31 * result + groupId;
            result = 31 * result + pos.hashCode();
            result = 31 * result + prevLevel;
            result = 31 * result + Float.floatToIntBits(hitDistance);
            result = 31 * result + Float.floatToIntBits(throttledThrust);
            result = 31 * result + (blockState != null ? blockState.hashCode() : 0);
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            return o instanceof ThrusterData other && pos.equals(other.pos) && prevLevel == other.prevLevel && groupId == other.groupId &&
                Float.compare(other.hitDistance, hitDistance) == 0 && Float.compare(other.throttledThrust, throttledThrust) == 0 && Objects.equals(blockState, other.blockState);
        }


        public void read(CompoundTag tag, HolderLookup.Provider provider) {
            this.groupId = tag.getInt("group");
            this.prevLevel = tag.getInt(SerializationConstants.LEVEL);
            this.hitDistance = tag.getFloat("hitDistance");
            this.throttledThrust = tag.getFloat("maxDistance");
            if (tag.contains("blockState", Tag.TAG_COMPOUND)) {
                this.blockState = NbtUtils.readBlockState(provider.lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK), tag.getCompound("blockState"));
            }
        }
    }

    public static record ThrusterGroupComponent(GroupData data, int groupId) implements IHasTextComponent {

        @Override
        public @NonNull Component getTextComponent() {
            return FNLang.THRUSTER_GROUP.translateColored(EnumColor.INDIGO, data.getName(), Integer.toString(groupId));
        }
    }

    public static record ThrusterContext(BlockPos pos, Direction side) {

    }
}