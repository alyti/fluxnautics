package dev.alyti.fluxnautics.lang;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.util.LangBuilder;
import mekanism.api.text.ILangEntry;
import net.minecraft.Util;
import org.jspecify.annotations.NonNull;

public enum FNLang implements ILangEntry {

    DESCRIPTION_ION_THRUSTER("description", "ion_thruster"),
    DESCRIPTION_THRUSTER_ARRAY_BOOSTER("description", "thruster_array_booster"),
    DESCRIPTION_THRUSTER_ARRAY_ALIGNMENT("description", "thruster_array_alignment"),
    DESCRIPTION_THRUSTER_ARRAY_CONTROLLER("description", "thruster_array_controller"),
    DESCRIPTION_THRUSTER_ARRAY_SUBCONTROLLER("description", "thruster_array_subcontroller"),
    DESCRIPTION_THRUSTER_ARRAY_STRUCTURAL("description", "thruster_array_structural"),
    DESCRIPTION_FIELD_DRIVE("description", "field_drive"),
    DESCRIPTION_HOLO("description", "holo"),
    DESCRIPTION_SPATIAL_ANCHOR("description", "spatial_anchor"),
    DESCRIPTION_STABILIZER("description", "stabilizer"),
    DESCRIPTION_CONDUIT_CONNECTOR("description", "conduit_connector"),
    DESCRIPTION_CONDUIT_PORT("description", "conduit_port"),
    DESCRIPTION_ANTI_GRAV("description", "anti_grav"),
    DESCRIPTION_FORCEFIELD_EMITTER("description", "forcefield_emitter"),
    DESCRIPTION_FORCEFIELD_HARMONIZER("description", "forcefield_harmonizer"),
    DESCRIPTION_CLOAK_GENERATOR("description", "cloak_generator"),
    DESCRIPTION_GRAY_GOO("description", "gray_goo"),
    DESCRIPTION_ASSEMBLY_TERMINAL("description", "assembly_terminal"),
    DESCRIPTION_DAEMON_PROTOCOL("description", "daemon_protocol"),
    DESCRIPTION_FLUX_TETHER("description", "quantum_lock"),
    DESCRIPTION_FLUX_TETHER_LINK("description", "quantum_lock_link"),
    DESCRIPTION_INERTIA_DAMPENER("description", "inertia_dampener"),

    CHEMICAL_ATTRIBUTE_WARPFUEL_DISTANCE("chemical", "attribute.warpfuel.distance"),
    CHEMICAL_ATTRIBUTE_WARPFUEL_INSTANT("chemical", "attribute.warpfuel.instant"),
    CHEMICAL_ATTRIBUTE_WARPFUEL_ENERGY("chemical", "attribute.warpfuel.energy"),
    CHEMICAL_ATTRIBUTE_WARPFUEL_CHARGE_TIME("chemical", "attribute.warpfuel.charge_time"),

    ISSUE_NOT_ENOUGH_WARPFUEL("gui", "issues.no_warpfuel"),

    THRUSTER_GROUP_CHANGED("thruster_array", "group_changed"),
    THRUSTER_DRIVER_CONTROL_CHANGED("thruster_array", "control_driver_changed"),
    THRUSTER_META("thruster_array", "meta"),
    THRUSTER_GROUP("thruster_array", "group"),

    ITEM_THRUSTER_ARRAY_BOOSTER_THRUST_CAPACITY("item", "thruster_array_booster.thrust_capacity"),

    GUI_FD_IMP_X("gui", "field_drive.impulse_x"),
    GUI_FD_IMP_Y("gui", "field_drive.impulse_y"),
    GUI_FD_IMP_Z("gui", "field_drive.impulse_z"),
    GUI_FD_IMP_MULT("gui", "field_drive.impulse_mult"),
    GUI_FD_GLOBAL("gui", "field_drive.global"),

    GUI_BUTTON_ADD_GROUP("gui", "button.add_group"),
    GUI_GROUP("gui", "group"),
    GUI_GROUP_COLOR("gui", "group.color"),
    GUI_GROUP_NAME("gui", "group.name"),
    GUI_GROUP_RAINBOW("gui", "group.rainbow"),
    GUI_GROUP_MERGED_FORCES("gui", "group.merged_forces"),
    GUI_GROUP_EFFECT("gui", "group.effect"),
    GUI_GROUP_EFFECT_LIGHTNING("gui", "group.effect.lightning"),
    GUI_GROUP_EFFECT_END_PORTAL("gui", "group.effect.end_portal"),

    GUI_GROUP_PLUME_EFFECT("gui", "group.effect.plume"),
    GUI_GROUP_PLUME_EFFECT_CLASSIC("gui", "group.effect.plume.classic"),
    GUI_GROUP_PLUME_EFFECT_FBM("gui", "group.effect.plume.fbm"),

