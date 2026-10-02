package dev.omnidisplay.region;
/** Same translation as Create sliding-door renderer; fully open remnants keep their old behavior. */
public final class SlidingScreenGeometry {
 private SlidingScreenGeometry(){}
 public static double slide(float animation){return animation*animation*13/16f;}
 public static double out(float animation){return Math.clamp(animation*10,0,1)/32f;}
 public static boolean visible(boolean open,float animation){return !open||animation<.99999f;}
}
