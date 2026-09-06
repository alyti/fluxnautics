vec2 posterize(vec2 color, float colors) {
    return floor(color * colors) / colors;
}

vec3 posterize(vec3 color, float colors) {
    return floor(color * colors) / colors;
}

vec4 simple_circle(vec2 uv, float radius) {
    float dist = length(uv) - radius;
    float mask = step(dist, 0.0);
    return vec4(mask);
}

// slightly modified from https://github.com/riccardoscalco/glsl-simple-alpha-compositing/blob/master/index.glsl
vec4 alpha_composite(vec4 background, vec4 foreground) {
    return foreground * foreground.a + background * background.a * (1.0 - foreground.a);
}

float pfresnel(vec3 viewPos, vec3 normal, float power) {
    vec3 V = normalize(viewPos);
    vec3 N = normalize(normal);
    return pow(1.0 - abs(dot(-V, N)), power);
}

vec3 oklabRainbow(float hue01, float chroma) {
    float h = hue01 * 6.2831853;
    float a_ = chroma * cos(h);
    float b_ = chroma * sin(h);
    float l_ = 0.8 + 0.3963377774 * a_ + 0.2158037573 * b_;
    float m_ = 0.8 - 0.1055613458 * a_ - 0.0638541728 * b_;
    float s_ = 0.8 - 0.0894841775 * a_ - 1.2914855480 * b_;
    float l = l_ * l_ * l_;
    float m = m_ * m_ * m_;
    float s = s_ * s_ * s_;
    vec3 lin = vec3(
    +4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
    -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
    -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s);
    vec3 srgb = mix(lin * 12.92,
    1.055 * pow(max(lin, vec3(0.0)), vec3(1.0 / 2.4)) - 0.055,
    step(vec3(0.0031308), lin));
    return clamp(srgb, 0.0, 1.0);
}