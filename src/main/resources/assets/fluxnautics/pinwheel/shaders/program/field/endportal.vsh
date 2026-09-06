vec4 projection_from_position(vec4 position) {
    vec4 projection = position * 0.5;
    projection.xy = vec2(projection.x + projection.w, projection.y + projection.w);
    projection.zw = position.zw;
    return projection;
}
layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;

//in vec3 Position;
//in vec3 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 texProj69;
out vec3 vColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    texProj69 = projection_from_position(gl_Position);
    vColor = Color.rgb;
}