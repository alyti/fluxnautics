package dev.alyti.fluxnautics.config;

import dev.alyti.fluxnautics.content.blocks.antigrav.AntiGravBlockEntity;
import dev.alyti.fluxnautics.content.blocks.daemon_protocol.DaemonProtocolBlockEntity;
import dev.alyti.fluxnautics.content.blocks.flux_tether.FluxTetherBlockEntity;
import dev.alyti.fluxnautics.content.blocks.inertia_dampener.InertiaDampenerBlockEntity;
import dev.alyti.fluxnautics.content.entities.gear.EntityMekaRobit;
import dev.alyti.fluxnautics.index.BoosterTier;
import mekanism.common.config.BaseMekanismConfig;
import mekanism.common.config.MekanismConfigTranslations;
import mekanism.common.config.value.CachedBooleanValue;
import mekanism.common.config.value.CachedDoubleValue;
import mekanism.common.config.value.CachedIntValue;
import mekanism.common.config.value.CachedLongValue;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Locale;

public class CommonConfig extends BaseMekanismConfig {

    private final ModConfigSpec configSpec;

    public final CachedDoubleValue thrusterArrayThrustValueOne;
    public final CachedLongValue thrusterArrayJoulePerThrustLevel;
    public final CachedLongValue thrusterArrayChemicalsPerThrustLevel;

    public final CachedDoubleValue fieldDriveImpulseMultCap;
    public final CachedLongValue fieldDriveJoulesBaseStorage;
    public final CachedLongValue fieldDriveJoulesPerImpulseMult;
    public final CachedLongValue fieldDriveChemicalsPerImpulseMult;
    public final CachedDoubleValue fieldDriveJoulesPerMassMult;

    public final CachedDoubleValue spatialAnchorJoulesPerMassMult;

    public final CachedDoubleValue antiGravJoulesPerMassMult;

    public final CachedDoubleValue forcefieldMaxFieldExpansion;
    public final CachedDoubleValue forcefieldTeleportThreshold;

    public final CachedDoubleValue forcefieldJoulePassiveMult;
    public final CachedDoubleValue forcefieldJoulePerMassPassiveMult;
    public final CachedDoubleValue forcefieldJoulePerImpactEntity;
    public final CachedDoubleValue forcefieldJoulePerImpactMass;
    public final CachedDoubleValue forcefieldImpactSpeedMult;
    public final CachedDoubleValue forcefieldJoulePerRaycast;
    public final CachedDoubleValue forcefieldRollingImpactThreshold;
    public final CachedIntValue forcefieldRollingImpactWindow;
    public final CachedLongValue forcefieldCooldownTicks;
    public final CachedDoubleValue forcefieldOverloadCooldownMult;
    public final CachedBooleanValue forcefieldRedirectRaycast;
    public final CachedDoubleValue forcefieldLaserDirectMult;

    public final CachedDoubleValue cloakGeneratorJoulesPerMassMult;
    public final CachedLongValue cloakGeneratorDecloakerModuleUsage;

    public final CachedDoubleValue stabilizerPassiveJoulesPerMassMult;
    public final CachedDoubleValue stabilizerActiveJoulesPerPnMult;
    public final CachedDoubleValue stabilizerBaseTorque;

    public final CachedDoubleValue conduitsMaxLength;
    public final CachedDoubleValue conduitsMaxStretch;

    public final CachedLongValue fluxTetherEnergyPerTick;
    public final CachedDoubleValue fluxTetherHoldEnergyPerSecond;
    public final CachedDoubleValue fluxTetherWorkEnergyPerMassBlock;
    public final CachedDoubleValue fluxTetherWorkEnergyPerInertiaRad;
    public final CachedLongValue fluxTetherBeamEntityCost;
    public final CachedDoubleValue fluxTetherBeamEntityDamage;
    public final CachedDoubleValue fluxTetherBeamRadius;
    public final CachedDoubleValue fluxTetherDisassembleThreshold;

    public final CachedDoubleValue mekaRobitTravelEnergyMultiplier;
    public final CachedDoubleValue mekaRobitAssistRange;

    public final CachedIntValue daemonProtocolPatrolRange;
    public final CachedIntValue daemonProtocolPatrolWaitTicks;
    public final CachedDoubleValue daemonProtocolRecallDistance;

//    public final CachedDoubleValue inertiaDampenerJoulesPerKineticUnit;
//    public final CachedDoubleValue inertiaDampenerLinearCalibration;

//    public final CachedIntValue grayGooTickInterval;
//    public final CachedIntValue grayGooTickContractingInterval;
//    public final CachedIntValue grayGooTickMaxMass;
//    public final CachedIntValue grayGooTickMaxRadius;



