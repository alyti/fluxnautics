package dev.alyti.fluxnautics.config;

import dev.alyti.fluxnautics.Fluxnautics;
import mekanism.common.config.IConfigTranslation;
import net.minecraft.Util;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public enum ConfigLocale implements IConfigTranslation {
    THRUSTER_ARRAY("general.machines.thruster_array", "Thruster Array", "Settings for configuring Thruster Array"),
    THRUSTER_ARRAY_THRUST_VALUE_ONE("general.machines.thruster_array.base_ropulsion", "Base Thrust Value", "Propulsion pN for thrust value of 1.0"),
    THRUSTER_ARRAY_JOULE_PER_THRUST_LEVEL("general.machines.thruster_array.joules_per_thrust_level", "Joules Per Thrust Level", "Base joules per tick per thruster, will be multiplied by the thrust level"),
    THRUSTER_ARRAY_CHEMICALS_PER_THRUST_LEVEL("general.machines.thruster_array.chemicals_per_thrust_level", "Chemicals Per Thrust Level", "Chemical per tick per thruster, will be multiplied by the thrust level"),

    FIELD_DRIVE("general.machines.field_drive", "Field Drive", "Settings for configuring Field Drive"),
    FIELD_DRIVE_IMPULSE_MULT_CAP("general.machines.field_drive.impulse_mult_cap", "Impulse Cap", "Maximum impulse allowed for the Field Drive"),
    FIELD_DRIVE_JOULE_BASE_STORAGE("general.machines.field_drive.joule_base_storage", "Base Energy Storage", "Base joule capacity"),
    FIELD_DRIVE_JOULE_PER_IMPULSE_MULT("general.machines.field_drive.joule_per_impulse_mult", "Energy Per Impulse", "Base joules per impulse mult point"),
    FIELD_DRIVE_JOULE_PER_MASS_MULT("general.machines.field_drive.joule_per_mass_mult", "Mass Energy Cost", "Joules per mass multiplier (How much the mass of the sublevel affects energy cost)"),
    FIELD_DRIVE_CHEMICALS_PER_IMPULSE_MULT("general.machines.field_drive.chemicals_per_impulse_mult", "Chemicals Per Impulse", "Base chemicals per impulse mult point"),

    SPATIAL_ANCHOR("general.machines.spatial_anchor", "Spatial Anchor", "Settings for configuring Spatial Anchor"),
    SPATIAL_ANCHOR_JOULE_PER_MASS_MULT("general.machines.spatial_anchor.joules_per_mass_mult", "Mass Energy Cost", "Joules per mass multiplier (How much the mass of the sublevel affects energy cost)"),

    ANTI_GRAV("general.machines.anti_grav", "Anti Grav", "Settings for configuring Anti Grav", true),
    ANTI_GRAV_JOULE_PER_MASS_MULT("general.machines.anti_grav.joules_per_mass_mult", "Mass Energy Cost", "Joules per mass multiplier (How much the mass of the sublevel affects energy cost)"),
    ANTI_GRAV_CALIBRATION_VALUE("general.machines.anti_grav.calibration_value", "Calibration Value", "The magic fudge number to make gravity negation behave better, tweak this if your stuff keeps floating away or not staying in the air despite antigrav working."),
    
    FORCEFIELD("general.machines.forcefield", "Force Field", "Settings for configuring Forcefield", true),
    FORCEFIELD_MAX_FIELD_EXPANSION("general.machines.forcefield.max_field_expansion", "Max Expansion", "How big the field bubble can be enlarged to in config."),
    FORCEFIELD_TELEPORT_THRESHOLD("general.machines.forcefield.teleport_threshold", "Teleport Threshold", "How far into the field sublevels must venture before they are teleported out."),
    FORCEFIELD_JOULE_PASSIVE_MULT("general.machines.forcefield.joules_passive_mult", "Passive Drain", "Passive Joule multiplier (volume(field) * passive_mult + (mass * per_mass_mult)/t)"),
    FORCEFIELD_JOULE_PER_MASS_PASSIVE_MULT("general.machines.forcefield.joules_per_mass_passive_mult", "Mass Energy Cost", "Joule per mass multiplier (How much the mass of the sublevel affects energy cost)"),
    FORCEFIELD_JOULE_PER_IMPACT_ENTITY("general.machines.forcefield.joules_per_impact_entity", "Entity Impact Cost", "Joule per entity impact multiplier (How much an entity bouncing off the field costs energy, decays over 1 second)"),
    FORCEFIELD_JOULE_PER_IMPACT_MASS("general.machines.forcefield.joules_per_impact_mass", "Mass Impact Cost", "Joule per impact mass multiplier (How much the mass of a colliding sublevel affects impact energy cost, decays over 1 second)"),
    FORCEFIELD_IMPACT_SPEED_MULT("general.machines.forcefield.impact_speed_mult", "Impact Speed Mult", "Speed of Impact multiplier on final cost"),
    FORCEFIELD_JOULE_PER_RAYCAST_MULT("general.machines.forcefield.joules_per_raycast_mult", "Raycast Impact Cost", "Joule per raycast multiplier (How much cancelling raycasts colliding with field costs, decays over 1 second)"),
    FORCEFIELD_REDIRECT_RAYCASTS("general.machines.forcefield.redirect_raycasys", "Redirect Raycasts", "Should raycasts (Level.clip) coming into forcefield be redirected to the edge of the forcefield."),
    FORCEFIELD_ROLLING_IMPACT_THRESHOLD("general.machines.forcefield.rolling_impact_threshold", "Rolling Impact Treshold", "% of field energy buffer to be exhausted in a single rolling window before cooldown is triggered"),
    FORCEFIELD_ROLLING_IMPACT_WINDOW("general.machines.forcefield.rolling_impact_window", "Rolling Impact Treshold", "how many ticks the rolling impact window should be, bigger window = more damage accumulation, easier to hit threshold, smaller window = less memory allocation harder to break shield"),
    FORCEFIELD_COOLDOWN_TICKS("general.machines.forcefield.cooldown_ticks", "Cooldown Ticks", "how many ticks the field should be disabled for after it has been drained of energy or the threshold has been hit"),
    FORCEFIELD_OVERLOAD_COOLDOWN_MULT("general.machines.forcefield.overload_cooldown_mult", "Overloack Cooldown Mult", "The Overload cooldown is triggered when the rolling impact threshold is hit and follows this formula: `cooldownTicks + ((sqrt(volume(field)) * log(overload_joules) / 10) * overloadCooldownMult)`, 0 would only use cooldownTicks"),
    FORCEFIELD_LASER_DIRECT_MULT("general.machines.forcefield.laser_direct_mult", "Direct energy damage from Lasers", "multiplier applied to laser damage, applicable only if raycasts are redirected and come from a mekanism laser source"),

    CLOAK_GENERATOR("general.machines.cloak_generator", "Cloak Generator", "Settings for configuring Cloak Generator", true),
    CLOAK_GENERATOR_JOULES_PER_MASS_MULT("general.machines.cloak_generator.joules_per_mass_mult", "Mass Energy Cost", "Joules per mass multiplier (How much the mass of the sublevel affects energy cost)"),
    CLOAK_GENERATOR_DECLOAKER_MODULE_USAGE("general.machines.cloak_generator.decloaker_module_usage", "Decloaker Usage", "Decloaker module usage"),

    STABILIZER("general.machines.stabilizer", "Stabilizer", "Settings for configuring Stabilizer", true),
    STABILIZER_PASSIVE_JOULES_PER_MASS_MULT("general.machines.stabilizer.joules_passive_mult", "Passive Drain", "Passive Joule multiplier (mass * joules_passive_mult)/t"),
    STABILIZER_ACTIVE_JOULES_PER_PN_MULT("general.machines.stabilizer.joules_active_mult", "Active Drain", "Active Joule multiplier (angular_impulse * joules_active_mult)/t"),
    STABILIZER_BASE_TORQUE("general.machines.stabilizer.base_torque", "Base Torque", "Base torque (how much 100% force value is)"),

    CONDUITS("general.machines.conduits", "Conduits", "Settings for configuring Conduits", true),
    CONDUITS_MAX_LENGTH("general.machines.conduits.max_length", "Max Length", "Maximum length for conduits"),
    CONDUITS_MAX_STRETCH("general.machines.conduits.max_stretch", "Max Stretch", "Maximum stretchiness for conduits"),

    FLUX_TETHER("general.machines.flux_tether", "Flux Tether", "Settings for configuring Flux Tether", true),
    FLUX_TETHER_ENERGY_PER_TICK("general.machines.flux_tether.energy_per_tick", "Servo Energy Base", "Base FE per tick for servo work, multiplied by the linear and angular cost factors"),
    FLUX_TETHER_HOLD_ENERGY_PER_SECOND("general.machines.flux_tether.hold_energy_per_second", "Hold Energy Per Second", "FE per second spent just holding a body engaged"),
    FLUX_TETHER_WORK_ENERGY_PER_MASS_BLOCK("general.machines.flux_tether.work_energy_per_mass_block", "Work Energy Per Mass Block", "FE per mass block moved per tick while the servo is working"),
    FLUX_TETHER_WORK_ENERGY_PER_INERTIA_RAD("general.machines.flux_tether.work_energy_per_inertia_rad", "Work Energy Per Inertia Rad", "FE per unit of inertia rotated per tick while the servo is working"),
    FLUX_TETHER_BEAM_ENTITY_COST("general.machines.flux_tether.beam_entity_cost", "Beam Entity Cost", "FE per tick the disassembly beam charges when it hits an entity"),
    FLUX_TETHER_BEAM_ENTITY_DAMAGE("general.machines.flux_tether.beam_entity_damage", "Beam Entity Damage", "Damage dealt by the disassembly beam to an entity per tick"),
    FLUX_TETHER_BEAM_RADIUS("general.machines.flux_tether.beam_radius", "Beam Radius", "Radius of the disassembly beam's hit box"),
    FLUX_TETHER_DISASSEMBLE_THRESHOLD("general.machines.flux_tether.disassemble_threshold", "Disassemble Threshold", "Magnetic distance beyond which the tether disassembles the link"),

    MEKA_ROBIT("general.machines.meka_robit", "Meka-Robit", "Settings for configuring the Meka-Robit drone", true),
    MEKA_ROBIT_FALLBACK_MAX_ENERGY("general.machines.meka_robit.fallback_max_energy", "Fallback Max Energy", "Energy capacity used when the drone's bound item stack provides no container"),
    MEKA_ROBIT_TRAVEL_ENERGY_MULTIPLIER("general.machines.meka_robit.travel_energy_multiplier", "Travel Energy Multiplier", "Energy per block squared of travel, scaled by whether the drone is a swarm member"),
    MEKA_ROBIT_ASSIST_RANGE("general.machines.meka_robit.assist_range", "Assist Range", "How far from the drone an assist mining or attack target may be (server-enforced)"),

    DAEMON_PROTOCOL("general.machines.daemon_protocol", "Daemon Protocol", "Settings for configuring Daemon Protocol", true),
    DAEMON_PROTOCOL_PATROL_RANGE("general.machines.daemon_protocol.patrol_range", "Patrol Range", "Radius around the daemon in which its drone patrols for hostiles"),
    DAEMON_PROTOCOL_PATROL_WAIT_TICKS("general.machines.daemon_protocol.patrol_wait_ticks", "Patrol Wait Ticks", "How long the drone waits between patrol waypoints"),
    DAEMON_PROTOCOL_RECALL_DISTANCE("general.machines.daemon_protocol.recall_distance", "Recall Distance", "Distance from the daemon beyond which its drone is teleported back home"),

    INERTIA_DAMPENER("general.machines.inertia_dampener", "Inertia Dampener", "Settings for configuring Inertia Dampener", true),
    INERTIA_DAMPENER_JOULES_PER_KINETIC_UNIT("general.machines.inertia_dampener.joules_per_kinetic_unit", "Energy Per Kinetic Unit", "Joules charged per unit of kinetic energy actually removed (or added) by the dampener"),
    INERTIA_DAMPENER_LINEAR_CALIBRATION("general.machines.inertia_dampener.linear_calibration", "Linear Calibration", "Maximum delta-v per second the dampener can apply on each linear axis"),

    THRUSTER_RENDERER("general.client.thruster_renderer", "Thruster Renderer", "Client-side settings for the ion thruster renderer", true),
    THRUSTER_RENDERER_PLUME_INTENSITY("general.client.thruster_renderer.plume_intensity", "Plume Intensity", "Multiplier on plume length (0 hides plumes entirely)"),
    THRUSTER_RENDERER_PLUME_CHASE_SPEED("general.client.thruster_renderer.plume_chase_speed", "Plume Chase Speed", "How quickly the plume length approaches the actual thrust level"),
    THRUSTER_RENDERER_TRAILS_ENABLED("general.client.thruster_renderer.trails_enabled", "Enable Trails", "Master toggle for the thruster trail renderer"),
    THRUSTER_RENDERER_TRAIL_LIFETIME("general.client.thruster_renderer.trail_lifetime", "Trail Lifetime", "Seconds a trail sample stays alive, which sets the emergent trail length"),
    THRUSTER_RENDERER_TRAIL_WIDTH_SCALE("general.client.thruster_renderer.trail_width_scale", "Trail Width Scale", "Multiplier on the trail cross-section size"),
    THRUSTER_RENDERER_TRAIL_SHAPE("general.client.thruster_renderer.trail_shape", "Trail Shape", "0 = ribbon, 1 = rect, 2 = profile"),

    GRAY_GOO_INTERVAL_TICKS("general.machines.gray_goo.interval_ticks", "", ""),
    GRAY_GOO_CONTRACTING_INTERVAL_TICKS("general.machines.gray_goo.contracting_interval_ticks", "", ""),
    GRAY_GOO_MAX_MASS("general.machines.gray_goo.max_mass", "", ""),
    GRAY_GOO_MAX_RADIUS("general.machines.gray_goo.max_radius", "", ""),

    ;


    private final String key;
    private final String title;
    private final String tooltip;
    private final String button;


    ConfigLocale(String path, String title, String tooltip) {
        this(path, title, tooltip, false);
    }

    ConfigLocale(String path, String title, String tooltip, boolean isSection) {
        this(path, title, tooltip, IConfigTranslation.getSectionTitle(title, isSection));
    }

    ConfigLocale(String path, String title, String tooltip, @Nullable String button) {
        this.key = Util.makeDescriptionId("configuration", Fluxnautics.id(path));
        this.title = title;
        this.tooltip = tooltip;
        this.button = button;
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public String tooltip() {
        return tooltip;
    }

    @Nullable
    @Override
    public String button() {
        return button;
    }
    @Override
    public @NonNull String getTranslationKey() {
        return key;
    }
}
