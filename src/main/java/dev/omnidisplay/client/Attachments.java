package dev.omnidisplay.client;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.Entity;
import java.util.*;
import java.lang.reflect.Field;

/** Published immutable snapshots: Flywheel may query acceptance from its worker. */
public final class Attachments {
 private static volatile Set<BlockEntity> hiddenBlocks=Set.of();
 private static volatile Set<Entity> hiddenEntities=Set.of();
 private static Set<ClientSubLevel> family=Set.of();
 private static net.minecraft.client.multiplayer.ClientLevel lastWorld;
 private static final Map<Class<?>,Optional<Field>> controllers=new HashMap<>();
 private static net.minecraft.client.multiplayer.ClientLevel graphWorld;private static long graphTick=Long.MIN_VALUE;private static final Map<ClientSubLevel,Set<ClientSubLevel>> reachableCache=new IdentityHashMap<>();
 private static final Set<Class<?>> warned=new HashSet<>();
 public static Set<Entity> contraptions(){return hiddenEntities;}
 public static Set<ClientSubLevel> structures(){return family;}
 public static boolean connected(ClientSubLevel sub){return family.contains(sub);}
 public static boolean hide(BlockEntity be){if(PartitionRenderer.normalPass())return false;return hiddenBlocks.contains(be)||(DisplayRenderer.active()&&DisplayRenderer.member(at(be.getBlockPos()))&&DisplayRenderer.keeps(be.getBlockPos())&&DisplayRenderer.panelScreen(be.getBlockPos()));}
 public static boolean hide(Entity e){if(PartitionRenderer.normalPass())return false;return hiddenEntities.contains(e);}
 private static ClientSubLevel at(BlockPos pos){
  var mc=Minecraft.getInstance();if(mc.level==null||pos==null)return null;
  var c=SubLevelContainer.getContainer(mc.level);if(c==null||!c.inBounds(pos))return null;
  var p=c.getPlot(new ChunkPos(pos));return p!=null&&p.getSubLevel() instanceof ClientSubLevel s?s:null;
 }
 public static boolean cockpitDoor(BlockEntity be){return DisplayRenderer.active()&&DisplayRenderer.member(at(be.getBlockPos()))&&DisplayRenderer.keeps(be.getBlockPos());}
 private static boolean shouldHide(BlockPos pos){var sub=at(pos);return sub!=null&&family.contains(sub)&&!DisplayRenderer.keeps(pos);}
 private static BlockPos controller(Entity e){
  var field=controllers.computeIfAbsent(e.getClass(),cl->{
   for(Class<?> c=cl;c!=null&&c!=Entity.class;c=c.getSuperclass())try{
    var f=c.getDeclaredField("controllerPos");if(f.getType()==BlockPos.class&&f.trySetAccessible())return Optional.of(f);
   }catch(NoSuchFieldException ignored){}
   return Optional.empty();
  });
  try{return field.isPresent()?(BlockPos)field.get().get(e):null;}catch(IllegalAccessException ignored){return null;}
 }
 public static Set<ClientSubLevel> reachable(ClientSubLevel root){var mc=Minecraft.getInstance();if(root==null||mc.level==null)return Set.of();
  if(graphWorld!=mc.level||graphTick!=mc.level.getGameTime()){reachableCache.clear();graphWorld=mc.level;graphTick=mc.level.getGameTime();}
  var cached=reachableCache.get(root);if(cached!=null)return cached;
   var container=SubLevelContainer.getContainer(mc.level);
   Map<ClientSubLevel,Set<ClientSubLevel>> edges=new IdentityHashMap<>();
   for(var sub:container.getAllSubLevels()){
    for(var actor:sub.getPlot().getBlockEntityActors())try{
     var deps=actor.sable$getConnectionDependencies();if(deps==null)continue;
     for(var dep:deps)if(dep instanceof ClientSubLevel child&&!child.isRemoved()){
      edges.computeIfAbsent(sub,k->new HashSet<>()).add(child);edges.computeIfAbsent(child,k->new HashSet<>()).add(sub);
     }
    }catch(RuntimeException ex){if(warned.add(actor.getClass()))org.slf4j.LoggerFactory.getLogger("OmniDisplay").warn("Cannot inspect connection actor {}",actor.getClass().getName(),ex);}
   }
   Map<java.util.UUID,ClientSubLevel> byId=new HashMap<>();for(var sub:container.getAllSubLevels())if(!sub.isRemoved())byId.put(sub.getUniqueId(),sub);
   var graph=new ArrayList<dev.omnidisplay.region.StructureGraph.Edge>();
   for(var entry:edges.entrySet())for(var child:entry.getValue())graph.add(new dev.omnidisplay.region.StructureGraph.Edge(entry.getKey().getUniqueId(),child.getUniqueId()));
   for(var link:ServerRenderState.currentLinks())graph.add(new dev.omnidisplay.region.StructureGraph.Edge(link.a(),link.b()));
   var reachable=dev.omnidisplay.region.StructureGraph.connected(root.getUniqueId(),graph);
   Set<ClientSubLevel> seen=new HashSet<>();for(var id:reachable){var sub=byId.get(id);if(sub!=null)seen.add(sub);}var result=Set.copyOf(seen);reachableCache.put(root,result);return result;

 }
 public static void update(){
  var mc=Minecraft.getInstance();
  if(mc.level!=lastWorld){hiddenBlocks=Set.of();hiddenEntities=Set.of();family=Set.of();lastWorld=mc.level;}
  if(mc.level==null)return;
  Set<BlockEntity> nextBlocks=new HashSet<>();Set<Entity> nextEntities=new HashSet<>();
  if(DisplayRenderer.active()){
   family=reachable(DisplayRenderer.owner());

   for(var s:family)for(var holder:s.getPlot().getLoadedChunks())for(var be:holder.getChunk().getBlockEntities().values())
    if(!be.isRemoved()&&(shouldHide(be.getBlockPos())||(DisplayRenderer.member(s)&&DisplayRenderer.keeps(be.getBlockPos())&&DisplayRenderer.panelScreen(be.getBlockPos()))))nextBlocks.add(be);
   for(var e:mc.level.entitiesForRendering())if(e instanceof AbstractContraptionEntity&&!e.isRemoved()){
    var pos=controller(e);if(pos!=null&&(family.contains(at(pos))))nextEntities.add(e);
   }
  }else family=Set.of();
  publish(nextBlocks,nextEntities);
 }
 private static void publish(Set<BlockEntity> blocks,Set<Entity> entities){
  var oldBlocks=hiddenBlocks;var oldEntities=hiddenEntities;
  hiddenBlocks=Set.copyOf(blocks);hiddenEntities=Set.copyOf(entities);
  if(lastWorld==null)return;
  var manager=VisualizationManager.get(lastWorld);if(manager==null)return;
  for(var be:blocks)if(!oldBlocks.contains(be))manager.blockEntities().queueRemove(be);
  for(var be:oldBlocks)if(!blocks.contains(be)&&!be.isRemoved())manager.blockEntities().queueAdd(be);
  for(var e:entities)if(!oldEntities.contains(e))manager.entities().queueRemove(e);
  for(var e:oldEntities)if(!entities.contains(e)&&!e.isRemoved())manager.entities().queueAdd(e);
 }
 public static void reset(){family=Set.of();publish(Set.of(),Set.of());}
}
