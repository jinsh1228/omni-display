package dev.omnidisplay.region;
/** Uniform transition avoids constructing the per-hexagon graph for large cabins. */
public final class LargeCabinFade {
 public static boolean enabled(Enclosure.Result shape){return shape!=null&&(shape.interior().size()>4096||shape.walls().size()>4096);}
 public static double amount(boolean base,long[] times,boolean[] targets,double now){
  double value=base?1:0;
  for(int i=0;i<times.length&&i<targets.length&&times[i]<=now;i++){
   double end=i+1<times.length?Math.min(now,times[i+1]):now;
   double t=Math.clamp((end-times[i])/ScreenWave.DURATION,0,1);t=t*t*(3-2*t);
   value+=((targets[i]?1:0)-value)*t;
  }
  return value;
 }
}
