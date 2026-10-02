package dev.omnidisplay.client;
import dev.omnidisplay.region.Enclosure;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import java.util.*;
/** Cache occupied interior cells, never rendered block states or model data. */
public final class CabinContentsCache {
 private record Entry(Enclosure.Result shape,long version,long tick,List<BlockPos> positions){}
 private static Object world;
 private static final Map<Object,Entry> entries=new IdentityHashMap<>();
 public static void clear(){world=null;entries.clear();}
 public static void changed(Level level,BlockPos pos){
  if(world!=level)return;var cell=new Enclosure.Cell(pos.getX(),pos.getY(),pos.getZ());
  entries.values().removeIf(e->e.shape.interior().contains(cell)||e.shape.walls().contains(cell));
 }
 static List<BlockPos> positions(DisplayRenderer.Body body){
  long omni$perf=dev.omnidisplay.diagnostics.PerformanceDiagnostics.begin(dev.omnidisplay.diagnostics.PerformanceDiagnostics.Phase.CONTENTS_CACHE);try{

  var level=Minecraft.getInstance().level;if(world!=level){clear();world=level;}
  long version=dev.omnidisplay.ScreenTopologyCache.version(level),tick=level.getGameTime();var old=entries.get(body.sub());
  if(old!=null&&old.shape.equals(body.shape())&&old.version==version&&tick>=old.tick)return old.positions;
  var result=new ArrayList<BlockPos>();
  for(var cell:body.shape().interior()){
   var pos=new BlockPos(cell.x(),cell.y(),cell.z());
   if(!level.hasChunkAt(pos))continue;var state=level.getBlockState(pos);
   if(!state.isAir()&&state.getRenderShape()==RenderShape.MODEL&&!dev.omnidisplay.OmniDisplay.isScreen(state)&&!DisplayRenderer.panelScreen(pos))result.add(pos);
  }
  var next=new Entry(body.shape(),version,tick,List.copyOf(result));entries.put(body.sub(),next);return next.positions;
 
  }finally{dev.omnidisplay.diagnostics.PerformanceDiagnostics.end(dev.omnidisplay.diagnostics.PerformanceDiagnostics.Phase.CONTENTS_CACHE,omni$perf);}
 }
}
