package dev.omnidisplay.region;
/** Shared animation clock; a replaceable presentation envelope for the HUD. */
public final class HudFade {
 public static double amount(boolean base,long[] times,boolean[] targets,double now){
  double alpha=base?1:0;
  for(int i=0;i<times.length&&times[i]<=now;i++){
   double end=i+1<times.length?Math.min(now,times[i+1]):now;
   double duration=targets[i]?ScreenWave.DURATION:10;
   double t=Math.clamp((end-times[i])/duration,0,1);t=t*t*(3-2*t);
   alpha+=((targets[i]?1:0)-alpha)*t;
  }
  return alpha;
 }
}
