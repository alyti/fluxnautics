#include veil:fog
#include fluxnautics:util
#include fluxnautics:noise

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;

uniform float time;

uniform float uTrailLen;
uniform float uTrailSeed;
uniform float uTrailRainbow;

uniform float uTrailSpan;
uniform float uTrailGamma;
uniform float uTrailFade;
uniform float uTrailNoise;
uniform float uTrailScale;
uniform float uTrailSpeed;
uniform float uTrailBrightness;

in float vertexDistance;
in vec2 lengthData;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    float t = clamp(lengthData.y, 0.0, 1.0);
    float lateral = clamp(lengthData.x, -1.0, 1.0);
    float rt = mod(time, 1000.0);

    float edge = pow(1.0 - abs(lateral), uTrailGamma > 0.0 ? uTrailGamma : 0.5);
    float fade = pow(1.0 - t, uTrailFade > 0.0 ? uTrailFade : 1.4);

    float nAmt = uTrailNoise > 0.0 ? uTrailNoise : 0.45;
    float scale = uTrailScale > 0.0 ? uTrailScale : 0.9;
    float spd = uTrailSpeed > 0.0 ? uTrailSpeed : 0.35;
    float axial = t * max(uTrailLen, 1.0) * scale;
    float wisp = 1.0;
    if (nAmt > 0.001) {
        float n = plumeFbm3(vec3(lateral * 2.0 + uTrailSeed * 13.0,
        axial - rt * spd,
        uTrailSeed * 71.0),
        vec3(16.0, spd * 1000.0, 1024.0));
        wisp = 1.0 - nAmt + nAmt * n;
    }

    float gain = uTrailBrightness > 0.0 ? uTrailBrightness : 1.0;
    float a = vertexColor.a * edge * fade * wisp * gain;
    if (a < 0.004) {
        discard;
    }

    vec3 tint;
    if (uTrailRainbow > 0.5) {
        float hue = vertexColor.r + t * uTrailSpan;
        tint = oklabRainbow(hue, vertexColor.g);
    } else {
        tint = vertexColor.rgb;
    }

    fragColor = vec4(tint, a) * ColorModulator * linear_fog_fade(vertexDistance, FogStart, FogEnd);
}
