public final class OptionalPhotonLoadTest {
 public static void main(String[] args)throws Exception{
  try{Class.forName("net.irisshaders.iris.Iris");throw new AssertionError("Test classpath unexpectedly includes Iris");}catch(ClassNotFoundException expected){}
  Class<?> adapter=Class.forName("dev.omnidisplay.client.IrisPhotonCompat");
  adapter.getMethod("refresh").invoke(null);
  if(!(boolean)adapter.getMethod("allowed").invoke(null)||(boolean)adapter.getMethod("enabled").invoke(null))throw new AssertionError("Missing Iris must retain ordinary partition rendering");
  if((boolean)adapter.getMethod("skipFrameUpdate",Object.class).invoke(null,new Object()))throw new AssertionError("Unrelated render notifier was suppressed");
  System.out.println("PASS: Photon adapter loads and keeps ordinary rendering without Iris/Voxy on the classpath");
 }
}
