package dev.alyti.fluxnautics.mixin.compatibility.flywheel;

import dev.alyti.fluxnautics.mixin_interface.field.VisualizationManagerExtension;
import dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VisualizationManagerImpl.class)
public class VisualizationManagerImplMixin implements VisualizationManagerExtension {

    @Unique
    private static boolean fluxnautics$drawingDiagram = false;

    @Inject(method = "supportsVisualization", at = @At("HEAD"), cancellable = true)
    private static void fluxnautics$supportsVisualization(final LevelAccessor level, final CallbackInfoReturnable<Boolean> cir) {
        if (fluxnautics$drawingDiagram) {
            cir.setReturnValue(false);
        }
    }

    @Override
    public void fluxnautics$setDrawingDiagram(final boolean drawing) {
        fluxnautics$drawingDiagram = drawing;
    }
}