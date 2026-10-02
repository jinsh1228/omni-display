package dev.omnidisplay.client;

import dev.omnidisplay.*;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import com.mojang.blaze3d.vertex.*;
import org.joml.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Draws on the existing block surface without adding collision or intercepting clicks. */
@EventBusSubscriber(modid=OmniDisplay.ID,value=Dist.CLIENT)
public final class PanelRendering {
 private record Frame(PoseStack pose,net.minecraft.core.BlockPos origin){}
 @SubscribeEvent public static void render(RenderLevelStageEvent event){
  long omni$perf=dev.omnidisplay.diagnostics.PerformanceDiagnostics.begin(dev.omnidisplay.diagnostics.PerformanceDiagnostics.Phase.PANELS);try{

  if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES)return;
  var mc=Minecraft.getInstance();if(mc.level==null)return;
  var camera=event.getCamera().getPosition();var container=SubLevelContainer.getContainer(mc.level);
  var buffers=mc.renderBuffers().bufferSource();float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
  boolean displaysActive=DisplayRenderer.active();var frames=new java.util.IdentityHashMap<ClientSubLevel,Frame>();
  for(var e:PanelStore.entries(mc.level)){
   if(!mc.level.hasChunkAt(e.pos))continue;var state=mc.level.getBlockState(e.pos);if(state.isAir())continue;
   ClientSubLevel sub=null;
   if(container!=null){var plot=container.getPlot(new ChunkPos(e.pos));if(plot!=null&&plot.getSubLevel() instanceof ClientSubLevel s)sub=s;}
   boolean active=displaysActive&&DisplayRenderer.screen(e.pos);
   var frame=frames.computeIfAbsent(sub,s->{
    var origin=s==null?net.minecraft.core.BlockPos.containing(camera):s.getPlot().getCenterBlock();
    Matrix4d transform=new Matrix4d().translation(-camera.x,-camera.y,-camera.z);
    if(s!=null)transform.mul(s.renderPose().bakeIntoMatrix(new Matrix4d()));
    transform.translate(origin.getX(),origin.getY(),origin.getZ());
    var stack=new PoseStack();stack.mulPose(new Matrix4f(transform));return new Frame(stack,origin);
   });
   var pose=frame.pose();var origin=frame.origin();pose.pushPose();
   try{
   pose.translate(e.pos.getX()-origin.getX(),e.pos.getY()-origin.getY(),e.pos.getZ()-origin.getZ());
   var matrix=pose.last().pose();double distance=matrix.m30()*matrix.m30()+matrix.m31()*matrix.m31()+matrix.m32()*matrix.m32();
   if(distance>256*256)continue;
   for(var face:Direction.values()){
    int bit=1<<face.get3DDataValue();if((e.mask&bit)==0)continue;
    boolean inside=active?DisplayRenderer.screenFace(e.pos,face):(e.interior&bit)!=0;
    if(active&&inside){
     var back=mc.getModelManager().getModel(ScreenEdges.model("panel_back_"+face.get3DDataValue()));
     mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(),buffers.getBuffer(RenderType.entityCutout(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)),state,back,1,1,1,LevelRenderer.getLightColor(mc.level,e.pos.relative(face)),OverlayTexture.NO_OVERLAY);
     if(inside){ScreenEdges.draw("edges_"+bit,state,pose,buffers,e.pos,partial);ScreenAnimation.draw(e.pos,face,face,false,pose,buffers,partial);}
    }else{
     var model=mc.getModelManager().getModel(ScreenEdges.model("panel_"+face.get3DDataValue()+"_"+(inside?1:0)));
     mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(),buffers.getBuffer(RenderType.entityCutout(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)),state,model,1,1,1,LevelRenderer.getLightColor(mc.level,e.pos.relative(face)),OverlayTexture.NO_OVERLAY);
     // Match the ordinary display's full-bright seam layer. Put it ahead of the overlay itself.
     if(inside){
      pose.pushPose();
      try{pose.translate(face.getStepX()/1024d,face.getStepY()/1024d,face.getStepZ()/1024d);ScreenEdges.draw("edges_"+bit,state,pose,buffers,e.pos,partial);}
      finally{pose.popPose();}
     }
    }
   }
   }finally{pose.popPose();}
  }
  buffers.endBatch();
 
  }finally{dev.omnidisplay.diagnostics.PerformanceDiagnostics.end(dev.omnidisplay.diagnostics.PerformanceDiagnostics.Phase.PANELS,omni$perf);}
 }
}
