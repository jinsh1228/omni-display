import dev.omnidisplay.region.TrapdoorGeometry;
public final class TrapdoorGeometryTest {
 public static void main(String[] args){
  for(boolean high:new boolean[]{false,true}){
   double lo=TrapdoorGeometry.plane(high,false),hi=TrapdoorGeometry.plane(high,true);
   if(hi-lo!=3/16d||lo!=(high?13/16d:0)||hi!=(high?1:3/16d))throw new AssertionError("Hatch screen differs from vanilla collision geometry");
  }
  System.out.println("PASS: hatch screen planes match vanilla 3/16 top/bottom and open side collision");
 }
}
