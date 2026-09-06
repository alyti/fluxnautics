package dev.alyti.fluxnautics.registry;

import dev.alyti.fluxnautics.Fluxnautics;
import mekanism.common.registration.impl.SoundEventDeferredRegister;
import mekanism.common.registration.impl.SoundEventRegistryObject;
import net.minecraft.sounds.SoundEvent;

public class FNSounds {
    private FNSounds() {
    }

    public static final SoundEventDeferredRegister SOUND_EVENTS = new SoundEventDeferredRegister(Fluxnautics.MODID);

    public static final SoundEventRegistryObject<SoundEvent> FORCEFIELD_EMITTER = SOUND_EVENTS.register("tile.forcefield_emitter");
    public static final SoundEventRegistryObject<SoundEvent> FLUX_TETHER_ASSEMBLE = SOUND_EVENTS.register("tile.flux_tether.assemble");
    public static final SoundEventRegistryObject<SoundEvent> FLUX_TETHER_DISASSEMBLE = SOUND_EVENTS.register("tile.flux_tether.disassemble");
    public static final SoundEventRegistryObject<SoundEvent> FLUX_TETHER_BEAM_TELEGRAPH = SOUND_EVENTS.register("tile.flux_tether.beam_telegraph");

}
