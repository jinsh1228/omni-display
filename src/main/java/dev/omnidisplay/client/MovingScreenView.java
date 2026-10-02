package dev.omnidisplay.client;
import dev.omnidisplay.*;
import dev.omnidisplay.region.Enclosure;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.*;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import org.joml.*;
import java.util.*;

@EventBusSubscriber(modid=OmniDisplay.ID,value=Dist.CLIENT)
public final class MovingScreenView {
 private record Snapshot(long tick,Map<BlockPos,MovingScreens.Screen> screens){}
 private static final Map<AbstractContraptionEntity,Snapshot> snapshots=new WeakHashMap<>();
 private static Map<BlockPos,MovingScreens.Screen> snapshot(AbstractContraptionEntity e){long tick=e.level().getGameTime();var old=snapshots.get(e);if(old!=null&&old.tick==tick)return old.screens;var map=new HashMap<BlockPos,MovingScreens.Screen>();for(var s:MovingScreens.screens(e))map.put(s.pos(),s);snapshots.put(e,new Snapshot(tick,Map.copyOf(map)));return map;}
 public record Candidate(ClientSubLevel sub,CabinEntity host,Enclosure.Result shape,AbstractContraptionEntity entity){}
 public static List<Candidate> candidates(){
  var mc=Minecraft.getInstance();var result=new ArrayList<Candidate>();
  for(var entity:mc.level.entitiesForRendering())if(entity instanceof AbstractContraptionEntity e&&!e.isRemoved()&&e.isReadyForRender()){
   var controller=HighlightStructures.controller(e);var parent=controller==null?null:HighlightStructures.at(mc.level,controller);
   if(!(parent instanceof ClientSubLevel sub))continue;var screens=MovingScreens.screens(e);if(screens.isEmpty())continue;
   try{
    var shape=MovingScreens.shape(screens);var selected=screens.stream().max(Comparator.comparingLong(s->s.settings().revision)).orElseThrow();
    var host=new CabinEntity(selected.pos(),OmniDisplay.DISPLAY.get().defaultBlockState());host.setLevel(mc.level);host.settings=selected.settings();
    result.add(new Candidate(sub,host,shape,e));
   }catch(IllegalArgumentException invalid){}
  }return result;
 }
 public static Matrix4d pose(AbstractContraptionEntity e,ClientSubLevel parent,float partial,boolean logical){
  var base=e.toGlobalVector(Vec3.ZERO,partial);var x=e.toGlobalVector(new Vec3(1,0,0),partial).subtract(base);var y=e.toGlobalVector(new Vec3(0,1,0),partial).subtract(base);var z=e.toGlobalVector(new Vec3(0,0,1),partial).subtract(base);
  base=base.add(e.getPrevAnchorVec().lerp(e.getAnchorVec(),partial).subtract(e.getAnchorVec()));
  var local=new Matrix4d(x.x,x.y,x.z,0,y.x,y.y,y.z,0,z.x,z.y,z.z,0,base.x,base.y,base.z,1);
  return (logical?parent.logicalPose():parent.renderPose()).bakeIntoMatrix(new Matrix4d()).mul(local);
 }
 public static boolean inward(AbstractContraptionEntity e,Enclosure.Result shape,BlockPos pos,Direction face){
  var screen=snapshot(e).get(pos);if(screen==null)return false;
  int mask=MovingScreens.faces(screen);return shape.open()?(mask&(1<<face.get3DDataValue()))!=0:shape.interior().contains(CabinManager.cell(pos.relative(face)));
 }
 public static void receive(MovingScreens.Sync p){
  var mc=Minecraft.getInstance();if(mc.level==null||!(mc.level.getEntity(p.entity()) instanceof AbstractContraptionEntity e)||e.getContraption()==null)return;
  var info=e.getContraption().getBlocks().get(p.pos());if(info==null)return;var state=info.state();if(state.hasProperty(OmniDisplay.DisplayBlock.FACES))state=state.setValue(OmniDisplay.DisplayBlock.FACES,p.faces()&63);
  var data=info.nbt()==null?new net.minecraft.nbt.CompoundTag():info.nbt().copy();data.merge(p.data());
  if(Objects.equals(info.nbt(),data)&&state==info.state())return;
  snapshots.remove(e);e.getContraption().getBlocks().put(p.pos(),new StructureBlockInfo(p.pos(),state,data));
  var be=e.getContraption().getBlockEntityClientSide(p.pos());if(be instanceof CabinEntity host)host.settings=CabinSettings.load(p.data().getCompound("cabin"),mc.level.registryAccess());
  DisplayRenderer.refresh();
 }
 public static void stop(AbstractContraptionEntity e){PacketDistributor.sendToServer(new MovingScreens.Action(e.getId(),2,0));}
 @SubscribeEvent public static void render(RenderLevelStageEvent event){
  if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES)return;
  var mc=Minecraft.getInstance();if(mc.level==null||!ServerRenderState.experimental)return;
  var buffers=mc.renderBuffers().bufferSource();var cam=event.getCamera().getPosition();float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
  // Ordinary pass: Create draws its entity normally, but attached panels still need an overlay.
  if(!DisplayRenderer.active()){
   for(var candidate:candidates())draw(candidate.entity,candidate.sub,candidate.shape,candidate.host.settings,event,buffers,false);
  }else for(var body:DisplayRenderer.bodies())if(body.moving()!=null)draw(body.moving(),body.sub(),body.shape(),body.settings(),event,buffers,true);
  buffers.endBatch();
 }
 private static void draw(AbstractContraptionEntity e,ClientSubLevel parent,Enclosure.Result shape,CabinSettings settings,RenderLevelStageEvent event,MultiBufferSource buffers,boolean active){
  if(e.isRemoved()||!e.isReadyForRender()||e.getContraption()==null)return;
  var mc=Minecraft.getInstance();var camera=event.getCamera().getPosition();float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
  var transform=new Matrix4d().translation(-camera.x,-camera.y,-camera.z).mul(pose(e,parent,partial,false));var source=e.getContraption().getContraptionWorld();
  if(active)for(var info:e.getContraption().getBlocks().values()){
   if(OmniDisplay.isScreen(info.state())||!shape.interior().contains(CabinManager.cell(info.pos())))continue;
   var pose=new PoseStack();pose.mulPose(new Matrix4f(transform));pose.translate(info.pos().getX(),info.pos().getY(),info.pos().getZ());
   if(info.state().getRenderShape()==RenderShape.MODEL)HighlightModels.render(source,info.pos(),info.state(),e.getContraption().getBlockEntityClientSide(info.pos()),pose,buffers,0xF000F0);
  }
  for(var screen:MovingScreens.screens(e)){
   var p=screen.pos();var state=screen.state();var pose=new PoseStack();pose.mulPose(new Matrix4f(transform));pose.translate(p.getX(),p.getY(),p.getZ());int mask=0;
   for(var face:Direction.values())if(inward(e,shape,p,face))mask|=1<<face.get3DDataValue();
   if(screen.mask()!=0){for(var face:Direction.values())if((screen.mask()&(1<<face.get3DDataValue()))!=0){
    boolean inside=(mask&(1<<face.get3DDataValue()))!=0;String model=active&&inside?"panel_back_"+face.get3DDataValue():"panel_"+face.get3DDataValue()+"_"+(inside?1:0);
    mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(),buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS)),state,mc.getModelManager().getModel(ScreenEdges.model(model)),1,1,1,0xF000F0,OverlayTexture.NO_OVERLAY);
   }}
   if(!active)continue;
   for(var face:Direction.values())if((mask&(1<<face.get3DDataValue()))!=0){
    pose.pushPose();try{
     double plane=state.is(OmniDisplay.THIN_DISPLAY.get())?ThinDisplayBlock.plane(state,face):state.is(OmniDisplay.TRAPDOOR.get())?CockpitTrapdoor.plane(state,face):face.getAxisDirection()==Direction.AxisDirection.POSITIVE?1:0;
     double offset=plane-(face.getAxisDirection()==Direction.AxisDirection.POSITIVE?1:0);pose.translate(face.getAxis()==Direction.Axis.X?offset:0,face.getAxis()==Direction.Axis.Y?offset:0,face.getAxis()==Direction.Axis.Z?offset:0);
     ScreenEdges.draw("edges_"+(1<<face.get3DDataValue()),state,pose,buffers,p,partial,1,settings);
    }finally{pose.popPose();}
    ScreenAnimation.draw(p,face,face,false,pose,buffers,partial,1,settings,shape,source,(position,direction)->inward(e,shape,position,direction));
   }
  }
 }
}
