package dev.omnidisplay.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
/** NeoForge model data preserves Copycats material, multi-part state and contextual geometry. */
final class HighlightModels {
 static void render(ClientLevel level,BlockPos pos,BlockState state,PoseStack pose,VertexConsumer builder){
  render(level,pos,state,pose,type->builder,0xF000F0);
 }
 static void render(ClientLevel level,BlockPos pos,BlockState state,PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light){
  render(level,pos,state,level.getBlockEntity(pos),pose,buffers,light);
 }
 static void render(net.minecraft.world.level.BlockAndTintGetter world,BlockPos pos,BlockState state,net.minecraft.world.level.block.entity.BlockEntity be,PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light){
  var renderer=Minecraft.getInstance().getBlockRenderer();var model=renderer.getBlockModel(state);
  var data=be==null?ModelData.EMPTY:be.getModelData();data=model.getModelData(world,pos,state,data);
  renderer.renderSingleBlock(state,pose,buffers,light,OverlayTexture.NO_OVERLAY,data,null);
 }
}
