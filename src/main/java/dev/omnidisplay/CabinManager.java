package dev.omnidisplay;

import java.util.*;
import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import net.createmod.catnip.data.Couple;
import dev.omnidisplay.region.Enclosure;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.joml.Vector3d;

@EventBusSubscriber(modid=OmniDisplay.ID)
public final class CabinManager {
 private static final Map<Level,State> worlds=new IdentityHashMap<>();
 private static class State{Set<CabinEntity> hosts=Collections.newSetFromMap(new IdentityHashMap<>());List<Cabin> cabins=new ArrayList<>();long scanned=-100;}
 public static void add(CabinEntity be){worlds.computeIfAbsent(be.getLevel(),l->new State()).hosts.add(be);}
 public static void remove(CabinEntity be){if(be.getLevel()==null||be.getLevel().isClientSide)return;var s=worlds.get(be.getLevel());if(s!=null)s.hosts.remove(be);}
 public static Enclosure.Cell cell(BlockPos p){return new Enclosure.Cell(p.getX(),p.getY(),p.getZ());}
 public static final class Cabin {
  final Level level;CabinEntity anchor;List<CabinEntity> members;public Enclosure.Result shape;public CabinSettings settings;Receiver receiver,mouseReceiver;boolean damaged;Boolean doorsLocked;com.simibubi.create.content.contraptions.AbstractContraptionEntity moving;
  Cabin(Level l,CabinEntity a,Enclosure.Result r,CabinSettings s){level=l;anchor=a;shape=r;settings=s;}
  void publish(){settings.revision=Math.max(settings.revision+1,System.currentTimeMillis());for(var be:members)if(!be.isRemoved())be.apply(settings);locks(settings.power||settings.animating(level.getGameTime()));}
  void locks(boolean locked){
   if(moving!=null)return;
   // Experimental displays remain powered while entrances are open.
   locked=dev.omnidisplay.region.RenderRules.locks(RenderConfig.experimental(),locked);
   if(doorsLocked!=null&&doorsLocked==locked)return;doorsLocked=locked;
   for(var cell:shape.walls()){var pos=new BlockPos(cell.x(),cell.y(),cell.z());var state=level.getBlockState(pos);
    if(state.is(OmniDisplay.DOOR.get())&&state.getValue(CockpitDoor.LOCKED)!=locked){level.setBlock(pos,state.setValue(CockpitDoor.LOCKED,locked),2);if(!locked)OmniDisplay.DOOR.get().neighborChanged(level.getBlockState(pos),level,pos,net.minecraft.world.level.block.Blocks.AIR,pos,false);}
    if(state.is(OmniDisplay.TRAPDOOR.get())&&state.getValue(CockpitDoor.LOCKED)!=locked){level.setBlock(pos,state.setValue(CockpitDoor.LOCKED,locked),2);if(!locked)OmniDisplay.TRAPDOOR.get().neighborChanged(level.getBlockState(pos),level,pos,net.minecraft.world.level.block.Blocks.AIR,pos,false);}
   }
  }
  void stop(){if(receiver!=null){receiver.latch.stop();receiver.alive=false;Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(level,receiver);receiver=null;}if(mouseReceiver!=null){mouseReceiver.latch.stop();mouseReceiver.alive=false;Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(level,mouseReceiver);mouseReceiver=null;}}
  void radio(){stop();if(moving!=null)return;if(settings.wireless&&!damaged){receiver=new Receiver(this,false);Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(level,receiver);receiver.latch.arm();}if(settings.mouseWireless&&!damaged){mouseReceiver=new Receiver(this,true);Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(level,mouseReceiver);mouseReceiver.latch.arm();}}
 }
 private static final class Receiver implements IRedstoneLinkable {
  final Cabin cabin;final Couple<Frequency> key;final dev.omnidisplay.region.PulseLatch latch=new dev.omnidisplay.region.PulseLatch();boolean alive=true;
  Receiver(Cabin c,boolean mouse){cabin=c;key=Couple.create(Frequency.of(mouse?c.settings.mouseFirst:c.settings.first),Frequency.of(mouse?c.settings.mouseSecond:c.settings.second));}
  public int getTransmittedStrength(){return 0;}
  public void setReceivedStrength(int n){latch.sample(n);}
  public boolean isListening(){return true;}
  public boolean isAlive(){return alive&&!cabin.anchor.isRemoved();}
  public Couple<Frequency> getNetworkKey(){return key;}
  public BlockPos getLocation(){return cabin.anchor.getBlockPos();}
 }
 public static void rebuild(Level level){
  long omni$perf=dev.omnidisplay.diagnostics.PerformanceDiagnostics.begin(dev.omnidisplay.diagnostics.PerformanceDiagnostics.Phase.CABIN_REBUILD);try{

  State state=worlds.computeIfAbsent(level,l->new State());state.scanned=level.getGameTime();
  PanelStore.refresh(level);
  state.hosts.removeIf(be->be.isRemoved()||be.getLevel()!=level);
  var hosts=new ArrayList<>(state.hosts);hosts.sort(Comparator.comparing(CabinEntity::getBlockPos));
  Set<BlockPos> seen=new HashSet<>();Set<Cabin> retained=Collections.newSetFromMap(new IdentityHashMap<>());List<Cabin> next=new ArrayList<>();Set<UUID> ids=new HashSet<>();
  for(var old:state.cabins)if(old.damaged&&old.settings.animating(level.getGameTime())){retained.add(old);next.add(old);ids.add(old.settings.id);for(var c:old.shape.walls())seen.add(new BlockPos(c.x(),c.y(),c.z()));}
  for(var seed:hosts){
   if(seen.contains(seed.getBlockPos()))continue;
   try{
    var shape=ScreenTopologyCache.scan(level,seed.getBlockPos());for(var p:shape.walls())seen.add(new BlockPos(p.x(),p.y(),p.z()));
    var members=hosts.stream().filter(be->shape.walls().contains(cell(be.getBlockPos()))).toList();
    Cabin existing=state.cabins.stream().filter(c->c.moving==null&&!retained.contains(c)&&(members.contains(c.anchor)||members.stream().anyMatch(be->c.settings.id!=null&&c.settings.id.equals(be.settings.id)))).findFirst().orElse(null);
    var latest=members.stream().max(Comparator.comparingLong(be->be.settings.revision)).orElse(seed);
    boolean topology=existing==null||!existing.shape.equals(shape);
    var c=existing!=null?existing:new Cabin(level,seed,shape,latest.settings.copy());
    boolean changed=topology;
    if(c.anchor.isRemoved()||!members.contains(c.anchor))c.anchor=members.get(0);
    if(latest.settings.revision>c.settings.revision){c.settings=latest.settings.copy();changed=true;}
    if(c.settings.id==null||!ids.add(c.settings.id)){c.settings.id=UUID.randomUUID();ids.add(c.settings.id);changed=true;}
    var newOrigin=new BlockPos(shape.min().x(),shape.min().y(),shape.min().z());
    if(!newOrigin.equals(c.settings.origin)){c.settings.moveOrigin(newOrigin);changed=true;}
    if(c.damaged){c.damaged=false;changed=true;}
    c.shape=shape;c.members=members;retained.add(c);next.add(c);
    ScreenFaces.paint(level,shape);
    if(changed||existing==null){c.publish();c.radio();}else{for(var be:members)be.apply(c.settings);c.locks(c.settings.power||c.settings.animating(level.getGameTime()));}
   }catch(IllegalArgumentException ex){seen.add(seed.getBlockPos());if(ex.getMessage()!=null&&(ex.getMessage().contains("4096")||ex.getMessage().contains("过大")))break;}
  }
  if(level instanceof ServerLevel server)for(var moving:MovingScreens.cabins(server)){next.add(moving);}
  for(var old:state.cabins)if(!retained.contains(old)){old.stop();old.locks(false);}state.cabins=next;
 
  }finally{dev.omnidisplay.diagnostics.PerformanceDiagnostics.end(dev.omnidisplay.diagnostics.PerformanceDiagnostics.Phase.CABIN_REBUILD,omni$perf);}
 }
 @SubscribeEvent public static void tick(LevelTickEvent.Post e){
  if(e.getLevel().isClientSide)return;PanelStore.refresh(e.getLevel());var state=worlds.get(e.getLevel());if(state==null)return;
  if(e.getLevel().getGameTime()-state.scanned>=20)rebuild(e.getLevel());
  Set<UUID> pulseGroups=new HashSet<>();
  for(var c:state.cabins){if(!RenderConfig.experimental()&&c.settings.power&&openEntrance(c)){c.settings.setPower(false,c.level.getGameTime(),null);c.publish();}if(c.moving==null&&RadarHooks.available())RadarBridge.tick(c);c.locks(c.settings.power||c.settings.animating(c.level.getGameTime()));if(c.receiver!=null){int pulses=c.receiver.latch.drain();if((pulses&1)!=0&&(!RenderConfig.experimental()||c.settings.group==null||pulseGroups.add(c.settings.group))){power(c,!c.settings.power,null);c.publish();}}if(c.mouseReceiver!=null&&(c.mouseReceiver.latch.drain()&1)!=0){c.settings.mouseLock=!c.settings.mouseLock;c.publish();}}
 }
 @SubscribeEvent public static void unload(LevelEvent.Unload e){if(e.getLevel().isClientSide())return;var state=worlds.remove(e.getLevel());if(state!=null)for(var c:state.cabins)c.stop();}
 public static Cabin find(Level level,BlockPos pos){var s=worlds.get(level);if(s==null)return null;return s.cabins.stream().filter(c->c.moving==null&&c.shape.walls().contains(cell(pos))).findFirst().orElse(null);}
 @SubscribeEvent public static void click(PlayerInteractEvent.RightClickBlock e){
  if(e.getHand()==InteractionHand.MAIN_HAND&&e.getItemStack().is(OmniDisplay.CABIN_SELECTOR.get())){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);if(!e.getLevel().isClientSide)OmniDisplay.CABIN_SELECTOR.get().useOn(new net.minecraft.world.item.context.UseOnContext(e.getEntity(),e.getHand(),e.getHitVec()));return;}
  if(e.getHand()==InteractionHand.MAIN_HAND&&e.getItemStack().is(OmniDisplay.HUD_LOCATOR.get())){
   e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);
   if(!e.getLevel().isClientSide&&e.getEntity() instanceof ServerPlayer sp)locateHud(sp,e.getPos());return;
  }
  if(e.getHand()==InteractionHand.MAIN_HAND&&e.getItemStack().is(OmniDisplay.HIGHLIGHT_TOOL.get())){
   e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);
   if(!e.getLevel().isClientSide)OmniDisplay.HIGHLIGHT_TOOL.get().useOn(new net.minecraft.world.item.context.UseOnContext(e.getEntity(),e.getHand(),e.getHitVec()));return;
  }
  if(e.getHand()!=InteractionHand.MAIN_HAND||!e.getItemStack().isEmpty())return;
  boolean panelFace=(PanelStore.mask(e.getLevel(),e.getPos())&(1<<e.getHitVec().getDirection().get3DDataValue()))!=0;
  if(e.getLevel().isClientSide){if(panelFace){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);}return;}
  if(!(e.getEntity() instanceof ServerPlayer sp))return;
  var state=e.getLevel().getBlockState(e.getPos());if(!state.is(OmniDisplay.DISPLAY.get())&&!state.is(OmniDisplay.THIN_DISPLAY.get())&&!((state.is(OmniDisplay.DOOR.get())||state.is(OmniDisplay.TRAPDOOR.get()))&&sp.isShiftKeyDown())&&!panelFace)return;
  e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);rebuild(e.getLevel());
  var c=find(e.getLevel(),e.getPos());if(c==null){sp.displayClientMessage(Component.literal("没有识别到完整座舱，请补齐显示器和座舱门。"),true);return;}
  if(sp.isShiftKeyDown())CabinPackets.send(sp,c,true);
  else{if(!power(c,!c.settings.power,sp,e.getPos()))return;c.publish();sp.displayClientMessage(Component.literal(c.settings.power?"座舱已开机":"座舱已关机"),true);}
 }
 public static Cabin byId(Level level,UUID id){var state=worlds.get(level);return state==null?null:state.cabins.stream().filter(c->id.equals(c.settings.id)).findFirst().orElse(null);}
 public static Cabin forHighlight(ServerPlayer player,BlockPos clicked){
  rebuild(player.level());var state=worlds.get(player.level());if(state==null)return null;
  var target=clicked==null?null:HighlightStructures.at(player.level(),clicked);Cabin chosen=null;double best=Double.POSITIVE_INFINITY;
  for(var c:state.cabins){var owner=body(c);if(target!=null&&!HighlightStructures.connected(player.serverLevel(),owner,target))continue;
   var eye=localEye(player,c);double distance=Double.POSITIVE_INFINITY;for(var w:c.shape.walls())distance=Math.min(distance,eye.distanceSquared(w.x()+.5,w.y()+.5,w.z()+.5));
   if(c.shape.interior().contains(cell(BlockPos.containing(eye.x,eye.y,eye.z))))distance*=.001;
   if(clicked==null&&distance>256)continue;if(distance<best){best=distance;chosen=c;}
  }if(chosen!=null&&RenderConfig.experimental()&&chosen.settings.group!=null){var group=chosen.settings.group;var selected=chosen;chosen=state.cabins.stream().filter(c->group.equals(c.settings.group)&&sameAssembly(selected,c)).sorted(Comparator.<Cabin>comparingInt(c->c.shape.walls().size()).reversed().thenComparing(c->c.anchor.getBlockPos())).findFirst().orElse(chosen);}return chosen;
 }
 public static dev.ryanhcode.sable.sublevel.SubLevel body(Cabin c){var p=c.moving==null?c.anchor.getBlockPos():HighlightStructures.controller(c.moving);return p==null?null:HighlightStructures.at(c.level,p);}
 public static Cabin moving(Level level,com.simibubi.create.content.contraptions.AbstractContraptionEntity entity){var state=worlds.get(level);return state==null?null:state.cabins.stream().filter(c->c.moving==entity).findFirst().orElse(null);}
 static boolean sameAssembly(Cabin a,Cabin b){return a.level==b.level&&a.level instanceof ServerLevel level&&HighlightStructures.connected(level,body(a),body(b));}
 static boolean group(Cabin a,Cabin b){
  if(!RenderConfig.experimental()||!sameAssembly(a,b))return false;
  var id=a.settings.group!=null?a.settings.group:UUID.randomUUID();var old=b.settings.group;
  var state=worlds.get(a.level);var members=state.cabins.stream().filter(c->sameAssembly(a,c)&&(c==a||c==b||id.equals(c.settings.group)||(old!=null&&old.equals(c.settings.group)))).toList();
  if(members.stream().mapToInt(c->c.shape.walls().size()).sum()>4096)return false;
  var bodies=new IdentityHashMap<Object,List<Cabin>>();
  for(var c:members)bodies.computeIfAbsent(c.moving!=null?c.moving:body(c),k->new ArrayList<>()).add(c);
  try{dev.omnidisplay.region.GroupedCabin.validateBodies(bodies.values().stream().map(local->local.stream().map(c->c.shape).toList()).toList());}catch(IllegalArgumentException tooLarge){return false;}
  for(var c:members){c.settings.group=id;c.settings.setPower(a.settings.power,c.level.getGameTime(),null);c.publish();}
  return true;
 }
 public static boolean power(Cabin c,boolean target,ServerPlayer player){return power(c,target,player,null);}
 public static boolean power(Cabin c,boolean target,ServerPlayer player,BlockPos clicked){
  if(target){
   boolean blocked=!dev.omnidisplay.region.RenderRules.mayStart(RenderConfig.experimental(),c.damaged,openEntrance(c));
   if(blocked){if(player!=null)player.displayClientMessage(Component.literal("无法开机：请修复座舱结构并关闭座舱门。"),true);return false;}
  }
  c.settings.setPower(target,c.level.getGameTime(),clicked);if(RenderConfig.experimental()&&c.settings.group!=null){var state=worlds.get(c.level);if(state!=null)for(var other:state.cabins)if(other!=c&&c.settings.group.equals(other.settings.group)&&sameAssembly(c,other)){other.settings.setPower(target,c.level.getGameTime(),null);other.publish();}}return true;
 }
 public static boolean openEntrance(Cabin c){if(c.moving!=null)return false;for(var cell:c.shape.walls()){var state=c.level.getBlockState(new BlockPos(cell.x(),cell.y(),cell.z()));if((state.is(OmniDisplay.DOOR.get())||state.is(OmniDisplay.TRAPDOOR.get()))&&state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN))return true;}return false;}
 public static void renderModeChanged(){
  for(var entry:new ArrayList<>(worlds.entrySet())){
   for(var c:new ArrayList<>(entry.getValue().cabins)){
    if(!RenderConfig.experimental()&&(c.shape.open()||openEntrance(c))&&c.settings.power)c.settings.setPower(false,c.level.getGameTime(),null);
    if(c.moving==null&&!RenderConfig.experimental()&&c.shape.open())for(var wall:c.shape.walls())PanelStore.paint(c.level,new BlockPos(wall.x(),wall.y(),wall.z()),0);
    c.doorsLocked=null;c.publish();
   }
   rebuild(entry.getKey());
  }
 }
 public static void broken(Level level,BlockPos pos){
  if(level.isClientSide)return;var c=find(level,pos);if(c==null||c.damaged||c.shape.open())return;
  if(!c.settings.power&&!c.settings.animating(level.getGameTime()))return;
  c.damaged=true;c.stop();c.settings.fault(level.getGameTime(),pos);
  c.members=c.members.stream().filter(be->!be.isRemoved()&&!be.getBlockPos().equals(pos)).toList();
  if(!c.members.isEmpty())c.anchor=c.members.get(0);
  c.publish();CabinPackets.fault(c);
 }
 public static void locateHud(ServerPlayer player,BlockPos clicked){
  rebuild(player.level());var state=worlds.get(player.level());Cabin chosen=clicked==null?null:find(player.level(),clicked);
  if(chosen!=null&&!allowed(player,chosen))chosen=null;
  double best=64;
  if(chosen==null&&state!=null)for(var c:state.cabins){
   var eye=localEye(player,c);double distance=Double.POSITIVE_INFINITY;
   for(var w:c.shape.walls())distance=Math.min(distance,eye.distanceSquared(w.x()+.5,w.y()+.5,w.z()+.5));
   if(c.shape.interior().contains(cell(BlockPos.containing(eye.x,eye.y,eye.z))))distance*=.01;
   if(distance<best){best=distance;chosen=c;}
  }
  if(chosen==null){player.displayClientMessage(Component.literal("附近没有可定位的座舱；请靠近贴片屏幕或完整座舱。"),true);return;}
  if(player.isShiftKeyDown())chosen.settings.hudCenter=null;
  else{var eye=localEye(player,chosen);var o=chosen.settings.origin;chosen.settings.hudCenter=new net.minecraft.world.phys.Vec3(eye.x-o.getX(),eye.y-o.getY(),eye.z-o.getZ());}
  if(RenderConfig.experimental()&&chosen.settings.group!=null){for(var c:state.cabins)if(c!=chosen&&chosen.settings.group.equals(c.settings.group)&&sameAssembly(chosen,c)){var eye=localEye(player,c);c.settings.hudCenter=player.isShiftKeyDown()?null:new net.minecraft.world.phys.Vec3(eye.x-c.settings.origin.getX(),eye.y-c.settings.origin.getY(),eye.z-c.settings.origin.getZ());c.publish();}}

  chosen.publish();player.displayClientMessage(Component.literal(player.isShiftKeyDown()?"HUD 已恢复自动中心":"HUD 中心已设为当前头部位置（共享）"),true);
 }
 private static Vector3d localEye(ServerPlayer player,Cabin c){
  if(c.moving!=null){var eye=player.getEyePosition();var parent=body(c);var eyeBody=HighlightStructures.at(player.level(),BlockPos.containing(eye));var point=new Vector3d(eye.x,eye.y,eye.z);if(eyeBody!=null&&eyeBody!=parent)eyeBody.logicalPose().transformPosition(point);if(parent!=null&&eyeBody!=parent)parent.logicalPose().transformPositionInverse(point);var value=c.moving.toLocalVector(new net.minecraft.world.phys.Vec3(point.x,point.y,point.z),1);return new Vector3d(value.x,value.y,value.z);}
  var eye=player.getEyePosition();var local=new Vector3d(eye.x,eye.y,eye.z);var container=SubLevelContainer.getContainer(player.serverLevel());
  if(container!=null&&container.inBounds(c.anchor.getBlockPos())){
   var plot=container.getPlot(new ChunkPos(c.anchor.getBlockPos()));
   var eyePlot=container.getPlot(new ChunkPos(BlockPos.containing(eye)));
   if(plot!=null&&eyePlot!=plot){if(eyePlot!=null)eyePlot.getSubLevel().logicalPose().transformPosition(local);plot.getSubLevel().logicalPose().transformPositionInverse(local);}
  }
  return local;
 }
 private static boolean allowed(ServerPlayer p,Cabin c){
  var local=localEye(p,c);
  if(c.shape.interior().contains(cell(BlockPos.containing(local.x,local.y,local.z))))return true;
  for(var w:c.shape.walls())if(local.distanceSquared(w.x()+.5,w.y()+.5,w.z()+.5)<=64)return true;return false;
 }
 public static void action(ServerPlayer p,CabinPackets.Action a){
  var c=find(p.level(),a.pos());if(c==null||(!allowed(p,c)&&!(a.op()>=8&&HighlightTool.bound(p.getMainHandItem(),c))))return;
  switch(a.op()){
   case 0->{CabinPackets.send(p,c,true);return;}
   case 1->{if(!power(c,!c.settings.power,p))return;}
   case 2->power(c,false,p);
   case 3->c.settings.opacity=Math.clamp(a.value(),0,100);
   case 4,5->{if(a.value()!=-1&&(a.value()<0||a.value()>=36)&&a.value()!=40)return;var item=a.value()==-1?ItemStack.EMPTY:p.getInventory().getItem(a.value()).copyWithCount(1);if(a.op()==4)c.settings.first=item;else c.settings.second=item;c.radio();}
   case 7->{if(c.settings.animating(c.level.getGameTime()))return;c.settings.heading=Math.floorMod(a.value(),360);}
   case 6->{c.settings.wireless=a.value()!=0;c.radio();}
   case 23->c.settings.mouseLock=!c.settings.mouseLock;
   case 24->{c.settings.mouseWireless=a.value()!=0;c.radio();}
   case 25,26->{if(a.value()!=-1&&(a.value()<0||a.value()>=36)&&a.value()!=40)return;var item=a.value()==-1?ItemStack.EMPTY:p.getInventory().getItem(a.value()).copyWithCount(1);if(a.op()==25)c.settings.mouseFirst=item;else c.settings.mouseSecond=item;c.radio();}
   case 21->{return;} // Legacy aim without clicked surface is no longer accepted.
   case 22->{return;} // Removed manual disconnect; only the physical link controls binding.
   case 18->c.settings.seamOpacity=Math.clamp(a.value(),0,100);
   case 19->{if(a.value()<0||a.value()>5)return;c.settings.hudMask^=1<<a.value();}
   case 12->{c.settings.highlightOpacity=Math.clamp(a.value(),0,100);for(int i=0;i<c.settings.highlights.size();i++){var h=c.settings.highlights.get(i);c.settings.highlights.set(i,h.withOpacity(c.settings.highlightOpacity));}}
   case 13->c.settings.hudScale=Math.clamp(a.value(),50,2000);
   case 27->c.settings.hudBrightness=Math.clamp(a.value(),0,300);
   case 15,16,17->{int shift=(17-a.op())*8;c.settings.hudColor=(c.settings.hudColor&~(255<<shift))|(Math.clamp(a.value(),0,255)<<shift);}
   case 14->c.settings.hudColor=a.value()&0xFFFFFF;
   case 8,9,10,11->{if(!HighlightTool.bound(p.getMainHandItem(),c))return;HighlightTool.action(p,c,a.op(),a.value());}
   default->{return;}
  }
  c.publish();if(a.op()>=8&&a.op()<=11)HighlightTool.send(p,c,false);else CabinPackets.send(p,c,false);
 }
}
