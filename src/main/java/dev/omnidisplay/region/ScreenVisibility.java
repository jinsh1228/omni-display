package dev.omnidisplay.region;
import org.joml.*;
/** Conservative homogeneous clipping: only reject a box entirely outside one clip plane. */
public final class ScreenVisibility {
 public static boolean intersects(Matrix4d clip,double x0,double y0,double z0,double x1,double y1,double z1){
  int outside=63;
  for(int i=0;i<8;i++){
   var p=clip.transform(new Vector4d((i&1)==0?x0:x1,(i&2)==0?y0:y1,(i&4)==0?z0:z1,1));
   if(!Double.isFinite(p.x)||!Double.isFinite(p.y)||!Double.isFinite(p.z)||!Double.isFinite(p.w))return true;
   double pad=.02*java.lang.Math.abs(p.w)+.0001;
   int bits=(p.x < -p.w-pad?1:0)|(p.x>p.w+pad?2:0)|(p.y < -p.w-pad?4:0)|(p.y>p.w+pad?8:0)|(p.z < -p.w-pad?16:0)|(p.z>p.w+pad?32:0);outside&=bits;
  }return outside==0;
 }
}
