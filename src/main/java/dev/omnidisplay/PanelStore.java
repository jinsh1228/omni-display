package dev.omnidisplay;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.*;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Surface metadata, never a replacement block or an extra block entity in the host cell. */
@EventBusSubscriber(modid=OmniDisplay.ID)
public final class PanelStore {
 public static final class Entry {
  public final BlockPos pos;
  public int mask,interior;
  String block;
  public CabinSettings settings=new CabinSettings();
  Host host;
  CompoundTag received;
  Entry(BlockPos p){pos=p.immutable();}
  CompoundTag save(HolderLookup.Provider registry){
   var t=new CompoundTag();t.putLong("pos",pos.asLong());t.putInt("mask",mask);t.putInt("interior",interior);t.putString("block",block);t.put("cabin",settings.save(registry));return t;
  }
  static Entry load(CompoundTag t,HolderLookup.Provider registry){
   var e=new Entry(BlockPos.of(t.getLong("pos")));e.mask=t.getInt("mask")&63;e.interior=t.getInt("interior")&e.mask;e.block=t.getString("block");e.settings=CabinSettings.load(t.getCompound("cabin"),registry);return e;
  }
 }
 private static final class Data extends SavedData {
  final Map<BlockPos,Entry> entries=new HashMap<>();
  long refreshed=Long.MIN_VALUE,synced=Long.MIN_VALUE;
  final ArrayDeque<BlockPos> repairs=new ArrayDeque<>();
  long repairTick=Long.MIN_VALUE;
  static Data load(CompoundTag t,HolderLookup.Provider r){var data=new Data();for(var tag:t.getList("panels",Tag.TAG_COMPOUND)){var e=Entry.load((CompoundTag)tag,r);if(e.mask!=0)data.entries.put(e.pos,e);}return data;}
  @Override public CompoundTag save(CompoundTag t,HolderLookup.Provider r){var list=new ListTag();for(var e:entries.values())list.add(e.save(r));t.put("panels",list);return t;}
 }
 private static final Map<Level,Data> worlds=new java.util.concurrent.ConcurrentHashMap<>();
 private static final ThreadLocal<Integer> transfers=ThreadLocal.withInitial(()->0);
 public static void beginTransfer(){transfers.set(transfers.get()+1);}
 public static void endTransfer(){int depth=transfers.get()-1;if(depth==0)transfers.remove();else transfers.set(depth);}
 public static void blockChanged(Level level,BlockPos pos){if(!level.isClientSide&&transfers.get()==0&&mask(level,pos)!=0)remove(level,pos,true);}
 private static Data data(Level level){return worlds.computeIfAbsent(level,l->l instanceof ServerLevel s?s.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Data::new,Data::load,null),"omni_display_panels"):new Data());}
 public static Collection<Entry> entries(Level level){return Collections.unmodifiableCollection(data(level).entries.values());}
 public static int mask(Level level,BlockPos pos){if(level==null)return 0;var e=data(level).entries.get(pos);return e==null?0:e.mask;}
 public static int interiorMask(Level level,BlockPos pos){if(level==null)return 0;var e=data(level).entries.get(pos);return e==null?0:e.interior;}
 public static CabinEntity hostAt(Level level,BlockPos pos){var e=data(level).entries.get(pos);return e==null?null:e.host;}
 public static List<CabinEntity> hosts(Level level,ChunkPos chunk){var out=new ArrayList<CabinEntity>();for(var e:entries(level))if(e.host!=null&&new ChunkPos(e.pos).equals(chunk))out.add(e.host);return out;}
 private static void host(Level level,Entry e){if(e.host==null){e.host=new Host(level,e);if(!level.isClientSide)CabinManager.add(e.host);}}
 private static void retire(Entry e){if(e.host!=null){e.host.setRemoved();e.host=null;}}
 public static void refresh(Level level){
  long omni$perf=dev.omnidisplay.diagnostics.PerformanceDiagnostics.begin(dev.omnidisplay.diagnostics.PerformanceDiagnostics.Phase.PANEL_REFRESH);try{

  if(level.isClientSide)return;var d=data(level);long now=level.getGameTime();
  if(d.refreshed==Long.MIN_VALUE||now<d.refreshed||now-d.refreshed>=20){
   d.refreshed=now;
   for(var e:new ArrayList<>(d.entries.values())){
    if(!level.hasChunkAt(e.pos)){retire(e);continue;}
    if(!BuiltInRegistries.BLOCK.getKey(level.getBlockState(e.pos).getBlock()).toString().equals(e.block)){remove(level,e.pos,true);continue;}
    host(level,e);
   }
  }
  // Spread repair packets across ticks; ordinary edits still synchronize immediately.
  if(d.synced==Long.MIN_VALUE||now<d.synced||now-d.synced>=100){
   d.synced=now;d.repairs.clear();d.repairs.addAll(d.entries.keySet());
  }
  if(d.repairTick==now)return;d.repairTick=now;
  int budget=dev.omnidisplay.region.RepairSyncBudget.perTick(d.entries.size());
  for(int i=0;i<budget&&!d.repairs.isEmpty();i++){
   var e=d.entries.get(d.repairs.removeFirst());if(e!=null&&e.host!=null)PacketDistributor.sendToPlayersInDimension((ServerLevel)level,new Sync(e.save(level.registryAccess())));
  }
 
  }finally{dev.omnidisplay.diagnostics.PerformanceDiagnostics.end(dev.omnidisplay.diagnostics.PerformanceDiagnostics.Phase.PANEL_REFRESH,omni$perf);}
 }
 public static boolean attach(Level level,BlockPos pos,Direction face){
  var state=level.getBlockState(pos);if(state.isAir()||OmniDisplay.isScreen(state))return false;
  int bit=1<<face.get3DDataValue();var d=data(level);var e=d.entries.get(pos);if(e!=null&&(e.mask&bit)!=0)return false;
  if(e==null){e=new Entry(pos);e.block=BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();d.entries.put(e.pos,e);}
  e.mask|=bit;host(level,e);changed(level,e);CabinManager.rebuild(level);return true;
 }
 public static boolean detach(Level level,BlockPos pos,Direction face,boolean drop){
  var e=data(level).entries.get(pos);int bit=1<<face.get3DDataValue();if(e==null||(e.mask&bit)==0)return false;
  CabinManager.broken(level,pos);e.mask&=~bit;e.interior&=e.mask;
  if(e.mask==0){data(level).entries.remove(pos);retire(e);}changed(level,e);
  if(drop)Block.popResource(level,pos,new ItemStack(OmniDisplay.PANEL_ITEM.get()));CabinManager.rebuild(level);return true;
 }
 private static void remove(Level level,BlockPos pos,boolean drop){
  var e=data(level).entries.remove(pos);if(e==null)return;CabinManager.broken(level,pos);retire(e);int count=Integer.bitCount(e.mask);e.mask=e.interior=0;changed(level,e);
  if(drop)Block.popResource(level,pos,new ItemStack(OmniDisplay.PANEL_ITEM.get(),count));
 }
 public static void paint(Level level,BlockPos pos,int mask){var e=data(level).entries.get(pos);if(e!=null&&e.interior!=(mask&e.mask)){e.interior=mask&e.mask;changed(level,e);}}
 private static void changed(Level level,Entry e){changed(level,e,true);}
 private static void changed(Level level,Entry e,boolean topology){if(topology)ScreenTopologyCache.invalidate(level);data(level).setDirty();if(level instanceof ServerLevel s)PacketDistributor.sendToPlayersInDimension(s,new Sync(e.save(level.registryAccess())));}
 private static final class Host extends CabinEntity {
  final Entry entry;
  Host(Level l,Entry e){super(e.pos,OmniDisplay.DISPLAY.get().defaultBlockState());entry=e;setLevel(l);settings=e.settings;}
  @Override public void apply(CabinSettings s){if(settings.id!=null&&settings.id.equals(s.id)&&settings.revision==s.revision)return;boolean topology=settings.manualFaces!=s.manualFaces;settings=s.copy();entry.settings=settings;changed(level,entry,topology);}
 }
 public record Sync(CompoundTag tag) implements CustomPacketPayload {
  public static final Type<Sync> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(OmniDisplay.ID,"panel_surface"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Sync> CODEC=StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG,Sync::tag,Sync::new);
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public static void register(PayloadRegistrar r){r.playToClient(Sync.TYPE,Sync.CODEC,(p,c)->c.enqueueWork(()->receive(c.player().level(),p.tag)));}
 public static void savePlot(dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot plot,CompoundTag tag){
  var level=plot.getSubLevel().getLevel();var base=plot.toGlobal(new ChunkPos(0,0));
  var offset=new BlockPos(base.getMinBlockX(),level.getMinBuildHeight(),base.getMinBlockZ());var list=new ListTag();
  for(var e:entries(level))if(plot.contains(new ChunkPos(e.pos))&&level.hasChunkAt(e.pos)
    &&BuiltInRegistries.BLOCK.getKey(level.getBlockState(e.pos).getBlock()).toString().equals(e.block))
   list.add(PanelBlueprint.map(e.save(level.registryAccess()),pos->pos.subtract(offset)));
  var portable=new CompoundTag();portable.putInt("version",1);portable.putInt("section_min",PanelBlueprint.minimumSection(tag));portable.put("entries",list);
  tag.put(PanelBlueprint.KEY,portable);
 }
 public static void loadPlot(dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot plot,CompoundTag tag){
  if(!tag.contains(PanelBlueprint.KEY,Tag.TAG_COMPOUND))return;
  var portable=tag.getCompound(PanelBlueprint.KEY);if(portable.getInt("version")!=1)return;
  var level=plot.getSubLevel().getLevel();var base=plot.toGlobal(new ChunkPos(0,0));
  var offset=new BlockPos(base.getMinBlockX(),PanelBlueprint.restoredYBase(tag,level.getMinBuildHeight()),base.getMinBlockZ());
  boolean blueprint=dev.ryanhcode.sable.api.schematic.SubLevelSchematicSerializationContext.getCurrentContext()!=null;
  for(var item:portable.getList("entries",Tag.TAG_COMPOUND)){
   var mapped=PanelBlueprint.map((CompoundTag)item,pos->pos.offset(offset));
   if(blueprint){var cabin=mapped.getCompound("cabin");cabin.remove("id");cabin.remove("waveTimes");cabin.remove("waveTargets");cabin.remove("clickOrigins");cabin.remove("faultOrigin");cabin.putInt("faultEvent",-1);cabin.putBoolean("waveBase",cabin.getBoolean("power"));}
   var e=Entry.load(mapped,level.registryAccess());
   if(e.mask==0||!plot.contains(new ChunkPos(e.pos))||!level.hasChunkAt(e.pos)
     ||!BuiltInRegistries.BLOCK.getKey(level.getBlockState(e.pos).getBlock()).toString().equals(e.block))continue;
   var old=data(level).entries.put(e.pos,e);if(old!=null)retire(old);
   host(level,e);changed(level,e);
  }
  // Normal cabin rebuild on the next tick assigns shared settings and interior faces.
 }
 private static void receive(Level l,CompoundTag t){
  long omni$perf=dev.omnidisplay.diagnostics.PerformanceDiagnostics.begin(dev.omnidisplay.diagnostics.PerformanceDiagnostics.Phase.PANEL_RECEIVE);try{
var d=data(l);var pos=BlockPos.of(t.getLong("pos"));var e=d.entries.get(pos);
  // Periodic repair packets often repeat identical metadata. Keep the live host and cached shape.
  if(e!=null&&t.equals(e.received)){host(l,e);return;}
  var fresh=Entry.load(t,l.registryAccess());
  boolean topology=e==null?fresh.mask!=0:e.mask!=fresh.mask||e.interior!=fresh.interior||!Objects.equals(e.block,fresh.block)||e.settings.manualFaces!=fresh.settings.manualFaces;
  if(topology)ScreenTopologyCache.invalidate(l);
  if(fresh.mask==0){if(e!=null)retire(e);d.entries.remove(fresh.pos);return;}
  if(e==null){e=fresh;d.entries.put(e.pos,e);}else{e.mask=fresh.mask;e.interior=fresh.interior;e.block=fresh.block;e.settings=fresh.settings;if(e.host!=null)e.host.settings=e.settings;}
  e.received=t.copy();host(l,e);
 
  }finally{dev.omnidisplay.diagnostics.PerformanceDiagnostics.end(dev.omnidisplay.diagnostics.PerformanceDiagnostics.Phase.PANEL_RECEIVE,omni$perf);}
 }
 @SubscribeEvent public static void watch(ChunkWatchEvent.Watch event){for(var e:entries(event.getLevel()))if(new ChunkPos(e.pos).equals(event.getPos()))PacketDistributor.sendToPlayer(event.getPlayer(),new Sync(e.save(event.getLevel().registryAccess())));}
 @SubscribeEvent public static void unload(LevelEvent.Unload event){if(event.getLevel() instanceof Level l){var d=worlds.remove(l);if(d!=null)for(var e:d.entries.values())retire(e);}}
 // Retain client metadata across chunk unloads; remove only on explicit sync or world unload.

 public static CompoundTag movingData(Level level,BlockPos pos){var e=data(level).entries.get(pos);if(e==null)return null;var t=e.save(level.registryAccess());if(e.settings.origin!=null)t.putLong("originOffset",e.settings.origin.subtract(pos).asLong());return t;}
 public static void extractMoving(Level level,BlockPos pos){var e=data(level).entries.remove(pos);if(e!=null){retire(e);e.mask=e.interior=0;changed(level,e);}}
 public static void restoreMoving(Level level,BlockPos target,BlockPos local,CompoundTag tag,com.simibubi.create.content.contraptions.StructureTransform transform){
  if(level.getBlockState(target).isAir())return;var e=new Entry(target);e.block=BuiltInRegistries.BLOCK.getKey(level.getBlockState(target).getBlock()).toString();e.settings=CabinSettings.load(PanelBlueprint.map(tag,transform::apply).getCompound("cabin"),level.registryAccess());
  for(var d:Direction.values()){int bit=1<<d.get3DDataValue(),next=1<<transform.rotateFacing(d).get3DDataValue();if((tag.getInt("mask")&bit)!=0)e.mask|=next;if((tag.getInt("interior")&bit)!=0)e.interior|=next;}
  if(tag.contains("originOffset"))e.settings.origin=transform.apply(local.offset(BlockPos.of(tag.getLong("originOffset"))));
  var old=data(level).entries.put(target,e);if(old!=null)retire(old);host(level,e);changed(level,e);
 }
 /** Called before Sable's block transfer. Snapshot first so overlapping transforms are safe. */
 public static List<CompoundTag> capture(ServerLevel level,Iterable<BlockPos> positions){var result=new ArrayList<CompoundTag>();var d=data(level);for(var p:positions){var e=d.entries.get(p);if(e!=null)result.add(e.save(level.registryAccess()));}return result;}
 public static void transfer(ServerLevel source,dev.ryanhcode.sable.api.SubLevelAssemblyHelper.AssemblyTransform transform,List<CompoundTag> snapshot){
  for(var t:snapshot){var pos=BlockPos.of(t.getLong("pos"));var e=data(source).entries.remove(pos);if(e!=null){retire(e);e.mask=e.interior=0;changed(source,e);}}
  var target=transform.getLevel();
  for(var t:snapshot){var old=Entry.load(t,target.registryAccess());var e=new Entry(transform.apply(old.pos));e.block=old.block;e.settings=old.settings;
   for(var face:Direction.values()){int before=1<<face.get3DDataValue(),after=1<<transform.getRotation().rotate(face).get3DDataValue();if((old.mask&before)!=0)e.mask|=after;if((old.interior&before)!=0)e.interior|=after;}
   // Relative highlight/radar coordinates follow the assembly; origin itself is absolute.
   if(e.settings.origin!=null)e.settings.origin=transform.apply(e.settings.origin);
   data(target).entries.put(e.pos,e);host(target,e);changed(target,e);
  }
 }
}
