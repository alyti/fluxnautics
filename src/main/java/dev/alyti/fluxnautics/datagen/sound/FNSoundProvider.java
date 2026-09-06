package dev.alyti.fluxnautics.datagen.sound;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.registry.FNSounds;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.function.UnaryOperator;

public class FNSoundProvider extends BaseSoundProvider {
    public FNSoundProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, existingFileHelper, Fluxnautics.MODID);
    }

    @Override
    public void registerSounds() {
        addTileSoundEvents();
    }

    private void addTileSoundEvents() {
        String basePath = "tile/";
        addSoundEventWithSubtitle(FNSounds.FORCEFIELD_EMITTER, basePath + "forcefield_emitter");
        addSoundEventWithSubtitle(FNSounds.FLUX_TETHER_ASSEMBLE, basePath + "flux_tether/assemble");
        addSoundEventWithSubtitle(FNSounds.FLUX_TETHER_DISASSEMBLE, basePath + "flux_tether/disassemble");
        addSoundEvent(FNSounds.FLUX_TETHER_BEAM_TELEGRAPH, basePath + "flux_tether/beam_telegraph", (def) -> def, UnaryOperator.identity());
    }
}
