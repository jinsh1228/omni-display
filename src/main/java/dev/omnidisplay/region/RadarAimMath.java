package dev.omnidisplay.region;
/** World-space segment/AABB selection, independent of voxel ray tracing. */
public final class RadarAimMath {
 public static boolean valid(double x,double y,double z,double dx,double dy,double dz){
  double length=dx*dx+dy*dy+dz*dz;
  return Double.isFinite(x)&&Double.isFinite(y)&&Double.isFinite(z)&&Double.isFinite(length)&&length>.81&&length<1.21;
 }
 public static double distance(double ax,double ay,double az,double bx,double by,double bz,double minX,double minY,double minZ,double maxX,double maxY,double maxZ){
  double[] start={ax,ay,az},delta={bx-ax,by-ay,bz-az},min={minX,minY,minZ},max={maxX,maxY,maxZ};double enter=0,exit=1;
  for(int i=0;i<3;i++){
   if(!Double.isFinite(start[i])||!Double.isFinite(delta[i])||!Double.isFinite(min[i])||!Double.isFinite(max[i])||min[i]>max[i])return Double.POSITIVE_INFINITY;
   if(Math.abs(delta[i])<1e-12){if(start[i]<min[i]||start[i]>max[i])return Double.POSITIVE_INFINITY;continue;}
   double a=(min[i]-start[i])/delta[i],b=(max[i]-start[i])/delta[i];
   enter=Math.max(enter,Math.min(a,b));exit=Math.min(exit,Math.max(a,b));if(enter>exit)return Double.POSITIVE_INFINITY;
  }
  return enter*Math.sqrt(delta[0]*delta[0]+delta[1]*delta[1]+delta[2]*delta[2]);
 }
}
