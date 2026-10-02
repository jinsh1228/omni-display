package dev.omnidisplay.region;

/** Unit-cell geometry shared by collision, screen planes and camera containment. */
public final class ThinGeometry {
 private ThinGeometry(){}
 public static final double THICKNESS=1/16d;
 public static double min(boolean positiveBoundary){return positiveBoundary?1-THICKNESS:0;}
 public static double max(boolean positiveBoundary){return positiveBoundary?1:THICKNESS;}
 public static double plane(boolean positiveBoundary,boolean positiveNormal){return positiveNormal?max(positiveBoundary):min(positiveBoundary);}
 public static boolean inside(boolean positiveBoundary,boolean positiveNormal,double coordinate){return (coordinate-plane(positiveBoundary,positiveNormal))*(positiveNormal?1:-1)>0;}
}
