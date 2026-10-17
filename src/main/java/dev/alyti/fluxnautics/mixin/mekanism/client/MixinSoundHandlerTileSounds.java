package dev.alyti.fluxnautics.mixin.mekanism.client;

import mekanism.client.sound.SoundHandler;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SoundHandler.class, remap = false)
public abstract class MixinSoundHandlerTileSounds {

    @Inject(method = "onTilePlaySound", at = @At("HEAD"))
    private static void fluxnautics$registerTileSound(PlaySoundEvent event, CallbackInfo ci) {
        SoundInstance sound = event.getSound();
        if (sound == null) {
            return;
        }

        ResourceLocation location = event.getOriginalSound().getLocation();
        if (!"fluxnautics".equals(location.getNamespace()) || !location.getPath().startsWith("tile.")) {
            return;
        }

        BlockPos pos = BlockPos.containing(sound.getX() - 0.5, sound.getY() - 0.5, sound.getZ() - 0.5);
        AccessorSoundHandler.fluxnautics$getTileSounds().put(pos.asLong(), sound);
    }
}
