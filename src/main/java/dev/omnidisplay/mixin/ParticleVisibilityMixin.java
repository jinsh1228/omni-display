package dev.omnidisplay.mixin;
import dev.omnidisplay.client.ParticleVisibility;
import net.minecraft.client.particle.*;
import net.minecraft.client.Camera;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(value=ParticleEngine.class,remap=false,priority=1000)
abstract class ParticleVisibilityMixin {
 @Redirect(method="render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/particle/Particle;render(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/Camera;F)V"))
 private void omni$render(Particle p,VertexConsumer vertices,Camera camera,float dt){
  float factor=ParticleVisibility.factor(p);if(factor<=0)return;
  if(factor>=1){p.render(vertices,camera,dt);return;}
  var access=(ParticleAlphaAccessor)p;float old=access.omni$alpha();access.omni$alpha(old*factor);
  try{p.render(vertices,camera,dt);}finally{access.omni$alpha(old);}
 }
}