    public CommonConfig() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        ConfigLocale.THRUSTER_ARRAY.applyToBuilder(builder).push("thruster_array");
        thrusterArrayThrustValueOne = CachedDoubleValue.wrap(this, ConfigLocale.THRUSTER_ARRAY_THRUST_VALUE_ONE.applyToBuilder(builder).defineInRange("basePropulsion", 500, 1, Double.MAX_VALUE));
        thrusterArrayJoulePerThrustLevel = CachedLongValue.wrap(this, ConfigLocale.THRUSTER_ARRAY_JOULE_PER_THRUST_LEVEL.applyToBuilder(builder).defineInRange("joulesPerTick", 120, 0, Long.MAX_VALUE));
        thrusterArrayChemicalsPerThrustLevel = CachedLongValue.wrap(this, ConfigLocale.THRUSTER_ARRAY_CHEMICALS_PER_THRUST_LEVEL.applyToBuilder(builder).defineInRange("chemicalsPerTick", 1, 0, Long.MAX_VALUE));
        builder.push("boosters");
        for (BoosterTier tier : BoosterTier.values()) {
            MekanismConfigTranslations.TierTranslations translations = MekanismConfigTranslations.TierTranslations.create(tier, "booster", name -> "Thrust Value added by " + name + " booster", null);
            String tierName = tier.getBaseTier().getSimpleName().toLowerCase(Locale.ROOT);
            CachedDoubleValue storageReference = CachedDoubleValue.wrap(this, translations.first().applyToBuilder(builder)
                    .defineInRange(tierName + "ThrustBudget", tier.getBaseThrustBudget(), 1, Long.MAX_VALUE));
            tier.setConfigReference(storageReference);
        }
        builder.pop();
        builder.pop();

        ConfigLocale.FIELD_DRIVE.applyToBuilder(builder).push("field_drive");
        fieldDriveImpulseMultCap = CachedDoubleValue.wrap(this, ConfigLocale.FIELD_DRIVE_IMPULSE_MULT_CAP.applyToBuilder(builder).defineInRange("impulseMultCap", 1000, 1, Double.MAX_VALUE));
        fieldDriveJoulesBaseStorage = CachedLongValue.wrap(this, ConfigLocale.FIELD_DRIVE_JOULE_BASE_STORAGE.applyToBuilder(builder).defineInRange("jouleBaseStorage", 1_000_000_000_000L, 800_000_000L, Long.MAX_VALUE));
        fieldDriveJoulesPerImpulseMult = CachedLongValue.wrap(this, ConfigLocale.FIELD_DRIVE_JOULE_PER_IMPULSE_MULT.applyToBuilder(builder).defineInRange("joulesPerImpulseMult", 100, 0, Long.MAX_VALUE));
        fieldDriveChemicalsPerImpulseMult = CachedLongValue.wrap(this, ConfigLocale.FIELD_DRIVE_CHEMICALS_PER_IMPULSE_MULT.applyToBuilder(builder).defineInRange("chemicalsPerImpulseMult", 1, 0L, 10L));
        fieldDriveJoulesPerMassMult = CachedDoubleValue.wrap(this, ConfigLocale.FIELD_DRIVE_JOULE_PER_MASS_MULT.applyToBuilder(builder).defineInRange("joulesPerMassMult", 0.5, 0, Double.MAX_VALUE));
        builder.pop();

        ConfigLocale.SPATIAL_ANCHOR.applyToBuilder(builder).push("spatial_anchor");
        spatialAnchorJoulesPerMassMult = CachedDoubleValue.wrap(this, ConfigLocale.SPATIAL_ANCHOR_JOULE_PER_MASS_MULT.applyToBuilder(builder).defineInRange("joulesPerMassMult", 0.25, 0, Double.MAX_VALUE));
        builder.pop();

        ConfigLocale.ANTI_GRAV.applyToBuilder(builder).push("anti_grav");
        antiGravJoulesPerMassMult = CachedDoubleValue.wrap(this, ConfigLocale.ANTI_GRAV_JOULE_PER_MASS_MULT.applyToBuilder(builder).defineInRange("joulesPerMassMult", 1, 0, Double.MAX_VALUE));
        AntiGravBlockEntity.setConfigReference(CachedDoubleValue.wrap(this, ConfigLocale.ANTI_GRAV_CALIBRATION_VALUE.applyToBuilder(builder).defineInRange("calibrationValue", AntiGravBlockEntity.DEFAULT_CALIBRATION, 0, Double.MAX_VALUE)));
        builder.pop();

