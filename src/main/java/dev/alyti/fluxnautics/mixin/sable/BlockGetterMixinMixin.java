package dev.alyti.fluxnautics.mixin.sable;

import com.bawnorton.mixinsquared.TargetHandler;
import dev.alyti.fluxnautics.FluxnauticsConfig;
import dev.alyti.fluxnautics.content.blocks.forcefield.ForcefieldEmitterBlockEntity;
import dev.alyti.fluxnautics.content.blocks.forcefield.ForcefieldRegistry;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.Position;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(value = BlockGetter.class, priority = 1600)
public interface BlockGetterMixinMixin {

    /**
     * @author Lunalyti
     * @reason Hijack clips into forcefields from outside
     */
    @TargetHandler(
        mixin = "dev.ryanhcode.sable.mixin.clip_overwrite.BlockGetterMixin",
        name = "clip"
    )
    @Inject(method = "@MixinSquared:Handler", at = @At("RETURN"), cancellable = true)
    default void clip(ClipContext clipContext, CallbackInfoReturnable<BlockHitResult> cir) {
        if (!FluxnauticsConfig.common.forcefieldRedirectRaycast.get())
            return;

        BlockHitResult originalResult = cir.getReturnValue();

        BlockGetter self = (BlockGetter) (Object) this;
        if (!(self instanceof Level level))
            return;
        if (level.isClientSide())
            return;

        Set<ForcefieldEmitterBlockEntity> fields = ForcefieldRegistry.getActiveFields(level);
        if (fields.isEmpty())
            return;

        Vec3 start = clipContext.getFrom();
        Vec3 end = clipContext.getTo();

        double originalDist = Double.MAX_VALUE;
        if (originalResult.getType() != HitResult.Type.MISS) {
            Position hit = originalResult.getLocation();

            if (Sable.HELPER.isInPlotGrid(level, hit)) {
                hit = Sable.HELPER.projectOutOfSubLevel(level, originalResult.getLocation());
            }

            originalDist = start.distanceToSqr((Vec3) hit);
        }

        BlockHitResult closestFieldHit = null;
        double closestFieldDist = Double.MAX_VALUE;
        ForcefieldEmitterBlockEntity closestField = null;

        for (ForcefieldEmitterBlockEntity emitter : fields) {
            BlockHitResult fieldHit = emitter.checkRaycast(start, end);
            if (fieldHit == null) continue;

            double dist = start.distanceToSqr(fieldHit.getLocation());
            if (dist < closestFieldDist) {
                closestFieldDist = dist;
                closestFieldHit = fieldHit;
                closestField = emitter;
            }
        }

        if (closestFieldHit != null && closestFieldDist < originalDist) {
            closestField.applyRaycastCost(clipContext, closestFieldHit);
            cir.setReturnValue(closestFieldHit);
        }
    }
}