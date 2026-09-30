import dev.omnidisplay.region.*;
public class ClickWaveLightTest {
 static void near(double a,double b){if(Math.abs(a-b)>1e-7)throw new AssertionError(a+" != "+b);}
 public static void main(String[] args){
  long[] on={0};boolean[] yes={true};boolean[] no={false};
  near(ScreenWave.amount(false,on,yes,6,1,0,-1,new double[]{0}),1);
  near(ScreenWave.amount(false,on,yes,6,0,0,-1,new double[]{1}),0);
  near(ScreenWave.amount(true,on,no,6,0,0,-1,new double[]{0}),0);
  near(ScreenWave.amount(true,on,no,6,1,0,-1,new double[]{1}),1);
  // A new click at another point must not change the previous wave at the reversal instant.
  for(double d:new double[]{0,.3,1})near(ScreenWave.amount(false,new long[]{0,12},new boolean[]{true,false},12,.6,0,-1,new double[]{d,1-d}),ScreenWave.amount(false,on,yes,12,.6,0,-1,new double[]{d}));
  near(ScreenWave.delay(false,.2,.7,0,-1,new double[]{Double.NaN}),.8);
  near(ScreenWave.delay(false,.2,.7,0,0,new double[]{Double.NaN}),.7);
  for(int brightness=0;brightness<=300;brightness++){
   int alpha=HudLight.alpha(175,brightness);if(alpha<0||alpha>255)throw new AssertionError("alpha");
   int color=HudLight.color(0x70e6de,brightness);if(color<0||color>0xffffff)throw new AssertionError("color");
  }
  near(HudLight.alpha(255,0),0);near(HudLight.color(0x70e6de,100),0x70e6de);
  System.out.println("PASS: click-origin startup/shutdown, distinct-origin reversal, legacy/fault fallback, brightness bounds");
 }
}
