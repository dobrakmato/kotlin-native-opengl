#version 450

layout(location = 0) in vec4 position;
layout(location = 1) in vec2 texCoord;

uniform mat4 mvp;

out vec2 texCoord0;

void main()
{
    texCoord0 = texCoord;
    gl_Position = mvp * position;
}