        ConfigLocale.FORCEFIELD.applyToBuilder(builder).push("forcefield");

        builder.push("properties");
        forcefieldMaxFieldExpansion = CachedDoubleValue.wrap(this, ConfigLocale.FORCEFIELD_MAX_FIELD_EXPANSION.applyToBuilder(builder).defineInRange("padding", 128.0, 2.0, 512.0));
        forcefieldTeleportThreshold = CachedDoubleValue.wrap(this, ConfigLocale.FORCEFIELD_TELEPORT_THRESHOLD.applyToBuilder(builder).defineInRange("teleportThreshold", 16.0, 0.0, 512.0));
        builder.pop();

        builder.push("usage");
        forcefieldJoulePassiveMult = CachedDoubleValue.wrap(this, ConfigLocale.FORCEFIELD_JOULE_PASSIVE_MULT.applyToBuilder(builder).defineInRange("passiveVolumeMultiplier", 0.25, 0.0, Double.MAX_VALUE));
        forcefieldJoulePerMassPassiveMult = CachedDoubleValue.wrap(this, ConfigLocale.FORCEFIELD_JOULE_PER_MASS_PASSIVE_MULT.applyToBuilder(builder).defineInRange("passiveMassMultiplier", 0.25, 0.0, Double.MAX_VALUE));
        forcefieldJoulePerImpactEntity = CachedDoubleValue.wrap(this, ConfigLocale.FORCEFIELD_JOULE_PER_IMPACT_ENTITY.applyToBuilder(builder).defineInRange("perImpactEntityMult", 500.0, 0.0, Double.MAX_VALUE));
        forcefieldJoulePerImpactMass = CachedDoubleValue.wrap(this, ConfigLocale.FORCEFIELD_JOULE_PER_IMPACT_MASS.applyToBuilder(builder).defineInRange("perImpactMassMult", 25.0, 0.0, Double.MAX_VALUE));
        forcefieldImpactSpeedMult = CachedDoubleValue.wrap(this, ConfigLocale.FORCEFIELD_IMPACT_SPEED_MULT.applyToBuilder(builder).defineInRange("impactSpeedMult", 1.0, 0.0, Double.MAX_VALUE));
        forcefieldJoulePerRaycast = CachedDoubleValue.wrap(this, ConfigLocale.FORCEFIELD_JOULE_PER_RAYCAST_MULT.applyToBuilder(builder).defineInRange("perRaycastMult", 100.0, 0.0, Double.MAX_VALUE));
        forcefieldRedirectRaycast = CachedBooleanValue.wrap(this, ConfigLocale.FORCEFIELD_REDIRECT_RAYCASTS.applyToBuilder(builder).define("redirectRaycasts", true));
        forcefieldCooldownTicks = CachedLongValue.wrap(this, ConfigLocale.FORCEFIELD_COOLDOWN_TICKS.applyToBuilder(builder).defineInRange("cooldownTicks", 200, 100L, 20 * 60 * 60 * 24));
        forcefieldOverloadCooldownMult = CachedDoubleValue.wrap(this, ConfigLocale.FORCEFIELD_OVERLOAD_COOLDOWN_MULT.applyToBuilder(builder).defineInRange("overloadCooldownMult", 1.0, 0.0, Double.MAX_VALUE));
        forcefieldRollingImpactThreshold = CachedDoubleValue.wrap(this, ConfigLocale.FORCEFIELD_ROLLING_IMPACT_THRESHOLD.applyToBuilder(builder).defineInRange("rollingImpactThreshold", 1.0, 0.01, Double.MAX_VALUE));
        forcefieldRollingImpactWindow = CachedIntValue.wrap(this, ConfigLocale.FORCEFIELD_ROLLING_IMPACT_WINDOW.applyToBuilder(builder).defineInRange("rollingImpactWindow", 100, 10, 1000));
        forcefieldLaserDirectMult = CachedDoubleValue.wrap(this, ConfigLocale.FORCEFIELD_LASER_DIRECT_MULT.applyToBuilder(builder).defineInRange("laserDirectMult", 2.0, 0.01, Double.MAX_VALUE));
        builder.pop();

        builder.pop();

