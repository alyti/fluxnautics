#version 410 core

uniform mat4 uTrailModelView;
uniform mat4 uTrailProjMat;

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec4 Color;

out float vertexDistance;
out vec2 lengthData;
out vec4 vertexColor;

void main() {
    vec4 viewPos = uTrailModelView * vec4(Position, 1.0);
    gl_Position = uTrailProjMat * viewPos;
    vertexDistance = length(viewPos.xyz);
    lengthData = UV0;
    vertexColor = Color;
}
