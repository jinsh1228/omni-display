package dev.omnidisplay;
import net.minecraft.server.level.ServerLevel;
import java.util.*;
/** Optional server adapter: no Synaxis classes are linked into the mod. */
public final class SynaxisLinks {
 public record Link(UUID a,UUID b){}
 private static java.lang.reflect.Method topology,edges,bodyA,bodyB,kind,id,ground;
 private static boolean checked,failed;
 public static List<Link> read(ServerLevel level){
  if(failed)return List.of();
  try {
   if(!checked){checked=true;
    if(!net.neoforged.fml.ModList.get().isLoaded("synaxis"))return List.of();
    var api=Class.forName("com.verr1.synaxis.foundation.physics.SynaxisPhysics");
    topology=api.getMethod("constraintTopology");
    edges=Class.forName("com.verr1.synaxis.foundation.physics.constraint.topology.ConstraintTopologyView").getMethod("edges",net.minecraft.resources.ResourceKey.class);
    var edge=Class.forName("com.verr1.synaxis.foundation.physics.constraint.topology.ConstraintTopologyEdge");bodyA=edge.getMethod("bodyA");bodyB=edge.getMethod("bodyB");kind=edge.getMethod("kind");
    var body=Class.forName("com.verr1.synaxis.foundation.physics.BodyId");id=body.getMethod("id");ground=body.getMethod("isGround");
   }
   if(topology==null)return List.of();
   Set<Link> result=new HashSet<>();
   for(var edge:(List<?>)edges.invoke(topology.invoke(null),level.dimension())){
    var a=bodyA.invoke(edge);var b=bodyB.invoke(edge);
    var x=(UUID)id.invoke(a);var y=(UUID)id.invoke(b);if(!dev.omnidisplay.region.StructureGraph.physical(kind.invoke(edge).toString(),(boolean)ground.invoke(a),(boolean)ground.invoke(b),x,y))continue;
    result.add(x.compareTo(y)<0?new Link(x,y):new Link(y,x));
   }
   return result.stream().sorted(Comparator.comparing(Link::a).thenComparing(Link::b)).limit(8192).toList();
  }catch(ReflectiveOperationException|RuntimeException ex){failed=true;org.slf4j.LoggerFactory.getLogger("OmniDisplay").warn("Synaxis topology adapter unavailable; preserving ordinary attachment detection",ex);return List.of();}
 }
}
