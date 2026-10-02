import dev.omnidisplay.region.*;
import java.util.*;
public final class SurfaceGroupTest {
 record Owner(Object ship,Object moving,Enclosure.Result shape){}
 record Face(Owner owner,int index){}
 static int comparisons;
 static boolean match(Owner a,Owner b){comparisons++;return a.ship==b.ship&&a.moving==b.moving&&a.shape.equals(b.shape);}
 static Enclosure.Result shape(int offset){var walls=new HashSet<Enclosure.Cell>();var inside=new HashSet<Enclosure.Cell>();for(int x=0;x<20;x++)for(int y=0;y<20;y++)for(int z=0;z<20;z++){var cell=new Enclosure.Cell(x+offset,y,z);if(x==0||y==0||z==0||x==19||y==19||z==19)walls.add(cell);else inside.add(cell);}return new Enclosure.Result(Set.copyOf(walls),Set.copyOf(inside),new Enclosure.Cell(offset,0,0),new Enclosure.Cell(offset+19,19,19));}
 static void check(boolean v){if(!v)throw new AssertionError();}
 public static void main(String[] args)throws Exception{
  var ship=new Object();var a=new Owner(ship,null,shape(0));var b=new Owner(ship,null,shape(0));check(a.shape!=b.shape&&a.shape.equals(b.shape));
  var faces=new ArrayList<Face>();for(int i=0;i<2168;i++)faces.add(new Face(a,i));var groups=new IdentitySurfaceGroups<Owner,Face>(faces,Face::owner);
  comparisons=0;check(groups.matching(b,SurfaceGroupTest::match).size()==2168);check(comparisons==1);
  comparisons=0;check(groups.matching(a,SurfaceGroupTest::match).size()==2168);check(comparisons==0);
  check(groups.matching(new Owner(ship,null,shape(40)),SurfaceGroupTest::match).isEmpty());check(groups.matching(new Owner(new Object(),null,b.shape),SurfaceGroupTest::match).isEmpty());check(groups.matching(new Owner(ship,new Object(),b.shape),SurfaceGroupTest::match).isEmpty());
  var c=new Owner(ship,null,shape(40));faces.add(new Face(c,2168));var mixed=new IdentitySurfaceGroups<Owner,Face>(faces,Face::owner);comparisons=0;check(mixed.matching(b,SurfaceGroupTest::match).size()==2168);check(comparisons==2);check(mixed.matching(c,SurfaceGroupTest::match).size()==1);
  faces.clear();check(groups.matching(b,SurfaceGroupTest::match).size()==2168);
  var renderer=RuntimeHooksTest.read("dev/omnidisplay/client/PartitionRenderer");boolean hooked=false;for(var method:renderer.methods)if(method.name.equals("drawSurfaces"))for(var instruction:method.instructions)if(instruction instanceof org.objectweb.asm.tree.MethodInsnNode call){check(!(call.owner.equals("dev/omnidisplay/region/Enclosure$Result")&&call.name.equals("equals")));if(call.owner.equals("dev/omnidisplay/client/ScreenSurfaceCache")&&call.name.equals("forBody"))hooked=true;}check(hooked);
  System.out.println("PASS: 2168 surfaces / 5832 interior cells compare once after equal-shape replacement; distinct cabins, ships, moving structures and identity fast path preserved");
 }
}
