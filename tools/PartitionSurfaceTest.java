import dev.omnidisplay.region.PartitionSurface;
public final class PartitionSurfaceTest {
 public static void main(String[] args){
  int[] axes={1,1,2,2,0,0};
  for(int face=0;face<6;face++)for(double plane:new double[]{0,3/16d,13/16d,1}){
   for(double u:new double[]{0,.5,1})for(double v:new double[]{0,.5,1}){
    var p=PartitionSurface.point(face,u,v,plane);if(p[axes[face]]!=plane)throw new AssertionError("Incorrect screen plane");
    for(int axis=0;axis<3;axis++)if(axis!=axes[face]&&(p[axis]<0||p[axis]>1))throw new AssertionError("Mask outside display cell");
   }
  }
  for(int face=0;face<6;face++)for(double plane:new double[]{0,1/16d,3/16d,13/16d,15/16d,1}){
   double[] eye={.5,.5,.5};double inward=face%2==1?1:-1;
   eye[axes[face]]=plane+inward*.25;
   if(!PartitionSurface.frontFacing(face,eye[0],eye[1],eye[2],plane))throw new AssertionError("Inward screen lost from an outside doorway");
   eye[axes[face]]=plane-inward*.25;
   if(PartitionSurface.frontFacing(face,eye[0],eye[1],eye[2],plane))throw new AssertionError("Screen back reveals the world");
   eye[axes[face]]=plane;
   if(PartitionSurface.frontFacing(face,eye[0],eye[1],eye[2],plane))throw new AssertionError("Edge-on screen leaks");
  }
  if(PartitionSurface.frontFacing(1,0,Double.NaN,0,0))throw new AssertionError("Invalid camera");
  // World-to-ship inversion supplies local coordinates; use huge storage coordinates without floats.
  double origin=30000000;
  if(PartitionSurface.distanceSquared(origin+3,2,2,origin,0,0,origin+4,4,4)!=0)throw new AssertionError("Inside distance");
  if(PartitionSurface.distanceSquared(origin+7,7,4,origin,0,0,origin+4,4,4)!=18)throw new AssertionError("Outside distance");
  try{PartitionSurface.point(6,0,0,0);throw new AssertionError();}catch(IllegalArgumentException expected){}
  System.out.println("PASS: screen planes, inward/outward/edge-on masks for all six directions, external observer selection distance");
 }
}
