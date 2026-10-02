#version 150
uniform sampler2D SceneDepth;
uniform sampler2D CombinedDepth;
uniform float DepthScale;
uniform vec2 DepthJitter;
uniform int HasLod;
uniform vec2 CombinedProjection;
uniform vec2 VanillaProjection;
in vec2 texCoord;
out vec4 fragColor;
void main(){
 vec2 halfPixel=0.5/vec2(textureSize(SceneDepth,0));
 vec2 sourceUv=clamp(texCoord*DepthScale+DepthJitter,halfPixel,vec2(DepthScale)-halfPixel);
 float depth=texture(SceneDepth,sourceUv).r;
 if(HasLod!=0&&depth>=1.0){
  float combined=texture(CombinedDepth,sourceUv).r;
  if(combined<1.0&&combined>0.0){
   float distance=CombinedProjection.y/(2.0*combined-1.0+CombinedProjection.x);
   depth=clamp((-VanillaProjection.x+VanillaProjection.y/distance)*0.5+0.5,0.0,1.0);
  }
 }
 fragColor=vec4(0.0);
 gl_FragDepth=depth;
}
