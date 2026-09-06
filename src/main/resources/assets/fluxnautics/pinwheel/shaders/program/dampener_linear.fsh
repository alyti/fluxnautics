#include fluxnautics:util
#include fluxnautics:noise

uniform float VeilRenderTime;

in vec2 vUV;
in vec4 vColor;
in vec3 vNormal;
in vec3 vViewPos;

out vec4 fragColor;

void main() {
    float t = mod(VeilRenderTime * 20.0, 1000.0); // ticks, plume loop period

    float fres = pfresnel(vViewPos, vNormal, 2.2);

    float flow = plumeFbm(vec2(vUV.x * 4.0 + t * 0.5, vUV.y * 2.0 + vUV.x * 1.7), vec2(500.0, 2000.0));
    flow = smoothstep(0.30, 1.0, flow);

    float breath = 0.85 + 0.15 * sin(VeilRenderTime * 2.3);

    float glow = (fres * 1.1 + 0.07 + flow * 0.5) * breath;
    fragColor = vec4(vColor.rgb * glow, vColor.a);
}
