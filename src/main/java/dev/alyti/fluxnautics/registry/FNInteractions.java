package dev.alyti.fluxnautics.registry;

import dev.alyti.fluxnautics.api.interaction.InteractCallback;
import dev.alyti.fluxnautics.content.items.drone.MekaRobitClientHandler;
import dev.alyti.fluxnautics.content.items.manipulator.MekaToolClientHandler;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.Set;

public class FNInteractions {

    public static Set<InteractCallback> CLICK_INTERACTION_ENTRIES = new ObjectOpenHashSet<>();

    public static MekaRobitClientHandler.MouseHandlerMekaRobit MEKA_ROBIT_MANAGER = register(new MekaRobitClientHandler.MouseHandlerMekaRobit());
    public static MekaToolClientHandler.MouseHandler MEKA_TOOL_MANAGER = register(new MekaToolClientHandler.MouseHandler());

    public static <T extends InteractCallback> T register(final T interaction) {
        CLICK_INTERACTION_ENTRIES.add(interaction);
        return interaction;
    }
}
