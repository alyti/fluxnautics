package dev.alyti.fluxnautics.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.alyti.fluxnautics.Fluxnautics;
import foundry.veil.api.client.render.VeilRenderBridge;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.TheEndPortalRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

public final class SFRenderTypes extends RenderType {
    private static final ResourceLocation HEIGHTMAP_TEXTURE = Fluxnautics.id("heightmap_texture");
    private static final ResourceLocation HEIGHTMAP_TESSELLATION = Fluxnautics.id("heightmap_tessellation");

    private static final RenderType LASER = create(
            Fluxnautics.MODID + ":laser",
            DefaultVertexFormat.POSITION_TEX_COLOR,
            VertexFormat.Mode.QUADS,
            TRANSIENT_BUFFER_SIZE,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(VeilRenderBridge.shaderState(Fluxnautics.id("new_laser/laser")))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .createCompositeState(true)
    );

    public static RenderType END_PORTAL_CUSTOM = RenderType.create(Fluxnautics.MODID + ":endportal",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(VeilRenderBridge.shaderState(Fluxnautics.id("field/endportal")))
                    .setTextureState(RenderStateShard.MultiTextureStateShard.builder()
                            .add(TheEndPortalRenderer.END_SKY_LOCATION, false, false)
                            .add(TheEndPortalRenderer.END_PORTAL_LOCATION, false, false).build())
                    .createCompositeState(false));

    private SFRenderTypes(final String name, final VertexFormat format, final VertexFormat.Mode mode, final int bufferSize, final boolean affectsCrumbling, final boolean sortOnUpload, final Runnable setupState, final Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static RenderType classicLaser() {
        return VeilRenderType.get(Fluxnautics.id("laser_classic"));
    }


    public static RenderType endPortalEffect() {
        return END_PORTAL_CUSTOM;
    }


    public static @Nullable RenderType heightmap(boolean tessellation) {
        return VeilRenderType.get(tessellation ? HEIGHTMAP_TESSELLATION : HEIGHTMAP_TEXTURE);
    }

    public static RenderType endPortalEyes(ResourceLocation emissive) {
        return endPortalEffect.apply(emissive, TransparencyStateShard.ADDITIVE_TRANSPARENCY);
    }

    public static final BiFunction<ResourceLocation, TransparencyStateShard, RenderType> endPortalEffect = Util.memoize(
        (resource, transparency) -> create(
            "fluxnautics:endportal_eyes",
            DefaultVertexFormat.POSITION_TEX_COLOR,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            true,
            CompositeState.builder()
                .setShaderState(VeilRenderBridge.shaderState(Fluxnautics.id("field/endportal_eyes")))
                .setTextureState(MultiTextureStateShard.builder()
                    .add(TheEndPortalRenderer.END_SKY_LOCATION, false, false)
                    .add(TheEndPortalRenderer.END_PORTAL_LOCATION, false, false)
                    .add(resource, false, false)
                    .build())
                .setTransparencyState(transparency)
                .setCullState(RenderStateShard.NO_CULL)
                .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                .createCompositeState(false)
        )
    );
}
