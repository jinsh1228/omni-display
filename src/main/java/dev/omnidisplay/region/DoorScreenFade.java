package dev.omnidisplay.region;
/** Screen fades out continuously as the physical sheet slides open, independent of OPEN flags. */
public final class DoorScreenFade {
 public static float coverage(float animation){
  if(!Float.isFinite(animation))return 0;
  float a=Math.clamp(animation,0f,1f);return 1-a*a*(3-2*a);
 }
}
