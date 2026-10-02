package dev.omnidisplay;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
/** Uses the same physical graph as transparency; CBC remains an optional integration. */
public final class HighlightStructures {
 private static final Set<Class<?>> warned=new HashSet<>();
 public static SubLevel at(Level level,BlockPos pos){var c=SubLevelContainer.getContainer(level);if(c==null||!c.inBounds(pos))return null;var plot=c.getPlot(new ChunkPos(pos));return plot==null?null:plot.getSubLevel();}
 public static boolean connected(ServerLevel level,SubLevel owner,SubLevel target){
  if(owner==null||target==null)return false;if(owner==target)return true;
  var edges=new ArrayList<dev.omnidisplay.region.StructureGraph.Edge>();
  for(var sub:SubLevelContainer.getContainer(level).getAllSubLevels())for(var actor:sub.getPlot().getBlockEntityActors())try{
   var deps=actor.sable$getConnectionDependencies();if(deps!=null)for(var child:deps)if(!child.isRemoved())edges.add(new dev.omnidisplay.region.StructureGraph.Edge(sub.getUniqueId(),child.getUniqueId()));
  }catch(RuntimeException error){if(warned.add(actor.getClass()))org.slf4j.LoggerFactory.getLogger("OmniDisplay").warn("Cannot inspect highlight connection actor {}",actor.getClass().getName(),error);}
  for(var link:SynaxisLinks.read(level))edges.add(new dev.omnidisplay.region.StructureGraph.Edge(link.a(),link.b()));
  return dev.omnidisplay.region.StructureGraph.connected(owner.getUniqueId(),edges).contains(target.getUniqueId());
 }
 public static AbstractContraptionEntity mounted(Level level,BlockPos pos){
  var be=level.getBlockEntity(pos);if(be==null)return null;
  for(String method:new String[]{"getContraption","getMovedContraption"})try{var result=be.getClass().getMethod(method).invoke(be);if(result instanceof AbstractContraptionEntity e&&!e.isRemoved())return e;}catch(ReflectiveOperationException ignored){}
  return null;
 }
 public static BlockPos controller(Entity entity){
  for(Class<?> type=entity.getClass();type!=null&&type!=Entity.class;type=type.getSuperclass())try{var f=type.getDeclaredField("controllerPos");if(f.getType()==BlockPos.class&&f.trySetAccessible())return (BlockPos)f.get(entity);}catch(ReflectiveOperationException ignored){}
  return null;
 }
}
