import dev.omnidisplay.region.HudDesign;
public class HudDesignTest {
 public static void main(String[] args){
  for(int mask=0;mask<64;mask++)for(double t:new double[]{0,1,30,1000}){
   var lines=HudDesign.front(t,120.5,-64,359,mask,true,true,true);
   if(mask==0&&!lines.isEmpty())throw new AssertionError("hidden instruments remain");
   for(var l:lines)if(!Double.isFinite(l.x()+l.y()+l.u()+l.v())||l.alpha()<0||l.alpha()>255)throw new AssertionError("invalid artwork");
  }
  if(HudDesign.target(0,0).equals(HudDesign.target(30,30)))throw new AssertionError("lock animation static");
  System.out.println("PASS: 64 instrument masks, finite geometry, target animation");
 }
}
