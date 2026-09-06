package dev.alyti.fluxnautics.client.render.cloak;

import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import io.github.ocelot.glslprocessor.api.GlslInjectionPoint;
import io.github.ocelot.glslprocessor.api.GlslParser;
import io.github.ocelot.glslprocessor.api.GlslSyntaxException;
import io.github.ocelot.glslprocessor.api.node.GlslTree;

public class CloakFadePreProcessor implements ShaderPreProcessor {

    @Override
    public void modify(final Context ctx, final GlslTree tree) throws GlslSyntaxException {
        if (!ctx.isFragment()) {
            return;
        }

        tree.getBody().add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("uniform float FluxnauticsCloakOpacity;"));

        tree.mainFunction().orElseThrow().getBody().add(GlslParser.parseExpression("""
            if (FluxnauticsCloakOpacity > 0.0 && FluxnauticsCloakOpacity < 1.0) {
                vec4 cloakColor = vec4(fragColor.rgb, fragColor.a * FluxnauticsCloakOpacity);
                
                // 8x8 Bayer matrix dithering approximation to create a digital "cloaking" dissolve
                float ditherPattern = fract(dot(gl_FragCoord.xy, vec2(12.9898, 78.233)) + FluxnauticsCloakOpacity);
                
                if (ditherPattern > FluxnauticsCloakOpacity) {
                    discard;
                }
                
                fragColor = cloakColor;
            }
            """));
    }
}