package dev.alyti.fluxnautics.api.content.redstone;

import java.util.*;
import java.util.function.Predicate;
import mekanism.api.RelativeSide;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.component.ITileComponent;
import mekanism.common.util.EnumUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

public class TileComponentRedstoneControl<TILE extends TileEntityMekanism,
    MODE extends Enum<MODE> & IRedstoneControlMode<? super TILE>> implements ITileComponent {

    private static final String NBT_SIDES = "sides";
    private static final String NBT_DIGITAL = "digital";
    private static final String NBT_INVERTED = "inverted";

    private final TILE tile;
    private final Class<MODE> modeClass;
    private final MODE[] modes;
    private final Predicate<RelativeSide> sideFilter;
    private final EnumSet<RelativeSide> invertedSides = EnumSet.noneOf(RelativeSide.class);
    private final Map<RelativeSide, MODE> sideModes = new EnumMap<>(RelativeSide.class);
    private final int[] lastSignal;
    private boolean digitalOverride;

    public TileComponentRedstoneControl(TILE tile, Class<MODE> modeClass) {
        this(tile, modeClass, side -> true);
    }

    public TileComponentRedstoneControl(TILE tile, Class<MODE> modeClass, Predicate<RelativeSide> sideFilter) {
        this.tile = tile;
        this.modeClass = modeClass;
        this.modes = modeClass.getEnumConstants();
        this.lastSignal = new int[this.modes.length];
        this.sideFilter = sideFilter;
        for (RelativeSide side : RelativeSide.values()) {
            sideModes.put(side, this.modes[0]);
        }
        tile.addComponent(this);
    }

    public boolean supports(IRedstoneControlMode<?> mode) {
        return modeIndex(mode) != -1;
    }

    public boolean isSideConfigurable(RelativeSide side) {
        return sideFilter.test(side);
    }

    public Class<MODE> getModeClass() {
        return modeClass;
    }
    public boolean isInverted(RelativeSide side) {
        return invertedSides.contains(side);
    }

    public boolean isDigitalOverride() {
        return digitalOverride;
    }

    public void setDigitalOverride(boolean override) {
        if (digitalOverride == override) {
            return;
        }
        digitalOverride = override;
        tile.markForSave();
        tile.sendUpdatePacket();
    }

    public void setInverted(RelativeSide side, boolean invert) {
        if (digitalOverride || !isSideConfigurable(side) || isInverted(side) == invert) {
            return;
        }
        if (invert) {
            invertedSides.add(side);
        } else {
            invertedSides.remove(side);
        }
        tile.markForSave();
        tile.sendUpdatePacket();
    }

    public boolean setModeOrdinal(RelativeSide side, int ordinal) {
        if (digitalOverride || !isSideConfigurable(side) || ordinal < 0 || ordinal >= modes.length) {
            return false;
        }
        setMode(side, modes[ordinal]);
        return true;
    }

    public void resetAll() {
        if (digitalOverride) {
            return;
        }
        for (RelativeSide side : EnumUtils.SIDES) {
            if (isSideConfigurable(side) && getMode(side) != modes[0]) {
                setMode(side, modes[0]);
            }
            invertedSides.remove(side);
        }
    }

    public MODE getMode(RelativeSide side) {
        return sideModes.get(side);
    }

    public boolean isDefault(RelativeSide side) {
        return sideModes.get(side) == modes[0];
    }

    public void setMode(RelativeSide side, MODE mode) {
        if (!supports(mode) || !isSideConfigurable(side) || sideModes.get(side) == mode) {
            return;
        }
        sideModes.put(side, mode);
        onModeChanged();
    }

    public IRedstoneControlMode<? super TILE> cycleMode(RelativeSide side, int shift) {
        IRedstoneControlMode<? super TILE> current = sideModes.get(side);
        if (!isSideConfigurable(side)) {
            return current;
        }
        int index = Math.floorMod(modeIndex(current) + shift, modes.length);
        MODE mode = modes[index];
        if (mode != current) {
            sideModes.put(side, mode);
            onModeChanged();
        }
        return mode;
    }

    private void onModeChanged() {
        tile.markForSave();
        tile.sendUpdatePacket();
    }

    private int modeIndex(IRedstoneControlMode<?> mode) {
        for (int i = 0; i < modes.length; i++) {
            if (modes[i] == mode) {
                return i;
            }
        }
        return -1;
    }

    public boolean tickServer() {
        Level level = tile.getLevel();
        if (level == null || modes.length == 0) {
            return false;
        }
        int[] aggregated = new int[modes.length];
        boolean[] sampled = new boolean[modes.length];
        Arrays.fill(aggregated, -1);
        BlockPos pos = tile.getBlockPos();
        Direction facing = tile.getDirection();
        for (Map.Entry<RelativeSide, MODE> entry : sideModes.entrySet()) {
            RelativeSide side = entry.getKey();
            IRedstoneControlMode<? super TILE> mode = entry.getValue();
            if (mode == modes[0] || !sideFilter.test(side)) {
                continue;
            }
            int index = modeIndex(mode);
            if (index == -1) {
                continue;
            }
            Direction worldSide = side.getDirection(facing);
            int signal = level.getSignal(pos.relative(worldSide), worldSide);
            int effective = isInverted(side) ? -signal : signal;
            if (!sampled[index]
                || Math.abs(effective) > Math.abs(aggregated[index])
                || (Math.abs(effective) == Math.abs(aggregated[index]) && effective > aggregated[index])) {
                aggregated[index] = effective;
                sampled[index] = true;
            }
        }
        boolean changed = false;
        for (int i = 0; i < modes.length; i++) {
            if (!sampled[i]) {
                continue;
            }
            int signal = aggregated[i];
            if (signal != 0 && lastSignal[i] == 0) {
                modes[i].onRisingEdge(tile, signal);
                changed = true;
            }
            if (signal > 0 || modes[i].drivesAtZeroSignal()) {
                modes[i].drive(tile, signal);
                if (signal != lastSignal[i]) {
                    changed = true;
                }
            }
            lastSignal[i] = signal;
        }
        return changed;
    }

    @Override
    public String getComponentKey() {
        return "redstoneConfig";
    }

    @Override
    public CompoundTag serialize(HolderLookup.Provider provider) {
        CompoundTag nbt = new CompoundTag();
        CompoundTag sides = writeSides();
        if (!sides.isEmpty()) {
            nbt.put(NBT_SIDES, sides);
        }

        int bits = 0;
        for (RelativeSide side : invertedSides) {
            bits |= 1 << side.ordinal();
        }
        nbt.putInt(NBT_INVERTED, bits);
        nbt.putBoolean(NBT_DIGITAL, digitalOverride);
        return nbt;
    }

    @Override
    public void deserialize(CompoundTag nbt, HolderLookup.Provider provider) {
        readSides(nbt.getCompound(NBT_SIDES));

        invertedSides.clear();
        int bits = nbt.getInt(NBT_INVERTED);
        for (RelativeSide side : EnumUtils.SIDES) {
            if ((bits & (1 << side.ordinal())) != 0) {
                invertedSides.add(side);
            }
        }
        digitalOverride = nbt.getBoolean(NBT_DIGITAL);
    }

    @Override
    public void addToUpdateTag(CompoundTag updateTag) {
        CompoundTag sides = writeSides();
        if (!sides.isEmpty() || digitalOverride || !invertedSides.isEmpty()) {
            CompoundTag nbt = new CompoundTag();
            nbt.put(NBT_SIDES, sides);
            int bits = 0;
            for (RelativeSide side : invertedSides) {
                bits |= 1 << side.ordinal();
            }
            nbt.putInt(NBT_INVERTED, bits);
            nbt.putBoolean(NBT_DIGITAL, digitalOverride);
            updateTag.put(getComponentKey(), nbt);
        }
    }

    @Override
    public void readFromUpdateTag(CompoundTag updateTag) {
        CompoundTag nbt = updateTag.getCompound(getComponentKey());
        if (!nbt.isEmpty()) {
            readSides(nbt.getCompound(NBT_SIDES));
            invertedSides.clear();
            int bits = nbt.getInt(NBT_INVERTED);
            for (RelativeSide side : EnumUtils.SIDES) {
                if ((bits & (1 << side.ordinal())) != 0) {
                    invertedSides.add(side);
                }
            }
            digitalOverride = nbt.getBoolean(NBT_DIGITAL);
        }
    }

    private CompoundTag writeSides() {
        CompoundTag sides = new CompoundTag();
        for (Map.Entry<RelativeSide, MODE> entry : sideModes.entrySet()) {
            if (entry.getValue() != modes[0]) {
                sides.putString(entry.getKey().name(), ((Enum<?>) entry.getValue()).name());
            }
        }
        return sides;
    }

    private void readSides(CompoundTag sides) {
        for (RelativeSide side : RelativeSide.values()) {
            sideModes.put(side, modes[0]);
        }
        for (String key : sides.getAllKeys()) {
            RelativeSide side;
            try {
                side = RelativeSide.valueOf(key.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                continue;
            }
            MODE mode = modeByName(sides.getString(key));
            if (mode != null && sideFilter.test(side)) {
                sideModes.put(side, mode);
            }
        }
    }

    private MODE modeByName(String name) {
        for (MODE mode : modes) {
            if (((Enum<?>) mode).name().equals(name)) {
                return mode;
            }
        }
        return null;
    }
}