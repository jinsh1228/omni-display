import dev.omnidisplay.region.ScreenWave;
public class ScreenWaveTest {
 static void near(double a,double b){if(Math.abs(a-b)>1e-7)throw new AssertionError(a+" != "+b);}
 static double value(boolean base,long[] t,boolean[] targets,double now,double d){return ScreenWave.amount(base,t,targets,now,d);}
 public static void main(String[] args){
  long[] start={100};boolean[] on={true};
  near(value(false,start,on,100,0),0);near(value(false,start,on,103,0),.5);
  near(value(false,start,on,106,0),1);near(value(false,start,on,106,1),0);near(value(false,start,on,130,1),1);
  boolean[] off={false};near(value(true,start,off,106,0),1);near(value(true,start,off,106,1),0);near(value(true,start,off,130,1),0);
  // Every cell preserves its actual previous brightness when a reversal arrives.
  for(double d:new double[]{0,.1,.4,.8,1}){
   double before=value(false,start,on,111,d);
   near(value(false,new long[]{100,111},new boolean[]{true,false},111,d),before);
   near(value(false,new long[]{100,111},new boolean[]{true,false},141,d),0);
   near(value(false,new long[]{100,111,115},new boolean[]{true,false,true},145,d),1);
  }
  // Damage overrides the usual reverse ordering, without discontinuity.
  near(ScreenWave.amount(true,start,off,106,.0,0,0),0);
  near(ScreenWave.amount(true,start,off,106,1,1,0),1);
  for(double d:new double[]{0,.25,.8,1}){
   double before=value(false,start,on,112,d);
   near(ScreenWave.amount(false,new long[]{100,112},new boolean[]{true,false},112,d,1-d,1),before);
   near(ScreenWave.amount(false,new long[]{100,112},new boolean[]{true,false},142,d,1-d,1),0);
  }
  near(value(true,new long[0],new boolean[0],1000,.4),1);
  System.out.println("PASS: startup, reverse shutdown, damage-origin shutdown, 30-tick completion, per-cell reversal continuity, rapid reversal, stable legacy state");
 }
}
