#version 410 core

uniform float uTessDensity;
uniform float uPlumeSpan;   

in float vertexDistance[];
in vec2 lengthData[];
in vec4 vertexColor[];
in vec3 modelPos[];

out float patchVertexDistance[];
out vec2 patchLengthData[];
out vec4 patchVertexColor[];
out vec3 patchModelPos[];

layout(vertices = 4) out;

void main() {
    gl_out[gl_InvocationID].gl_Position = gl_in[gl_InvocationID].gl_Position;
    patchVertexDistance[gl_InvocationID] = vertexDistance[gl_InvocationID];
    patchLengthData[gl_InvocationID] = lengthData[gl_InvocationID];
    patchVertexColor[gl_InvocationID] = vertexColor[gl_InvocationID];
    patchModelPos[gl_InvocationID] = modelPos[gl_InvocationID];

    if (gl_InvocationID == 0) {
        float density = uTessDensity > 0.0 ? uTessDensity : 12.0;
        float dist = 0.25 * (vertexDistance[0] + vertexDistance[1]
        + vertexDistance[2] + vertexDistance[3]);
        float lod = density / max(dist, 2.0);
        lod /= 1.0 + 0.10 * max(uPlumeSpan - 1.0, 0.0);

        float eNear = distance(modelPos[0], modelPos[1]); 
        float eFar  = distance(modelPos[3], modelPos[2]); 
        float eL    = distance(modelPos[0], modelPos[3]); 
        float eR    = distance(modelPos[1], modelPos[2]); 

        gl_TessLevelOuter[0] = clamp(eL * lod, 1.0, 32.0);
        gl_TessLevelOuter[1] = clamp(eNear * lod, 1.0, 16.0);
        gl_TessLevelOuter[2] = clamp(eR * lod, 1.0, 32.0);
        gl_TessLevelOuter[3] = clamp(eFar * lod, 1.0, 16.0);
        gl_TessLevelInner[0] = clamp(max(eNear, eFar) * lod, 1.0, 16.0);
        gl_TessLevelInner[1] = clamp(max(eL, eR) * lod, 1.0, 32.0);
    }
}
