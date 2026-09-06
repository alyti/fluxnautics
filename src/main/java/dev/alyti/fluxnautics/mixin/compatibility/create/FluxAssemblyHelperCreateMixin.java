package dev.alyti.fluxnautics.mixin.compatibility.create;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import dev.alyti.fluxnautics.util.FluxAssemblyHelper;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.List;

@Mixin(value = FluxAssemblyHelper.class, remap = false)
public class FluxAssemblyHelperCreateMixin {

    @Inject(method = "fluxnautics$disassembleIntersectingContraptions", at = @At("HEAD"))
    private static void fluxnautics$handleCreateContraptions(Level level, BoundingBox3ic assemblyBounds, Collection<BlockPos> blocks, boolean passGluesBack, Collection<Object> collectedGlues, CallbackInfo ci) {
        AABB assemblyBoundsD = new AABB(
                assemblyBounds.minX(), assemblyBounds.minY(), assemblyBounds.minZ(),
                assemblyBounds.maxX() + 1, assemblyBounds.maxY() + 1, assemblyBounds.maxZ() + 1
        );

        List<ControlledContraptionEntity> intersectingContraptions = level.getEntitiesOfClass(ControlledContraptionEntity.class, assemblyBoundsD.inflate(2.0));

        for (ControlledContraptionEntity contraptionEntity : intersectingContraptions) {
            final ControlledContraptionEntityAccessor accessor = ((ControlledContraptionEntityAccessor) contraptionEntity);
            final BlockPos controllerPos = accessor.getControllerPos();
            if (blocks.contains(controllerPos)) {
                Contraption contraption = contraptionEntity.getContraption();
                StructureTransform transform = accessor.invokeMakeStructureTransform();

                for (BlockPos contraptionBlock : contraption.getBlocks().keySet()) {
                    BlockPos targetPos = transform.apply(contraptionBlock);
                    blocks.add(targetPos);
                }

                if (passGluesBack) {
                    List<AABB> superGlue = ((ContraptionAccessor) contraption).getSuperGlue();
                    for (AABB aabb : superGlue) {
                        aabb = new AABB(transform.apply(new Vec3(aabb.minX, aabb.minY, aabb.minZ)),
                                transform.apply(new Vec3(aabb.maxX, aabb.maxY, aabb.maxZ)));
                        collectedGlues.add(aabb);
                    }
                    superGlue.clear();
                }
                contraptionEntity.disassemble();

                BlockEntity blockEntity = level.getBlockEntity(controllerPos);
                // Reflection fallback for simulated's IControlContraptionExtension if simulated isn't loaded at compile time
                try {
                    Class<?> extClass = Class.forName("dev.simulated_team.simulated.mixin_interface.create_assembly.IControlContraptionExtension");
                    if (extClass.isInstance(blockEntity)) {
                        extClass.getMethod("sable$disassemble").invoke(blockEntity);
                    }
                } catch (Exception ignored) { }
            }
        }
    }

    @Inject(method = "fluxnautics$moveGlues", at = @At("HEAD"))
    private static void fluxnautics$moveCreateGlues(Level level, BlockPos offset, Collection<Object> contraptionGlues, Collection<Object> superGlues, Collection<Object> honeyGlues, CallbackInfo ci) {
        Vec3 offsetVec = Vec3.atLowerCornerOf(offset);
        for (Object obj : contraptionGlues) {
            if (obj instanceof AABB box) {
                level.addFreshEntity(new SuperGlueEntity(level, box.move(offsetVec)));
            }
        }
        for (Object obj : superGlues) {
            if (obj instanceof SuperGlueEntity glue) {
                glue.remove(Entity.RemovalReason.KILLED);
                level.addFreshEntity(new SuperGlueEntity(level, glue.getBoundingBox().move(offsetVec)));
            }
        }
    }

    @Inject(method = "fluxnautics$handleGlueEntityDisassemble", at = @At("HEAD"), cancellable = true)
    private static void fluxnautics$handleCreateGlueDisassemble(Level level, Entity entity, SubLevelAssemblyHelper.AssemblyTransform transform, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof SuperGlueEntity) {
            AABB box = entity.getBoundingBox();
            box = new AABB(transform.apply(new Vec3(box.minX, box.minY, box.minZ)),
                    transform.apply(new Vec3(box.maxX, box.maxY, box.maxZ)));
            entity.remove(Entity.RemovalReason.KILLED);
            level.addFreshEntity(new SuperGlueEntity(level, box));
            cir.setReturnValue(true);
        }
    }
}