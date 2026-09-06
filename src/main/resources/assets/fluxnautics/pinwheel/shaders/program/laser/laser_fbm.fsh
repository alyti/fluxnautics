#include veil:fog
#include fluxnautics:util
#include fluxnautics:noise

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;

uniform float time;

uniform float uStreakSpeed;
uniform float uStreakFine;

uniform float uHueSpan;
uniform float uHueWave;
uniform float uHueWaveLen;
uniform float uHueWaveSpeed;

uniform float uPlumeSpan;
uniform float uPlumePower;
uniform float uPlumeLength;
uniform float uLayerFade;
uniform float uPlumeSeed;

in float vertexDistance;
in vec2 lengthData;
in vec4 vertexColor;
in vec2 vLateral;

out vec4 fragColor;

// #veil:normal
const vec3 normal = vec3(0.0, 0.0, -1.0);

void main() {

    // #veil:albedo
    float endTaper = max(lengthData.y - 1.0, 0.0);
    float lengthPos = min(lengthData.y, 1.0);
    float radialDist = 0.1;
    float coreGlow = pow(1.0 - radialDist, 2.0);
    float halo = exp(-radialDist * 3.0);

    float plumeLen = uPlumeLength > 0.0 ? uPlumeLength : uPlumePower * max(uPlumeSpan, 1.0);
    plumeLen = max(plumeLen, 0.5);
    float fade = uLayerFade > 0.0 ? min(uLayerFade, 1.0) : 1.0;

    float powerN = clamp(uPlumePower, 0.0, 4.0);
    float broad = 1.0 / (1.0 + 0.16 * max(uPlumeSpan - 1.0, 0.0));

    float longN = clamp((plumeLen - 12.0) / 24.0, 0.0, 1.0);
    float coreExp = mix(0.5, 0.28, clamp((plumeLen - 8.0) / 32.0, 0.0, 1.0));
    float coreFade = pow(1.0 - lengthPos, coreExp);
    float tipZone = clamp(2.5 / max(plumeLen, 1.0), 0.03, 0.30);
    float tipFade = 1.0 - longN * smoothstep(1.0 - tipZone, 1.0, lengthPos);

    float axialScale = max(0.25 * plumeLen, 1.1);
    float spd = uStreakSpeed > 0.0 ? uStreakSpeed : 1.2;
    float tWrap = mod(time, 1000.0);
    float lateralScale = 1.1 * broad;
    float streak = plumeFbm3(vec3(vLateral.x * lateralScale + uPlumeSeed * 64.0,
    vLateral.y * lateralScale + uPlumeSeed * 61.0,
    lengthPos * axialScale - tWrap * spd),
    vec3(64.0, 64.0, spd * 1000.0));

    float fineW = (uStreakFine > 0.0 ? uStreakFine : 0.5) * (1.0 + 0.4 * powerN);
    float lines = plumeFbm3(vec3(vLateral.x * 1.6 * broad + uPlumeSeed * 47.0,
    vLateral.y * 1.6 * broad + uPlumeSeed * 53.0,
    lengthPos * axialScale * 1.8 - tWrap * spd * 2.0),
    vec3(64.0, 64.0, spd * 2000.0));

    float turb = streak * 0.75 + lines * fineW;
    turb = turb * turb * (1.3 - 0.5 * lengthPos);

    float intensity = (coreFade * (coreGlow * 1.2 + halo * 0.6) + turb * 1.2 * coreFade) * (1.0 + 0.55 * powerN) * fade;

    vec4 color = vertexColor;
    if (vertexColor.a > 0.99) {
        float rt = mod(time, 1000.0);
        float span    = uHueSpan      > 0.0 ? uHueSpan      : 1.0;
        float waveAmp = uHueWave      > 0.0 ? uHueWave      : 0.3;
        float waveLen = uHueWaveLen   > 0.0 ? uHueWaveLen   : 1.0;
        float waveSpd = uHueWaveSpeed > 0.0 ? uHueWaveSpeed : 0.4;
        float hue = vertexColor.r
        + lengthPos * span
        + lengthPos * waveAmp * sin(6.2831853 * (lengthPos * waveLen - rt * waveSpd));
        float rainbowDim = vertexColor.b > 0.001 ? vertexColor.b : 1.0;
        color = vec4(oklabRainbow(hue, vertexColor.g) * rainbowDim, 10.0 / 60.0);
    }

    float alphaShape = max(1.0 - endTaper, 0.0) * tipFade;
    color.a *= 3.0 * intensity * alphaShape;
    color.rgb *= coreGlow * coreFade * 2.0;

    fragColor = color * ColorModulator * linear_fog_fade(vertexDistance, FogStart, FogEnd);
}
