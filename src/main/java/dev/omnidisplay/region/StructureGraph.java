package dev.omnidisplay.region;
import java.util.*;
/** UUID graph deliberately traverses unloaded intermediate bodies as well. */
public final class StructureGraph {
 public record Edge(UUID a,UUID b){}
 public static boolean physical(String kind,boolean groundA,boolean groundB,UUID a,UUID b){return !groundA&&!groundB&&!a.equals(b)&&Set.of("FIXED","FREE","ROTARY","GENERIC").contains(kind);}
 public static Set<UUID> connected(UUID root,Collection<Edge> edges){
  Map<UUID,Set<UUID>> adjacent=new HashMap<>();for(var e:edges){adjacent.computeIfAbsent(e.a(),k->new HashSet<>()).add(e.b());adjacent.computeIfAbsent(e.b(),k->new HashSet<>()).add(e.a());}
  Set<UUID> seen=new HashSet<>();var queue=new ArrayDeque<UUID>();queue.add(root);while(!queue.isEmpty()){var id=queue.remove();if(seen.add(id))queue.addAll(adjacent.getOrDefault(id,Set.of()));}return Set.copyOf(seen);
 }
}
