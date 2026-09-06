#version 150

uniform sampler2D SkySampler;
uniform float VeilRenderTime;

in vec4 texProj0;
in vec3 vColor;

out vec4 fragColor;

// --- Noise Functions ---

vec4 perm(vec4 x) {
    x = ((x * 34.0) + 1.0) * x;
    return x - floor(x * (1.0 / 289.0)) * 289.0;
}

float noise(vec3 p)
{
    vec3 a = floor(p);
    vec3 d = p - a;
    d = d * d * (3.0 - 2.0 * d);

    vec4 b = a.xxyy + vec4(0.0, 1.0, 0.0, 1.0);
    vec4 k1 = perm(b.xyxy);
    vec4 k2 = perm(k1.xyxy + b.zzww);

    vec4 c = k2 + a.zzzz;
    vec4 k3 = perm(c);
    vec4 k4 = perm(c + 1.0);

    vec4 o1 = fract(k3 * 0.02439024);
    vec4 o2 = fract(k4 * 0.02439024);

    vec4 o3 = o2 * d.z + o1 * (1.0 - d.z);
    vec2 o4 = o3.yw * d.x + o3.xz * (1.0 - d.x);

    return o4.y * d.y + o4.x * (1.0 - d.y);
}

vec4 getColor(vec2 uv, vec3 col, float seed, float timeOffset)
{
    vec3 p = vec3(uv, VeilRenderTime * 1.5 + seed);

    uv += noise(p * 0.7) * 0.35 - noise(-p * 1.2) * 0.2;

    float angle = length(uv) * 2.0 + VeilRenderTime;
    float c = cos(angle);
    float s = sin(angle);
    mat2 rot = mat2(c, -s, s, c);
    uv = rot * uv;

    float l = length(uv);

    vec3 n = normalize(vec3(uv, -1.0));
    vec3 view_dir = vec3(0.0, 0.0, 1.0);
    const vec3 light_dir = vec3(0.6666, 0.6666, -0.3333);

    float light = 0.5 * pow(max(0.0, dot(n, normalize(light_dir + view_dir))), 32.0);

    float min_res = 1000.0;
    float m = smoothstep(1.0, 1.0 - 3.0 / min_res, l);

    vec3 color = pow(max(0.0, 2.0 - sqrt(l)), 4.0) * col;

    vec3 shift = vec3(sin(VeilRenderTime + timeOffset), cos(VeilRenderTime * 0.7 + timeOffset), sin(VeilRenderTime * 0.5)) * 0.3;
    col = clamp(col + shift, 0.0, 1.0);

    vec3 nc = abs(n + vec3(0, 0, 0.3));
    col = normalize(col + pow(nc, vec3(7.0)) * vec3(7)) + col * 0.35;

    color = mix(color, col, m) + light;
    return vec4(color, m);
}

void main() {
    vec2 screenUV = texProj0.xy / texProj0.w;
    vec2 uv = (screenUV - 0.5) * 2.0;

    float pulse = 1.8 + 0.4 * sin(VeilRenderTime * 3.0);
    uv *= pulse;

    vec4 skyColor = texture(SkySampler, screenUV);

    vec4 col1 = getColor(uv * 0.98, vec3(1.0, 0.37, 1.0), 0.0, 0.0);
    vec4 col2 = getColor(uv, vec3(0.1, 0.55, 1.0), -612.734, 1.0);

    vec3 plasma = mix(col1.rgb, col2.rgb, clamp(0.5 + 0.5 * (col2.w - col1.w), 0.0, 1.0));

    float mask = clamp(0.5 + 0.5 * (col2.w - col1.w), 0.0, 1.0);

    vec3 finalColor = mix(skyColor.rgb, plasma, mask * 0.5 + 0.2);

//    float scanline = sin(screenUV.y * 800.0) * 0.04;
//    finalColor -= scanline;

    fragColor = vec4(finalColor, 1.0);
}