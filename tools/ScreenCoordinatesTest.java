import dev.omnidisplay.region.*;
import org.joml.*;
public class ScreenCoordinatesTest {
 public static void main(String[] args){
  var anchor=new Vector3d(20481032,130,20640776);
  int oldFailures=0;
  for(int i=0;i<720;i++){
   var q=new Quaterniond().rotationXYZ(i*.017,i*.031,i*.007);
   double drift=1+1e-7;q.set(q.x*drift,q.y*drift,q.z*drift,q.w*drift);
   var pose=new Matrix4d().translation(-3566,-50,-3861).rotate(q).translate(-anchor.x,-anchor.y,-anchor.z);
   var local=new Vector3d(anchor).add(.8,.5,.8);var world=pose.transformPosition(new Vector3d(local));
   var old=new Matrix4d(pose).invert().transformPosition(new Vector3d(world));
   var fixed=ScreenCoordinates.localEye(pose,world);
   if(old.distance(local)>1)oldFailures++;
   if(fixed.distance(local)>1e-5)throw new AssertionError("large plot inverse mismatch: "+fixed.distance(local));
   if(!PartitionSurface.frontFacing(2,fixed.x-anchor.x,fixed.y-anchor.y,fixed.z-anchor.z,2)||!PartitionSurface.frontFacing(4,fixed.x-anchor.x,fixed.y-anchor.y,fixed.z-anchor.z,2))throw new AssertionError("interior north/west faces culled");
  }
  if(oldFailures==0)throw new AssertionError("failed to reproduce old inverse error");
  System.out.println("PASS: reproduced "+oldFailures+" large-coordinate inverse failures; corrected 720 poses and north/west interior face classification");
 }
}
