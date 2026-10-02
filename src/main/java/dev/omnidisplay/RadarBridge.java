package dev.omnidisplay;
import java.util.*;
import com.happysg.radar.block.datalink.*;
import com.happysg.radar.block.controller.networkcontroller.NetworkFiltererBlockEntity;
import com.happysg.radar.block.behavior.networks.NetworkData;
import com.happysg.radar.block.behavior.networks.config.DetectionConfig;
import com.happysg.radar.block.radar.behavior.IRadar;
import com.happysg.radar.block.radar.track.RadarTrack;
import com.happysg.radar.compat.vs2.SableUtils;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3d;
/** Radar remains authoritative; this bridge contributes one cockpit terminal per filterer. */
public final class RadarBridge {
 private static final String OWNER="omni_cabin",LINK="omni_link_offset";
 private static final java.lang.reflect.Field SAFE_ZONES;
 static {java.lang.reflect.Field field=null;try{field=NetworkFiltererBlockEntity.class.getDeclaredField("safeZones");field.setAccessible(true);}catch(ReflectiveOperationException ignored){}SAFE_ZONES=field;}
 private static void notice(ServerPlayer p,String text){p.displayClientMessage(Component.literal(text),true);}
 public static void place(PlayerInteractEvent.RightClickBlock e){
  if(!(e.getItemStack().getItem() instanceof DataLinkBlockItem item))return;
  e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);
  if(e.getLevel().isClientSide||!(e.getEntity() instanceof ServerPlayer p))return;
  var level=p.serverLevel();var stack=e.getItemStack();var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
  var target=NbtUtils.readBlockPos(tag,"SelectedFiltererPos").orElse(null);
  if(target==null||!(level.getBlockEntity(target) instanceof NetworkFiltererBlockEntity controller)){notice(p,"请先按雷达原版操作，用数据链接器配对网络控制器。");return;}
  CabinManager.rebuild(level);var c=CabinManager.find(level,e.getPos());if(c==null){notice(p,"请先围成完整座舱。");return;}
  controller(c);
  if(c.settings.radarLink!=null){notice(p,"此座舱已有雷达连接，请先拆掉旧链接器。");return;}
  var claim=controller.getPersistentData();
  if(claim.hasUUID(OWNER)){
   var old=controller.getBlockPos().offset(BlockPos.of(claim.getLong(LINK)));
   if(!level.hasChunkAt(old)||level.getBlockEntity(old) instanceof DataLinkBlockEntity){notice(p,"此网络控制器已连接一个座舱。");return;}
   claim.remove(OWNER);claim.remove(LINK);
  }
  var ctx=new BlockPlaceContext(new UseOnContext(p,e.getHand(),e.getHitVec()));var linkPos=ctx.getClickedPos();
  double range=com.happysg.radar.config.RadarConfig.server().radarLinkRange.get();
  if(SableUtils.getWorldVec(level,linkPos).distanceTo(SableUtils.getWorldVec(level,target))>range){notice(p,"超过雷达数据链接距离。");return;}
  var result=item.place(ctx);if(!result.consumesAction()||!(level.getBlockEntity(linkPos) instanceof DataLinkBlockEntity link)){notice(p,"链接器放置失败，请确认表面旁有空间。");return;}
  link.target(target);link.getPersistentData().putUUID(OWNER,c.settings.id);link.notifyUpdate();
  var state=level.getBlockState(linkPos);if(state.hasProperty(DataLinkBlock.LINK_STYLE))level.setBlock(linkPos,state.setValue(DataLinkBlock.LINK_STYLE,DataLinkBlock.LinkStyle.RADAR),3);
  claim.putUUID(OWNER,c.settings.id);claim.putLong(LINK,linkPos.subtract(target).asLong());controller.setChanged();
  c.settings.radarLink=linkPos.subtract(c.settings.origin);c.settings.radarController=target.subtract(c.settings.origin);c.publish();
  tag.remove("SelectedFiltererPos");stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
  notice(p,"座舱已连接雷达网络。开机后空手右键外部已探测目标锁定。");
 }
 // Resolve physical attachment, not an expendable cabin UUID or the link's LED.
 private static DataLinkBlockEntity attachedLink(CabinManager.Cabin c){
  if(c.settings.radarLink!=null){var pos=c.settings.origin.offset(c.settings.radarLink);
   if(c.level.hasChunkAt(pos)&&c.level.getBlockEntity(pos) instanceof DataLinkBlockEntity link
     &&c.shape.walls().contains(CabinManager.cell(pos.relative(link.getDirection()))))return link;
  }
  for(var wall:c.shape.walls())for(var direction:Direction.values()){
   var pos=new BlockPos(wall.x(),wall.y(),wall.z()).relative(direction);
   if(c.level.hasChunkAt(pos)&&c.level.getBlockEntity(pos) instanceof DataLinkBlockEntity link
     &&pos.relative(link.getDirection()).equals(new BlockPos(wall.x(),wall.y(),wall.z())))return link;
  }
  return null;
 }
 private static NetworkFiltererBlockEntity controller(CabinManager.Cabin c){
  var link=attachedLink(c);
  if(link==null){
   if(c.settings.radarLink!=null){var pos=c.settings.origin.offset(c.settings.radarLink);
    if(c.level.hasChunkAt(pos)&&!(c.level.getBlockState(pos).getBlock() instanceof DataLinkBlock))disconnect(c);
   }
   return null;
  }
  var pos=link.getBlockPos();var offset=pos.subtract(c.settings.origin);boolean changed=!offset.equals(c.settings.radarLink);
  c.settings.radarLink=offset;
  // The radar accessor can return transformed world coordinates on moving ships.
  // Prefer our known block-storage position, retained relative to the cabin.
  NetworkFiltererBlockEntity f=null;
  if(c.settings.radarController!=null){var saved=c.settings.origin.offset(c.settings.radarController);
   if(c.level.hasChunkAt(saved)&&c.level.getBlockEntity(saved) instanceof NetworkFiltererBlockEntity found)f=found;
  }
  if(f==null){var raw=NbtUtils.readBlockPos(link.saveWithoutMetadata(c.level.registryAccess()),"TargetOffset").orElse(null);
   if(raw!=null){var target=pos.offset(raw);if(c.level.hasChunkAt(target)&&c.level.getBlockEntity(target) instanceof NetworkFiltererBlockEntity found)f=found;}
  }
  if(f==null){var target=link.getTargetPosition();
   if(target!=null&&c.level.hasChunkAt(target)&&c.level.getBlockEntity(target) instanceof NetworkFiltererBlockEntity found)f=found;
  }
  if(f==null){if(changed)c.publish();return null;}
  var claim=f.getPersistentData();
  if(claim.hasUUID(OWNER)){
   var old=f.getBlockPos().offset(BlockPos.of(claim.getLong(LINK)));
   if(!old.equals(pos)&&(!c.level.hasChunkAt(old)||c.level.getBlockEntity(old) instanceof DataLinkBlockEntity)){
    if(changed)c.publish();return null;
   }
  }
  claim.putUUID(OWNER,c.settings.id);claim.putLong(LINK,pos.subtract(f.getBlockPos()).asLong());f.setChanged();
  if(!link.getPersistentData().hasUUID(OWNER)||!link.getPersistentData().getUUID(OWNER).equals(c.settings.id)){
   link.getPersistentData().putUUID(OWNER,c.settings.id);link.setChanged();
  }
  var relative=f.getBlockPos().subtract(c.settings.origin);changed|=!relative.equals(c.settings.radarController);
  c.settings.radarController=relative;if(changed)c.publish();return f;
 }
 public static void disconnect(CabinManager.Cabin c){
  if(c.settings.radarController!=null){var pos=c.settings.origin.offset(c.settings.radarController);if(c.level.hasChunkAt(pos)&&c.level.getBlockEntity(pos) instanceof NetworkFiltererBlockEntity f){var t=f.getPersistentData();if(t.hasUUID(OWNER)&&t.getUUID(OWNER).equals(c.settings.id)){t.remove(OWNER);t.remove(LINK);f.setChanged();}}}
  if(c.settings.radarLink!=null){var p=c.settings.origin.offset(c.settings.radarLink);if(c.level.hasChunkAt(p)&&c.level.getBlockEntity(p) instanceof DataLinkBlockEntity link){link.getPersistentData().remove(OWNER);link.setChanged();}}
  c.settings.radarLink=null;c.settings.radarController=null;c.publish();
 }
 private static List<RadarTrack> tracks(ServerLevel level,NetworkFiltererBlockEntity f){
  var group=NetworkData.get(level).getGroup(level.dimension(),f.getBlockPos());
  if(group==null||group.radarPos==null||!level.hasChunkAt(group.radarPos)||!(level.getBlockEntity(group.radarPos) instanceof IRadar radar)||!radar.isRunning())return List.of();
  var filter=group.detectionTag==null?DetectionConfig.DEFAULT:DetectionConfig.fromTag(group.detectionTag);
  return radar.getTracks().stream().filter(Objects::nonNull).filter(filter::test).toList();
 }
 public static boolean inside(ServerPlayer p,CabinManager.Cabin c){
  if(!c.settings.power||c.damaged)return false;
  var local=new Vector3d(p.getX(),p.getEyeY(),p.getZ());var container=SubLevelContainer.getContainer(p.serverLevel());var plot=container.getPlot(new ChunkPos(c.anchor.getBlockPos()));
  if(plot==null)return false;plot.getSubLevel().logicalPose().transformPositionInverse(local);
  return c.shape.interior().contains(CabinManager.cell(BlockPos.containing(local.x,local.y,local.z)));
 }
 public static void tick(CabinManager.Cabin c){
  long omni$perf=dev.omnidisplay.diagnostics.PerformanceDiagnostics.begin(dev.omnidisplay.diagnostics.PerformanceDiagnostics.Phase.RADAR);try{

  if(c.level.getGameTime()%4!=0)return;
  var f=controller(c);var level=(ServerLevel)c.level;
  var tag=new CompoundTag();tag.putBoolean("radarStatus",true);tag.putUUID("cabin",c.settings.id);tag.putBoolean("linked",f!=null);
  if(f!=null){var group=NetworkData.get(level).getGroup(level.dimension(),f.getBlockPos());if(group!=null&&group.selectedTargetId!=null){
   for(var track:tracks(level,f))if(group.selectedTargetId.equals(track.id())){var pos=track.position();if(!"Sable:ship".equals(track.entityType()))pos=pos.add(0,track.getEnityHeight()*.5,0);tag.putString("id",track.id());tag.putDouble("x",pos.x);tag.putDouble("y",pos.y);tag.putDouble("z",pos.z);break;}
  }}
  for(var p:level.players())if(inside(p,c))PacketDistributor.sendToPlayer(p,new CabinPackets.Data(c.anchor.getBlockPos(),false,tag));
 
  }finally{dev.omnidisplay.diagnostics.PerformanceDiagnostics.end(dev.omnidisplay.diagnostics.PerformanceDiagnostics.Phase.RADAR,omni$perf);}
 }
 private static final Map<ServerPlayer,Long> clicks=new WeakHashMap<>();
 public static void select(ServerPlayer p,CabinManager.Cabin c,CabinPackets.Aim aim){
  if(!c.settings.mouseLock||!inside(p,c)||!p.getMainHandItem().isEmpty()||p.isShiftKeyDown())return;
  if(!c.shape.walls().contains(CabinManager.cell(aim.surface()))||!ScreenFaces.isScreen(c.level,aim.surface()))return;
  if(!dev.omnidisplay.region.RadarAimMath.valid(aim.eye().x,aim.eye().y,aim.eye().z,aim.direction().x,aim.direction().y,aim.direction().z)
    ||aim.eye().distanceToSqr(p.getEyePosition())>256)return;
  long now=c.level.getGameTime();long elapsed=now-clicks.getOrDefault(p,now-10);if(elapsed>=0&&elapsed<4)return;clicks.put(p,now);
  var f=controller(c);if(f==null){notice(p,"数据链接器对应的网络控制器暂不可用，请检查链接器配对和控制器。");return;}
  var level=p.serverLevel();var start=aim.eye();var end=start.add(aim.direction().normalize().scale(8192));
  var ignore=family(level,c);RadarTrack chosen=null;double nearest=8192;
  var detected=tracks(level,f);
  for(var track:detected){
   if(track.id().equals(p.getUUID().toString()))continue;
   double distance=hit(level,track,start,end,ignore);if(distance<nearest){nearest=distance;chosen=track;}
  }
  if(chosen==null){long ships=detected.stream().filter(RadarBridge::isShip).count();
   notice(p,detected.isEmpty()?"雷达网络当前没有有效探测目标。":"未瞄准已探测目标（结构 "+ships+"，其他 "+(detected.size()-ships)+"）。");return;}
  try{if(SAFE_ZONES==null)throw new IllegalAccessException("safeZones");
   @SuppressWarnings("unchecked") var zones=new ArrayList<AABB>((List<AABB>)SAFE_ZONES.get(f));
   f.receiveSelectedTargetFromMonitor(chosen,zones);notice(p,"已提交雷达锁定目标。");
  }catch(ReflectiveOperationException ex){notice(p,"雷达版本接口不匹配，未修改锁定目标。");}
  tick(c);
 }
 private static Set<SubLevel> family(ServerLevel level,CabinManager.Cabin c){
  var container=SubLevelContainer.getContainer(level);var plot=container.getPlot(new ChunkPos(c.anchor.getBlockPos()));Set<SubLevel> found=new HashSet<>();if(plot==null)return found;
  Map<SubLevel,Set<SubLevel>> edges=new HashMap<>();for(var s:container.getAllSubLevels())for(var actor:s.getPlot().getBlockEntityActors()){var deps=actor.sable$getConnectionDependencies();if(deps==null)continue;for(var dep:deps)if(dep instanceof SubLevel child){edges.computeIfAbsent(s,k->new HashSet<>()).add(child);edges.computeIfAbsent(child,k->new HashSet<>()).add(s);}}
  var todo=new ArrayDeque<SubLevel>();todo.add(plot.getSubLevel());while(!todo.isEmpty()){var s=todo.remove();if(found.add(s))todo.addAll(edges.getOrDefault(s,Set.of()));}return found;
 }
 private static boolean isShip(RadarTrack track){return track.trackCategory()==com.happysg.radar.block.radar.track.TrackCategory.SABLE||"Sable:ship".equalsIgnoreCase(track.entityType());}
 private static double hit(ServerLevel level,RadarTrack track,Vec3 a,Vec3 b,Set<SubLevel> ignore){
  UUID id;try{id=UUID.fromString(track.id());}catch(IllegalArgumentException ex){return Double.POSITIVE_INFINITY;}
  if(isShip(track)){
   var sub=SubLevelContainer.getContainer(level).getSubLevel(id);
   if(sub!=null)return ignore.contains(sub)?Double.POSITIVE_INFINITY:shipHit(level,sub,a,b);
   // A valid radar track may outlive the loaded sublevel: aim at its reported position.
   var center=track.position();double pad=Math.max(1.5,a.distanceTo(center)*.02);
   return boxHit(new AABB(center.x-pad,center.y-pad,center.z-pad,center.x+pad,center.y+pad,center.z+pad),a,b);
  }
  var e=level.getEntity(id);if(!(e instanceof net.minecraft.world.entity.LivingEntity)||!e.isAlive())return Double.POSITIVE_INFINITY;
  return e.getBoundingBox().inflate(.5).clip(a,b).map(a::distanceTo).orElse(Double.POSITIVE_INFINITY);
 }
 private static double boxHit(AABB box,Vec3 a,Vec3 b){
  return dev.omnidisplay.region.RadarAimMath.distance(a.x,a.y,a.z,b.x,b.y,b.z,box.minX,box.minY,box.minZ,box.maxX,box.maxY,box.maxZ);
 }
 private static double shipHit(ServerLevel level,SubLevel sub,Vec3 a,Vec3 b){
  var bounds=sub.boundingBox();var box=new AABB(bounds.minX(),bounds.minY(),bounds.minZ(),bounds.maxX(),bounds.maxY(),bounds.maxZ());
  double pad=Math.max(.5,a.distanceTo(box.getCenter())*.015);
  return boxHit(box.inflate(pad),a,b);
 }
}

