package dev.omnidisplay.client;

import dev.omnidisplay.OmniDisplay;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import java.util.*;

/** Original implementation: cached ordinary block models, bright texture, configurable opacity.
 * No geometry/code/assets are copied from Flight Control. Dynamic visuals remain hidden.
 */
@EventBusSubscriber(modid=OmniDisplay.ID,value=Dist.CLIENT)
public final class HologramRenderer {
 private static final int MAX_BLOCKS=16384;
 private record Mesh(ClientSubLevel sub,BlockPos origin,VertexBuffer buffer,int opacity){}
 private static final List<Mesh> meshes=new ArrayList<>();
 private static volatile Set<ClientSubLevel> builtFor=Set.of();
 private static volatile boolean dirty;
 private static long topologyVersion=-1;
 private static Object selection;
 private static long revision;
 private static long builtTick=Long.MIN_VALUE;
 private static boolean warned,failed;
 public static void changed(net.minecraft.world.level.Level level,BlockPos pos){
  if(level!=Minecraft.getInstance().level)return;
  var chunk=new net.minecraft.world.level.ChunkPos(pos);for(var sub:builtFor)if(sub.getPlot().contains(chunk)){dirty=true;break;}
 }
 public static void clear(){ContraptionHighlights.clear();for(var mesh:meshes)mesh.buffer.close();meshes.clear();builtFor=Set.of();selection=null;builtTick=Long.MIN_VALUE;failed=false;warned=false;}
 private static void rebuild(Set<ClientSubLevel> ships,long tick){
  long omni$perf=dev.omnidisplay.diagnostics.PerformanceDiagnostics.begin(dev.omnidisplay.diagnostics.PerformanceDiagnostics.Phase.HOLOGRAM_BUILD);try{

  for(var mesh:meshes)mesh.buffer.close();meshes.clear();
  var mc=Minecraft.getInstance();int count=0;dirty=false;topologyVersion=dev.omnidisplay.ScreenTopologyCache.version(mc.level);
  // Deterministic order; source geometry is reused between rebuilds while transforms stay live.
  var ordered=new ArrayList<>(ships);ordered.sort(Comparator.comparing(s->s.getUniqueId().toString()));
  for(var sub:ordered){
   if(count>=MAX_BLOCKS)break;
   var origin=sub.getPlot().getCenterBlock();VertexBuffer gpu=null;
   Map<Integer,ByteBufferBuilder> memories=new HashMap<>();Map<Integer,BufferBuilder> builders=new HashMap<>();
   try{
    var pose=new PoseStack();
    outer:for(var holder:sub.getPlot().getLoadedChunks()){
     var chunk=holder.getChunk();var sections=chunk.getSections();
     for(int sectionIndex=0;sectionIndex<sections.length;sectionIndex++){
      var section=sections[sectionIndex];if(section.hasOnlyAir())continue;
      int ybase=chunk.getSectionYFromSectionIndex(sectionIndex)*16;
      for(int y=0;y<16;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++){
       var state=section.getBlockState(x,y,z);if(OmniDisplay.isScreen(state))continue;if(state.getRenderShape()!=RenderShape.MODEL)continue;
       var p=new BlockPos(chunk.getPos().getMinBlockX()+x,ybase+y,chunk.getPos().getMinBlockZ()+z);
       if(DisplayRenderer.member(sub)&&DisplayRenderer.panelScreen(p))continue;
       int alpha=DisplayRenderer.settings().highlightAt(sub.getUniqueId(),p,origin,sub==DisplayRenderer.owner());
       if(DisplayRenderer.member(sub)&&DisplayRenderer.keeps(p)&&alpha<0)continue;
       if(alpha==0)continue;
       var builder=builders.computeIfAbsent(alpha,k->{var memory=new ByteBufferBuilder(32768);memories.put(k,memory);return new BufferBuilder(memory,VertexFormat.Mode.QUADS,DefaultVertexFormat.NEW_ENTITY);});
       if(count>=MAX_BLOCKS)break outer;
       count++;
       pose.pushPose();pose.translate(p.getX()-origin.getX(),p.getY()-origin.getY(),p.getZ()-origin.getZ());
       try {HighlightModels.render(mc.level,p,state,pose,builder);}
       finally {pose.popPose();}
      }
     }
    }
    for(var entry:builders.entrySet()){var data=entry.getValue().build();
    if(data!=null){gpu=new VertexBuffer(VertexBuffer.Usage.STATIC);gpu.bind();try{gpu.upload(data);}finally{VertexBuffer.unbind();}meshes.add(new Mesh(sub,origin,gpu,entry.getKey()));gpu=null;}
   }}finally{if(gpu!=null)gpu.close();for(var memory:memories.values())memory.close();}
  }
  if(count>=MAX_BLOCKS&&!warned){warned=true;if(mc.player!=null)mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("全息测试版最多显示16384个普通方块，超出部分继续隐藏。"),false);}
  builtFor=Set.copyOf(ships);selection=DisplayRenderer.selection();builtTick=tick;revision=DisplayRenderer.settings().revision;
 
  }finally{dev.omnidisplay.diagnostics.PerformanceDiagnostics.end(dev.omnidisplay.diagnostics.PerformanceDiagnostics.Phase.HOLOGRAM_BUILD,omni$perf);}
 }
 @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.HIGHEST) public static void render(RenderLevelStageEvent event){
  if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
  if(!DisplayRenderer.active()){if(!meshes.isEmpty()||failed)clear();return;}
  if(failed)return;
  var mc=Minecraft.getInstance();var ships=new HashSet<>(Attachments.structures());ships.add(DisplayRenderer.owner());
  try {
   long tick=mc.level.getGameTime();
   if(!ships.equals(builtFor)||selection!=DisplayRenderer.selection()||builtTick==Long.MIN_VALUE||tick<builtTick||dirty||topologyVersion!=dev.omnidisplay.ScreenTopologyCache.version(mc.level)||revision!=DisplayRenderer.settings().revision)rebuild(ships,tick);
   RenderType type=RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
   float[] previous=RenderSystem.getShaderColor().clone();
   boolean depthWrite=org.lwjgl.opengl.GL11.glGetBoolean(org.lwjgl.opengl.GL11.GL_DEPTH_WRITEMASK);
   type.setupRenderState();
   try {
    RenderSystem.setShaderColor(1,1,1,DisplayRenderer.opacity());
    RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.depthMask(false);RenderSystem.enableCull();
    // Keep normal scene depth testing: cockpit and external terrain still occlude the hologram.
    var camera=event.getCamera().getPosition();
    var shader=Objects.requireNonNull(RenderSystem.getShader());
    for(var mesh:meshes){
     if(mesh.sub.isRemoved())continue;
     RenderSystem.setShaderColor(1,1,1,mesh.opacity<0?DisplayRenderer.opacity():mesh.opacity/100f);
     var p=mesh.origin;
     var relative=new Matrix4d().translation(-camera.x,-camera.y,-camera.z)
       .mul(mesh.sub.renderPose().bakeIntoMatrix(new Matrix4d())).translate(p.getX(),p.getY(),p.getZ());
     var view=new Matrix4f(event.getModelViewMatrix()).mul(new Matrix4f(relative));
     mesh.buffer.bind();mesh.buffer.drawWithShader(view,event.getProjectionMatrix(),shader);
    }
    ContraptionHighlights.draw(event);
   }finally{VertexBuffer.unbind();type.clearRenderState();RenderSystem.setShaderColor(previous[0],previous[1],previous[2],previous[3]);RenderSystem.depthMask(depthWrite);}
  }catch(RuntimeException error){
   clear();failed=true;
   org.slf4j.LoggerFactory.getLogger("OmniDisplay").error("Hologram failed; reverting to full transparency until mode reset",error);
   if(mc.player!=null)mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("全息绘制失败，已退回完全透视；请提供latest.log。"),false);
  }
 }
}
