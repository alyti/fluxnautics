uniform float ShadowVolumeSize;
uniform float StartY;
uniform sampler2D SkySampler;
uniform sampler2D ShadowDepthSampler;
uniform sampler2D ShadowStrengthSampler;
uniform float VeilRenderTime;

in vec2 texCoord;
in vec2 shadowTexCoord;
in vec4 vertexColor;
in float planeY;

out vec4 fragColor;

void main() {
    vec4 color = texture(SkySampler, texCoord);
    float depth = texture(ShadowDepthSampler, shadowTexCoord).r;
    float shadowStrength = texture(ShadowStrengthSampler, shadowTexCoord).a * 10.0;
    float anim = -3.5 + (sin(VeilRenderTime * 10.0) * 8.0);
    float fragY = (StartY - ShadowVolumeSize) + (depth) * (512 + anim) + 0.5;

    if (depth < 1.0) {
        vec4 col = (vertexColor * color + vertexColor * 0.025);

        if (fragY < planeY -2) {
            col.a = 0.0;
        } else {
            float strength = (2.0 * depth) - (fragY - planeY) / 5.0;
            strength = clamp(strength, 0.0, 1.0);
            strength = pow(strength, 3.0);
            col.a = max(0.0, col.a * strength) * shadowStrength;
        }

        fragColor = col;
    } else {
        discard;
    }
}