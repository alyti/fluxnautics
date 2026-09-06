package dev.alyti.fluxnautics.content.blocks.thruster;

import dev.alyti.fluxnautics.config.group.GroupConfig;
import dev.alyti.fluxnautics.index.ThrusterDriver;
import dev.alyti.fluxnautics.lang.FNLang;
import dev.alyti.fluxnautics.multiblock.ThrusterArrayMultiblockData;
import dev.alyti.fluxnautics.registry.FNBlocks;
import mekanism.api.IConfigurable;
import mekanism.api.text.EnumColor;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.proxy.ProxyConfigurable;
import mekanism.common.capabilities.resolver.BasicSidedCapabilityResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;

public class ThrusterArraySubControllerBlockEntity extends ThrusterArrayStructuralBlockEntity implements ProxyConfigurable.ISidedConfigurable {


    public static final ICapabilityProvider<ThrusterArraySubControllerBlockEntity, @Nullable Direction, IConfigurable> CONFIGURABLE_PROVIDER =
            capabilityProvider(Capabilities.CONFIGURABLE, (tile, cap) -> new BasicSidedCapabilityResolver<>(tile, cap, ProxyConfigurable::new));


    public ThrusterArraySubControllerBlockEntity(BlockPos pos, BlockState state) {
        this(FNBlocks.THRUSTER_ARRAY_SUBCONTROLLER, pos, state);
    }

    public ThrusterArraySubControllerBlockEntity(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state);
        delaySupplier = NO_DELAY;
        for (Direction dir : Direction.values()) {
            sideConfigs.put(dir, new SideConfig(0, Optional.empty()));
        }
    }

