import dev.omnidisplay.region.SlidingScreenGeometry;
public class SlidingScreenGeometryTest {
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args){
  check(SlidingScreenGeometry.visible(true,0));
  for(int i=0;i<1000;i++){
   float a=i/1000f;check(SlidingScreenGeometry.visible(true,a));check(SlidingScreenGeometry.visible(false,a));
   check(Math.abs(SlidingScreenGeometry.slide(a)-a*a*13/16f)<1e-7);
   check(Math.abs(SlidingScreenGeometry.out(a)-Math.clamp(a*10,0,1)/32f)<1e-7);
   // Opening and closing must use the same position, with no snap at the OPEN flag change.
   if(i>0)check(SlidingScreenGeometry.slide(a)>SlidingScreenGeometry.slide((i-1)/1000f));
  }
  check(!SlidingScreenGeometry.visible(true,1));check(SlidingScreenGeometry.visible(false,1));
  check(SlidingScreenGeometry.slide(1)==13/16d);check(SlidingScreenGeometry.out(1)==1/32d);
  System.out.println("PASS: opening/closing door-mask continuity; terminal open remnant unchanged; model translation alignment");
 }
}
