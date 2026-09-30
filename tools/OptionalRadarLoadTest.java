public class OptionalRadarLoadTest {
 public static void main(String[] args)throws Exception {
  try{Class.forName("com.happysg.radar.CreateRadar",false,OptionalRadarLoadTest.class.getClassLoader());throw new AssertionError("Radar must be absent for this test");}catch(ClassNotFoundException expected){}
  for(String name:new String[]{"dev.omnidisplay.RadarHooks","dev.omnidisplay.CabinManager","dev.omnidisplay.client.RadarOverlay"}){
   var c=Class.forName(name,false,OptionalRadarLoadTest.class.getClassLoader());c.getDeclaredMethods();c.getDeclaredFields();
  }
  System.out.println("PASS: optional integration entry points load without radar classes (no game bootstrap)");
 }
}
