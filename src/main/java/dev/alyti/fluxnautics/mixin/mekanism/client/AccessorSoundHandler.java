package dev.alyti.fluxnautics.mixin.mekanism.client;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import mekanism.client.sound.SoundHandler;
import net.minecraft.client.resources.sounds.SoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = SoundHandler.class, remap = false)
public interface AccessorSoundHandler {

    @Accessor(value = "soundMap", remap = false)
    static Long2ObjectMap<SoundInstance> fluxnautics$getTileSounds() {
        throw new AssertionError();
    }
}