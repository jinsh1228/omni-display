package dev.omnidisplay.client;
import dev.omnidisplay.OmniDisplay;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import com.mojang.blaze3d.vertex.*;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** Every client discovers powered cabins; partition screens remain visible from outside. */
@EventBusSubscriber(modid=OmniDisplay.ID,value=Dist.CLIENT)
public final class DisplayRenderer {
 private static ClientSubLevel owner;
 private static BlockPos min,max;
 private static dev.omnidisplay.CabinEntity host;
 private record Candidate(ClientSubLevel sub,dev.omnidisplay.CabinEntity host,dev.omnidisplay.region.Enclosure.Result shape,dev.omnidisplay.region.Enclosure.Result hudShape,com.simibubi.create.content.contraptions.AbstractContraptionEntity moving){
  Candidate(ClientSubLevel sub,dev.omnidisplay.CabinEntity host,dev.omnidisplay.region.Enclosure.Result shape){this(sub,host,shape,shape,null);}
  Candidate(ClientSubLevel sub,dev.omnidisplay.CabinEntity host,dev.omnidisplay.region.Enclosure.Result shape,dev.omnidisplay.region.Enclosure.Result hudShape){this(sub,host,shape,hudShape,null);}
  Body body(){return new Body(sub,shape,moving,host.settings);}
 }

 private static List<Candidate> activeBodies=List.of();
 private static List<Body> bodyViews=List.of();
 private static Candidate primary;
 public static Matrix4d hudPose(float partial,boolean logical){return primary!=null&&primary.moving!=null?MovingScreenView.pose(primary.moving,primary.sub,partial,logical):logical?owner.logicalPose().bakeIntoMatrix(new Matrix4d()):owner.renderPose().bakeIntoMatrix(new Matrix4d());}
 private static final Map<BlockPos,Candidate> surfaces=new HashMap<>();private static dev.omnidisplay.region.Enclosure.Result hudShape;
 public static dev.omnidisplay.CabinSettings surfaceSettings(BlockPos p){var c=surfaces.get(p);return c!=null&&screen(p)?c.host.settings:settings();}
 public static dev.omnidisplay.region.Enclosure.Result surfaceShape(BlockPos p){var c=surfaces.get(p);return c!=null&&screen(p)?c.shape:automatic;}
 public record Body(ClientSubLevel sub,dev.omnidisplay.region.Enclosure.Result shape,com.simibubi.create.content.contraptions.AbstractContraptionEntity moving,dev.omnidisplay.CabinSettings settings){
  public Matrix4d pose(float partial){return moving==null?sub.renderPose().bakeIntoMatrix(new Matrix4d()):MovingScreenView.pose(moving,sub,partial,false);}
  public net.minecraft.world.level.Level level(){return moving==null?Minecraft.getInstance().level:moving.getContraption().getContraptionWorld();}
  public BlockPos origin(){return moving==null?sub.getPlot().getCenterBlock():BlockPos.ZERO;}
  public boolean inward(BlockPos pos,net.minecraft.core.Direction face){return moving==null?dev.omnidisplay.ScreenFaces.inward(level(),surfaceShape(pos),pos,face):MovingScreenView.inward(moving,shape,pos,face);}
 }
 public static List<Body> bodies(){if(emergency!=null&&owner!=null&&automatic!=null)return List.of(new Body(owner,automatic,null,emergency));return bodyViews;}
 public static boolean screen(BlockPos p){if(emergency!=null&&automatic!=null)return automatic.walls().contains(cell(p));var key=cell(p);for(var c:activeBodies)if(c.moving==null&&c.shape.walls().contains(key))return true;return false;}
 /** Use the same live face assignment for overlays, hidden hosts and the partition mask. */
 public static boolean screenFace(BlockPos p,net.minecraft.core.Direction face){var key=cell(p);for(var b:bodies())if(b.moving()==null&&b.shape().walls().contains(key)&&b.inward(p,face))return true;return false;}
 public static boolean panelScreen(BlockPos p){return dev.omnidisplay.PanelStore.mask(Minecraft.getInstance().level,p)!=0&&screen(p);}
 public static ClientSubLevel surfaceOwner(BlockPos p){var c=surfaces.get(p);return c==null?owner:c.sub;}
 public static boolean member(ClientSubLevel sub){for(var c:activeBodies)if(c.moving==null&&c.sub==sub)return true;return false;}
 private static boolean powered(Candidate c){return !c.sub.isRemoved()&&!c.host.isRemoved()&&(c.host.settings.power||c.host.settings.animating(world.getGameTime()));}
 private static List<Candidate> assembly(Candidate selected){
  if(!ServerRenderState.experimental||selected.host.settings.group==null)return List.of(selected);
  var reachable=Attachments.reachable(selected.sub);
  return candidates.stream().filter(c->selected.host.settings.group.equals(c.host.settings.group)&&reachable.contains(c.sub)&&(c.moving==null||!c.moving.isRemoved())).toList();
 }
 private static final List<Candidate> candidates=new ArrayList<>();
 private static dev.omnidisplay.CabinSettings emergency;
 private record Fault(ClientSubLevel sub,dev.omnidisplay.CabinSettings settings,dev.omnidisplay.region.Enclosure.Result shape){}
 private static final Map<UUID,Fault> faults=new HashMap<>();
 public static void fault(dev.omnidisplay.CabinPackets.Data data){
  var mc=Minecraft.getInstance();if(mc.level==null)return;var container=SubLevelContainer.getContainer(mc.level);if(container==null)return;
  var plot=container.getPlot(new ChunkPos(data.pos()));if(plot==null||!(plot.getSubLevel() instanceof ClientSubLevel sub))return;
  var settings=dev.omnidisplay.CabinSettings.load(data.tag(),mc.level.registryAccess());
  Set<dev.omnidisplay.region.Enclosure.Cell> walls=new HashSet<>(),inside=new HashSet<>();
  for(long v:data.tag().getLongArray("walls"))walls.add(cell(BlockPos.of(v)));
  for(long v:data.tag().getLongArray("inside"))inside.add(cell(BlockPos.of(v)));
  if(walls.isEmpty()||inside.isEmpty())return;
  int lx=Integer.MAX_VALUE,ly=lx,lz=lx,hx=Integer.MIN_VALUE,hy=hx,hz=hx;
  for(var c:walls){lx=Math.min(lx,c.x());ly=Math.min(ly,c.y());lz=Math.min(lz,c.z());hx=Math.max(hx,c.x());hy=Math.max(hy,c.y());hz=Math.max(hz,c.z());}
  var shape=new dev.omnidisplay.region.Enclosure.Result(Set.copyOf(walls),Set.copyOf(inside),new dev.omnidisplay.region.Enclosure.Cell(lx,ly,lz),new dev.omnidisplay.region.Enclosure.Cell(hx,hy,hz));
  faults.put(settings.id,new Fault(sub,settings,shape));
 }
 private static AABB region;
 
