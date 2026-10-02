package dev.omnidisplay.client;
/** Heading: north=0, east=90. Pitch: nose up positive. Roll: right side up positive. */
public record HudAngles(double heading,double pitch,double roll) {
 public static HudAngles fromVectors(double x,double y,double z,double rightY,double upY){
  return new HudAngles((Math.toDegrees(Math.atan2(x,-z))+360)%360,
   Math.toDegrees(Math.asin(Math.max(-1,Math.min(1,y)))),Math.toDegrees(Math.atan2(rightY,upY)));
 }
}
