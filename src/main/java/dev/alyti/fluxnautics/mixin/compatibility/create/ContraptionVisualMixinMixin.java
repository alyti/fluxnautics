package dev.alyti.fluxnautics.mixin.compatibility.create;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.render.ContraptionVisual;
import dev.alyti.fluxnautics.client.CloakedSublevelCache;
import dev.engine_room.flywheel.api.visualization.VisualEmbedding;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.AbstractEntityVisual;
import dev.ryanhcode.sable.neoforge.compatibility.flywheel.FlywheelCompatNeoForge;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ContraptionVisual.class, priority = 1500)
public abstract class ContraptionVisualMixinMixin extends AbstractEntityVisual<AbstractContraptionEntity> {

    @Unique
    private static final Matrix4f FLUXNAUTICS$HIDDEN_TRANSFORM = new Matrix4f().translation(0.0f, -999999.0f, 0.0f);

    @Unique
    private static final Matrix3f FLUXNAUTICS$HIDDEN_NORMAL = new Matrix3f();

    @Final
    @Shadow(remap = false)
    protected VisualEmbedding embedding;

    public ContraptionVisualMixinMixin(final VisualizationContext ctx, final AbstractContraptionEntity entity, final float partialTick) {
        super(ctx, entity, partialTick);
    }

    @TargetHandler(
            mixin = "dev.ryanhcode.sable.neoforge.mixin.compatibility.create.contraptions.ContraptionVisualMixin",
            name = "sable$setEmbeddingMatrices"
    )
    @Inject(
            method = "@MixinSquared:Handler",
            at = @At("TAIL")
    )
    private void fluxnautics$hideCloakedContraption(float partialTick, CallbackInfo ci, CallbackInfo ci2, @Local FlywheelCompatNeoForge.SubLevelFlwRenderState state) {
        if (CloakedSublevelCache.isCloaked(this.entity.level().dimension(), state.subLevelID)) {
            this.embedding.transforms(FLUXNAUTICS$HIDDEN_TRANSFORM, FLUXNAUTICS$HIDDEN_NORMAL);
        }
    }
}