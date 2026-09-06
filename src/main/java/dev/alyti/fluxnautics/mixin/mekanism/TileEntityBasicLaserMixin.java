package dev.alyti.fluxnautics.mixin.mekanism;

import com.llamalad7.mixinextras.sugar.Local;
import dev.alyti.fluxnautics.capabilities.Capabilities;
import dev.alyti.fluxnautics.mixin_helper.ClipContextEnergyHolder;
import dev.alyti.fluxnautics.mixin_interface.mekanism.ILaserAccess;
import mekanism.api.lasers.ILaserDissipation;
import mekanism.common.capabilities.energy.LaserEnergyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = mekanism.common.tile.laser.TileEntityBasicLaser.class, remap = false)
public abstract class TileEntityBasicLaserMixin implements ILaserAccess {

    @Invoker("toFire")
    @Override
    public abstract long fluxnautics$toFire();

    @Invoker("getEnergyContainer")
    public abstract LaserEnergyContainer fluxnautics$getEnergyContainer();

    @Inject(method = "onUpdateServer", at = @At("HEAD"))
    private void fluxnautics$startEnergyTrace(CallbackInfoReturnable<Boolean> cir) {
        ClipContextEnergyHolder.setEnergy(Math.min(fluxnautics$getEnergyContainer().getEnergy(), this.fluxnautics$toFire()));
    }

    @Inject(method = "onUpdateServer", at = @At("RETURN"))
    private void fluxnautics$endEnergyTrace(CallbackInfoReturnable<Boolean> cir) {
        ClipContextEnergyHolder.clearEnergy();
    }

    // Intercept the initialization "double dissipationPercent = 0;"
    @ModifyVariable(method = "onUpdateServer", at = @At("STORE"), name = "dissipationPercent")
    private double fluxnautics$initDissipation(double dissipationPercent, @Local Entity entity) {
        if (entity instanceof LivingEntity livingEntity) {
            ILaserDissipation cap = livingEntity.getCapability(Capabilities.LASER_DISSIPATION_ENTITY);
            if (cap != null)
                return cap.getDissipationPercent();
        }
        return dissipationPercent;
    }

    // Intercept the initialization "double refractionPercent = 0;"
    @ModifyVariable(method = "onUpdateServer", at = @At("STORE"), name = "refractionPercent")
    private double fluxnautics$initRefraction(double refractionPercent, @Local Entity entity) {
        if (entity instanceof LivingEntity livingEntity) {
            ILaserDissipation cap = livingEntity.getCapability(Capabilities.LASER_DISSIPATION_ENTITY);
            if (cap != null)
                return cap.getRefractionPercent();
        }
        return refractionPercent;
    }
}
