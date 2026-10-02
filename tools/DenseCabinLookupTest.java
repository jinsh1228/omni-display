import dev.omnidisplay.region.Enclosure;
import java.util.*;
public final class DenseCabinLookupTest {
 static int hash(Enclosure.Cell c,boolean legacy){return legacy?(c.x()*31+c.y())*31+c.z():c.hashCode();}
 static long probes(List<Enclosure.Cell> cells,boolean legacy){var table=new Enclosure.Cell[cells.size()*2];long steps=0;for(var c:cells){int i=Math.floorMod(hash(c,legacy),table.length);while(table[i]!=null){steps++;i=(i+1)%table.length;}table[i]=c;steps++;}for(var c:cells){int i=Math.floorMod(hash(c,legacy),table.length);while(!c.equals(table[i])){steps++;i=(i+1)%table.length;}steps++;}return steps;}
 public static void main(String[] args){
  for(int base:new int[]{0,20481030,-20481030}){
   var cells=new ArrayList<Enclosure.Cell>();for(int x=1;x<18;x++)for(int y=1;y<18;y++)for(int z=1;z<18;z++)cells.add(new Enclosure.Cell(base+x,128+y,base+z));Collections.shuffle(cells,new Random(7));
   long before=probes(cells,true),after=probes(cells,false);if(after*3>=before)throw new AssertionError("Dense lookup distribution did not improve: "+before+" -> "+after);
   var set=Set.copyOf(cells);for(var c:cells)if(!set.contains(new Enclosure.Cell(c.x(),c.y(),c.z())))throw new AssertionError();
   if(set.contains(new Enclosure.Cell(base,128,base)))throw new AssertionError();
   System.out.println("PASS: dense 4913-cell lookup probes at "+base+": "+before+" -> "+after+"; coordinate equality unchanged");
  }
 }
}
