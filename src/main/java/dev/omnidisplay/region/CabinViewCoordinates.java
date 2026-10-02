package dev.omnidisplay.region;
import org.joml.Vector3d;
import java.util.function.UnaryOperator;
/** An entity simulation position must be paired with the current logical pose. */
public final class CabinViewCoordinates {
 public static Vector3d localEye(double x,double y,double z,boolean alreadyLocal,UnaryOperator<Vector3d> worldToLocal){
  var p=new Vector3d(x,y,z);return alreadyLocal?p:worldToLocal.apply(p);
 }
}
