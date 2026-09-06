package dev.alyti.fluxnautics;

import dev.alyti.fluxnautics.config.ClientConfig;
import dev.alyti.fluxnautics.config.CommonConfig;
import dev.alyti.fluxnautics.config.ConfigHelper;
import mekanism.common.config.IMekanismConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FluxnauticsConfig {

    private static final Map<IConfigSpec, IMekanismConfig> KNOWN_CONFIGS = new HashMap<>();

    public static final CommonConfig common = new CommonConfig();
    public static final ClientConfig client = new ClientConfig();

    public static void registerConfigs(ModContainer modContainer) {
        ConfigHelper.registerConfig(KNOWN_CONFIGS, modContainer, common);
        ConfigHelper.registerConfig(KNOWN_CONFIGS, modContainer, client);
    }

    static void onLoad(final ModConfigEvent event) {
        ConfigHelper.onConfigLoad(event, Fluxnautics.MODID, KNOWN_CONFIGS);
        if (Fluxnautics.MODID.equals(event.getConfig().getModId())
                && event.getConfig().getType() == ModConfig.Type.CLIENT) {
            client.applyToRenderers();
        }
    }

    public static Collection<IMekanismConfig> getConfigs() {
        return Collections.unmodifiableCollection(KNOWN_CONFIGS.values());
    }
}
