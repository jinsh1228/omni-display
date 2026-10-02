import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
public class RuntimeHooksTest {
 static ClassNode read(String name)throws Exception{var node=new ClassNode();try(var in=RuntimeHooksTest.class.getClassLoader().getResourceAsStream(name+".class")){if(in==null)throw new AssertionError(name);new ClassReader(in).accept(node,0);}return node;}
 public static void main(String[] args)throws Exception{
  var particle=read("net/minecraft/client/particle/Particle");for(String field:new String[]{"x","y","z","alpha"})if(particle.fields.stream().noneMatch(f->f.name.equals(field)&&(f.desc.equals(field.equals("alpha")?"F":"D"))))throw new AssertionError(field);
  var engine=read("net/minecraft/client/particle/ParticleEngine");String descriptor="(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V";
  int count=0;for(var m:engine.methods)if(m.name.equals("render")&&m.desc.equals(descriptor))for(var ins:m.instructions)if(ins instanceof MethodInsnNode call&&call.owner.equals("net/minecraft/client/particle/Particle")&&call.name.equals("render")&&call.desc.equals("(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/Camera;F)V"))count++;
  if(count!=1)throw new AssertionError("particle render target count "+count);
  var mixin=read("dev/omnidisplay/mixin/ParticleVisibilityMixin");if(mixin.methods.stream().noneMatch(m->m.name.equals("omni$render")))throw new AssertionError("missing hook");
  int priority=1000;for(var annotation:mixin.invisibleAnnotations)if(annotation.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;"))for(int i=0;i<annotation.values.size();i+=2)if(annotation.values.get(i).equals("priority"))priority=(Integer)annotation.values.get(i+1);
  if(priority<=500)throw new AssertionError("Cannot inject into AsyncParticles overwrite at priority 500");
  var source=read("dev/omnidisplay/mixin/ParticleSourceMixin");if(source.methods.stream().anyMatch(m->m.name.equals("omni$beforeTransform")))throw new AssertionError("Do not inject into Sable-owned merged conversion method");
  var mod=read("dev/omnidisplay/OmniDisplay");if(mod.methods.stream().noneMatch(m->m.name.equals("<init>")&&m.desc.equals("(Lnet/neoforged/bus/api/IEventBus;Lnet/neoforged/fml/ModContainer;)V")))throw new AssertionError("mod constructor");
  System.out.println("PASS: installed particle render hook descriptor, ownership/alpha fields, server config registration constructor");
 }
}
