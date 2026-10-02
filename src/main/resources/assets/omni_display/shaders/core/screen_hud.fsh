#version 150
uniform sampler2D MaskSampler;
uniform vec2 MaskSize;
uniform vec4 ColorModulator;
in vec4 vertexColor;
out vec4 fragColor;
void main(){
 vec4 surface=texture(MaskSampler,gl_FragCoord.xy/MaskSize);
 if(surface.r*surface.a<0.5)discard;
 fragColor=vertexColor*ColorModulator;
}
