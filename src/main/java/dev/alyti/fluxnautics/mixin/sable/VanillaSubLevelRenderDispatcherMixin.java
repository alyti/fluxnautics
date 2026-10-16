package dev.alyti.fluxnautics.mixin.sable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.shaders.Uniform;
import dev.alyti.fluxnautics.client.CloakRenderStateTracker;
import dev.alyti.fluxnautics.client.CloakedSublevelCache;
import dev.ryanhcode.sable.mixin.sublevel_render.RenderSectionAccessor;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.dispatcher.VanillaSubLevelRenderDispatcher;
import dev.ryanhcode.sable.sublevel.render.vanilla.VanillaChunkedSubLevelRenderData;
import dev.alyti.fluxnautics.client.util.SimpleSubLevelGroupRenderer;
import foundry.veil.api.compat.IrisCompat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mixin(VanillaSubLevelRenderDispatcher.class)
public abstract class VanillaSubLevelRenderDispatcherMixin {
    @WrapOperation(method = "renderSectionLayer", at = @At(target="Ldev/ryanhcode/sable/sublevel/render/vanilla/VanillaChunkedSubLevelRenderData;renderChunkedSubLevel(Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/ShaderInstance;Lorg/joml/Matrix4f;DDD)V", value = "INVOKE"))
    private static void fluxnautics$wrapRenderLayer(VanillaChunkedSubLevelRenderData instance, RenderType layer, ShaderInstance shader, Matrix4f modelView, double camX, double camY, double camZ, Operation<Void> original) {
        float opacity = CloakRenderStateTracker.getOpacity(instance.getSubLevel());
        final Uniform cloakOpacity = shader.getUniform("FluxnauticsCloakOpacity");
        if (cloakOpacity != null) {
            cloakOpacity.set(opacity);
            cloakOpacity.upload();
        }
        final boolean blendFade = cloakOpacity == null && CloakIrisBlendFade.isActive() && opacity > 0.0F && opacity < 1.0F;
        if (blendFade)
            CloakIrisBlendFade.begin(opacity);
        original.call(instance, layer, shader, modelView, camX, camY, camZ);
        if (blendFade)
            CloakIrisBlendFade.end();
    }

    @Inject(method = "renderSectionLayer", at = @At("RETURN"))
    private void fluxnautics$resetCloakOpacity(Iterable<ClientSubLevel> sublevels, RenderType renderType, ShaderInstance shader, double cameraX, double cameraY, double cameraZ, Matrix4f modelView, Matrix4f projection, float partialTicks, CallbackInfo ci) {
        final Uniform cloakOpacity = shader.getUniform("FluxnauticsCloakOpacity");
        if (cloakOpacity != null) {
            cloakOpacity.set(0.0f);
            cloakOpacity.upload();
        }
    }


    @ModifyVariable(
            method = "renderSectionLayer",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private Iterable<ClientSubLevel> fluxnautics$filterCloakedSublevelsRenderSection(Iterable<ClientSubLevel> sublevels) {
        return fluxnautics$filterCloakedSublevels(sublevels, true);
    }

    @ModifyVariable(
            method = "renderAfterSections",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private Iterable<ClientSubLevel> fluxnautics$filterCloakedSublevelsRenderAfter(Iterable<ClientSubLevel> sublevels) {
        return fluxnautics$filterCloakedSublevels(sublevels, false);
    }

    @ModifyVariable(
            method = "renderBlockEntities",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private Iterable<ClientSubLevel> fluxnautics$filterCloakedSublevelsRenderBE(Iterable<ClientSubLevel> sublevels) {
        return fluxnautics$filterCloakedSublevels(sublevels, false);
    }

    @ModifyVariable(
            method = "updateCulling",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private Iterable<ClientSubLevel> fluxnautics$filterCloakedSublevelsUpdateCulling(Iterable<ClientSubLevel> sublevels) {
        return fluxnautics$filterCloakedSublevels(sublevels, false);
    }

    @WrapOperation(method = "renderBlockEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$CompiledSection;getRenderableBlockEntities()Ljava/util/List;"))
    private List<BlockEntity> fluxnautics$includeGlobalBlockEntitiesInSimpleRender(SectionRenderDispatcher.CompiledSection compiled, Operation<List<BlockEntity>> original, @Local(name = "renderSection") SectionRenderDispatcher.RenderSection renderSection) {
        final List<BlockEntity> blockEntities = original.call(compiled);
        if (!SimpleSubLevelGroupRenderer.RENDERING_SIMPLE) {
            return blockEntities;
        }
        final Set<BlockEntity> globalBlockEntities = ((RenderSectionAccessor) renderSection).getGlobalBlockEntities();
        if (globalBlockEntities == null || globalBlockEntities.isEmpty()) {
            return blockEntities;
        }
        final List<BlockEntity> merged = new ArrayList<>(blockEntities.size() + globalBlockEntities.size());
        merged.addAll(blockEntities);
        merged.addAll(globalBlockEntities);
        return merged;
    }


    @Unique
    private static Iterable<ClientSubLevel> fluxnautics$filterCloakedSublevels(Iterable<ClientSubLevel> sublevels, boolean fullyCloaked) {
        if (CloakedSublevelCache.isEmpty()) {
            return sublevels;
        }

        List<ClientSubLevel> filtered = new ArrayList<>();
        for (ClientSubLevel sublevel : sublevels) {
            if (fullyCloaked) {
                if (CloakRenderStateTracker.getOpacity(sublevel) > 0.0f) {
                    filtered.add(sublevel);
                }
            } else {
                if (!CloakedSublevelCache.isCloaked(sublevel)) {
                    filtered.add(sublevel);
                }
            }
        }
        return filtered;
    }


    public static final class CloakIrisBlendFade {
        public static boolean isActive() {
            if (!IrisCompat.isLoaded())
                return false;
            if (IrisCompat.INSTANCE == null)
                return false;
            return IrisCompat.INSTANCE.areShadersLoaded();
        }
        public static void begin(float opacity) {
            GL14.glBlendColor(1.0F, 1.0F, 1.0F, opacity);
            GL11.glEnable(GL11.GL_BLEND);
            GL20.glBlendFuncSeparate(GL14.GL_CONSTANT_ALPHA, GL14.GL_ONE_MINUS_CONSTANT_ALPHA, GL11.GL_ZERO, GL11.GL_ONE);
        }
        public static void end() {
            GL11.glDisable(GL11.GL_BLEND);
            GL20.glBlendFuncSeparate(GL11.GL_ONE, GL11.GL_ZERO, GL11.GL_ONE, GL11.GL_ZERO);
        }
    }
}