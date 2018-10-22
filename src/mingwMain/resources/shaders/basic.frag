#version 450

layout(location = 0) uniform sampler2D tex;

in vec2 texCoord0;

void main() {
	gl_FragColor = texture(tex, texCoord0) ; //vec4(texCoord0.x,texCoord0.y,sin(texCoord0.x*4),1);
}