        ConfigLocale.CLOAK_GENERATOR.applyToBuilder(builder).push("cloak_generator");
        cloakGeneratorJoulesPerMassMult = CachedDoubleValue.wrap(this, ConfigLocale.CLOAK_GENERATOR_JOULES_PER_MASS_MULT.applyToBuilder(builder).defineInRange("joulesPerMassMult", 1, 0, Double.MAX_VALUE));
        cloakGeneratorDecloakerModuleUsage = CachedLongValue.wrap(this, ConfigLocale.CLOAK_GENERATOR_DECLOAKER_MODULE_USAGE.applyToBuilder(builder).defineInRange("decloakerModuleUsage", 10_000L, 0, Long.MAX_VALUE));
        builder.pop();

        ConfigLocale.STABILIZER.applyToBuilder(builder).push("stabilizer");
        stabilizerPassiveJoulesPerMassMult = CachedDoubleValue.wrap(this, ConfigLocale.STABILIZER_PASSIVE_JOULES_PER_MASS_MULT.applyToBuilder(builder).defineInRange("passiveJoulesMult", 0.1, 0, Double.MAX_VALUE));
        stabilizerActiveJoulesPerPnMult = CachedDoubleValue.wrap(this, ConfigLocale.STABILIZER_ACTIVE_JOULES_PER_PN_MULT.applyToBuilder(builder).defineInRange("activeJoulesMult", 1, 0, Double.MAX_VALUE));
        stabilizerBaseTorque = CachedDoubleValue.wrap(this, ConfigLocale.STABILIZER_BASE_TORQUE.applyToBuilder(builder).defineInRange("baseTorque", 2048, 1, Double.MAX_VALUE));

        builder.pop();
        ConfigLocale.CONDUITS.applyToBuilder(builder).push("conduits");
        conduitsMaxLength = CachedDoubleValue.wrap(this, ConfigLocale.CONDUITS_MAX_LENGTH.applyToBuilder(builder).defineInRange("maxLength", 64, 8, Double.MAX_VALUE));
        conduitsMaxStretch = CachedDoubleValue.wrap(this, ConfigLocale.CONDUITS_MAX_STRETCH.applyToBuilder(builder).defineInRange("maxStretch", 32, 80, Double.MAX_VALUE));
        builder.pop();

        ConfigLocale.FLUX_TETHER.applyToBuilder(builder).push("flux_tether");
        fluxTetherEnergyPerTick = CachedLongValue.wrap(this, ConfigLocale.FLUX_TETHER_ENERGY_PER_TICK.applyToBuilder(builder).defineInRange("energyPerTick", FluxTetherBlockEntity.ENERGY_PER_TICK, 0, Long.MAX_VALUE));
        fluxTetherHoldEnergyPerSecond = CachedDoubleValue.wrap(this, ConfigLocale.FLUX_TETHER_HOLD_ENERGY_PER_SECOND.applyToBuilder(builder).defineInRange("holdEnergyPerSecond", FluxTetherBlockEntity.ENERGY_HOLD_FE_PER_SECOND, 0, Double.MAX_VALUE));
        fluxTetherWorkEnergyPerMassBlock = CachedDoubleValue.wrap(this, ConfigLocale.FLUX_TETHER_WORK_ENERGY_PER_MASS_BLOCK.applyToBuilder(builder).defineInRange("workEnergyPerMassBlock", FluxTetherBlockEntity.ENERGY_WORK_FE_PER_MASS_BLOCK, 0, Double.MAX_VALUE));
        fluxTetherWorkEnergyPerInertiaRad = CachedDoubleValue.wrap(this, ConfigLocale.FLUX_TETHER_WORK_ENERGY_PER_INERTIA_RAD.applyToBuilder(builder).defineInRange("workEnergyPerInertiaRad", FluxTetherBlockEntity.ENERGY_WORK_FE_PER_INERTIA_RAD, 0, Double.MAX_VALUE));
        fluxTetherBeamEntityCost = CachedLongValue.wrap(this, ConfigLocale.FLUX_TETHER_BEAM_ENTITY_COST.applyToBuilder(builder).defineInRange("beamEntityCost", FluxTetherBlockEntity.BEAM_ENTITY_COST, 0, Long.MAX_VALUE));
        fluxTetherBeamEntityDamage = CachedDoubleValue.wrap(this, ConfigLocale.FLUX_TETHER_BEAM_ENTITY_DAMAGE.applyToBuilder(builder).defineInRange("beamEntityDamage", FluxTetherBlockEntity.BEAM_ENTITY_DAMAGE, 0, Double.MAX_VALUE));
        fluxTetherBeamRadius = CachedDoubleValue.wrap(this, ConfigLocale.FLUX_TETHER_BEAM_RADIUS.applyToBuilder(builder).defineInRange("beamRadius", FluxTetherBlockEntity.BEAM_RADIUS, 0.05, 8));
        fluxTetherDisassembleThreshold = CachedDoubleValue.wrap(this, ConfigLocale.FLUX_TETHER_DISASSEMBLE_THRESHOLD.applyToBuilder(builder).defineInRange("disassembleThreshold", FluxTetherBlockEntity.DISASSEMBLE_THRESHOLD, 1, 64));
        builder.pop();

