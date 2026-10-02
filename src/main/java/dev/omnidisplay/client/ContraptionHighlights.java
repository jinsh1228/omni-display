package dev.omnidisplay.client;
import dev.omnidisplay.HighlightStructures;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.*;
import java.util.*;
/** Cached block geometry; the live entity transform follows yaw, pitch and its parent ship. */
final class ContraptionHighlights {
 private record Cached(Object source,int size,long tick,VertexBuffer buffer){}
 private static final Map<UUID,Cached> cache=new HashMap<>();
 static void clear(){for(var c:cache.values())if(c.buffer!=null)c.buffer.close();cache.clear();}
 static void draw(RenderLevelStageEvent event){
  var mc=Minecraft.getInstance();Set<UUID> seen=new HashSet<>();int remaining=16384;
  for(var entity:Attachments.contraptions()){
   if(!(entity instanceof AbstractContraptionEntity e)||e.isRemoved()||!e.isReadyForRender())continue;
   var controller=HighlightStructures.controller(e);var parent=controller==null?null:HighlightStructures.at(mc.level,controller);
   int alpha=DisplayRenderer.settings().structureOpacity(e.getUUID(),"contraption");
   if(alpha<0&&parent!=null)alpha=DisplayRenderer.settings().structureOpacity(parent.getUniqueId(),"ship");if(alpha<=0)continue;
   var contraption=e.getContraption();if(contraption==null)continue;
   var blocks=contraption.getBlocks();if(blocks.size()>remaining)continue;remaining-=blocks.size();seen.add(e.getUUID());
   var cached=cache.get(e.getUUID());long tick=mc.level.getGameTime();
   if(cached==null||cached.source!=contraption||cached.size!=blocks.size()||tick-cached.tick>=20){
    if(cached!=null&&cached.buffer!=null)cached.buffer.close();cache.remove(e.getUUID());
    VertexBuffer gpu=null;
    try(var memory=new ByteBufferBuilder(32768)){
     var builder=new BufferBuilder(memory,VertexFormat.Mode.QUADS,DefaultVertexFormat.NEW_ENTITY);var pose=new PoseStack();
     for(var info:blocks.values())if(info.state().getRenderShape()==RenderShape.MODEL){
      var p=info.pos();pose.pushPose();pose.translate(p.getX(),p.getY(),p.getZ());
      try{HighlightModels.render(contraption.getContraptionWorld(),p,info.state(),contraption.getBlockEntityClientSide(p),pose,type->builder,0xF000F0);}finally{pose.popPose();}
     }
     var data=builder.build();if(data!=null){gpu=new VertexBuffer(VertexBuffer.Usage.STATIC);gpu.bind();try{gpu.upload(data);}finally{VertexBuffer.unbind();}}
     cached=new Cached(contraption,blocks.size(),tick,gpu);cache.put(e.getUUID(),cached);gpu=null;
    }finally{if(gpu!=null)gpu.close();}
   }
   if(cached.buffer==null)continue;
   float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
   var base=e.toGlobalVector(Vec3.ZERO,partial);var x=e.toGlobalVector(new Vec3(1,0,0),partial).subtract(base);var y=e.toGlobalVector(new Vec3(0,1,0),partial).subtract(base);var z=e.toGlobalVector(new Vec3(0,0,1),partial).subtract(base);
   base=base.add(e.getPrevAnchorVec().lerp(e.getAnchorVec(),partial).subtract(e.getAnchorVec()));
   var local=new Matrix4d(x.x,x.y,x.z,0,y.x,y.y,y.z,0,z.x,z.y,z.z,0,base.x,base.y,base.z,1);
   var camera=event.getCamera().getPosition();var world=new Matrix4d().translation(-camera.x,-camera.y,-camera.z);
   if(parent instanceof dev.ryanhcode.sable.sublevel.ClientSubLevel sub)world.mul(sub.renderPose().bakeIntoMatrix(new Matrix4d()));
   world.mul(local);RenderSystem.setShaderColor(1,1,1,alpha/100f);
   cached.buffer.bind();cached.buffer.drawWithShader(new Matrix4f(event.getModelViewMatrix()).mul(new Matrix4f(world)),event.getProjectionMatrix(),Objects.requireNonNull(RenderSystem.getShader()));
  }
  cache.entrySet().removeIf(entry->{if(seen.contains(entry.getKey()))return false;if(entry.getValue().buffer!=null)entry.getValue().buffer.close();return true;});
 }
}
