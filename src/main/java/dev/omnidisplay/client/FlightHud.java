package dev.omnidisplay.client;

import dev.omnidisplay.OmniDisplay;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import org.joml.Vector3d;
import org.lwjgl.glfw.GLFW;

/** Cockpit-centered spherical vector instruments. */
@EventBusSubscriber(modid=OmniDisplay.ID,value=Dist.CLIENT)
public final class FlightHud {
 private static final KeyMapping TOGGLE=new KeyMapping("key.omni_display.hud",GLFW.GLFW_KEY_J,"key.categories.omni_display");
 private static final KeyMapping ALIGN=new KeyMapping("key.omni_display.align",GLFW.GLFW_KEY_K,"key.categories.omni_display");
 private static boolean enabled=true;
 private static int brightness=150;private static double stroke=1;private static int tint=0x78FFFF;private static final Vector3d cameraOffset=new Vector3d();
 private static ClientSubLevel last;
 private static Vector3d lastCenter;private static Object telemetrySelection;private static long lastTime=Long.MIN_VALUE;private static double speed;
 private static org.joml.Quaterniond geometryRotation;
 private static final Vector3d forward=new Vector3d(0,0,-1);
 @SubscribeEvent public static void tick(ClientTickEvent.Post e){
  var mc=Minecraft.getInstance();var owner=DisplayRenderer.owner();
  if(owner!=last){last=owner;forward.set(0,0,-1);boundsSelection=null;bounds=null;}
  var settings=DisplayRenderer.settings();if(settings!=null){double h=Math.toRadians(settings.heading);forward.set(Math.sin(h),0,-Math.cos(h));}
  if(owner!=null&&mc.level!=null){var box=DisplayRenderer.hudBounds();if(box!=null){
   var center=box.getCenter();var position=DisplayRenderer.hudPose(1,true).transformPosition(new Vector3d(center.x,center.y,center.z));long time=mc.level.getGameTime();
   if(telemetrySelection!=DisplayRenderer.selection()){lastCenter=null;telemetrySelection=DisplayRenderer.selection();speed=0;}
   if(lastCenter!=null&&time>lastTime){double sample=position.distance(lastCenter)*20/(time-lastTime);speed=Double.isFinite(sample)?sample:0;}
   lastCenter=new Vector3d(position);lastTime=time;
  }}else{lastCenter=null;telemetrySelection=null;speed=0;}
  while(TOGGLE.consumeClick())if(mc.screen==null){enabled=!enabled;notice(enabled?"飞行 HUD 已开启":"飞行 HUD 已关闭");}
  while(ALIGN.consumeClick())if(mc.screen==null&&owner!=null){
   var look=mc.gameRenderer.getMainCamera().getLookVector();
   var local=owner.renderPose().orientation().conjugate(new org.joml.Quaterniond()).transform(new Vector3d(look));
   local.y=0;
   if(local.lengthSquared()<.01){notice("请沿座舱前方平视后再校准。");continue;}
   if(settings!=null&&settings.animating(mc.level.getGameTime())){notice("请等待开关机动画结束后再校准。");continue;}
   forward.set(local.normalize());DisplayRenderer.align((int)Math.round(Math.toDegrees(Math.atan2(forward.x,-forward.z))));notice("已提交共享机头方向校准。");
  }

 }
 private static void notice(String s){var p=Minecraft.getInstance().player;if(p!=null)p.displayClientMessage(Component.literal(s),true);}
 @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOW) public static void draw(RenderLevelStageEvent e){
  long omni$perf=dev.omnidisplay.diagnostics.PerformanceDiagnostics.begin(dev.omnidisplay.diagnostics.PerformanceDiagnostics.Phase.HUD);try{

  if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
  var mc=Minecraft.getInstance();
  if(!enabled||mc.screen!=null||!PartitionRenderer.hudVisible())return;
  var owner=DisplayRenderer.owner();if(owner==null)return;
  var settings=DisplayRenderer.settings();if(settings==null)return;brightness=settings.hudBrightness;if(brightness==0)return;
  double now=mc.level.getGameTime()+e.getPartialTick().getGameTimeDeltaPartialTick(false);
  float hudAlpha=(float)dev.omnidisplay.region.HudFade.amount(settings.waveBase,settings.waveTimes,settings.waveTargets,now);
  if(hudAlpha<=.001f)return;
  if(boundsSelection!=DisplayRenderer.selection()||boundsRevision!=settings.revision){
   boundsSelection=DisplayRenderer.selection();boundsRevision=settings.revision;bounds=DisplayRenderer.hudBounds();
  }
  if(bounds==null)return;
  // Compute in double precision before camera subtraction: plot coordinates can be huge.
  var center=bounds.getCenter();
  var world=DisplayRenderer.hudPose(e.getPartialTick().getGameTimeDeltaPartialTick(false),false)
    .transformPosition(new Vector3d(center.x,center.y,center.z));
  double altitude=world.y;
  var cam=e.getCamera().getPosition();world.sub(cam.x,cam.y,cam.z);
  double radius=(.5*Math.min(bounds.getXsize(),Math.min(bounds.getYsize(),bounds.getZsize()))+.45)*(settings.hudScale/100d);
  cameraOffset.set(world);
  var view=new org.joml.Matrix4f(e.getModelViewMatrix()).translate((float)world.x,(float)world.y,(float)world.z);
  try(var memory=new com.mojang.blaze3d.vertex.ByteBufferBuilder(65536)){
   var b=new com.mojang.blaze3d.vertex.BufferBuilder(memory,
     com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
     com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR);
   geometryRotation=null;stroke=.7;tint=dev.omnidisplay.region.HudDesign.CYAN;
   // Sparse world-stabilised horizon, distinct from the cockpit-fixed front console.
   if((settings.hudMask&4)!=0)for(int h=0;h<360;h+=4)segment(b,radius,h,0,h+2.8,0,155);
   if((settings.hudMask&2)!=0)for(int pitch=-60;pitch<=60;pitch+=10){if(pitch==0)continue;
    for(int h=0;h<360;h+=90){for(int sign:new int[]{-1,1}){segment(b,radius,h+sign*4,pitch,h+sign*9,pitch,115);segment(b,radius,h+sign*9,pitch,h+sign*9,pitch+(pitch>0?-1:1),115);}label(b,radius,Integer.toString(pitch),h,pitch+1,135);}
   }
   if((settings.hudMask&1)!=0){tint=dev.omnidisplay.region.HudDesign.ICE;for(int h=0;h<360;h+=10){segment(b,radius,h,-.6,h,.6,150);if(h%30==0)label(b,radius,Integer.toString(h),h,3,160);}}
   geometryRotation=new org.joml.Quaterniond(owner.renderPose().orientation());
   var direction=geometryRotation.transform(new Vector3d(forward));double heading=Math.toDegrees(Math.atan2(direction.x,-direction.z));
   for(var line:dev.omnidisplay.region.HudDesign.front(now,speed,altitude,heading,settings.hudMask,RadarOverlay.linked(),settings.mouseLock,RadarOverlay.locked())){
    tint=line.color();stroke=line.weight();segment(b,radius,settings.heading+line.x(),line.y(),settings.heading+line.u(),line.v(),line.alpha());
   }
   geometryRotation=null;stroke=1;
   var mesh=b.build();if(mesh==null)return;
   var old=com.mojang.blaze3d.systems.RenderSystem.getShaderColor().clone();
   boolean depth=org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
   boolean cull=org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_CULL_FACE);
   boolean blend=org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);
   boolean write=org.lwjgl.opengl.GL11.glGetBoolean(org.lwjgl.opengl.GL11.GL_DEPTH_WRITEMASK);
   try(var gpu=new com.mojang.blaze3d.vertex.VertexBuffer(com.mojang.blaze3d.vertex.VertexBuffer.Usage.DYNAMIC)){
    gpu.bind();gpu.upload(mesh);
    com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,hudAlpha);
    com.mojang.blaze3d.systems.RenderSystem.disableCull();com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
    if(PartitionRenderer.hudUsesMask())com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();else com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
    com.mojang.blaze3d.systems.RenderSystem.depthMask(false);
    gpu.drawWithShader(view,e.getProjectionMatrix(),PartitionRenderer.hudShader());
   }finally{
    com.mojang.blaze3d.vertex.VertexBuffer.unbind();
    com.mojang.blaze3d.systems.RenderSystem.setShaderColor(old[0],old[1],old[2],old[3]);
    com.mojang.blaze3d.systems.RenderSystem.depthMask(write);
    if(cull)com.mojang.blaze3d.systems.RenderSystem.enableCull();
    if(depth)com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();else com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
    if(!blend)com.mojang.blaze3d.systems.RenderSystem.disableBlend();
   }
  }
 
  }finally{dev.omnidisplay.diagnostics.PerformanceDiagnostics.end(dev.omnidisplay.diagnostics.PerformanceDiagnostics.Phase.HUD,omni$perf);}
 }
 private static Object boundsSelection;private static long boundsRevision;
 private static net.minecraft.world.phys.AABB bounds;
 private static Vector3d point(double r,double h,double p){h=Math.toRadians(h);p=Math.toRadians(p);var v=new Vector3d(r*Math.cos(p)*Math.sin(h),r*Math.sin(p),-r*Math.cos(p)*Math.cos(h));return geometryRotation==null?v:geometryRotation.transform(v);}
 private static void vertex(com.mojang.blaze3d.vertex.BufferBuilder b,Vector3d p,int a){b.addVertex((float)p.x,(float)p.y,(float)p.z).setColor((tint>>16)&255,(tint>>8)&255,tint&255,a);}
 private static void segment(com.mojang.blaze3d.vertex.BufferBuilder b,double r,double h0,double p0,double h1,double p1,int alpha){
  var a=point(r,h0,p0);var c=point(r,h1,p1);var side=new Vector3d(c).sub(a).cross(new Vector3d(a).add(c).mul(.5).add(cameraOffset));
  if(side.lengthSquared()<1e-16)return;side.normalize(r*.00065*stroke);
  int color=tint;int coverage=dev.omnidisplay.region.HudLight.alpha(alpha,brightness);
  quad(b,a,c,side,dev.omnidisplay.region.HudLight.color(color,brightness),coverage);
 }
 private static void quad(com.mojang.blaze3d.vertex.BufferBuilder b,Vector3d a,Vector3d c,Vector3d side,int color,int alpha){
  int previous=tint;tint=color;
  vertex(b,new Vector3d(a).sub(side),alpha);vertex(b,new Vector3d(c).sub(side),alpha);vertex(b,new Vector3d(c).add(side),alpha);vertex(b,new Vector3d(a).add(side),alpha);tint=previous;
 }
 // Seven-segment vector digits: no texture or font filtering, legible from inside the sphere.
 private static final int[] DIGITS={63,6,91,79,102,109,125,7,127,111};
 private static final double[][] STROKES={{0,2,1,2},{1,2,1,1},{1,1,1,0},{0,0,1,0},{0,1,0,0},{0,2,0,1},{0,1,1,1}};
 private static void label(com.mojang.blaze3d.vertex.BufferBuilder b,double r,String s,double heading,double pitch,int a){
  double scale=1.1,width=(s.length()*1.5-.5)*scale;
  double stretch=1/Math.max(.25,Math.cos(Math.toRadians(pitch)));
  for(int i=0;i<s.length();i++){
   char c=s.charAt(i);int bits=c=='-'?64:c=='S'?109:c=='H'?118:c==' '?0:DIGITS[c-'0'];
   for(int j=0;j<7;j++)if((bits&(1<<j))!=0){var line=STROKES[j];
    segment(b,r,heading+(-width/2+(i*1.5+line[0])*scale)*stretch,pitch+line[1]*scale,
      heading+(-width/2+(i*1.5+line[2])*scale)*stretch,pitch+line[3]*scale,a);
   }
  }
 }
 @EventBusSubscriber(modid=OmniDisplay.ID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
 public static final class Keys{
  @SubscribeEvent public static void register(RegisterKeyMappingsEvent e){e.register(TOGGLE);e.register(ALIGN);}
 }
}
