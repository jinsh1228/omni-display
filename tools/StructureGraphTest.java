import dev.omnidisplay.region.StructureGraph;
import java.util.*;
public class StructureGraphTest {
 public static void main(String[] args){
  UUID a=UUID.randomUUID(),b=UUID.randomUUID(),c=UUID.randomUUID(),d=UUID.randomUUID(),other=UUID.randomUUID();
  var edges=List.of(new StructureGraph.Edge(a,b),new StructureGraph.Edge(b,c),new StructureGraph.Edge(c,a),new StructureGraph.Edge(c,d),new StructureGraph.Edge(other,UUID.randomUUID()));
  if(!StructureGraph.connected(a,edges).equals(Set.of(a,b,c,d)))throw new AssertionError("cycle/multi-hop");
  if(!StructureGraph.connected(a,List.of()).equals(Set.of(a)))throw new AssertionError("disconnect removes stale family");
  if(!StructureGraph.connected(a,List.of(new StructureGraph.Edge(a,b),new StructureGraph.Edge(c,d))).equals(Set.of(a,b)))throw new AssertionError("broken joint");
  for(String kind:List.of("FIXED","FREE","ROTARY","GENERIC"))if(!StructureGraph.physical(kind,false,false,a,b))throw new AssertionError(kind);
  if(StructureGraph.physical("NO_CONTACT",false,false,a,b)||StructureGraph.physical("UNKNOWN",false,false,a,b)||StructureGraph.physical("FIXED",true,false,a,b)||StructureGraph.physical("ROTARY",false,true,a,b)||StructureGraph.physical("FIXED",false,false,a,a))throw new AssertionError("ground/nonphysical");
  System.out.println("PASS: joint chains/cycles, unloaded bridge identities, unrelated structures, disconnect cleanup, ground and no-contact exclusion");
 }
}
