package dev.omnidisplay;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import dev.omnidisplay.region.Enclosure;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.*;
/** Cached flood fills, invalidated by screen edits, panel metadata and chunk lifecycle. */
@EventBusSubscriber(modid=OmniDisplay.ID)
public final class ScreenTopologyCache {
 private record Entry(long generation,long tick,boolean mode,dev.omnidisplay.region.ComponentScanMemo<BlockPos,Enclosure.Result> memo){}
 private static final Map<Level,Long> versions=new IdentityHashMap<>();
 private static final Map<Level,Entry> cache=new IdentityHashMap<>();
 public static synchronized long version(Level l){return versions.getOrDefault(l,0L);}
 public static synchronized void invalidate(Level l){if(l!=null){versions.put(l,version(l)+1);cache.remove(l);}}
 public static synchronized Enclosure.Result scan(Level l,BlockPos seed){
  long omni$perf=dev.omnidisplay.diagnostics.PerformanceDiagnostics.begin(dev.omnidisplay.diagnostics.PerformanceDiagnostics.Phase.TOPOLOGY_SCAN);try{

  boolean mode=l.isClientSide?dev.omnidisplay.client.ServerRenderState.experimental:RenderConfig.experimental();long tick=l.getGameTime(),v=version(l);
  var old=cache.get(l);
  if(old==null||!dev.omnidisplay.region.TopologyCachePolicy.reusable(v,old.generation,mode,old.mode,tick,old.tick))old=new Entry(v,tick,mode,new dev.omnidisplay.region.ComponentScanMemo<>());
  cache.put(l,new Entry(v,tick,mode,old.memo));
  return old.memo.scan(seed,()->ScreenFaces.collect(l,seed).stream().map(c->new BlockPos(c.x(),c.y(),c.z())).toList(),
   component->{var cells=new HashSet<Enclosure.Cell>();for(var p:component)cells.add(new Enclosure.Cell(p.getX(),p.getY(),p.getZ()));return ScreenFaces.detect(l,cells,seed);},
   shape->shape.walls().stream().map(c->new BlockPos(c.x(),c.y(),c.z())).toList());
 
  }finally{dev.omnidisplay.diagnostics.PerformanceDiagnostics.end(dev.omnidisplay.diagnostics.PerformanceDiagnostics.Phase.TOPOLOGY_SCAN,omni$perf);}
 }
 @SubscribeEvent public static void chunk(ChunkEvent.Load e){invalidate(e.getLevel() instanceof Level l?l:null);}
 @SubscribeEvent public static void unchunk(ChunkEvent.Unload e){invalidate(e.getLevel() instanceof Level l?l:null);}
 @SubscribeEvent public static synchronized void unload(LevelEvent.Unload e){if(e.getLevel() instanceof Level l){cache.remove(l);versions.remove(l);}}
}
