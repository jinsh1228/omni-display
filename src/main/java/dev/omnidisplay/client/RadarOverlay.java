package dev.omnidisplay.client;
import dev.omnidisplay.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Vector3f;
import java.util.UUID;
/** Only the network's selected target is rendered. No detected-target clutter. */
@EventBusSubscriber(modid=OmniDisplay.ID,value=Dist.CLIENT)
public final class RadarOverlay {
 private static boolean networkLinked;private static long acquired;
 private static UUID cabin;private static String targetId="";private static Vec3 target,previous;private static long received;
 private static net.minecraft.client.multiplayer.ClientLevel world;
 public static void receive(CabinPackets.Data data){
  var mc=Minecraft.getInstance();var t=data.tag();if(mc.level==null)return;var id=t.getString("id");
  var next=t.contains("id")?new Vec3(t.getDouble("x"),t.getDouble("y"),t.getDouble("z")):null;
  if(!id.equals(targetId)||world!=mc.level)acquired=mc.level.getGameTime();networkLinked=t.getBoolean("linked");
  previous=world==mc.level&&target!=null&&id.equals(targetId)?target:next;target=next;targetId=id;world=mc.level;received=world.getGameTime();cabin=t.getUUID("cabin");
 }
 @SubscribeEvent public static void input(InputEvent.InteractionKeyMappingTriggered e){
  var mc=Minecraft.getInstance();var s=DisplayRenderer.settings();
  if(!e.isUseItem()||mc.player==null||mc.screen!=null||mc.player.isShiftKeyDown()||!mc.player.getMainHandItem().isEmpty()||!RadarHooks.available()||!DisplayRenderer.viewActive()||s==null||!s.mouseLock||!s.power||s.radarLink==null)return;
  if(mc.level==null||!(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit)
    ||hit.getType()!=net.minecraft.world.phys.HitResult.Type.BLOCK
    ||!ScreenFaces.isScreen(mc.level,hit.getBlockPos()))return;
  var anchor=DisplayRenderer.anchor();if(anchor==null)return;
  e.setCanceled(true);e.setSwingHand(false);if(e.getHand()!=InteractionHand.MAIN_HAND)return;var camera=mc.gameRenderer.getMainCamera();
  PacketDistributor.sendToServer(new CabinPackets.Aim(anchor,hit.getBlockPos(),camera.getPosition(),new Vec3(camera.getLookVector())));
 }
 @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOW) public static void render(RenderLevelStageEvent e){
  if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;var mc=Minecraft.getInstance();var s=DisplayRenderer.settings();
  if(mc.level!=world||mc.screen!=null||!PartitionRenderer.hudVisible()||s==null||s.radarLink==null||!s.id.equals(cabin)||target==null||world.getGameTime()-received>20)return;
  float partial=e.getPartialTick().getGameTimeDeltaPartialTick(false);double now=world.getGameTime()+partial;
  if(s.hudBrightness==0)return;
  float alpha=(float)dev.omnidisplay.region.HudFade.amount(s.waveBase,s.waveTimes,s.waveTargets,now);if(alpha<.001)return;
  var point=previous==null?target:previous.lerp(target,Math.clamp((now-received)/4,0,1));var camera=e.getCamera();var eye=camera.getPosition();var offset=point.subtract(eye);
  if(offset.dot(new Vec3(camera.getLookVector()))<=0)return;
  var right=new Vector3f(1,0,0).rotate(camera.rotation());var up=new Vector3f(0,1,0).rotate(camera.rotation());double radius=Math.max(.2,offset.length()*.018),thickness=radius*.09;
  try(var memory=new ByteBufferBuilder(16384)){
   var b=new BufferBuilder(memory,VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
   for(var line:dev.omnidisplay.region.HudDesign.target(now,now-acquired)){
    var a=new Vector3f(right).mul((float)(line.x()*radius)).fma((float)(line.y()*radius),up);
    var c=new Vector3f(right).mul((float)(line.u()*radius)).fma((float)(line.v()*radius),up);
    double dx=line.u()-line.x(),dy=line.v()-line.y(),len=Math.hypot(dx,dy);if(len<1e-8)continue;
    var side=new Vector3f(right).mul((float)(-dy/len*radius*.022*line.weight())).fma((float)(dx/len*radius*.022*line.weight()),up);
    float coverage=alpha*dev.omnidisplay.region.HudLight.alpha(line.alpha(),s.hudBrightness)/255f;
    outline(b,offset,a,c,side,dev.omnidisplay.region.HudLight.color(line.color(),s.hudBrightness),coverage);
   }
   var mesh=b.build();if(mesh==null)return;var color=RenderSystem.getShaderColor().clone();
   boolean depth=org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_DEPTH_TEST),blend=org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND),cull=org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_CULL_FACE),write=org.lwjgl.opengl.GL11.glGetBoolean(org.lwjgl.opengl.GL11.GL_DEPTH_WRITEMASK);
   try(var gpu=new VertexBuffer(VertexBuffer.Usage.DYNAMIC)){
    gpu.bind();gpu.upload(mesh);RenderSystem.setShaderColor(1,1,1,1);RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableCull();
    gpu.drawWithShader(e.getModelViewMatrix(),e.getProjectionMatrix(),PartitionRenderer.hudShader());
   }finally{VertexBuffer.unbind();RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]);if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();if(!blend)RenderSystem.disableBlend();if(cull)RenderSystem.enableCull();RenderSystem.depthMask(write);}
  }
 }
 public static boolean linked(){var mc=Minecraft.getInstance();var s=DisplayRenderer.settings();return mc.level!=null&&mc.level==world&&s!=null&&s.id.equals(cabin)&&world.getGameTime()-received<=20&&networkLinked;}
 public static boolean locked(){return linked()&&target!=null;}
 private static void outline(BufferBuilder b,Vec3 offset,Vector3f a,Vector3f c,Vector3f side,int color,float alpha){
  mark(b,offset,new Vector3f(a).sub(side),color,alpha);mark(b,offset,new Vector3f(c).sub(side),color,alpha);mark(b,offset,new Vector3f(c).add(side),color,alpha);mark(b,offset,new Vector3f(a).add(side),color,alpha);
 }
 private static void mark(BufferBuilder b,Vec3 p,Vector3f v,int color,float alpha){b.addVertex((float)p.x+v.x,(float)p.y+v.y,(float)p.z+v.z).setColor((color>>16)&255,(color>>8)&255,color&255,Math.round(alpha*255));}
}