    GUI_GROUP_DRIVER("gui", "group.driver"),
    GUI_GROUP_DRIVER_DIRECT("gui", "group.driver.direct"),
    GUI_GROUP_DRIVER_VECTOR("gui", "group.driver.vector"),
    GUI_GROUP_DRIVER_HOVER("gui", "group.driver.hover"),

    GUI_GROUP_ALIGNMENT("gui", "group.alignment"),
    GUI_ITEM_GROUP("gui", "item.group"),

    EMITTER_CONFIG("gui", "emitter.tooltip"),
    EMITTER_PASSIVE_RAINBOW("gui", "emitter.rainbow.passive"),
    EMITTER_ACTIVE_RAINBOW("gui", "emitter.rainbow.active"),
    EMITTER_ALLOW_PASSIVE_MOBS("gui", "emitter.allow.passive_mobs"),
    EMITTER_ALLOW_HOSTILE_MOBS("gui", "emitter.allow.hostile_mobs"),
    EMITTER_ALLOW_PROJECTILES("gui", "emitter.allow.projectiles"),
    EMITTER_ALLOW_FOREIGN_PLAYERS("gui", "emitter.allow.foreign_players"),
    EMITTER_ALLOW_FOREIGN_SUBLEVELS("gui", "emitter.allow.foreign_sublevels"),
    EMITTER_ALLOW_EVERYTHING_ELSE("gui", "emitter.allow.everything_else"),
    EMITTER_FIELD_EXPANSION("gui", "emitter.field_expansion"),
    EMITTER_PER_IMPACT_USAGE("gui", "emitter.per_impact_usage"),
    EMITTER_VOLUME("gui", "emitter.volume"),
    EMITTER_FREQUENCY_ITEM("gui", "emitter.frequency_item"),
    EMITTER_COOLDOWN("gui", "emitter.cooldown"),

    BLUEPRINT_FORCE_ARROW_MAGNITUDE("gui", "blueprint.force_arrow_magnitude"),
    BLUEPRINT_MERGED_FORCE_ARROW("gui", "blueprint.merged_force_arrow"),
    BLUEPRINT_FORCE_ARROW("gui", "blueprint.force_arrow"),
    BLUEPRINT_MERGING_NUMERAL("gui", "blueprint.merging_numeral"),
    BLUEPRINT_CONFIG_MERGE_FORCES("gui", "blueprint.config_merge_forces"),
    BLUEPRINT_CONFIG_SHOW_COM("gui", "blueprint.config_show_com"),

    BLUEPRINT_INFO_MASS("gui", "blueprint.info_mass"),
    BLUEPRINT_INFO_UUID("gui", "blueprint.info_uuid"),
    BLUEPRINT_INFO_NAME_UUID("gui", "blueprint.info_name_uuid"),

    CREATIVE_TAB_THRUSTER_ARRAY_MULTIBLOCK("creativetab", "thruster_array_multiblock"),
    CREATIVE_TAB_EVERYTHING_ELSE("creativetab", "everything_else"),

    STABILIZER_FORCE_PERCENT("gui", "stabilizer.force_percent"),
    STABILIZER_DAMPENING_PERCENT("gui", "stabilizer.dampening_percent"),
    STABILIZER_TARGET_PITCH_DEGREES("gui", "stabilizer.target_pitch_degrees"),
    STABILIZER_TARGET_YAW_DEGREES("gui", "stabilizer.target_yaw_degrees"),
    STABILIZER_TARGET_ROLL_DEGREES("gui", "stabilizer.target_roll_degrees"),
    STABILIZER_ACCEPT_ERR_DEGREES("gui", "stabilizer.accept_err_degrees"),
    STABILIZER_TOGGLE_PITCH("gui", "stabilizer.toggle_pitch"),
    STABILIZER_TOGGLE_YAW("gui", "stabilizer.toggle_yaw"),
    STABILIZER_TOGGLE_ROLL("gui", "stabilizer.toggle_roll"),
    STABILIZER_TOGGLE_BIDIRECTIONAL_TORQUE("gui", "stabilizer.toggle_bidirectional_torque"),


    MEKA_ROBIT_MODE("meka_robit", "mode"),
    MEKA_ROBIT_MODE_PATROL("meka_robit", "mode.patrol"),
    MEKA_ROBIT_MODE_ASSIST("meka_robit", "mode.assist"),
    MEKA_ROBIT_MODE_DELETE("meka_robit", "mode.delete"),
    MEKA_ROBIT_MODE_MANUAL("meka_robit", "mode.manual"),

