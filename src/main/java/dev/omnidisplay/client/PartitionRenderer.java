package dev.omnidisplay.client;

import dev.omnidisplay.*;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.*;
import net.minecraft.client.renderer.*;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.DoorBlock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import org.joml.*;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;

/** Experimental two-scene compositor. Uses normal scene depth to mask visible screen surfaces. */
@EventBusSubscriber(modid=OmniDisplay.ID,value=Dist.CLIENT)
public final class PartitionRenderer {
 private static final KeyMapping TOGGLE=new KeyMapping("key.omni_display.partition",GLFW.GLFW_KEY_F8,"key.categories.omni_display");
 private static boolean normalPass,prepared,failed,lastMode,singlePass,invisibleFrame;
 private static ShaderInstance composite,photonDepth,hudClip,surfaceMask;
 private static boolean maskReady;
 private static final dev.omnidisplay.region.RenderRecovery recovery=new dev.omnidisplay.region.RenderRecovery();
 private static long lastFailureNotice=Long.MIN_VALUE;
 public static boolean hudVisible(){return !invisibleFrame&&DisplayRenderer.active()&&(maskReady||DisplayRenderer.viewActive());}
 /** The mask already includes foreground cabin occlusion; screen-scene depth must not hide HUD. */
 public static boolean hudUsesMask(){return maskReady&&hudClip!=null;}
 public static ShaderInstance hudShader(){
  if(!maskReady||hudClip==null)return GameRenderer.getPositionColorShader();
  hudClip.setSampler("MaskSampler",mask.getColorTextureId());
  hudClip.safeGetUniform("MaskSize").set((float)mask.width,(float)mask.height);
  return hudClip;
 }
 private static boolean photonFrame;
 private static RenderTarget normal,exterior,mask;
 private static net.minecraft.client.multiplayer.ClientLevel world;
 public static boolean enabled(){return ServerRenderState.ready&&ServerRenderState.experimental;}
 public static boolean normalPass(){return (normalPass||invisibleFrame||IrisPhotonCompat.shadowPass())&&RenderSystem.isOnRenderThread();}
 public static boolean available(){return composite!=null&&surfaceMask!=null&&!failed&&IrisPhotonCompat.allowed();}
 public static void registerKeys(RegisterKeyMappingsEvent event){event.register(TOGGLE);}
 @SubscribeEvent public static void tick(ClientTickEvent.Post event){
  var mc=Minecraft.getInstance();
  if(lastMode!=enabled()){release();failed=false;recovery.reset();lastMode=enabled();}
  if(world!=mc.level){release();failed=false;recovery.reset();lastFailureNotice=Long.MIN_VALUE;world=mc.level;}
  if(failed&&world!=null&&recovery.retry(world.getGameTime(),ScreenTopologyCache.version(world))){release();failed=false;DisplayRenderer.invalidate();}
  while(TOGGLE.consumeClick())if(mc.screen==null)mc.setScreen(new ModSettingsScreen(null));
 }
 private static void release(){invisibleFrame=false;checkedWorld=checkedShape=null;checkedTick=Long.MIN_VALUE;animatedDoors=java.util.List.of();maskReady=false;for(var target:new RenderTarget[]{normal,exterior,mask})if(target!=null)target.destroyBuffers();normal=exterior=mask=null;prepared=false;IrisPhotonCompat.reset();}
 private static void targets(RenderTarget main){
  if(normal!=null&&(normal.width!=main.width||normal.height!=main.height||normal.isStencilEnabled()!=main.isStencilEnabled()))release();
  if(normal==null){
   normal=new TextureTarget(main.width,main.height,true,Minecraft.ON_OSX);
   exterior=new TextureTarget(main.width,main.height,true,Minecraft.ON_OSX);
   mask=new TextureTarget(main.width,main.height,true,Minecraft.ON_OSX);
   // Surface UVs need more precision than an 8-bit color mask for fine pixel grids.
   RenderSystem.bindTexture(mask.getColorTextureId());
   GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL30.GL_RGBA16F,main.width,main.height,0,GL11.GL_RGBA,GL11.GL_FLOAT,(java.nio.ByteBuffer)null);
   for(var target:new RenderTarget[]{normal,exterior,mask}){target.setFilterMode(GL11.GL_NEAREST);if(main.isStencilEnabled())target.enableStencil();}
  }
 }
 private static void copy(RenderTarget from,RenderTarget to,int bits){
  GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,from.frameBufferId);
  GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,to.frameBufferId);
  GL30.glBlitFramebuffer(0,0,from.width,from.height,0,0,to.width,to.height,bits,GL11.GL_NEAREST);
  Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
 }
 private static Object checkedWorld,checkedShape;private static long checkedTick=Long.MIN_VALUE,checkedVersion=-1;private static boolean closedShell;
 private static java.util.List<BlockPos> animatedDoors=java.util.List.of();
 /** Cache static shell checks until topology changes; moving doors are checked each frame. */
 private static boolean closedCabin(float partial){
  var shape=DisplayRenderer.enclosure();var mc=Minecraft.getInstance();
  if(DisplayRenderer.bodies().size()!=1||DisplayRenderer.bodies().get(0).moving()!=null||shape==null||!dev.omnidisplay.region.AdaptiveRenderRules.singleScene(shape.open(),DisplayRenderer.viewActive()))return false;
  long tick=mc.level.getGameTime(),version=ScreenTopologyCache.version(mc.level);
  if(checkedWorld!=mc.level||!java.util.Objects.equals(checkedShape,shape)||tick<checkedTick||checkedVersion!=version){
   checkedWorld=mc.level;checkedShape=shape;checkedTick=tick;checkedVersion=version;closedShell=true;var doors=new java.util.HashSet<BlockPos>();
   for(var cell:shape.walls()){
    var pos=new BlockPos(cell.x(),cell.y(),cell.z());
    if(!mc.level.hasChunkAt(pos)||!ScreenFaces.isScreen(mc.level,pos)){closedShell=false;break;}
    var state=mc.level.getBlockState(pos);
    if(state.is(OmniDisplay.DOOR.get())){
     if(state.getValue(DoorBlock.OPEN)){closedShell=false;break;}
     doors.add(state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)==net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER?pos.below():pos);
    }
    if(state.is(OmniDisplay.TRAPDOOR.get())&&state.getValue(CockpitTrapdoor.OPEN)){closedShell=false;break;}
    for(var face:Direction.values()){
     var n=pos.relative(face);if(shape.interior().contains(new dev.omnidisplay.region.Enclosure.Cell(n.getX(),n.getY(),n.getZ()))&&!ScreenFaces.supportsFace(mc.level,pos,face)){closedShell=false;break;}
    }
    if(!closedShell)break;
   }animatedDoors=java.util.List.copyOf(doors);
  }
  if(!closedShell)return false;
  for(var pos:animatedDoors){
   if(!(mc.level.getBlockEntity(pos) instanceof com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlockEntity be))return false;
   float animation=((dev.omnidisplay.mixin.DoorAnimationAccessor)be).omni$animation().getValue(partial);
   if(!dev.omnidisplay.region.AdaptiveRenderRules.doorClosed(false,animation))return false;
  }return true;
 }
 /** Runs two complete world-render lifecycles sequentially; camera and hand setup stay in GameRenderer. */
 public static void renderScenes(LevelRenderer renderer,DeltaTracker delta,boolean outline,Camera camera,GameRenderer game,LightTexture light,Matrix4f view,Matrix4f projection){
  long omni$perf=dev.omnidisplay.diagnostics.PerformanceDiagnostics.begin(dev.omnidisplay.diagnostics.PerformanceDiagnostics.Phase.WORLD_SCENES);try{

  dev.omnidisplay.diagnostics.PerformanceDiagnostics.sceneMode("ordinary");
  invisibleFrame=false;CabinInstruments.beginScene();maskReady=false;IrisPhotonCompat.refresh();prepared=false;photonFrame=false;singlePass=false;
  if(enabled()&&!failed&&IrisPhotonCompat.enabled()&&!IrisPhotonCompat.allowed())fail(new IllegalStateException("Shader adapter interrupted; scheduling cockpit recovery"));
  if(!enabled()||!available()||!DisplayRenderer.active()){
   IrisPhotonCompat.idle();
   timedWorld(renderer,delta,outline,camera,game,light,view,projection);return;
  }
  boolean visible;
  try{visible=ScreenSurfaceCache.potentiallyVisible(camera,view,projection);}catch(RuntimeException error){fail(error);IrisPhotonCompat.abortPair();IrisPhotonCompat.idle();timedWorld(renderer,delta,outline,camera,game,light,view,projection);return;}
  if(!visible){
   dev.omnidisplay.diagnostics.PerformanceDiagnostics.sceneMode("invisible");
   invisibleFrame=true;IrisPhotonCompat.beginNormalScene();
   timedWorld(renderer,delta,outline,camera,game,light,view,projection);return;
  }
  var main=Minecraft.getInstance().getMainRenderTarget();
  boolean closed;
  try{closed=closedCabin(delta.getGameTimeDeltaPartialTick(false));}catch(RuntimeException error){fail(error);IrisPhotonCompat.idle();timedWorld(renderer,delta,outline,camera,game,light,view,projection);return;}
  if(closed){
   dev.omnidisplay.diagnostics.PerformanceDiagnostics.sceneMode("single");
   // Release dual-scene histories; the shader pack now advances once per ordinary world frame.
   try{IrisPhotonCompat.beginSingleScene();targets(main);photonFrame=IrisPhotonCompat.enabled();singlePass=true;prepared=true;}
   catch(RuntimeException error){fail(error);main.bindWrite(true);}
   try{timedWorld(renderer,delta,outline,camera,game,light,view,projection);}
   catch(RuntimeException error){singlePass=false;prepared=false;throw error;}
   // GameRenderer dispatches AFTER_LEVEL after this wrapper returns.
   // Keep the pending frame alive until compose() has applied the screen filter.
   return;
  }
  try{targets(main);main.bindWrite(true);IrisPhotonCompat.beginPair();}catch(RuntimeException error){fail(error);main.bindWrite(true);}
  if(!available()){timedWorld(renderer,delta,outline,camera,game,light,view,projection);return;}
  dev.omnidisplay.diagnostics.PerformanceDiagnostics.sceneMode("dual");
  normalPass=true;
  try{timedWorld(renderer,delta,outline,camera,game,light,new Matrix4f(view),new Matrix4f(projection));}
  finally{normalPass=false;}
  if(!IrisPhotonCompat.finishNormal()){IrisPhotonCompat.abortPair();return;}
  try{
   copy(main,normal,GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
   photonFrame=IrisPhotonCompat.enabled();
   if(photonFrame)normalizeDepth(normal,projection);
   main.clear(Minecraft.ON_OSX);main.bindWrite(true);prepared=true;
  }catch(ReflectiveOperationException|RuntimeException error){IrisPhotonCompat.abortPair();fail(new RuntimeException(error));main.bindWrite(true);return;}
  IrisPhotonCompat.beginScreen();CabinInstruments.beginScene();
  try{
   timedWorld(renderer,delta,outline,camera,game,light,new Matrix4f(view),new Matrix4f(projection));
   if(photonFrame){copy(main,exterior,GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);normalizeDepth(exterior,projection);}
  }catch(ReflectiveOperationException|RuntimeException error){fail(new RuntimeException(error));}
  finally{try{IrisPhotonCompat.finishPair();}catch(RuntimeException error){fail(error);}finally{main.bindWrite(true);}}
  if(!available()||!prepared){copy(normal,main,GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);prepared=false;}
 
  }finally{dev.omnidisplay.diagnostics.PerformanceDiagnostics.end(dev.omnidisplay.diagnostics.PerformanceDiagnostics.Phase.WORLD_SCENES,omni$perf);}
 }
 private static void timedWorld(LevelRenderer renderer,DeltaTracker delta,boolean outline,Camera camera,GameRenderer game,LightTexture light,Matrix4f view,Matrix4f projection){
  long start=dev.omnidisplay.diagnostics.PerformanceDiagnostics.begin(dev.omnidisplay.diagnostics.PerformanceDiagnostics.Phase.WORLD_PASS);
  try{renderer.renderLevel(delta,outline,camera,game,light,view,projection);}
  finally{dev.omnidisplay.diagnostics.PerformanceDiagnostics.end(dev.omnidisplay.diagnostics.PerformanceDiagnostics.Phase.WORLD_PASS,start);}
 }
 /** Reproject jittered/low-resolution Photon depth, including its separate Voxy combined depth. */
 private static void normalizeDepth(RenderTarget target,Matrix4f projection)throws ReflectiveOperationException{
  if(photonDepth==null)throw new IllegalStateException("Photon depth shader not registered");
  photonDepth.setSampler("SceneDepth",IrisPhotonCompat.sourceDepth());
  photonDepth.setSampler("CombinedDepth",IrisPhotonCompat.combinedDepth());
  photonDepth.safeGetUniform("DepthScale").set(IrisPhotonCompat.scale());
  photonDepth.safeGetUniform("DepthJitter").set(IrisPhotonCompat.jitter().x()/2,IrisPhotonCompat.jitter().y()/2);
  photonDepth.safeGetUniform("HasLod").set(IrisPhotonCompat.hasLod()?1:0);
  photonDepth.safeGetUniform("CombinedProjection").set(IrisPhotonCompat.combinedA(),IrisPhotonCompat.combinedB());
  photonDepth.safeGetUniform("VanillaProjection").set(projection.m22(),projection.m32());
  target.bindWrite(true);RenderSystem.colorMask(false,false,false,false);
  try{drawQuad(photonDepth);}finally{RenderSystem.colorMask(true,true,true,true);Minecraft.getInstance().getMainRenderTarget().bindWrite(true);}
 }
 private static void fail(RuntimeException error){
  failed=true;prepared=false;maskReady=false;singlePass=false;
  var mc=Minecraft.getInstance();long tick=mc.level==null?0:mc.level.getGameTime();
  recovery.fail(tick,ScreenTopologyCache.version(mc.level));ScreenSurfaceCache.clear();
  if(lastFailureNotice==Long.MIN_VALUE||tick-lastFailureNotice>=200||tick<lastFailureNotice){
   lastFailureNotice=tick;org.slf4j.LoggerFactory.getLogger("OmniDisplay").error("Partition rendering frame failed; normal view retained; automatic recovery scheduled",error);
   var p=mc.player;if(p!=null)p.displayClientMessage(net.minecraft.network.chat.Component.literal("显示面渲染暂时中断，正在重新识别座舱并自动恢复；详情见 latest.log。"),false);
  }
 }
 @SubscribeEvent(priority=EventPriority.HIGH) public static void compose(RenderLevelStageEvent event){
  long omni$perf=dev.omnidisplay.diagnostics.PerformanceDiagnostics.begin(dev.omnidisplay.diagnostics.PerformanceDiagnostics.Phase.COMPOSE);try{

  if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL||normalPass||!prepared)return;
  var main=Minecraft.getInstance().getMainRenderTarget();
  try{
   if(singlePass){
    copy(main,normal,GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
    if(photonFrame){IrisPhotonCompat.singleDepthProfile();normalizeDepth(normal,event.getProjectionMatrix());}
    copy(normal,exterior,GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
   }else copy(main,exterior,photonFrame?GL11.GL_COLOR_BUFFER_BIT:GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
   mask.setClearColor(0,0,0,0);mask.clear(Minecraft.ON_OSX);copy(normal,mask,GL11.GL_DEPTH_BUFFER_BIT);
   mask.bindWrite(true);
   // A visible panel can lie inside the camera near plane. Clamp mask depth instead
   // of clipping it away; retain depth testing against foreground cabin objects.
   boolean depthClamp=GL11.glIsEnabled(GL32.GL_DEPTH_CLAMP);
   GL11.glEnable(GL32.GL_DEPTH_CLAMP);
   try{drawSurfaces(event);}finally{if(!depthClamp)GL11.glDisable(GL32.GL_DEPTH_CLAMP);}
   main.bindWrite(true);
   composite.setSampler("NormalSampler",normal.getColorTextureId());composite.setSampler("ScreenSampler",exterior.getColorTextureId());
   composite.setSampler("MaskSampler",mask.getColorTextureId());composite.setSampler("NormalDepth",normal.getDepthTextureId());composite.setSampler("ScreenDepth",exterior.getDepthTextureId());
   var f=ServerRenderState.filter;
   composite.safeGetUniform("FilterStrength").set(f.getBoolean("enabled")?java.lang.Math.clamp(f.getFloat("strength"),0f,1f):0f);
   composite.safeGetUniform("ColorSettings").set(f.getFloat("brightness"),f.getFloat("contrast"),f.getFloat("blackLift"),f.getBoolean("tint")?1f:0f);
   composite.safeGetUniform("SurfaceSettings").set(f.getBoolean("pixels")?f.getFloat("pixelStrength"):0f,f.getBoolean("scanlines")?f.getFloat("scanlineStrength"):0f,f.getBoolean("edges")?f.getFloat("edgeStrength"):0f,0f);
   composite.safeGetUniform("SurfaceDensity").set((float)f.getInt("pixelDensity"),(float)f.getInt("scanlineDensity"));
   drawComposite();maskReady=true;recovery.reset();
  }catch(ReflectiveOperationException|RuntimeException error){fail(new RuntimeException(error));try{IrisPhotonCompat.abortPair();if(normal!=null)copy(normal,main,GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);}catch(RuntimeException restore){error.addSuppressed(restore);}}
  finally{prepared=false;singlePass=false;main.bindWrite(true);RenderSystem.enableDepthTest();RenderSystem.depthFunc(GL11.GL_LEQUAL);RenderSystem.depthMask(true);RenderSystem.enableCull();RenderSystem.setShaderColor(1,1,1,1);}
 
  }finally{dev.omnidisplay.diagnostics.PerformanceDiagnostics.end(dev.omnidisplay.diagnostics.PerformanceDiagnostics.Phase.COMPOSE,omni$perf);}
 }
 private static void drawSurfaces(RenderLevelStageEvent event){
  long omni$perf=dev.omnidisplay.diagnostics.PerformanceDiagnostics.begin(dev.omnidisplay.diagnostics.PerformanceDiagnostics.Phase.MASK_BUILD);try{

  var mc=Minecraft.getInstance();var owner=DisplayRenderer.owner();var shape=DisplayRenderer.enclosure();if(owner==null||shape==null)return;
  var camera=event.getCamera().getPosition();
  ScreenSurfaceCache.faces();
  for(var body:DisplayRenderer.bodies()){owner=body.sub();if(owner.isRemoved())continue;var origin=body.origin();var pose=body.pose(event.getPartialTick().getGameTimeDeltaPartialTick(false));
  var localCamera=dev.omnidisplay.region.ScreenCoordinates.localEye(pose,new Vector3d(camera.x,camera.y,camera.z));
  var relative=new Matrix4d().translation(-camera.x,-camera.y,-camera.z).mul(pose).translate(origin.getX(),origin.getY(),origin.getZ());
  try(var memory=new ByteBufferBuilder(262144)){
   var builder=new BufferBuilder(memory,VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);int count=0;
   for(var surface:ScreenSurfaceCache.forBody(body)){
    var pos=surface.pos();var state=body.level().getBlockState(pos);if(state.getBlock()!=surface.state().getBlock())continue;var face=surface.face();double plane=surface.plane();boolean door=surface.door(),hatch=surface.hatch();
    double doorX=0,doorZ=0;float coverage=1;
    if(door){
     var base=state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)==net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER?pos.below():pos;
     float animation=state.getValue(DoorBlock.OPEN)?1:0;
     if(body.level().getBlockEntity(base) instanceof com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlockEntity be)animation=((dev.omnidisplay.mixin.DoorAnimationAccessor)be).omni$animation().getValue(event.getPartialTick().getGameTimeDeltaPartialTick(false));
     coverage=dev.omnidisplay.region.DoorScreenFade.coverage(animation);if(coverage<=.001f)continue;
     var facing=state.getValue(DoorBlock.FACING);var side=facing.getClockWise();
     if(state.getValue(DoorBlock.HINGE)==net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT)side=side.getOpposite();
     double slide=dev.omnidisplay.region.SlidingScreenGeometry.slide(animation),out=dev.omnidisplay.region.SlidingScreenGeometry.out(animation);
     doorX=side.getStepX()*slide+facing.getStepX()*out;doorZ=side.getStepZ()*slide+facing.getStepZ()*out;
    }
    if(hatch&&state.getValue(CockpitTrapdoor.OPEN))continue;
    if(!dev.omnidisplay.region.PartitionSurface.frontFacing(face.get3DDataValue(),localCamera.x-pos.getX()-doorX,localCamera.y-pos.getY(),localCamera.z-pos.getZ()-doorZ,plane))continue;
     // Offset toward the cabin to pass the normal screen's own depth, not foreground objects.
     plane+=face.getAxisDirection()==Direction.AxisDirection.POSITIVE?.003:-.003;
     for(double[] uv:new double[][]{{0,0},{1,0},{1,1},{0,1}}){
      var v=dev.omnidisplay.region.PartitionSurface.point(face.get3DDataValue(),uv[0],uv[1],plane);
      builder.addVertex((float)(pos.getX()-origin.getX()+v[0]+doorX),(float)(pos.getY()-origin.getY()+v[1]),(float)(pos.getZ()-origin.getZ()+v[2]+doorZ)).setUv((float)uv[0],(float)uv[1]).setColor(255,255,255,java.lang.Math.round(255*coverage));
     }count++;
   }
   if(count==0)continue;
   RenderSystem.enableDepthTest();RenderSystem.depthFunc(GL11.GL_LEQUAL);RenderSystem.depthMask(false);RenderSystem.disableBlend();RenderSystem.disableCull();RenderSystem.setShaderColor(1,1,1,1);
   try(var gpu=new VertexBuffer(VertexBuffer.Usage.DYNAMIC)){gpu.bind();gpu.upload(builder.buildOrThrow());gpu.drawWithShader(new Matrix4f(event.getModelViewMatrix()).mul(new Matrix4f(relative)),event.getProjectionMatrix(),surfaceMask);}
   finally{VertexBuffer.unbind();RenderSystem.depthMask(true);}
  }
  }
 
  }finally{dev.omnidisplay.diagnostics.PerformanceDiagnostics.end(dev.omnidisplay.diagnostics.PerformanceDiagnostics.Phase.MASK_BUILD,omni$perf);}
 }
 private static void drawComposite(){drawQuad(composite);}
 private static void drawQuad(ShaderInstance shader){
  RenderSystem.disableBlend();RenderSystem.disableCull();RenderSystem.enableDepthTest();RenderSystem.depthFunc(GL11.GL_ALWAYS);RenderSystem.depthMask(true);RenderSystem.setShaderColor(1,1,1,1);
  try(var memory=new ByteBufferBuilder(256);var gpu=new VertexBuffer(VertexBuffer.Usage.DYNAMIC)){
   var b=new BufferBuilder(memory,VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX);
   b.addVertex(-1,-1,0).setUv(0,0);b.addVertex(1,-1,0).setUv(1,0);b.addVertex(1,1,0).setUv(1,1);b.addVertex(-1,1,0).setUv(0,1);
   gpu.bind();gpu.upload(b.buildOrThrow());gpu.drawWithShader(new Matrix4f(),new Matrix4f(),shader);
  }finally{VertexBuffer.unbind();}
 }
 @EventBusSubscriber(modid=OmniDisplay.ID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
 public static final class Shaders {
  @SubscribeEvent public static void register(RegisterShadersEvent event)throws java.io.IOException{
   event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.fromNamespaceAndPath(OmniDisplay.ID,"screen_surface_mask"),DefaultVertexFormat.POSITION_TEX_COLOR),shader->surfaceMask=shader);
   event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.fromNamespaceAndPath(OmniDisplay.ID,"screen_hud"),DefaultVertexFormat.POSITION_COLOR),shader->{hudClip=shader;maskReady=false;});
   event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.fromNamespaceAndPath(OmniDisplay.ID,"partition_composite"),DefaultVertexFormat.POSITION_TEX),shader->{composite=shader;failed=false;IrisPhotonCompat.reset();});
   event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.fromNamespaceAndPath(OmniDisplay.ID,"partition_photon_depth"),DefaultVertexFormat.POSITION_TEX),shader->photonDepth=shader);
  }
 }
}
