#version 450

layout(location = 0) in vec4 position;

uniform mat4 mvp;

out vec2 texCoord0;

void main()
{
    texCoord0 = position.xy;
    gl_Position = mvp * position;
}