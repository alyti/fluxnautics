package dev.alyti.fluxnautics.content.blocks.thruster;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import dev.alyti.fluxnautics.Fluxnautics;
import mekanism.api.radial.ClassBasedRadialData;
import mekanism.api.radial.RadialData;
import mekanism.api.radial.mode.IRadialMode;
import mekanism.api.radial.mode.NestedRadialMode;
import mekanism.api.text.EnumColor;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NonNull;

public final class ThrusterRadials {

    private ThrusterRadials() {}

    public static final RadialData<ThrusterFacingMode> FACING =
        new EnumRadialDataFN<>(Fluxnautics.id("thruster_facing"), ThrusterFacingMode.AUTO);

    public static final RadialData<ThrusterSnapMode> SNAP =
        new EnumRadialDataFN<>(Fluxnautics.id("thruster_snap"), ThrusterSnapMode.FREE);

    public record GroupSlice(int groupId, Component sliceName, ResourceLocation icon) implements IRadialMode {

        public static final int AUTO_REPRESENTATION = 0;
        public static final int HINT_REPRESENTATION = 1;
        public static final int GROUP_REPRESENTATION_OFFSET = 1_000_000;

        public static final GroupSlice AUTO = new GroupSlice(-1,
            Component.translatable("placement.fluxnautics.groups.auto"), iconOf("groups_auto"));

        public static final GroupSlice HINT = new GroupSlice(-2,
            Component.translatable("placement.fluxnautics.groups.hint"), iconOf("groups_hint"));

        public static GroupSlice of(int groupId) {
            return new GroupSlice(groupId, Component.translatable("placement.fluxnautics.groups.slice", groupId),
                iconOf("group"));
        }

        public boolean isAuto() {
            return groupId == -1;
        }

        public boolean isHint() {
            return groupId == -2;
        }

        public int networkRepresentation() {
            if (isAuto()) {
                return AUTO_REPRESENTATION;
            }
            if (isHint()) {
                return HINT_REPRESENTATION;
            }
            return GROUP_REPRESENTATION_OFFSET + groupId;
        }

        @Override
        public EnumColor color() {
            return null;
        }

        private static ResourceLocation iconOf(String name) {
            return Fluxnautics.id("gui/radial/" + name + ".png");
        }
    }

    public record UnbindMode() implements IRadialMode {

        public static final UnbindMode INSTANCE = new UnbindMode();

        @Override
        public Component sliceName() {
            return Component.translatable("placement.fluxnautics.root.unbind");
        }

        @Override
        public ResourceLocation icon() {
            return Fluxnautics.id("gui/radial/unbind.png");
        }

        @Override
        public EnumColor color() {
            return EnumColor.DARK_RED;
        }
    }

    private static final class DynamicGroupsRadialData extends RadialData<IRadialMode> {

        private DynamicGroupsRadialData(ResourceLocation identifier) {
            super(identifier);
        }

        @Override
        public List<IRadialMode> getModes() {
            ThrusterPlacementHelper.ThrusterPlacementSettings settings = ThrusterPlacementHelper.ThrusterPlacementSettings.get(null);
            List<IRadialMode> modes = new ArrayList<>();
            modes.add(GroupSlice.AUTO);
            if (settings.isBound()) {
                for (int groupId : settings.knownGroups()) {
                    modes.add(GroupSlice.of(groupId));
                }
                // keep a forced selection visible/highlighted even before the array picks it up
                if (!settings.groupAuto() && !settings.knownGroups().contains(settings.forcedGroup())) {
                    modes.add(GroupSlice.of(settings.forcedGroup()));
                }
            } else {
                modes.add(GroupSlice.HINT);
            }
            return modes;
        }

        @Override
        public int tryGetNetworkRepresentation(IRadialMode mode) {
            return mode instanceof GroupSlice slice ? slice.networkRepresentation() : -1;
        }

        @Override
        public IRadialMode fromNetworkRepresentation(int networkRepresentation) {
            if (networkRepresentation == GroupSlice.AUTO_REPRESENTATION) {
                return GroupSlice.AUTO;
            }
            if (networkRepresentation == GroupSlice.HINT_REPRESENTATION) {
                return GroupSlice.HINT;
            }
            if (networkRepresentation >= GroupSlice.GROUP_REPRESENTATION_OFFSET) {
                return GroupSlice.of(networkRepresentation - GroupSlice.GROUP_REPRESENTATION_OFFSET);
            }
            return null;
        }

        @Override
        public int index(List<IRadialMode> modes, IRadialMode mode) {
            return modes.indexOf(mode);
        }
    }

