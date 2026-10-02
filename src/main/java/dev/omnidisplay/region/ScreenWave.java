package dev.omnidisplay.region;
/** Time-based shared wave, including continuous reversal while a previous wave is unfinished. */
public final class ScreenWave {
 public static final int SPREAD=24,FADE=6,DURATION=SPREAD+FADE;
 public static double amount(boolean base,long[] times,boolean[] targets,double now,double distance){
  return amount(base,times,targets,now,distance,distance,-1);
 }
 public static double delay(boolean target,double distance,double faultDistance,int event,int faultEvent){return event==faultEvent?faultDistance:(target?distance:1-distance);}
 public static double delay(boolean target,double distance,double faultDistance,int event,int faultEvent,double[] clicks){return event<clicks.length&&Double.isFinite(clicks[event])?clicks[event]:delay(target,distance,faultDistance,event,faultEvent);}
 public static double amount(boolean base,long[] times,boolean[] targets,double now,double distance,double faultDistance,int faultEvent){return amount(base,times,targets,now,distance,faultDistance,faultEvent,new double[0]);}
 public static double amount(boolean base,long[] times,boolean[] targets,double now,double distance,double faultDistance,int faultEvent,double[] clicks){
  double value=base?1:0;
  for(int i=0;i<times.length&&times[i]<=now;i++){
   double end=i+1<times.length?Math.min(now,times[i+1]):now;
   double t=Math.clamp((end-times[i]-delay(targets[i],distance,faultDistance,i,faultEvent,clicks)*SPREAD)/FADE,0,1);t=t*t*(3-2*t);
   value+=( (targets[i]?1:0)-value)*t;
  }
  return value;
 }
}
