package dev.alyti.fluxnautics.mixin.field;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.shaders.Uniform;
import dev.alyti.fluxnautics.client.util.SimpleSubLevelGroupRenderer;
import dev.ryanhcode.sable.sublevel.render.dispatcher.VanillaSubLevelRenderDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(VanillaSubLevelRenderDispatcher.class)
public class VanillaSubLevelRenderDispatcherMixin {

    @WrapOperation(method = "renderSectionLayer", at = @At(value = "INVOKE", target = "Ldev/ryanhcode/sable/sublevel/render/dispatcher/VanillaSubLevelRenderDispatcher;setupDynamicEffects(Lnet/minecraft/client/renderer/ShaderInstance;ZZ)V", ordinal = 0))
    private void fluxnautics$overrideNormalLighting(final ShaderInstance shader, final boolean onSubLevel, final boolean upload, final Operation<Void> original) {
        if (SimpleSubLevelGroupRenderer.RENDERING_SIMPLE) {
            original.call(shader, false, upload);
            return;
        }
        original.call(shader, onSubLevel, upload);
    }


    @Inject(method = "setupDynamicEffects", at = @At("TAIL"))
    private static void setupDynamicEffects(final ShaderInstance shader, final boolean onSubLevel, final boolean upload, final CallbackInfo ci) {
        final Uniform cameraY = shader.getUniform("FieldCameraY");

        if (cameraY != null) {
            final Minecraft minecraft = Minecraft.getInstance();
//            final EndSeaPhysics physics = EndSeaPhysicsData.of(minecraft.level);

            cameraY.set((float) minecraft.gameRenderer.getMainCamera().getPosition().y);
            if (onSubLevel /* && physics != null */) {
//                final float y = (float) (minecraft.gameRenderer.getMainCamera().getPosition().y - physics.startY());
//
//                cameraY.set(y);
                if (upload) {
                    cameraY.upload();
                }
            } else {
//                cameraY.set(0f);
                if (upload) {
                    cameraY.upload();
                }
            }
        }
    }

}