    public static final RadialData<IRadialMode> GROUPS = new DynamicGroupsRadialData(Fluxnautics.id("thruster_groups"));

    private static final class RootRadialData extends RadialData<IRadialMode> {

        private RootRadialData(ResourceLocation identifier) {
            super(identifier);
        }

        @Override
        public List<IRadialMode> getModes() {
            return List.of(
                new NestedRadialMode(FACING, Component.translatable("placement.fluxnautics.root.facing"),
                    icon("facing"), EnumColor.AQUA),
                new NestedRadialMode(GROUPS, Component.translatable("placement.fluxnautics.root.groups"),
                    icon("groups"), EnumColor.BRIGHT_GREEN),
                new NestedRadialMode(SNAP, Component.translatable("placement.fluxnautics.root.snap"),
                    icon("snap"), EnumColor.YELLOW),
                UnbindMode.INSTANCE);
        }

        @Override
        public int tryGetNetworkRepresentation(IRadialMode mode) {
            return getModes().indexOf(mode);
        }

        @Override
        public IRadialMode fromNetworkRepresentation(int networkRepresentation) {
            List<IRadialMode> modes = getModes();
            return networkRepresentation >= 0 && networkRepresentation < modes.size() ? modes.get(networkRepresentation) : null;
        }

        private static ResourceLocation icon(String name) {
            return Fluxnautics.id("gui/radial/" + name + ".png");
        }
    }

    public static final RadialData<IRadialMode> ROOT = new RootRadialData(Fluxnautics.id("thruster_placement_root"));

    private static final class EnumRadialDataFN<M extends Enum<M> & IRadialMode> extends ClassBasedRadialData<M> {

        private final List<M> modes;
        private final M defaultMode;

        private EnumRadialDataFN(ResourceLocation identifier, M defaultMode) {
            super(identifier, defaultMode.getDeclaringClass());
            this.modes = List.of(this.clazz.getEnumConstants());
            this.defaultMode = defaultMode;
        }

        @Override
        public List<M> getModes() {
            return modes;
        }

        @Nullable
        @Override
        public M getDefaultMode(List<M> modes) {
            return defaultMode;
        }

        @Override
        public int index(List<M> modes, M mode) {
            return mode.ordinal();
        }

        @Override
        public int getNetworkRepresentation(M mode) {
            return mode.ordinal();
        }

        @Override
        public M fromNetworkRepresentation(int networkRepresentation) {
            return networkRepresentation >= 0 && networkRepresentation < modes.size() ? modes.get(networkRepresentation) : null;
        }
    }

    public enum ThrusterFacingMode implements StringRepresentable, IRadialMode {

        AUTO("auto", EnumColor.BRIGHT_GREEN),
        FACE_PLAYER("player", EnumColor.AQUA),
        FACE_AWAY("away", EnumColor.YELLOW),
        FACE_CLICK("click", EnumColor.ORANGE);

        public static final ThrusterFacingMode[] VALUES = values();

        private final String name;
        private final EnumColor color;

        ThrusterFacingMode(String name, EnumColor color) {
            this.name = name;
            this.color = color;
        }

        public ThrusterFacingMode adjust(int shift) {
            return VALUES[Math.floorMod(ordinal() + shift, VALUES.length)];
        }

        @Override
        public @NonNull Component sliceName() {
            return Component.translatable("placement.fluxnautics.facing." + name);
        }

        @Override
        public @NonNull ResourceLocation icon() {
            return Fluxnautics.id("gui/radial/facing_" + name + ".png");
        }

        @Override
        public EnumColor color() {
            return color;
        }

        @Override
        public @NonNull String getSerializedName() {
            return name;
        }
    }

    public enum ThrusterSnapMode implements StringRepresentable, IRadialMode {

        FREE("free", EnumColor.GRAY),
        EXTEND_ONLY("extend", EnumColor.BRIGHT_GREEN);

        public static final ThrusterSnapMode[] VALUES = values();

        private final String name;
        private final EnumColor color;

        ThrusterSnapMode(String name, EnumColor color) {
            this.name = name;
            this.color = color;
        }

        public ThrusterSnapMode adjust(int shift) {
            return VALUES[Math.floorMod(ordinal() + shift, VALUES.length)];
        }

        public boolean isExtendOnly() {
            return this == EXTEND_ONLY;
        }

        @Override
        public Component sliceName() {
            return Component.translatable("placement.fluxnautics.snap." + name);
        }

        @Override
        public ResourceLocation icon() {
            return Fluxnautics.id("gui/radial/snap_" + name + ".png");
        }

        @Override
        public EnumColor color() {
            return color;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