    RADIAL_SWARM("radial", "swarm"),
    RADIAL_SWARM_COUNT("radial", "swarm.count"),
    RADIAL_SWARM_OFF("radial", "swarm.off"),
    RADIAL_SWARM_TWO("radial", "swarm.two"),
    RADIAL_SWARM_FOUR("radial", "swarm.four"),
    RADIAL_SWARM_SIX("radial", "swarm.six"),
    RADIAL_SWARM_EIGHT("radial", "swarm.eight"),
    RADIAL_SWARM_TEN("radial", "swarm.ten"),
    RADIAL_SWARM_TWELVE("radial", "swarm.twelve"),
    RADIAL_SWARM_FOURTEEN("radial", "swarm.fourteen"),
    RADIAL_SWARM_SIXTEEN("radial", "swarm.sixteen"),

    RADIAL_SWARM_FORMATION("radial", "swarm.formation"),
    RADIAL_SWARM_FORMATION_COUNT("radial", "swarm.formation.count"),
    RADIAL_SWARM_FORMATION_CIRCLE_H("radial", "swarm.formation.circle_h"),
    RADIAL_SWARM_FORMATION_CIRCLE_V("radial", "swarm.formation.circle_v"),
    RADIAL_SWARM_FORMATION_SPHERE("radial", "swarm.formation.sphere"),

    MODULE_DECLOAKER("module", "decloaker"),

    REDSTONE_CONTROL("redstone", "control"),
    REDSTONE_SIDE_UNAVAILABLE("redstone", "side_unavailable"),
    REDSTONE_INVERT("redstone", "invert"),
    REDSTONE_INVERTED("redstone", "inverted"),
    REDSTONE_RESET_ALL("redstone", "reset_all"),
    REDSTONE_OVERRIDE_ACTIVE("redstone", "override_active"),
    REDSTONE_OVERRIDE_LOCKED("redstone", "override_locked"),
    REDSTONE_SIDES_HINT("redstone", "sides_hint"),
    REDSTONE_CYCLE_HINT("redstone", "cycle_hint"),
    REDSTONE_RESET_HINT("redstone", "reset_hint"),
    REDSTONE_INVERT_HINT("redstone", "invert_hint"),

    PLACEMENT_ROOT_FACING("placement", "root.facing"),
    PLACEMENT_ROOT_GROUPS("placement", "root.groups"),
    PLACEMENT_ROOT_SNAP("placement", "root.snap"),
    PLACEMENT_ROOT_UNBIND("placement", "root.unbind"),

    PLACEMENT_FACING_AUTO("placement", "facing.auto"),
    PLACEMENT_FACING_PLAYER("placement", "facing.player"),
    PLACEMENT_FACING_AWAY("placement", "facing.away"),
    PLACEMENT_FACING_CLICK("placement", "facing.click"),

    PLACEMENT_SNAP_FREE("placement", "snap.free"),
    PLACEMENT_SNAP_EXTEND("placement", "snap.extend"),

    PLACEMENT_GROUPS_AUTO("placement", "groups.auto"),
    PLACEMENT_GROUPS_HINT("placement", "groups.hint"),
    PLACEMENT_GROUPS_SLICE("placement", "groups.slice"),

    PLACEMENT_MESSAGES_FACING("placement", "messages.facing"),
    PLACEMENT_MESSAGES_SNAP("placement", "messages.snap"),
    PLACEMENT_MESSAGES_GROUP_AUTO("placement", "messages.group.auto"),
    PLACEMENT_MESSAGES_GROUP_FORCED("placement", "messages.group.forced"),
    PLACEMENT_MESSAGES_PLACED_AUTO("placement", "messages.placed.auto"),
    PLACEMENT_MESSAGES_PLACED_FORCED("placement", "messages.placed.forced"),
    PLACEMENT_MESSAGES_PLACED_ITEM("placement", "messages.placed.item"),
    PLACEMENT_MESSAGES_BOUND("placement", "messages.bound"),
    PLACEMENT_MESSAGES_UNBOUND("placement", "messages.unbound"),
    PLACEMENT_MESSAGES_BIND_FAILED("placement", "messages.bind_failed"),
    PLACEMENT_MESSAGES_NEED_BIND("placement", "messages.need_bind"),

    PLACEMENT_BOUND_LABEL("placement", "bound_label"),
    PLACEMENT_TOOLTIP("placement", "tooltip")
    ;

    private final String key;

    FNLang(String type, String path) {
        this(Util.makeDescriptionId(type, Fluxnautics.id(path)));
    }

    FNLang(String key) {
        this.key = key;
    }

    @Override
    public @NonNull String getTranslationKey() {
        return key;
    }

    public static LangBuilder builder() {
        return new LangBuilder(Fluxnautics.MODID);
    }

    public LangBuilder with(final Object... args) {
        return builder().translate(key, args);
    }
}