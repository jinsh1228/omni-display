package dev.omnidisplay.client;

import dev.omnidisplay.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.decoration.slidingDoor.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

/** Full-bright lines only: transparent interiors, no emitted world light and no bloom dependency. */
public final class ScreenEdges {
 private static final java.util.Map<String,ModelResourceLocation> modelIds=new java.util.concurrent.ConcurrentHashMap<>();
 public static ModelResourceLocation model(String name){return modelIds.computeIfAbsent(name,key->ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(OmniDisplay.ID,"block/"+key)));}
 public static void draw(String name,BlockState state,PoseStack pose,MultiBufferSource buffers,net.minecraft.core.BlockPos pos,float partial){
  draw(name,state,pose,buffers,pos,partial,1);
 }
 public static void draw(String name,BlockState state,PoseStack pose,MultiBufferSource buffers,net.minecraft.core.BlockPos pos,float partial,float opacity){
  draw(name,state,pose,buffers,pos,partial,opacity,null);
 }
 public static void draw(String name,BlockState state,PoseStack pose,MultiBufferSource buffers,net.minecraft.core.BlockPos pos,float partial,float opacity,CabinSettings moving){
  var mc=Minecraft.getInstance();
  float alpha=opacity;
  if(moving!=null||DisplayRenderer.active()&&DisplayRenderer.screen(pos)){
   var s=moving!=null?moving:DisplayRenderer.surfaceSettings(pos);alpha*=(float)dev.omnidisplay.region.SeamFade.amount(s.waveBase,s.waveTimes,s.waveTargets,mc.level.getGameTime()+partial,s.seamOpacity/100d);
  }
  if(alpha<=.001f)return;
  var out=buffers.getBuffer(alpha>=.999f?RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS):RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
  mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(),
   alpha>=.999f?out:new AlphaConsumer(out,alpha),state,
   mc.getModelManager().getModel(model(name)),1,1,1,0xF000F0,OverlayTexture.NO_OVERLAY);
 }
 private record AlphaConsumer(com.mojang.blaze3d.vertex.VertexConsumer out,float alpha) implements com.mojang.blaze3d.vertex.VertexConsumer {
  public com.mojang.blaze3d.vertex.VertexConsumer addVertex(float x,float y,float z){out.addVertex(x,y,z);return this;}
  public com.mojang.blaze3d.vertex.VertexConsumer setColor(int r,int g,int b,int a){out.setColor(r,g,b,Math.round(a*alpha));return this;}
  public com.mojang.blaze3d.vertex.VertexConsumer setUv(float u,float v){out.setUv(u,v);return this;}
  public com.mojang.blaze3d.vertex.VertexConsumer setUv1(int u,int v){out.setUv1(u,v);return this;}
  public com.mojang.blaze3d.vertex.VertexConsumer setUv2(int u,int v){out.setUv2(u,v);return this;}
  public com.mojang.blaze3d.vertex.VertexConsumer setNormal(float x,float y,float z){out.setNormal(x,y,z);return this;}
 }
 public static final class Display implements BlockEntityRenderer<OmniDisplay.DisplayEntity>{
  @Override public void render(OmniDisplay.DisplayEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
   var state=be.getBlockState();int mask=state.getValue(OmniDisplay.DisplayBlock.FACES);
   if(state.is(OmniDisplay.THIN_DISPLAY.get())||state.is(OmniDisplay.TRAPDOOR.get())){
    for(var face:net.minecraft.core.Direction.values())if((mask&(1<<face.get3DDataValue()))!=0){
     double offset=(state.is(OmniDisplay.TRAPDOOR.get())?CockpitTrapdoor.plane(state,face):ThinDisplayBlock.plane(state,face))-(face.getAxisDirection()==net.minecraft.core.Direction.AxisDirection.POSITIVE?1:0);
     pose.pushPose();pose.translate(face.getAxis()==net.minecraft.core.Direction.Axis.X?offset:0,face.getAxis()==net.minecraft.core.Direction.Axis.Y?offset:0,face.getAxis()==net.minecraft.core.Direction.Axis.Z?offset:0);
     draw("edges_"+(1<<face.get3DDataValue()),state,pose,buffers,be.getBlockPos(),partial);pose.popPose();
    }
   }else if(mask!=0)draw("edges_"+mask,state,pose,buffers,be.getBlockPos(),partial);
   for(var face:net.minecraft.core.Direction.values())if((mask&(1<<face.get3DDataValue()))!=0)ScreenAnimation.draw(be.getBlockPos(),face,face,false,pose,buffers,partial);
  }
 }
 public static final class Door extends SlidingDoorRenderer{
  public Door(BlockEntityRendererProvider.Context context){super(context);}
  private void opaquePanel(BlockState state,float animation,float opacity,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
   var facing=state.getValue(DoorBlock.FACING);var side=facing.getClockWise();if(state.getValue(DoorBlock.HINGE)==DoorHingeSide.LEFT)side=side.getOpposite();
   double slide=dev.omnidisplay.region.SlidingScreenGeometry.slide(animation),out=dev.omnidisplay.region.SlidingScreenGeometry.out(animation);
   var consumer=new AlphaConsumer(buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS)),opacity);
   for(var half:net.minecraft.world.level.block.state.properties.DoubleBlockHalf.values()){
    pose.pushPose();
    try{
     pose.translate(side.getStepX()*slide+facing.getStepX()*out,half==net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER?.9980469:0,side.getStepZ()*slide+facing.getStepZ()*out);
     var material=state.setValue(DoorBlock.OPEN,false).setValue(DoorBlock.HALF,half).setValue(CockpitDoor.SIDES,0);
     Minecraft.getInstance().getBlockRenderer().renderSingleBlock(material,pose,type->consumer,light,overlay);
    }finally{pose.popPose();}
   }
  }
  @Override protected void renderSafe(SlidingDoorBlockEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){

   var state=be.getBlockState();int mask=state.getValue(CockpitDoor.SIDES);
   float animation=((dev.omnidisplay.mixin.DoorAnimationAccessor)be).omni$animation().getValue(partial);
   var settings=DisplayRenderer.surfaceSettings(be.getBlockPos());var shape=DisplayRenderer.enclosure();
   boolean fading=PartitionRenderer.enabled()&&settings!=null&&(settings.power||settings.animating(be.getLevel().getGameTime()))&&DisplayRenderer.screen(be.getBlockPos());
   float coverage=fading?dev.omnidisplay.region.DoorScreenFade.coverage(animation):1;
   if(!Attachments.cockpitDoor(be))super.renderSafe(be,partial,pose,buffers,light,overlay);
   else if(fading&&coverage<.999f)opaquePanel(state,animation,1-coverage,pose,buffers,light,overlay);
   if(mask==0||coverage<=.001f)return;
   var facing=state.getValue(DoorBlock.FACING);var side=facing.getClockWise();
   if(state.getValue(DoorBlock.HINGE)==DoorHingeSide.LEFT)side=side.getOpposite();
   float a=((dev.omnidisplay.mixin.DoorAnimationAccessor)be).omni$animation().getValue(partial),slide=a*a*13/16f,out=Math.clamp(a*10,0,1)/32f;
   pose.pushPose();
   try{
    pose.translate(side.getStepX()*slide+facing.getStepX()*out,0,side.getStepZ()*slide+facing.getStepZ()*out);
    // Match the closed brass-door blockstate rotations; animation translates that geometry.
    int degrees=switch(facing){case SOUTH->90;case WEST->180;case NORTH->270;default->0;};
    pose.translate(.5,0,.5);pose.mulPose(Axis.YP.rotationDegrees(-degrees));pose.translate(-.5,0,-.5);
    draw("door_edges_"+mask,state,pose,buffers,be.getBlockPos(),partial,coverage);
    if((mask&1)!=0)ScreenAnimation.draw(be.getBlockPos(),facing,net.minecraft.core.Direction.EAST,true,pose,buffers,partial,coverage);
    if((mask&2)!=0)ScreenAnimation.draw(be.getBlockPos(),facing.getOpposite(),net.minecraft.core.Direction.WEST,true,pose,buffers,partial,coverage);
    pose.translate(0,.9980469,0);draw("door_edges_"+mask,state,pose,buffers,be.getBlockPos(),partial,coverage);
    if((mask&1)!=0)ScreenAnimation.draw(be.getBlockPos().above(),facing,net.minecraft.core.Direction.EAST,true,pose,buffers,partial,coverage);
    if((mask&2)!=0)ScreenAnimation.draw(be.getBlockPos().above(),facing.getOpposite(),net.minecraft.core.Direction.WEST,true,pose,buffers,partial,coverage);
   }finally{pose.popPose();}
  }
 }
}
