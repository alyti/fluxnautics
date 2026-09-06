#include veil:fog
#include fluxnautics:util

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;

uniform float time;

in float vertexDistance;
in vec2 lengthData;
in vec4 vertexColor;

out vec4 fragColor;

// #veil:normal
const vec3 normal = vec3(0.0, 0.0, -1.0);

float noise(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {

    // #veil:albedo
    float endTaper = max(lengthData.y - 1.0, 0.0);

    float lengthPos = min(lengthData.y, 1.0);

    float coreFade = 1.0 - lengthPos;
    coreFade = pow(coreFade, 0.5);

    float radialDist = 0.1;
    float coreGlow = 1.0 - radialDist;
    coreGlow = pow(coreGlow, 2.0);

    float halo = exp(-radialDist * 3.0);

    float flicker = 0.85 + 0.15 * sin(time * 32.0 - lengthPos * 40.0);
    flicker *= 0.95 + 0.05 * noise(vec2(lengthPos * 5.0, time * 2.0));

    float wisp1 = sin(lengthPos * 15.0 - time * 3.0 + radialDist * 5.0) * 0.5 + 0.5;
    float wisp2 = sin(lengthPos * 20.0 + time * 4.0 - radialDist * 7.0) * 0.5 + 0.5;
    float turbulence = (wisp1 * 0.3 + wisp2 * 0.2) * (1.0 - lengthPos);

    float intensity = coreFade * (coreGlow * 1.5 + halo * 0.8 + turbulence) * flicker;

    vec4 color = vertexColor;
    if (vertexColor.a > 0.99) {
        float dim = vertexColor.b > 0.001 ? vertexColor.b : 1.0;
        color = vec4(oklabRainbow(vertexColor.r, vertexColor.g) * dim, 10.0 / 60.0);
    }

    color.a *= 3.0 * intensity * max(1.0 - endTaper, 0.0);
    color.rgb *= coreGlow * coreFade * 2.0;

    fragColor = color * ColorModulator * linear_fog_fade(vertexDistance, FogStart, FogEnd);
}