 private static dev.omnidisplay.region.Enclosure.Result automatic;
 private static long lastScan,lastVersion=-1;
 public static void refresh(){lastScan=-100;ScreenSurfaceCache.clear();}
 public static void invalidate(){ScreenSurfaceCache.clear();dev.omnidisplay.ScreenTopologyCache.invalidate(world);lastScan=-100;clear();}
 private static net.minecraft.client.multiplayer.ClientLevel world;
 private static ByteBufferBuilder memory;
 private static MultiBufferSource.BufferSource buffers;
 public static Object selection(){return region;}
 /** Interior bounds exclude the display shell. */
 public static AABB hudBounds(){
  return hudBounds(hudShape!=null?hudShape:automatic,settings());
 }
 private static dev.omnidisplay.region.Enclosure.Result boundsShape;
 private static net.minecraft.world.phys.Vec3 boundsCenter;
 private static BlockPos boundsOrigin;
 private static AABB boundsValue;
 public static AABB hudBounds(dev.omnidisplay.region.Enclosure.Result automatic,dev.omnidisplay.CabinSettings selected){
  if(automatic==null)return region;
  var center=selected==null?null:selected.hudCenter;var origin=selected==null?null:selected.origin;
  if(automatic==boundsShape&&Objects.equals(center,boundsCenter)&&Objects.equals(origin,boundsOrigin))return boundsValue;
  boundsShape=automatic;boundsCenter=center;boundsOrigin=origin;return boundsValue=calculateHudBounds(automatic,selected);
 }
 private static AABB calculateHudBounds(dev.omnidisplay.region.Enclosure.Result automatic,dev.omnidisplay.CabinSettings selected){
  if(automatic==null)return region;
  int x0=Integer.MAX_VALUE,y0=x0,z0=x0,x1=Integer.MIN_VALUE,y1=x1,z1=x1;
  for(var c:automatic.interior()){
   x0=Math.min(x0,c.x());y0=Math.min(y0,c.y());z0=Math.min(z0,c.z());
   x1=Math.max(x1,c.x());y1=Math.max(y1,c.y());z1=Math.max(z1,c.z());
  }
  var box=new AABB(x0,y0,z0,x1+1.0,y1+1.0,z1+1.0);
  if(automatic.open()){
   int lx=Integer.MAX_VALUE,ly=lx,lz=lx,hx=Integer.MIN_VALUE,hy=hx,hz=hx;
   for(var c:automatic.walls()){lx=Math.min(lx,c.x());ly=Math.min(ly,c.y());lz=Math.min(lz,c.z());hx=Math.max(hx,c.x());hy=Math.max(hy,c.y());hz=Math.max(hz,c.z());}
   double rx=(hx-lx+1)*.5,ry=(hy-ly+1)*.5,rz=(hz-lz+1)*.5;
   box=new AABB(automatic.centerX()-rx,automatic.centerY()-ry,automatic.centerZ()-rz,automatic.centerX()+rx,automatic.centerY()+ry,automatic.centerZ()+rz);
  }
  var settings=selected;if(settings!=null&&settings.hudCenter!=null&&settings.origin!=null){
   var target=settings.hudCenter.add(settings.origin.getX(),settings.origin.getY(),settings.origin.getZ());var old=box.getCenter();box=box.move(target.subtract(old));
  }
  return box;
 }

