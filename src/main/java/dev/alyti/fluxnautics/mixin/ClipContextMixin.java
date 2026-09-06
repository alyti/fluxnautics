package dev.alyti.fluxnautics.mixin;

import dev.alyti.fluxnautics.mixin_helper.ClipContextEnergyHolder;
import dev.alyti.fluxnautics.mixin_interface.minecraft.IClipContextExtension;
import net.minecraft.world.level.ClipContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClipContext.class)
public class ClipContextMixin implements IClipContextExtension {

    @Unique
    private long fluxnautics$energy = 0L;

    @Override
    public void fluxnautics$setEnergy(long energy) {
        this.fluxnautics$energy = energy;
    }

    @Override
    public long fluxnautics$getEnergy() {
        return this.fluxnautics$energy;
    }

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void fluxnautics$carryEnergy(CallbackInfo ci) {
        // If a laser is currently firing on this thread, carry the energy over to this new ClipContext
        long activeEnergy = ClipContextEnergyHolder.getEnergy();
        if (activeEnergy > 0) {
            this.fluxnautics$energy = activeEnergy;
        }
    }
}