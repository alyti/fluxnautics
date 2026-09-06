#include fluxnautics:util
#include fluxnautics:noise

uniform float VeilRenderTime;

in vec2 vUV;
in vec4 vColor;
in vec3 vNormal;
in vec3 vViewPos;

out vec4 fragColor;

void main() {
    float fres = pfresnel(vViewPos, vNormal, 3.0);
    float frost = plumeFbm(vUV * 6.0 + 11.0, vec2(4096.0));
    frost = smoothstep(0.30, 0.85, frost);

    float timeIdx = floor(VeilRenderTime * 1.5);
    vec2 cell = floor(vUV * 9.0);
    float twinkle = step(0.985, plumeHash(cell + timeIdx * vec2(17.0, 43.0)))
    * step(0.5, vColor.a);

    float breath = 0.82 + 0.18 * sin(VeilRenderTime * 1.9);
    float glow = (fres * 1.25 + frost * 0.40 + twinkle * 1.4 + (1.0 - fres) * 0.08) * breath;
    fragColor = vec4(vColor.rgb * glow, vColor.a);
}