        ConfigLocale.MEKA_ROBIT.applyToBuilder(builder).push("meka_robit");
        mekaRobitTravelEnergyMultiplier = CachedDoubleValue.wrap(this, ConfigLocale.MEKA_ROBIT_TRAVEL_ENERGY_MULTIPLIER.applyToBuilder(builder).defineInRange("travelEnergyMultiplier", EntityMekaRobit.DISTANCE_MULTIPLIER, 0, Double.MAX_VALUE));
        mekaRobitAssistRange = CachedDoubleValue.wrap(this, ConfigLocale.MEKA_ROBIT_ASSIST_RANGE.applyToBuilder(builder).defineInRange("assistRange", EntityMekaRobit.DEFAULT_MAX_ASSIST_RANGE, 2, 64));
        builder.pop();

        ConfigLocale.DAEMON_PROTOCOL.applyToBuilder(builder).push("daemon_protocol");
        daemonProtocolPatrolRange = CachedIntValue.wrap(this, ConfigLocale.DAEMON_PROTOCOL_PATROL_RANGE.applyToBuilder(builder).defineInRange("patrolRange", DaemonProtocolBlockEntity.DEFAULT_PATROL_RANGE, 4, 256));
        daemonProtocolPatrolWaitTicks = CachedIntValue.wrap(this, ConfigLocale.DAEMON_PROTOCOL_PATROL_WAIT_TICKS.applyToBuilder(builder).defineInRange("patrolWaitTicks", DaemonProtocolBlockEntity.DEFAULT_PATROL_WAIT_TICKS, 0, 1200));
        daemonProtocolRecallDistance = CachedDoubleValue.wrap(this, ConfigLocale.DAEMON_PROTOCOL_RECALL_DISTANCE.applyToBuilder(builder).defineInRange("recallDistance", DaemonProtocolBlockEntity.DEFAULT_RECALL_DISTANCE, 8, 512));
        builder.pop();

        ConfigLocale.INERTIA_DAMPENER.applyToBuilder(builder).push("inertia_dampener");
        InertiaDampenerBlockEntity.setConfigReference(
                CachedDoubleValue.wrap(this, ConfigLocale.INERTIA_DAMPENER_JOULES_PER_KINETIC_UNIT.applyToBuilder(builder).defineInRange("joulesPerKineticUnit", InertiaDampenerBlockEntity.DEFAULT_JOULES_PER_KINETIC_UNIT, 0, Double.MAX_VALUE)),
                CachedDoubleValue.wrap(this, ConfigLocale.INERTIA_DAMPENER_LINEAR_CALIBRATION.applyToBuilder(builder).defineInRange("linearCalibration", InertiaDampenerBlockEntity.DEFAULT_LINEAR_CALIBRATION, 0.01, Double.MAX_VALUE)));
        builder.pop();

//        builder.push("gray_goo");
//        grayGooTickInterval = CachedIntValue.wrap(this, ConfigLocale.GRAY_GOO_INTERVAL_TICKS.applyToBuilder(builder).defineInRange("tick_interval", 10, 0, Integer.MAX_VALUE));
//        grayGooTickContractingInterval = CachedIntValue.wrap(this, ConfigLocale.GRAY_GOO_CONTRACTING_INTERVAL_TICKS.applyToBuilder(builder).defineInRange("tick_interval_contracting", 5, 0, Integer.MAX_VALUE));
//        grayGooTickMaxMass = CachedIntValue.wrap(this, ConfigLocale.GRAY_GOO_MAX_MASS.applyToBuilder(builder).defineInRange("max_mass", 16_000, 32, Integer.MAX_VALUE));
//        grayGooTickMaxRadius = CachedIntValue.wrap(this, ConfigLocale.GRAY_GOO_MAX_RADIUS.applyToBuilder(builder).defineInRange("max_radius",  64, 3, Integer.MAX_VALUE));
//        builder.pop();

        configSpec = builder.build();
    }

    @Override
    public String getFileName() {
        return "common";
    }

    @Override
    public String getTranslation() {
        return "";
    }

    @Override
    public ModConfigSpec getConfigSpec() {
        return configSpec;
    }

    @Override
    public ModConfig.Type getConfigType() {
        return ModConfig.Type.COMMON;
    }
}
