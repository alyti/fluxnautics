package dev.alyti.fluxnautics.mixin.compatibility.simulated;

import com.simibubi.create.content.contraptions.AssemblyException;
import dev.alyti.fluxnautics.util.FluxAssemblyException;
import dev.alyti.fluxnautics.util.FluxAssemblyHelper;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.simulated_team.simulated.content.entities.honey_glue.HoneyGlueEntity;
import dev.simulated_team.simulated.util.assembly.SimAssemblyContraption;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(value = FluxAssemblyHelper.class, remap = false)
public class FluxAssemblyHelperSimulatedMixin {

    @Inject(method = "fluxnautics$searchStructure", at = @At("HEAD"), cancellable = true)
    private static void fluxnautics$useSimulatedSearch(Level level, BlockPos selfPos, BlockPos toAssemble, boolean includeStart, boolean includeEncasingGlue, Collection<BlockPos> blocks, Collection<Object> superGlues, Collection<Object> honeyGlues, CallbackInfo ci) {
        try {
            SimAssemblyContraption contraption = new SimAssemblyContraption(includeStart ? null : selfPos, !includeEncasingGlue);
            contraption.searchMovedStructure(level, toAssemble);
            blocks.addAll(contraption.getBlocks());
            superGlues.addAll(contraption.getGlues());
            honeyGlues.addAll(contraption.getHoneyGlues());
        } catch (AssemblyException e) {
            throw new FluxAssemblyException(e.component);
        }
        ci.cancel();
    }

    @Inject(method = "fluxnautics$moveGlues", at = @At("HEAD"))
    private static void fluxnautics$moveHoneyGlues(Level level, BlockPos offset, Collection<Object> contraptionGlues, Collection<Object> superGlues, Collection<Object> honeyGlues, CallbackInfo ci) {
        Vec3 offsetVec = Vec3.atLowerCornerOf(offset);
        for (Object obj : honeyGlues) {
            if (obj instanceof HoneyGlueEntity glue) {
                glue.remove(Entity.RemovalReason.KILLED);
                AABB newBB = glue.getBoundingBox().move(offsetVec);
                HoneyGlueEntity newHoneyGlue = new HoneyGlueEntity(level, newBB);
                level.addFreshEntity(newHoneyGlue);
                newHoneyGlue.setBoundsAndSync(newBB);
            }
        }
    }

    @Inject(method = "fluxnautics$handleGlueEntityDisassemble", at = @At("HEAD"), cancellable = true)
    private static void fluxnautics$handleHoneyGlueDisassemble(Level level, Entity entity, SubLevelAssemblyHelper.AssemblyTransform transform, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof HoneyGlueEntity) {
            AABB box = entity.getBoundingBox();
            box = new AABB(transform.apply(new Vec3(box.minX, box.minY, box.minZ)),
                    transform.apply(new Vec3(box.maxX, box.maxY, box.maxZ)));
            entity.remove(Entity.RemovalReason.KILLED);
            HoneyGlueEntity newHoneyGlue = new HoneyGlueEntity(level, box);
            level.addFreshEntity(newHoneyGlue);
            newHoneyGlue.setBoundsAndSync(box);
            cir.setReturnValue(true);
        }
    }
}