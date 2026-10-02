import org.joml.*;
public final class PanelTransformTest {
 public static void main(String[] args){
  var origin=new Vector3d(20481032,130,20640776);var eye=new Vector3d(-3566,-50,-3861);
  for(int i=0;i<360;i++){
   var local=new Matrix4d().translation(eye).rotate(new Quaterniond().rotationXYZ(i*.03,i*.04,i*.02)).translate(new Vector3d(origin).negate());
   var base=new Matrix4d().translation(new Vector3d(eye).negate()).mul(local);
   for(int face=0;face<6;face++){
    var pos=new Vector3d(origin).add((face-3)*3,face-2,2-face);
    var old=new Matrix4f(new Matrix4d(base).translate(pos));
    var cached=new Matrix4f(new Matrix4d(base).translate(origin)).translate((float)(pos.x-origin.x),(float)(pos.y-origin.y),(float)(pos.z-origin.z));
    for(int v=0;v<8;v++){var point=new Vector3f(v&1,(v>>1)&1,(v>>2)&1);if(old.transformPosition(new Vector3f(point)).distance(cached.transformPosition(new Vector3f(point)))>0.00001f)throw new AssertionError("Plot-relative panel precision regression");}
   }
  }
  System.out.println("PASS: shared panel transforms preserve all corners across 360 moving poses at 20-million plot coordinates");
 }
}
