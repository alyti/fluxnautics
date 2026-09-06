#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 ScreenSize;

in vec2 texCoord;
in vec3 vColor;

out vec4 fragColor;

void main() {
    vec2 uv = texCoord;

    vec2 center = uv - 0.5;
    center.x *= ScreenSize.x / ScreenSize.y;
    float dist = length(center);

    if (dist < 0.63) {
        discard;
    }

    vec4 color = texture(DiffuseSampler, uv);
    if (dist > 0.5) {
        float aberration = (dist - 0.5) * 0.025;
        color.r = texture(DiffuseSampler, uv + vec2(aberration, 0.0)).r;
        color.b = texture(DiffuseSampler, uv - vec2(aberration, 0.0)).b;
    }

    float lum = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    color.rgb = mix(vec3(lum), color.rgb, 1.15);
    color.rgb *= 1.05;

    float vignette = smoothstep(0.9, 0.65, dist);
    color.rgb *= vignette;

    float scanline = sin(uv.y * ScreenSize.y * 1.5) * 0.5 + 0.5;
    color.rgb += vColor * scanline * 0.04;

    float edgeFringe = smoothstep(0.6, 0.9, dist);
    color.rgb += vColor * edgeFringe * 0.15;

    fragColor = color;
}