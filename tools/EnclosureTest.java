import dev.omnidisplay.region.Enclosure;
import dev.omnidisplay.region.Enclosure.Cell;
import java.util.*;
public class EnclosureTest {
 static Set<Cell> cube(int ox,int oy,int oz,int size){var result=new HashSet<Cell>();for(int x=0;x<size;x++)for(int y=0;y<size;y++)for(int z=0;z<size;z++)if(x==0||y==0||z==0||x==size-1||y==size-1||z==size-1)result.add(new Cell(ox+x,oy+y,oz+z));return result;}
 static void check(boolean ok){if(!ok)throw new AssertionError();}
 static void rejected(Set<Cell> walls,Cell seed){try{Enclosure.detect(walls,seed);throw new AssertionError("Unexpected enclosure");}catch(IllegalArgumentException expected){}}
 public static void main(String[] args){
  var seed=new Cell(0,0,0);var box=cube(0,0,0,5);var r=Enclosure.detect(box,seed);check(r.interior().size()==27);check(r.walls().size()==98);
  box.remove(new Cell(2,2,0));rejected(box,seed);
  box.add(new Cell(2,2,0));check(Enclosure.detect(box,seed).interior().size()==27);
  box.add(new Cell(1000,1000,1000));check(Enclosure.detect(box,seed).walls().size()==98);
  box.addAll(cube(4,4,4,5));check(Enclosure.detect(box,seed).interior().size()==54);
  var shifted=cube(30000000,-20,-30000000,5);check(Enclosure.detect(shifted,new Cell(30000000,-20,-30000000)).interior().size()==27);
  check(Enclosure.detect(cube(0,0,0,19),seed).interior().size()==4913);
  check(Enclosure.detect(cube(0,0,0,20),seed).interior().size()==5832);
  rejected(cube(0,0,0,28),seed);
  var open=new HashSet<Cell>();for(int x=0;x<5;x++)for(int y=0;y<5;y++)open.add(new Cell(x,y,0));rejected(open,seed);
  System.out.println("PASS: closed shell, leak, repair, unrelated screens, corner-connected chambers, large coordinates, size limit, open sheet");
 }
}
