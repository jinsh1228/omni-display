import dev.omnidisplay.region.HudFade;
public class HudFadeTest {
 static void near(double a,double b){if(Math.abs(a-b)>1e-8)throw new AssertionError(a+" != "+b);}
 public static void main(String[] args){
  long[] t={100};boolean[] on={true},off={false};
  near(HudFade.amount(false,t,on,100),0);near(HudFade.amount(false,t,on,115),.5);near(HudFade.amount(false,t,on,130),1);
  near(HudFade.amount(true,t,off,100),1);near(HudFade.amount(true,t,off,105),.5);near(HudFade.amount(true,t,off,110),0);
  double before=HudFade.amount(false,t,on,112);
  near(HudFade.amount(false,new long[]{100,112},new boolean[]{true,false},112),before);
  near(HudFade.amount(false,new long[]{100,112},new boolean[]{true,false},122),0);
  near(HudFade.amount(false,new long[]{100,112,116},new boolean[]{true,false,true},146),1);
  System.out.println("PASS: HUD fade-in/out, endpoints, interruption continuity, rapid reversal");
 }
}
