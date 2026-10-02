package dev.omnidisplay.region;

public final class TrapdoorGeometry {
 private TrapdoorGeometry(){}
 public static double plane(boolean attachedPositive,boolean normalPositive){return attachedPositive?(normalPositive?1:13/16d):(normalPositive?3/16d:0);}
}
