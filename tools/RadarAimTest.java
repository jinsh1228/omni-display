import dev.omnidisplay.region.RadarAimMath;
public class RadarAimTest {
 static void equal(double actual,double expected){if(Math.abs(actual-expected)>1e-7||Double.isNaN(actual))throw new AssertionError(actual+" != "+expected);}
 public static void main(String[] args){
  equal(RadarAimMath.distance(100,70,100,100,70,8292,95,65,200,105,75,210),100);
  equal(RadarAimMath.distance(100,70,100,8292,70,100,200,65,95,210,75,105),100);
  equal(RadarAimMath.distance(100,70,100,-8092,70,100,-10,65,95,0,75,105),100);
  equal(RadarAimMath.distance(0,0,0,100,100,0,10,10,-1,20,20,1),Math.sqrt(200));
  equal(RadarAimMath.distance(0,0,0,0,0,100,-1,-1,-1,1,1,1),0);
  if(Double.isFinite(RadarAimMath.distance(0,0,0,0,0,100,-1,-1,-10,1,1,-5)))throw new AssertionError("behind camera");
  if(Double.isFinite(RadarAimMath.distance(0,0,0,0,0,100,10,-1,10,20,1,20)))throw new AssertionError("parallel miss");
  if(!RadarAimMath.valid(100,70,100,0,0,1)||RadarAimMath.valid(Double.NaN,0,0,0,0,1)||RadarAimMath.valid(0,0,0,0,0,0))throw new AssertionError("validation");
  System.out.println("PASS: translated/rotated world rays, inside box, behind camera, miss, invalid input");
 }
}
