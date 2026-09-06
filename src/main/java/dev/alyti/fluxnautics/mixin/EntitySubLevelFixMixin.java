package dev.alyti.fluxnautics.mixin;

import dev.alyti.fluxnautics.content.entities.gear.EntityMekaRobit;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntitySubLevelFixMixin {

    // Prevents Sable from instantly snapping the drone's position to the local plot coordinates (millions of blocks away)
    @Inject(method = "setPos(DDD)V", at = @At("HEAD"), cancellable = true)
    private void fluxnautics$preventLocalSnap(double x, double y, double z, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof EntityMekaRobit drone && drone.isLocallyPiloted()) {
            // If the target position is far away, it's a local sublevel coordinate, so we cancel it to stay in global space
            if (self.distanceToSqr(x, y, z) > 10000.0D * 10000.0D) {
                ci.cancel();
            }
        }
    }

    // Prevents Sable from lerping the drone to the local plot coordinates
    @Inject(method = "lerpTo", at = @At("HEAD"), cancellable = true)
    private void fluxnautics$preventLocalLerp(double x, double y, double z, float yRot, float xRot, int lerpSteps, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof EntityMekaRobit drone && drone.isLocallyPiloted()) {
            if (self.distanceToSqr(x, y, z) > 10000.0D * 10000.0D) {
                ci.cancel();
            }
        }
    }
}