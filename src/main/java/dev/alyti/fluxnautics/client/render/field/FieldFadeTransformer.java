package dev.alyti.fluxnautics.client.render.field;

import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import io.github.ocelot.glslprocessor.api.GlslSyntaxException;
import io.github.ocelot.glslprocessor.api.node.GlslTree;

public class FieldFadeTransformer implements ShaderPreProcessor {

    @Override
    public void modify(final Context ctx, final GlslTree tree) throws GlslSyntaxException {
//        if (ctx instanceof final MinecraftContext minecraftContext) {
//            final List<RenderType> renderTypes = RenderType.chunkBufferLayers();
//
//            boolean anyMatches = false;
//
//            for (final RenderType renderType : renderTypes) {
//                if (ctx.isVertex() && minecraftContext.shaderInstance().equals("rendertype_%s".formatted(renderType.name))) {
//                    anyMatches = true;
//                }
//            }
//
//            if (!anyMatches) {
//                return;
//            }
//        } else {
//            return;
//        }
//
//        tree.getBody().add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("uniform float FieldCameraY;"));
//
//        // Add NormalMat if we're lacking it
//        if (tree.field("NormalMat").isEmpty()) {
//            tree.getBody().add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("uniform mat3 NormalMat;"));
//        }
//
//        final List<GlslNode> body = tree.mainFunction().orElseThrow().getBody();
//        body.add(GlslParser.parseExpression("""
//                    if (FieldCameraY != 0.0) {
//                        vertexColor.rgb = mix(vertexColor.rgb, vec3(0.086, 0.078, 0.109) * 2.0, clamp((-(inverse(NormalMat) * (ModelViewMat * vec4(pos, 0.0)).rgb).y - FieldCameraY) / 30.0, 0.0, 1.0));
//                    }
//                """));
    }
}