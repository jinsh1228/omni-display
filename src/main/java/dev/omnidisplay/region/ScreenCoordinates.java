package dev.omnidisplay.region;
import org.joml.*;
/** Render poses can carry an ORTHONORMAL hint despite small quaternion drift.
 * Transpose-based inversion amplifies that drift by the huge plot translation. */
public final class ScreenCoordinates {
 private ScreenCoordinates(){}
 public static Vector3d localEye(Matrix4dc pose,Vector3dc eye){
  return new Matrix4d(pose).assume(0).invert().transformPosition(new Vector3d(eye));
 }
}
