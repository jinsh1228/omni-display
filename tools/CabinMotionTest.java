import dev.omnidisplay.region.CabinViewCoordinates;
import org.joml.*;
public class CabinMotionTest {
 static void near(Vector3d a,Vector3d b){if(a.distance(b)>1e-6)throw new AssertionError(a+" != "+b);}
 static boolean inside(Vector3d p){return p.x>0&&p.x<4&&p.y>0&&p.y<4&&p.z>0&&p.z<4;}
 public static void main(String[] args){
  Vector3d eye=new Vector3d(1.5,2.2,1.5);
  for(int i=0;i<720;i++){
   Matrix4d pose=new Matrix4d().translation(i*125.0,70+i*.7,-i*90.0).rotateXYZ(i*.37,i*.59,i*.81);
   Matrix4d inverse=new Matrix4d(pose).invert();Vector3d world=pose.transformPosition(new Vector3d(eye));
   var actual=CabinViewCoordinates.localEye(world.x,world.y,world.z,false,inverse::transformPosition);near(actual,eye);if(!inside(actual))throw new AssertionError("false cabin exit");
   Vector3d outside=pose.transformPosition(new Vector3d(8,2,2));if(inside(CabinViewCoordinates.localEye(outside.x,outside.y,outside.z,false,inverse::transformPosition)))throw new AssertionError("exit not detected");
  }
  near(CabinViewCoordinates.localEye(1.5,2.2,1.5,true,p->{throw new AssertionError("local eye transformed twice");}),eye);
  Matrix4d newPose=new Matrix4d().translation(100,0,0).rotateY(1);
  if(inside(newPose.invert().transformPosition(new Vector3d(eye))))throw new AssertionError("old camera should reproduce stale-pose mismatch");
  System.out.println("PASS: 720 rapid translation/rotation poses; local-coordinate path; actual exit; stale-camera regression");
 }
}
