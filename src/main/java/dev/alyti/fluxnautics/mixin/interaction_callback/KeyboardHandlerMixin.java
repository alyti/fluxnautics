package dev.alyti.fluxnautics.mixin.interaction_callback;

import dev.alyti.fluxnautics.FluxnauticsClientEvents;
import dev.alyti.fluxnautics.api.interaction.InteractCallback;
import dev.alyti.fluxnautics.util.FNDistUtil;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "keyPress", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;", ordinal = 0, opcode = 180/*GETFIELD*/), cancellable = true)
    private void fluxnautics$preOnPress(final long windowPointer, final int key, final int scanCode, final int action, final int modifiers, final CallbackInfo ci) {
        if (this.minecraft.screen == null) {
            if (FNDistUtil.getClientPlayer() != null && !FNDistUtil.getClientPlayer().isSpectator()) {
                final InteractCallback.Result status = FluxnauticsClientEvents.onBeforeMouseInput(InteractCallback.Input.key(key, scanCode), modifiers, action);
                if (status.cancelled()) {
                    ci.cancel();
                }
            }
        }
    }
}