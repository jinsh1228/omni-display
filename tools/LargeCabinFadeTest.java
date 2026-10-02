import dev.omnidisplay.region.*;
import java.util.*;
import org.objectweb.asm.tree.*;
public final class LargeCabinFadeTest {
 static void check(boolean value){if(!value)throw new AssertionError();}
 static Enclosure.Result shape(int interior){var cells=new HashSet<Enclosure.Cell>();for(int i=0;i<interior;i++)cells.add(new Enclosure.Cell(i,0,0));return new Enclosure.Result(Set.of(new Enclosure.Cell(-1,0,0)),Set.copyOf(cells),new Enclosure.Cell(-1,0,0),new Enclosure.Cell(interior,0,0));}
 public static void main(String[] args)throws Exception{
  check(!LargeCabinFade.enabled(shape(4096)));check(LargeCabinFade.enabled(shape(4097)));check(LargeCabinFade.enabled(shape(4913)));
  long[] times={0};boolean[] on={true};check(LargeCabinFade.amount(false,times,on,0)==0);check(LargeCabinFade.amount(false,times,on,15)==.5);check(LargeCabinFade.amount(false,times,on,30)==1);
  check(LargeCabinFade.amount(true,times,new boolean[]{false},15)==.5);check(LargeCabinFade.amount(true,times,new boolean[]{false},30)==0);
  double before=LargeCabinFade.amount(false,times,on,10),at=LargeCabinFade.amount(false,new long[]{0,10},new boolean[]{true,false},10);check(Math.abs(before-at)<1e-12);check(LargeCabinFade.amount(false,new long[]{0,10},new boolean[]{true,false},40)==0);
  double previous=0;for(int i=0;i<=300;i++){double now=i/10d,value=LargeCabinFade.amount(false,times,on,now);check(value>=previous&&value<=1);previous=value;}
  var renderer=RuntimeHooksTest.read("dev/omnidisplay/client/ScreenAnimation");boolean found=false;for(var method:renderer.methods)if(method.name.equals("draw")){int enabled=-1,uniform=-1,select=-1,i=0;for(var ins:method.instructions){if(ins instanceof MethodInsnNode c){if(c.owner.equals("dev/omnidisplay/region/LargeCabinFade")&&c.name.equals("enabled"))enabled=i;if(c.name.equals("drawUniform"))uniform=i;if(c.name.equals("select"))select=i;}i++;}if(enabled>=0){check(uniform>enabled&&select>uniform);found=true;}}check(found);
  System.out.println("PASS: 4096/4097-cell threshold, 4913-cell large cabin, uniform fade endpoints/continuity/reversal and early branch before hex graph selection");
 }
}
