import dev.omnidisplay.client.HudAngles;
public class HudAnglesTest {
 static void eq(double a,double b){if(Math.abs(a-b)>1e-6)throw new AssertionError(a+" != "+b);}
 public static void main(String[] args){
  eq(HudAngles.fromVectors(0,0,-1,0,1).heading(),0);
  eq(HudAngles.fromVectors(1,0,0,0,1).heading(),90);
  eq(HudAngles.fromVectors(0,0,1,0,1).heading(),180);
  eq(HudAngles.fromVectors(-1,0,0,0,1).heading(),270);
  eq(HudAngles.fromVectors(0,.5,-Math.sqrt(.75),0,1).pitch(),30);
  eq(HudAngles.fromVectors(0,-.5,-Math.sqrt(.75),0,1).pitch(),-30);
  eq(HudAngles.fromVectors(0,0,-1,.5,Math.sqrt(.75)).roll(),30);
  eq(HudAngles.fromVectors(0,1.00000001,0,0,1).pitch(),90);
  System.out.println("PASS: cardinal headings, positive/negative pitch, roll, numerical clamp");
 }
}