//    @Override
//    public void addLines(List<Component> list) {
//        ThrusterArrayMultiblockData data = this.getMultiblock();
//        if (!data.isFormed()) {
//            list.add(Component.literal("Not formed, invalid multiblock.").withStyle(ChatFormatting.RED).withStyle(ChatFormatting.ITALIC));
//            return;
//        }
//
//        list.add(Component.literal("Component info:").withStyle(ChatFormatting.AQUA).withStyle(ChatFormatting.UNDERLINE));
//
//        list.add(Component.literal("Side Configuration:"));
//
//        for (Direction relativeDir : Direction.values()) {
//            SideConfig config = sideConfigs.getOrDefault(relativeDir, SideConfig.DEFAULT);
//            if (config == null || config.group == 0) {
//                list.add(Component.literal(" %s - unbound".formatted(getSideName(relativeDir))).withStyle(ChatFormatting.YELLOW));
//            } else {
//                list.add(Component.literal(" %s - %s".formatted(getSideName(relativeDir), config.asComponent())).withStyle(ChatFormatting.AQUA));
//            }
//        }
//
//        super.addLines(list);
//    }

    public record SideConfig(int group, Optional<Integer> driverControl) {
        public static final SideConfig DEFAULT = new SideConfig(0, Optional.empty());
        public Component asComponent() {
            return driverControl.map(integer -> Component.literal("Group %s, Driver %s".formatted(group, integer))).orElseGet(() -> Component.literal("Group %s".formatted(group)));
        }
    }

    public EnumMap<Direction, SideConfig> sideConfigs = new EnumMap<>(Direction.class);
    private boolean formedLatch = false;

    private Direction getOrientation() {
        if (level != null) {
            BlockState state = level.getBlockState(getBlockPos());
            if (state.hasProperty(BlockStateProperties.FACING)) {
                return state.getValue(BlockStateProperties.FACING);
            }
        }
        return Direction.NORTH;
    }

    private Direction toRelative(Direction worldDir) {
        Direction facing = getOrientation();
        if (facing.getAxis() == Direction.Axis.Y) {
            return worldDir; // vertical mount: no defined local frame, stay world-aligned
        }
        if (worldDir == facing) return Direction.NORTH;
        if (worldDir == facing.getOpposite()) return Direction.SOUTH;
        if (worldDir == facing.getCounterClockWise()) return Direction.WEST;
        if (worldDir == facing.getClockWise()) return Direction.EAST;
        return worldDir; // UP / DOWN
    }

    private Direction toWorld(Direction relativeDir) {
        Direction facing = getOrientation();
        if (facing.getAxis() == Direction.Axis.Y) {
            return relativeDir;
        }
        if (relativeDir == Direction.NORTH) return facing;
        if (relativeDir == Direction.SOUTH) return facing.getOpposite();
        if (relativeDir == Direction.WEST) return facing.getCounterClockWise();
        if (relativeDir == Direction.EAST) return facing.getClockWise();
        return relativeDir;
    }
    private String getSideName(Direction relativeDir) {
        return switch (relativeDir) {
            case NORTH -> "Front";
            case SOUTH -> "Back";
            case WEST -> "Left";
            case EAST -> "Right";
            case UP -> "Top";
            case DOWN -> "Bottom";
        };
    }

    private record DriverBinding(ThrusterDriver driver, Object rawDriver, Map<Integer, BiConsumer<Integer, Object>> options) {}

    @Nullable
    private DriverBinding resolveDriverBinding(ThrusterArrayMultiblockData multiblock, int group) {
        if (multiblock == null || !multiblock.isFormed() || group <= 0 || group >= multiblock.thrusters.groups.size()) {
            return null;
        }
        ThrusterArrayMultiblockData.GroupData groupData = multiblock.thrusters.groups.get(group);
        if (groupData == null || groupData.digitalControl) {
            return null;
        }
        GroupConfig<?, ?> driverOpt = groupData.getConfig("driver");
        if (driverOpt == null || !(driverOpt.get() instanceof ThrusterDriver driver)) {
            return null;
        }
        Object rawDriver = groupData.driverStore.get(driver);
        if (rawDriver == null) {
            return null;
        }
        Map<Integer, BiConsumer<Integer, Object>> options = driver.redstoneToDriver(rawDriver);
        if (options == null || options.isEmpty()) {
            return null;
        }
        return new DriverBinding(driver, rawDriver, options);
    }

    private Optional<Integer> initializeDriverControlForGroup(ThrusterArrayMultiblockData multiblock, int group) {
        DriverBinding binding = resolveDriverBinding(multiblock, group);
        if (binding == null) return Optional.empty();
        List<Integer> keys = new ArrayList<>(binding.options().keySet());
        Collections.sort(keys);
        return Optional.of(keys.getFirst());
    }

    private Optional<Integer> cycleDriverControl(ThrusterArrayMultiblockData multiblock, int group, Optional<Integer> currentControl, boolean up) {
        DriverBinding binding = resolveDriverBinding(multiblock, group);
        if (binding == null) return Optional.empty();
        List<Integer> keys = new ArrayList<>(binding.options().keySet());
        Collections.sort(keys);
        int idx = keys.indexOf(currentControl.orElse(-1));
        if (idx == -1) {
            return Optional.of(up ? keys.getFirst() : keys.getLast());
        }
        return up ? (idx + 1 >= keys.size() ? Optional.empty() : Optional.of(keys.get(idx + 1)))
            : (idx - 1 < 0 ? Optional.empty() : Optional.of(keys.get(idx - 1)));
    }

    @NotNull
    @Override
    public InteractionResult onRightClick(@NotNull Player player, @NotNull Direction side) {
        if (!isRemote()) {
            ThrusterArrayMultiblockData multiblock = getMultiblock();
            if (multiblock == null || !multiblock.isFormed()) {
                player.displayClientMessage(Component.literal("Thruster array not formed"), true);
                return InteractionResult.SUCCESS;
            }
            interactSide(multiblock, toRelative(side), true, player);
        }
        return InteractionResult.SUCCESS;
    }

    @NotNull
    @Override
    public InteractionResult onSneakRightClick(@NotNull Player player, @NotNull Direction side) {
        if (!isRemote()) {
            ThrusterArrayMultiblockData multiblock = getMultiblock();
            if (multiblock == null || !multiblock.isFormed()) {
                player.displayClientMessage(Component.literal("Thruster array not formed"), true);
                return InteractionResult.SUCCESS;
            }
            interactSide(multiblock, toRelative(side), false, player);
        }
        return InteractionResult.SUCCESS;
    }

    private void interactSide(ThrusterArrayMultiblockData multiblock, Direction relativeSide, boolean up, Player player) {
        SideConfig config = sideConfigs.getOrDefault(relativeSide, SideConfig.DEFAULT);
        int group = config.group();
        Optional<Integer> driverControl = config.driverControl();
        Optional<Integer> next;
        String value;
        if (driverControl.isPresent()) {
            next = cycleDriverControl(multiblock, group, driverControl, up);
            value = next.map(Object::toString).orElse("None");
        } else {
            group = multiblock.thrusters.bumpGroup(group, up);
            next = initializeDriverControlForGroup(multiblock, group);
            value = Integer.toString(group);
        }
        sideConfigs.put(relativeSide, new SideConfig(group, next));
        player.displayClientMessage(
            (driverControl.isPresent() ? FNLang.THRUSTER_DRIVER_CONTROL_CHANGED : FNLang.THRUSTER_GROUP_CHANGED)
                .translateColored(EnumColor.GRAY, String.format("%s (%s)", value, getSideName(relativeSide))),
            true);
        markForSave();
    }


    @Override
    protected boolean onUpdateServer(ThrusterArrayMultiblockData multiblock) {
        boolean needsPacket = super.onUpdateServer(multiblock);
        boolean formed = multiblock.isFormed();
        if (formed) {
            assert level != null;
            if (!formedLatch) {
                revalidateAfterFormation(multiblock);
            }
            formedLatch = true;
            int maxGroup = multiblock.thrusters.groups.size() - 1;

            int[] bestPlain = new int[maxGroup + 1];
            boolean[] anyPlain = new boolean[maxGroup + 1];
            Map<Integer, Map<Integer, Integer>> bestDriver = new HashMap<>();
            Map<Integer, DriverBinding> bindings = new HashMap<>();

            for (Direction relativeDir : Direction.values()) {
                SideConfig config = sideConfigs.getOrDefault(relativeDir, SideConfig.DEFAULT);
                if (config.group() <= 0 || config.group() > maxGroup) {
                    continue;
                }
                ThrusterArrayMultiblockData.GroupData groupData = multiblock.thrusters.groups.get(config.group());
                if (groupData == null) {
                    continue;
                }
                if (groupData.digitalControl) {
                    if (config.driverControl().isPresent()) {
                        sideConfigs.put(relativeDir, new SideConfig(config.group(), Optional.empty()));
                        markForSave();
                    }
                    continue;
                }
                Direction worldDir = toWorld(relativeDir);
                int signal = level.getSignal(getBlockPos().relative(worldDir), worldDir);

                if (config.driverControl().isEmpty()) {
                    if (!anyPlain[config.group()] || signal > bestPlain[config.group()]) {
                        bestPlain[config.group()] = signal;
                        anyPlain[config.group()] = true;
                    }
                    continue;
                }
                DriverBinding binding = resolveDriverBinding(multiblock, config.group());
                if (binding == null || !binding.options().containsKey(config.driverControl().get())) {
                    sideConfigs.put(relativeDir, new SideConfig(config.group(), Optional.empty()));
                    markForSave();
                    if (binding == null) {
                        if (!anyPlain[config.group()] || signal > bestPlain[config.group()]) {
                            bestPlain[config.group()] = signal;
                            anyPlain[config.group()] = true;
                        }
                    }
                    continue;
                }
                bindings.put(config.group(), binding);
                bestDriver.computeIfAbsent(config.group(), g -> new HashMap<>())
                    .merge(config.driverControl().get(), signal, Math::max);
            }

            for (int group = 1; group <= maxGroup; group++) {
                if (anyPlain[group]) {
                    multiblock.thrusters.groups.get(group).thrustLevel = multiblock.getThrustLevelFromRedstoneLevel(bestPlain[group]);
                }
            }
            for (Map.Entry<Integer, Map<Integer, Integer>> e : bestDriver.entrySet()) {
                DriverBinding binding = bindings.get(e.getKey());
                for (Map.Entry<Integer, Integer> opt : e.getValue().entrySet()) {
                    binding.options().get(opt.getKey()).accept(opt.getValue(), binding.rawDriver());
                }
            }
        } else {
            formedLatch = false;
        }
        setActive(formed);
        return needsPacket;
    }

    private void revalidateAfterFormation(ThrusterArrayMultiblockData multiblock) {
        boolean changed = false;
        int maxGroup = multiblock.thrusters.groups.size() - 1;
        for (Direction dir : Direction.values()) {
            SideConfig config = sideConfigs.getOrDefault(dir, SideConfig.DEFAULT);
            if (config.group() < 0 || config.group() > maxGroup) {
                sideConfigs.put(dir, SideConfig.DEFAULT);
                changed = true;
            } else if (config.driverControl().isPresent() && resolveDriverBinding(multiblock, config.group()) == null) {
                sideConfigs.put(dir, new SideConfig(config.group(), Optional.empty()));
                changed = true;
            }
        }
        if (changed) {
            markForSave();
        }
    }

    @Override
    public void writeSustainedData(HolderLookup.Provider provider, CompoundTag dataMap) {
        super.writeSustainedData(provider, dataMap);
        CompoundTag sidesTag = new CompoundTag();
        for (Map.Entry<Direction, SideConfig> entry : sideConfigs.entrySet()) {
            CompoundTag sideTag = new CompoundTag();
            sideTag.putInt("group", entry.getValue().group());
            entry.getValue().driverControl().ifPresent(v -> sideTag.putInt("driverControl", v));
            sidesTag.put(entry.getKey().getName(), sideTag);
        }
        dataMap.put("sideConfigs", sidesTag);
    }

    @Override
    public void readSustainedData(HolderLookup.Provider provider, CompoundTag dataMap) {
        super.readSustainedData(provider, dataMap);
        if (dataMap.contains("sideConfigs")) {
            CompoundTag sidesTag = dataMap.getCompound("sideConfigs");
            for (Direction dir : Direction.values()) {
                if (sidesTag.contains(dir.getName())) {
                    CompoundTag sideTag = sidesTag.getCompound(dir.getName());
                    int group = sideTag.getInt("group");
                    Optional<Integer> driverControl = sideTag.contains("driverControl") ? Optional.of(sideTag.getInt("driverControl")) : Optional.empty();
                    sideConfigs.put(dir, new SideConfig(group, driverControl));
                }
            }
        } else {
            int legacyGroup = dataMap.getInt("group");
            Optional<Integer> legacyDriver = Optional.empty();
            if (dataMap.contains("driverControl")) {
                legacyDriver = Optional.of(dataMap.getInt("driverControl"));
            }
            sideConfigs.put(Direction.NORTH, new SideConfig(legacyGroup, legacyDriver));
        }
    }
}