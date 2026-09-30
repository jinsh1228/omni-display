import dev.omnidisplay.region.ThinGeometry;
public class ThinGeometryTest {
 static void check(boolean v){if(!v)throw new AssertionError();}
 public static void main(String[] args){
  for(boolean edge:new boolean[]{false,true}){
   double lo=ThinGeometry.min(edge),hi=ThinGeometry.max(edge);
   check(hi-lo==1/16d);check(lo==0||hi==1);check(!(lo<.5&&hi>.5));
   check(!ThinGeometry.inside(edge,false,(lo+hi)/2));check(!ThinGeometry.inside(edge,true,(lo+hi)/2));
   check(ThinGeometry.inside(edge,false,lo-.001));check(ThinGeometry.inside(edge,true,hi+.001));
   check(!ThinGeometry.inside(edge,false,hi+.001));check(!ThinGeometry.inside(edge,true,lo-.001));
  }
  // Standing in the formerly empty portion of a boundary cell must stay in the cabin.
  check(ThinGeometry.inside(false,true,.8));check(ThinGeometry.inside(true,false,.2));
  System.out.println("PASS: edge-aligned collision, matching screen planes, interior/exterior and empty boundary-cell camera positions");
 }
}