 public static ClientSubLevel owner(){return owner;}
 public static boolean keeps(BlockPos p){var key=cell(p);for(var c:activeBodies)if(c.moving==null&&(c.shape.interior().contains(key)||c.shape.walls().contains(key)))return true;return emergency!=null&&automatic!=null&&(automatic.interior().contains(key)||automatic.walls().contains(key));}
 private static final KeyMapping RESTORE=new KeyMapping("key.omni_display.restore",GLFW.GLFW_KEY_H,"key.categories.omni_display");
 private static void message(String text){var p=Minecraft.getInstance().player;if(p!=null)p.displayClientMessage(Component.literal(text),true);}
 private static void clear(){boundsShape=null;boundsValue=null;CabinContentsCache.clear();ScreenSurfaceCache.clear();ScreenAnimation.reset();HologramRenderer.clear();Attachments.reset();owner=null;primary=null;activeBodies=List.of();bodyViews=List.of();hudShape=null;min=max=null;host=null;emergency=null;region=null;automatic=null;if(memory!=null){memory.close();memory=null;buffers=null;}}
 private static boolean valid(){var mc=Minecraft.getInstance();return owner!=null&&mc.level==world&&!owner.isRemoved()&&(emergency!=null||host!=null&&!host.isRemoved()&&(primary==null||primary.moving==null||!primary.moving.isRemoved()));}
 public static boolean active(){
  if(automatic!=null&&automatic.open()&&!PartitionRenderer.enabled())return false;
  if(PartitionRenderer.normalPass()||(PartitionRenderer.enabled()&&!PartitionRenderer.available()))return false;
  if(!valid()||settings()==null||(!settings().power&&!settings().animating(world.getGameTime()))||region==null)return false;
  if(PartitionRenderer.enabled()&&PartitionRenderer.available())return true;
  return cameraInside();
 }
 /** Instruments and cockpit controls remain local to an observer inside the cabin. */
 public static boolean viewActive(){return active()&&cameraInside();}
 private static boolean cameraInside(){
  var mc=Minecraft.getInstance();
  if(mc.options.getCameraType().isFirstPerson())return emergency!=null?insideLogicalView(owner,automatic,region):activeBodies.stream().anyMatch(DisplayRenderer::insideCandidate);
  // Detached third-person cameras must still actually be inside the cabin.
  var cam=mc.gameRenderer.getMainCamera().getPosition();
  if(emergency!=null)return contains(automatic,region,owner.renderPose().transformPositionInverse(new Vector3d(cam.x,cam.y,cam.z)));
  return activeBodies.stream().anyMatch(c->contains(c.shape,null,dev.omnidisplay.region.ScreenCoordinates.localEye(c.body().pose(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)),new Vector3d(cam.x,cam.y,cam.z))));
 }
 private static boolean contains(dev.omnidisplay.region.Enclosure.Result shape,AABB bounds,Vector3d point){
  if(point==null||!Double.isFinite(point.x)||!Double.isFinite(point.y)||!Double.isFinite(point.z))return false;
  if(shape==null)return bounds!=null&&bounds.contains(point.x,point.y,point.z);
  var p=BlockPos.containing(point.x,point.y,point.z);if(shape.interior().contains(cell(p)))return true;
  // The empty portion of a boundary cell remains usable on the cabin side of a thin sheet.
  var level=Minecraft.getInstance().level;if(level==null||!shape.walls().contains(cell(p)))return false;
  var state=level.getBlockState(p);if(!state.is(OmniDisplay.THIN_DISPLAY.get())&&!state.is(OmniDisplay.TRAPDOOR.get()))return false;
  for(var face:net.minecraft.core.Direction.values()){
   if(!shape.interior().contains(cell(p.relative(face)))||!dev.omnidisplay.ScreenFaces.supportsFace(level,p,face))continue;
   double value=switch(face.getAxis()){case X->point.x-p.getX();case Y->point.y-p.getY();case Z->point.z-p.getZ();};
   if(state.is(OmniDisplay.TRAPDOOR.get())){double plane=dev.omnidisplay.CockpitTrapdoor.plane(state,face);if(face.getAxisDirection()==net.minecraft.core.Direction.AxisDirection.POSITIVE?value>=plane:value<=plane)return true;continue;}
   if(dev.omnidisplay.region.ThinGeometry.inside(state.getValue(dev.omnidisplay.ThinDisplayBlock.ATTACHED_FACE).getAxisDirection()==net.minecraft.core.Direction.AxisDirection.POSITIVE,face.getAxisDirection()==net.minecraft.core.Direction.AxisDirection.POSITIVE,value))return true;
  }
  return false;
 }
 private static boolean insideCandidate(Candidate c){
  if(c.moving==null)return insideLogicalView(c.sub,c.shape,null);
  var mc=Minecraft.getInstance();var entity=mc.getCameraEntity();if(entity==null||c.moving.isRemoved())return false;
  var eye=entity.getEyePosition();var point=new Vector3d(eye.x,eye.y,eye.z);var plot=SubLevelContainer.getContainer(mc.level).getPlot(new ChunkPos(BlockPos.containing(eye)));if(plot!=null)plot.getSubLevel().logicalPose().transformPosition(point);
  return contains(c.shape,null,dev.omnidisplay.region.ScreenCoordinates.localEye(MovingScreenView.pose(c.moving,c.sub,1,true),point));
 }
 /** Match simulation coordinates with logicalPose, never with last frame's camera/renderPose. */
 private static boolean insideLogicalView(ClientSubLevel sub,dev.omnidisplay.region.Enclosure.Result shape,AABB bounds){
  var mc=Minecraft.getInstance();var view=mc.getCameraEntity();if(view==null||mc.level==null)return false;
  var eye=view.getEyePosition();var container=SubLevelContainer.getContainer(mc.level);if(container==null)return false;
  // Seated/local entities may already live in the sublevel's storage coordinates.
  var plot=container.getPlot(new ChunkPos(BlockPos.containing(eye.x,eye.y,eye.z)));
  if(plot!=null&&plot.getSubLevel()!=sub){var global=plot.getSubLevel().logicalPose().transformPosition(new Vector3d(eye.x,eye.y,eye.z));return contains(shape,bounds,sub.logicalPose().transformPositionInverse(global));}
  var local=dev.omnidisplay.region.CabinViewCoordinates.localEye(eye.x,eye.y,eye.z,plot!=null,sub.logicalPose()::transformPositionInverse);
  return contains(shape,bounds,local);
 }
 public static Iterable<ClientSubLevel> visible(Iterable<ClientSubLevel> original){
  if(!active())return original;
  List<ClientSubLevel> result=new ArrayList<>();for(var s:original)if(!member(s)&&s!=owner&&!Attachments.connected(s))result.add(s);return result;
 }
 private static dev.omnidisplay.region.Enclosure.Cell cell(BlockPos p){return new dev.omnidisplay.region.Enclosure.Cell(p.getX(),p.getY(),p.getZ());}
 public static dev.omnidisplay.CabinSettings settings(){return emergency!=null?emergency:host==null?null:host.settings;}
 public static dev.omnidisplay.region.Enclosure.Result enclosure(){return automatic;}
 public static void align(int heading){if(primary!=null&&primary.moving!=null)net.neoforged.neoforge.network.PacketDistributor.sendToServer(new dev.omnidisplay.MovingScreens.Action(primary.moving.getId(),7,heading));else if(host!=null)net.neoforged.neoforge.network.PacketDistributor.sendToServer(new dev.omnidisplay.CabinPackets.Action(host.getBlockPos(),7,heading));}
 public static BlockPos anchor(){return host==null?null:host.getBlockPos();}
 public static float opacity(){return settings()==null?.1f:settings().opacity/100f;}
 private static boolean inView(Candidate candidate){
  var frustum=Minecraft.getInstance().levelRenderer.getFrustum();if(frustum==null)return true;
  var lo=candidate.shape.min();var hi=candidate.shape.max();
  double x0=Double.POSITIVE_INFINITY,y0=x0,z0=x0,x1=Double.NEGATIVE_INFINITY,y1=x1,z1=x1;
  var transform=candidate.body().pose(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
  // Bound all eight transformed corners; a rotated ship cannot use its storage-space AABB.
  for(int i=0;i<8;i++){
   var corner=transform.transformPosition(new Vector3d((i&1)==0?lo.x():hi.x()+1d,(i&2)==0?lo.y():hi.y()+1d,(i&4)==0?lo.z():hi.z()+1d));
   x0=Math.min(x0,corner.x);y0=Math.min(y0,corner.y);z0=Math.min(z0,corner.z);
   x1=Math.max(x1,corner.x);y1=Math.max(y1,corner.y);z1=Math.max(z1,corner.z);
  }
  return frustum.isVisible(new AABB(x0,y0,z0,x1,y1,z1));
 }
 private static void discover(){
  long omni$perf=dev.omnidisplay.diagnostics.PerformanceDiagnostics.begin(dev.omnidisplay.diagnostics.PerformanceDiagnostics.Phase.DISCOVERY);try{

  candidates.clear();surfaces.clear();var mc=Minecraft.getInstance();if(mc.level==null)return;
  var container=SubLevelContainer.getContainer(mc.level);if(container==null)return;
  for(var sub:container.getAllSubLevels())if(sub instanceof ClientSubLevel client){
   Set<BlockPos> seen=new HashSet<>();
   var allHosts=new ArrayList<net.minecraft.world.level.block.entity.BlockEntity>();
   for(var holder:client.getPlot().getLoadedChunks()){allHosts.addAll(holder.getChunk().getBlockEntities().values());allHosts.addAll(dev.omnidisplay.PanelStore.hosts(mc.level,holder.getChunk().getPos()));}
   for(var be:allHosts)
    if(be instanceof dev.omnidisplay.CabinEntity h&&!h.isRemoved()&&h.settings.id!=null&&!seen.contains(h.getBlockPos())){
     try{
      var shape=dev.omnidisplay.ScreenTopologyCache.scan(mc.level,h.getBlockPos());
      for(var c:shape.walls())seen.add(new BlockPos(c.x(),c.y(),c.z()));
      candidates.add(new Candidate(client,h,shape));
     }catch(IllegalArgumentException ignored){if(ignored.getMessage()!=null&&(ignored.getMessage().contains("4096")||ignored.getMessage().contains("过大")))return;}
    }
  }
  if(ServerRenderState.experimental)candidates.addAll(MovingScreenView.candidates().stream().map(v->new Candidate(v.sub(),v.host(),v.shape(),v.shape(),v.entity())).toList());
  for(var c:candidates)if(c.moving==null)for(var wall:c.shape.walls())surfaces.put(new BlockPos(wall.x(),wall.y(),wall.z()),c);
  if(ServerRenderState.experimental){
   var original=new ArrayList<>(candidates);candidates.clear();Set<Candidate> seenGroups=new HashSet<>();
   for(var c:original){if(!seenGroups.add(c))continue;var group=c.host.settings.group;
    var members=group==null?List.of(c):original.stream().filter(n->n.sub==c.sub&&n.moving==c.moving&&group.equals(n.host.settings.group)).toList();seenGroups.addAll(members);
    if(members.size()==1){candidates.add(c);continue;}
    var dominant=members.stream().sorted(Comparator.<Candidate>comparingInt(n->n.shape.walls().size()).reversed().thenComparing(n->n.host.getBlockPos())).findFirst().orElse(c);
    try{var merged=dev.omnidisplay.region.GroupedCabin.combine(members.stream().map(n->n.shape).toList(),dominant.shape);candidates.add(new Candidate(c.sub,dominant.host,merged,dominant.shape));}catch(IllegalArgumentException tooLarge){candidates.addAll(members);}
   }
  }
 
  }finally{dev.omnidisplay.diagnostics.PerformanceDiagnostics.end(dev.omnidisplay.diagnostics.PerformanceDiagnostics.Phase.DISCOVERY,omni$perf);}
 }
 @SubscribeEvent public static void tick(ClientTickEvent.Post event){
  var mc=Minecraft.getInstance();
  if(world!=mc.level){clear();candidates.clear();faults.clear();world=mc.level;lastScan=-100;}
  if(world==null||mc.player==null){Attachments.update();return;}
  if(world.getGameTime()-lastScan>=10||lastVersion!=dev.omnidisplay.ScreenTopologyCache.version(world)){discover();lastScan=world.getGameTime();lastVersion=dev.omnidisplay.ScreenTopologyCache.version(world);}
  Candidate chosen=null;
  faults.values().removeIf(f->!f.settings.animating(world.getGameTime())||f.sub.isRemoved());
  Fault fault=null;for(var f:faults.values()){if(insideLogicalView(f.sub,f.shape,null)){fault=f;break;}}
  for(var c:candidates){
   if(c.sub.isRemoved()||c.host.isRemoved()||c.moving!=null&&c.moving.isRemoved())continue;
   if(insideCandidate(c)&&(chosen==null||(!chosen.host.settings.power&&c.host.settings.power)))chosen=c;
  }
  // Outside observers still see the nearest powered cabin's inward-facing displays.
  // The depth-tested per-face mask, rather than observer membership, controls the actual pixels.
  if(chosen==null&&fault==null&&PartitionRenderer.enabled()&&PartitionRenderer.available()){
   var camera=mc.gameRenderer.getMainCamera().getPosition();
   double best=Math.pow(mc.options.getEffectiveRenderDistance()*16d,2);
   for(var c:candidates){
    if(c.sub.isRemoved()||c.host.isRemoved()||c.moving!=null&&c.moving.isRemoved()||(!c.host.settings.power&&!c.host.settings.animating(world.getGameTime())))continue;
    if(!inView(c))continue;
    var local=dev.omnidisplay.region.ScreenCoordinates.localEye(c.body().pose(mc.getTimer().getGameTimeDeltaPartialTick(false)),new Vector3d(camera.x,camera.y,camera.z));
    var lo=c.shape.min();var hi=c.shape.max();
    double distance=dev.omnidisplay.region.PartitionSurface.distanceSquared(local.x,local.y,local.z,lo.x(),lo.y(),lo.z(),hi.x()+1d,hi.y()+1d,hi.z()+1d);
    if(distance<best){best=distance;chosen=c;}
   }
  }
  List<Candidate> selectedBodies=chosen==null?List.of():assembly(chosen);
  if(!selectedBodies.isEmpty())chosen=selectedBodies.stream().sorted(Comparator.<Candidate>comparingInt(c->c.hudShape.walls().size()).reversed().thenComparing(c->c.host.getBlockPos())).findFirst().orElse(chosen);
  if(fault!=null){
   if(owner!=fault.sub||!fault.shape.equals(automatic)){clear();owner=fault.sub;automatic=fault.shape;var lo=automatic.min();var hi=automatic.max();min=new BlockPos(lo.x(),lo.y(),lo.z());max=new BlockPos(hi.x(),hi.y(),hi.z());region=new AABB(min.getX(),min.getY(),min.getZ(),max.getX()+1d,max.getY()+1d,max.getZ()+1d);}
   emergency=fault.settings;host=null;
  }
  else if(chosen==null){if(owner!=null)clear();}
  else{
   if(owner!=chosen.sub||!chosen.shape.equals(automatic)){
    clear();owner=chosen.sub;automatic=chosen.shape;
    var lo=automatic.min();var hi=automatic.max();min=new BlockPos(lo.x(),lo.y(),lo.z());max=new BlockPos(hi.x(),hi.y(),hi.z());
    region=new AABB(min.getX(),min.getY(),min.getZ(),max.getX()+1.0,max.getY()+1.0,max.getZ()+1.0);
   }
   host=chosen.host;primary=chosen;hudShape=chosen.hudShape;emergency=null;
   if(!activeBodies.equals(selectedBodies))ScreenSurfaceCache.clear();activeBodies=List.copyOf(selectedBodies);bodyViews=activeBodies.stream().map(c->c.body()).toList();
  }
  while(RESTORE.consumeClick())if(mc.screen==null&&host!=null&&viewActive())
   {if(primary!=null&&primary.moving!=null)MovingScreenView.stop(primary.moving);else net.neoforged.neoforge.network.PacketDistributor.sendToServer(new dev.omnidisplay.CabinPackets.Action(host.getBlockPos(),2,0));}
  Attachments.update();
 }
 @SubscribeEvent public static void render(RenderLevelStageEvent event){
  if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES||!active())return;
  var mc=Minecraft.getInstance();
  if(memory==null){memory=new ByteBufferBuilder(262144);buffers=MultiBufferSource.immediate(memory);}
  var camera=event.getCamera().getPosition();
  for(var body:bodies()){if(body.moving()!=null)continue;var owner=body.sub();var min=new BlockPos(body.shape().min().x(),body.shape().min().y(),body.shape().min().z());var max=new BlockPos(body.shape().max().x(),body.shape().max().y(),body.shape().max().z());
  // Convert huge plot coordinates to a nearby origin before converting to floats.
  Matrix4d transform=new Matrix4d().translation(-camera.x,-camera.y,-camera.z)
    .mul(owner.renderPose().bakeIntoMatrix(new Matrix4d())).translate(min.getX(),min.getY(),min.getZ());
  // AFTER_BLOCK_ENTITIES already installs the camera model-view in RenderSystem.
  // Buffered entity shaders apply it once; vertices must remain camera-relative.
  PoseStack pose=new PoseStack();pose.mulPose(new Matrix4f(transform));
  try {
   for(var p:CabinContentsCache.positions(body)){
    if(!keeps(p)||settings().highlightAt(owner.getUniqueId(),p,owner.getPlot().getCenterBlock(),true)>=0)continue;
    var state=world.getBlockState(p);if(OmniDisplay.isScreen(state)||panelScreen(p))continue;if(state.getRenderShape()!=RenderShape.MODEL)continue;
    pose.pushPose();pose.translate(p.getX()-min.getX(),p.getY()-min.getY(),p.getZ()-min.getZ());
    try{HighlightModels.render(world,p,state,pose,buffers,LevelRenderer.getLightColor(world,p));}
    finally{pose.popPose();}
   }
  }finally{buffers.endBatch();}
  }
 }
 @EventBusSubscriber(modid=OmniDisplay.ID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
 public static final class Keys {@SubscribeEvent public static void register(RegisterKeyMappingsEvent e){e.register(RESTORE);PartitionRenderer.registerKeys(e);}}
}
