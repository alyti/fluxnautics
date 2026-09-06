#version 410 core
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec4 Color;

out float vertexDistance;
out vec2 lengthData;
out vec4 vertexColor;
out vec3 modelPos;

void main() {
    vec4 viewPos = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPos;
    vertexDistance = length(viewPos.xyz);
    lengthData = UV0;
    vertexColor = Color;
    modelPos = Position;
}
