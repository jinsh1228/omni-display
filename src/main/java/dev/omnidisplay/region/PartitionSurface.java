package dev.omnidisplay.region;

/** Face UV mapping shared with screen geometry; no framebuffer or game bootstrap required. */
public final class PartitionSurface {
 private PartitionSurface(){}
 public static double[] point(int face,double u,double v,double plane){return switch(face){
  case 0->new double[]{u,plane,1-v};case 1->new double[]{u,plane,v};
  case 2->new double[]{1-u,1-v,plane};case 3->new double[]{u,1-v,plane};
  case 4->new double[]{plane,1-v,u};case 5->new double[]{plane,1-v,1-u};
  default->throw new IllegalArgumentException("Unknown screen face");
 };}
 /** Camera coordinates relative to the screen's cell, before the mask's depth offset. */
 public static boolean frontFacing(int face,double x,double y,double z,double plane){
  if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||!Double.isFinite(plane))return false;
  double coordinate=switch(face){case 0,1->y;case 2,3->z;case 4,5->x;default->throw new IllegalArgumentException("Unknown screen face");};
  return (face%2==1?coordinate-plane:plane-coordinate)>1e-7;
 }
 public static double distanceSquared(double x,double y,double z,double x0,double y0,double z0,double x1,double y1,double z1){
  if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))return Double.POSITIVE_INFINITY;
  double dx=Math.max(Math.max(x0-x,0),x-x1),dy=Math.max(Math.max(y0-y,0),y-y1),dz=Math.max(Math.max(z0-z,0),z-z1);
  return dx*dx+dy*dy+dz*dz;
 }
}
