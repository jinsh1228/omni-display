package dev.omnidisplay.client;
import com.simibubi.create.content.redstone.nixieTube.NixieTubeBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.*;
import java.util.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Restore dynamic tube characters omitted by storage-coordinate distance culling. */
@EventBusSubscriber(modid=dev.omnidisplay.OmniDisplay.ID,value=Dist.CLIENT)
public final class CabinInstruments {
 private static final Set<NixieTubeBlockEntity> rendered=Collections.newSetFromMap(new IdentityHashMap<>());
 public static void beginScene(){rendered.clear();}
 public static void mark(NixieTubeBlockEntity tube){rendered.add(tube);}
 public static Vec3 eye(NixieTubeBlockEntity tube,Entity entity,float partial){
  var eye=entity.getEyePosition(partial);var sub=DisplayRenderer.surfaceOwner(tube.getBlockPos());
  if(sub==null||!DisplayRenderer.active()||!DisplayRenderer.keeps(tube.getBlockPos()))return eye;
  var container=SubLevelContainer.getContainer(tube.getLevel());if(container==null)return eye;
  var tubePlot=container.getPlot(new ChunkPos(tube.getBlockPos()));if(tubePlot==null||tubePlot.getSubLevel()!=sub)return eye;
  var eyePlot=container.getPlot(new ChunkPos(BlockPos.containing(eye)));if(eyePlot==tubePlot)return eye;
  var local=sub.renderPose().transformPositionInverse(new Vector3d(eye.x,eye.y,eye.z));return new Vec3(local.x,local.y,local.z);
 }
 @SubscribeEvent public static void render(RenderLevelStageEvent event){
  if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES||!DisplayRenderer.active())return;
  var mc=Minecraft.getInstance();var sub=DisplayRenderer.owner();if(sub==null)return;
  for(var body:DisplayRenderer.bodies()){if(body.moving()!=null)continue;sub=body.sub();var origin=sub.getPlot().getCenterBlock();var cam=event.getCamera().getPosition();
  var transform=new Matrix4d().translation(-cam.x,-cam.y,-cam.z).mul(sub.renderPose().bakeIntoMatrix(new Matrix4d())).translate(origin.getX(),origin.getY(),origin.getZ());
  var buffers=mc.renderBuffers().bufferSource();float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
  for(var holder:sub.getPlot().getLoadedChunks())for(var be:holder.getChunk().getBlockEntities().values()){
   if(!(be instanceof NixieTubeBlockEntity tube)||be.isRemoved()||rendered.contains(tube)||!DisplayRenderer.keeps(be.getBlockPos())||Attachments.hide(be))continue;
   var renderer=mc.getBlockEntityRenderDispatcher().getRenderer(tube);if(renderer==null)continue;
   var pose=new PoseStack();pose.mulPose(new Matrix4f(transform));var pos=be.getBlockPos();pose.translate(pos.getX()-origin.getX(),pos.getY()-origin.getY(),pos.getZ()-origin.getZ());
   renderer.render(tube,partial,pose,buffers,net.minecraft.client.renderer.LevelRenderer.getLightColor(mc.level,pos),net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
  }
  buffers.endBatch();
  }
 }
}
