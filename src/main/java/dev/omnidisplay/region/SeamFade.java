package dev.omnidisplay.region;
/** Separate envelope keeps seams visible during startup, and restores them during shutdown. */
public final class SeamFade {
 public static double amount(boolean base,long[] times,boolean[] targets,double now,double target){
  double alpha=base?target:1;
  for(int i=0;i<times.length&&times[i]<=now;i++){
   double end=i+1<times.length?Math.min(now,times[i+1]):now;
   double delay=targets[i]?ScreenWave.DURATION:0;
   double t=Math.clamp((end-times[i]-delay)/10,0,1);t=t*t*(3-2*t);
   alpha+=((targets[i]?target:1)-alpha)*t;
  }
  return alpha;
 }
}
