#include fluxnautics:util
#include fluxnautics:noise

uniform float VeilRenderTime;

in vec2 vUV;
in vec4 vColor;
in vec3 vNormal;
in vec3 vViewPos;

out vec4 fragColor;

void main() {
    float t = mod(VeilRenderTime * 20.0, 1000.0);
    float fres = pfresnel(vViewPos, vNormal, 2.0);
    float uvMask = step(0.001, vUV.x + vUV.y);

    float bands = plumeFbm(vec2(vUV.x * 3.0, vUV.y * 5.0 - t * 0.15), vec2(4096.0, 150.0));
    bands = smoothstep(0.35, 1.0, bands) * uvMask;

    float fade = smoothstep(0.0, 0.18, vUV.y) * smoothstep(1.0, 0.82, vUV.y);
    float breath = 0.85 + 0.15 * sin(VeilRenderTime * 1.7);

    float glow = (fres * 1.05 + 0.10 + bands * 0.85 * fade) * breath;
    fragColor = vec4(vColor.rgb * glow, vColor.a);
}