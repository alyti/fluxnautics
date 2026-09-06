#version 150

in vec3 Position;
in vec4 Color;

uniform mat4 ProjMat;
uniform mat4 ModelViewMat;

out vec2 texCoord;
out vec3 vColor;

void main() {
    texCoord = Position.xy * 0.5 + 0.5;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vColor = Color.rgb;
}