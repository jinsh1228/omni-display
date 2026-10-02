import dev.omnidisplay.region.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
public final class BrokenCabinCacheTest {
 public static void main(String[] args){
  Set<Enclosure.Cell> shell=new HashSet<>();for(int x=0;x<19;x++)for(int y=0;y<19;y++)for(int z=0;z<19;z++)if(x==0||y==0||z==0||x==18||y==18||z==18)shell.add(new Enclosure.Cell(x,y,z));
  var hole=new Enclosure.Cell(9,9,0);shell.remove(hole);
  var memo=new ComponentScanMemo<Enclosure.Cell,Enclosure.Result>();var collect=new AtomicInteger();var detect=new AtomicInteger();
  for(var seed:shell)try{memo.scan(seed,()->{collect.incrementAndGet();return shell;},cells->{detect.incrementAndGet();return Enclosure.detect(new HashSet<>(cells),seed);},Enclosure.Result::walls);throw new AssertionError("Broken shell accepted");}catch(IllegalArgumentException expected){}
  if(collect.get()!=1||detect.get()!=1)throw new AssertionError("Repeated failed flood fills: "+detect);
  shell.add(hole);memo.clear();var seed=shell.iterator().next();var repaired=memo.scan(seed,()->shell,cells->Enclosure.detect(new HashSet<>(cells),seed),Enclosure.Result::walls);
  if(repaired.interior().size()!=4913)throw new AssertionError("Repair did not recover");
  for(var cell:shell)if(memo.scan(cell,()->{throw new AssertionError("Successful component rescanned");},cells->{throw new AssertionError();},Enclosure.Result::walls)!=repaired)throw new AssertionError();
  System.out.println("PASS: 1945 broken-screen seeds require one failed scan; repair invalidation recovers 4913-cell cabin and reuses success");
 }
}
