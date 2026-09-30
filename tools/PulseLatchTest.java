import dev.omnidisplay.region.PulseLatch;
public class PulseLatchTest {
 static void eq(int n,int expected){if(n!=expected)throw new AssertionError(n+" != "+expected);}
 public static void main(String[] a){
  var p=new PulseLatch();p.sample(15);p.arm();eq(p.drain(),0); // loading while high is not a pulse
  p.sample(15);p.sample(3);eq(p.drain(),0); // held input and strength changes
  p.sample(0);p.sample(1);eq(p.drain(),1);eq(p.drain(),0);
  p.sample(0);p.sample(15);p.sample(0);p.sample(15);eq(p.drain(),2); // two edges cancel two toggles
  p.stop();p.sample(0);p.sample(15);eq(p.drain(),0);
  p.arm();p.sample(15);eq(p.drain(),0);p.sample(0);p.sample(15);eq(p.drain(),1);
  System.out.println("PASS: initial-high, held-high, analog changes, repeated edges, drain, stop/rearm");
 }
}
