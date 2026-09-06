package dev.alyti.fluxnautics.config;

import dev.alyti.fluxnautics.client.render.ThrusterTrailManager;
import mekanism.common.config.BaseMekanismConfig;
import mekanism.common.config.value.CachedBooleanValue;
import mekanism.common.config.value.CachedDoubleValue;
import mekanism.common.config.value.CachedIntValue;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ClientConfig extends BaseMekanismConfig {

    private final ModConfigSpec configSpec;

    public final CachedDoubleValue thrusterPlumeIntensity;
    public final CachedDoubleValue thrusterPlumeChaseSpeed;
    public final CachedBooleanValue thrusterTrailsEnabled;
    public final CachedDoubleValue thrusterTrailLifetime;
    public final CachedDoubleValue thrusterTrailWidthScale;
    public final CachedIntValue thrusterTrailShape;

    public ClientConfig() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        ConfigLocale.THRUSTER_RENDERER.applyToBuilder(builder).push("thruster_renderer");
        thrusterPlumeIntensity = CachedDoubleValue.wrap(this, ConfigLocale.THRUSTER_RENDERER_PLUME_INTENSITY.applyToBuilder(builder).defineInRange("plumeIntensity", 1.0, 0, 4));
        thrusterPlumeChaseSpeed = CachedDoubleValue.wrap(this, ConfigLocale.THRUSTER_RENDERER_PLUME_CHASE_SPEED.applyToBuilder(builder).defineInRange("plumeChaseSpeed", 0.05, 0.005, 1));
        thrusterTrailsEnabled = CachedBooleanValue.wrap(this, ConfigLocale.THRUSTER_RENDERER_TRAILS_ENABLED.applyToBuilder(builder).define("trailsEnabled", true));
        thrusterTrailLifetime = CachedDoubleValue.wrap(this, ConfigLocale.THRUSTER_RENDERER_TRAIL_LIFETIME.applyToBuilder(builder).defineInRange("trailLifetime", 2.0, 0.25, 8));
        thrusterTrailWidthScale = CachedDoubleValue.wrap(this, ConfigLocale.THRUSTER_RENDERER_TRAIL_WIDTH_SCALE.applyToBuilder(builder).defineInRange("trailWidthScale", 1.0, 0.1, 4));
        thrusterTrailShape = CachedIntValue.wrap(this, ConfigLocale.THRUSTER_RENDERER_TRAIL_SHAPE.applyToBuilder(builder).defineInRange("trailShape", ThrusterTrailManager.SHAPE_PROFILE, ThrusterTrailManager.SHAPE_RIBBON, ThrusterTrailManager.SHAPE_PROFILE));
        builder.pop();

        configSpec = builder.build();
    }

    public void applyToRenderers() {
        ThrusterTrailManager.enabled = thrusterTrailsEnabled.getOrDefault();
        ThrusterTrailManager.lifetimeS = (float) thrusterTrailLifetime.getOrDefault();
        ThrusterTrailManager.widthScale = (float) thrusterTrailWidthScale.getOrDefault();
        ThrusterTrailManager.shapeMode = thrusterTrailShape.getOrDefault();
    }

    @Override
    public String getFileName() {
        return "client";
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
        return ModConfig.Type.CLIENT;
    }
}
