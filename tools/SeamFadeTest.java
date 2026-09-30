import dev.omnidisplay.region.SeamFade;
public class SeamFadeTest {
 static void near(double a,double b){if(Math.abs(a-b)>1e-8)throw new AssertionError(a+" != "+b);}
 public static void main(String[] args){
  long[] t={100};boolean[] on={true};
  near(SeamFade.amount(false,t,on,130,.2),1);
  near(SeamFade.amount(false,t,on,135,.2),.6);
  near(SeamFade.amount(false,t,on,140,.2),.2);
  near(SeamFade.amount(false,t,on,140,0),0);
  near(SeamFade.amount(false,t,on,140,1),1);
  near(SeamFade.amount(true,new long[]{150},new boolean[]{false},155,.2),.6);
  near(SeamFade.amount(false,new long[]{100,135},new boolean[]{true,false},135,.2),.6);
  near(SeamFade.amount(false,new long[]{100,135},new boolean[]{true,false},145,.2),1);
  System.out.println("PASS: post-startup seam fade, 0/20/100%, shutdown restoration, reversal continuity");
 }
}
