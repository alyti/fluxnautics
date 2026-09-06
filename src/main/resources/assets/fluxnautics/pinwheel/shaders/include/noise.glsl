float plumeHash(vec2 cell) {
    ivec2 c = ivec2(cell);
    uint h = uint(c.x) * 374761393u + uint(c.y) * 668265263u;
    h = (h ^ (h >> 13u)) * 1274126177u;
    h ^= h >> 16u;
    return float(h & 0x00FFFFFFu) / 16777216.0;
}

float plumeNoise(vec2 p, vec2 period) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f); // smoothstep fade
    vec2 c00 = mod(i, period);
    vec2 c10 = mod(i + vec2(1.0, 0.0), period);
    vec2 c01 = mod(i + vec2(0.0, 1.0), period);
    vec2 c11 = mod(i + vec2(1.0, 1.0), period);
    float a = plumeHash(c00);
    float b = plumeHash(c10);
    float c = plumeHash(c01);
    float d = plumeHash(c11);
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float plumeFbm(vec2 p, vec2 period) {
    float v = 0.0;
    float amp = 0.5;
    float norm = 0.0;
    vec2 freq = vec2(1.0);
    for (int k = 0; k < 4; k++) {
        v += amp * plumeNoise(p * freq + float(k) * 17.31, period * freq);
        norm += amp;
        amp *= 0.55;
        freq *= 2.0;
    }
    return v / norm;
}

float plumeHash3(vec3 cell) {
    ivec3 c = ivec3(cell);
    uint h = uint(c.x) * 374761393u + uint(c.y) * 668265263u + uint(c.z) * 1442695041u;
    h = (h ^ (h >> 13u)) * 1274126177u;
    h ^= h >> 16u;
    return float(h & 0x00FFFFFFu) / 16777216.0; // [0, 1)
}

float plumeNoise3(vec3 p, vec3 period) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f); // smoothstep fade
    vec3 c = mod(i, period);
    vec3 c1 = mod(i + 1.0, period);
    float n000 = plumeHash3(c);
    float n100 = plumeHash3(vec3(c1.x, c.y, c.z));
    float n010 = plumeHash3(vec3(c.x, c1.y, c.z));
    float n110 = plumeHash3(vec3(c1.x, c1.y, c.z));
    float n001 = plumeHash3(vec3(c.x, c.y, c1.z));
    float n101 = plumeHash3(vec3(c1.x, c.y, c1.z));
    float n011 = plumeHash3(vec3(c.x, c1.y, c1.z));
    float n111 = plumeHash3(vec3(c1.x, c1.y, c1.z));
    return mix(mix(mix(n000, n100, f.x), mix(n010, n110, f.x), f.y),
    mix(mix(n001, n101, f.x), mix(n011, n111, f.x), f.y), f.z);
}

float plumeFbm3(vec3 p, vec3 period) {
    float v = 0.0;
    float amp = 0.5;
    float norm = 0.0;
    vec3 freq = vec3(1.0);
    for (int k = 0; k < 3; k++) {
        v += amp * plumeNoise3(p * freq + float(k) * 17.31, period * freq);
        norm += amp;
        amp *= 0.55;
        freq *= 2.0;
    }
    return v / norm;
}
