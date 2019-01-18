#version 450

layout(location = 0) uniform sampler2D tex;

in vec2 texCoord0;

void main() {
	gl_FragColor = texture(tex, texCoord0);
}
