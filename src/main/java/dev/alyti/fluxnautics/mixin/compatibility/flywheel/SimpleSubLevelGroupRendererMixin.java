package dev.alyti.fluxnautics.mixin.compatibility.flywheel;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.alyti.fluxnautics.client.util.SimpleSubLevelGroupRenderer;
import dev.alyti.fluxnautics.mixin_interface.field.VisualManagerExtension;
import dev.alyti.fluxnautics.mixin_interface.field.VisualizationManagerExtension;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.ryanhcode.sable.mixinhelpers.sublevel_render.vanilla.VanillaSubLevelBlockEntityRenderer;
import dev.ryanhcode.sable.mixinterface.BlockEntityRenderDispatcherExtension;
import dev.ryanhcode.sable.neoforge.mixinhelper.compatibility.flywheel.SubLevelEmbedding;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderData;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(SimpleSubLevelGroupRenderer.class)
public class SimpleSubLevelGroupRendererMixin {

    @Inject(method="renderGroup", at=@At(value="INVOKE", target = "Ldev/ryanhcode/sable/sublevel/render/dispatcher/SubLevelRenderDispatcher;renderBlockEntities(Ljava/lang/Iterable;Ldev/ryanhcode/sable/sublevel/render/dispatcher/SubLevelRenderDispatcher$BlockEntityRenderer;DDDF)V"))
    private static void fluxnauticsFlywheel$renderGroup(ClientLevel level, Collection<ClientSubLevel> subLevels, AdvancedFbo fbo, Matrix4f modelView, Matrix4f projectionMat, Vector3d cameraPosition, Quaternionf orientation, float partialTicks, boolean renderPlayers, CallbackInfo ci, @Local(name = "beRenderer") VanillaSubLevelBlockEntityRenderer beRenderer) {
        final VisualizationManager visualizationManager = VisualizationManager.get(level);

        // Render block-entities with visuals normally
        if (visualizationManager instanceof final VisualizationManagerExtension extension) {
            extension.fluxnautics$setDrawingDiagram(true);

            for (final ClientSubLevel beSubLevel : subLevels) {
                final BlockEntityRenderDispatcherExtension dispatcher = (BlockEntityRenderDispatcherExtension) beRenderer.getBlockEntityRenderDispatcher();

                final SubLevelEmbedding embeddingInfo = ((VisualManagerExtension) visualizationManager.blockEntities()).fluxnautics$getBEEmbeddingInfo(beSubLevel);

                if (embeddingInfo == null) {
                    continue;
                }

                final Vector3d chunkOffset = new Vector3d();
                final Matrix4f transformation = new Matrix4f();
                final Matrix4f transformationInverse = new Matrix4f();

                final SubLevelRenderData data = beSubLevel.getRenderData();

                beSubLevel.renderPose().rotationPoint().negate(chunkOffset.zero());
                data.getTransformation(cameraPosition.x, cameraPosition.y, cameraPosition.z, transformation);

                final Vector3f c = transformation.invert(transformationInverse).transformPosition(new Vector3f());
                dispatcher.sable$setCameraPosition(new Vec3(c.x - chunkOffset.x(), c.y - chunkOffset.y(), c.z - chunkOffset.z()));

                final PoseStack beMatrices = new PoseStack();
                beMatrices.pushPose();
                beMatrices.mulPose(transformation);
                beRenderer.renderBlockEntities(embeddingInfo.blockEntities(), beMatrices, partialTicks, -chunkOffset.x, -chunkOffset.y, -chunkOffset.z);
                beMatrices.popPose();

                dispatcher.sable$setCameraPosition(null);
            }

            extension.fluxnautics$setDrawingDiagram(false);
        }
    }
}
