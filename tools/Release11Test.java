import dev.omnidisplay.region.DoorScreenFade;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
public class Release11Test {
 public static void main(String[] args)throws Exception{
  if(DoorScreenFade.coverage(0)!=1||DoorScreenFade.coverage(1)!=0||DoorScreenFade.coverage(Float.NaN)!=0)throw new AssertionError("fade endpoints");
  float prior=1;for(int i=1;i<=1000;i++){float next=DoorScreenFade.coverage(i/1000f);if(next>prior||prior-next>.002)throw new AssertionError("fade discontinuity");prior=next;}
  var nixie=RuntimeHooksTest.read("com/simibubi/create/content/redstone/nixieTube/NixieTubeRenderer");int eyes=0;
  for(var method:nixie.methods)if(method.name.equals("renderAsSignal"))for(var instruction:method.instructions)if(instruction instanceof MethodInsnNode call&&call.owner.equals("net/minecraft/world/entity/Entity")&&call.name.equals("getEyePosition")&&call.desc.equals("(F)Lnet/minecraft/world/phys/Vec3;"))eyes++;
  if(eyes!=1)throw new AssertionError("Create 6.0.10 signal eye hook mismatch");
  var config=Files.readString(Path.of("src/main/java/dev/omnidisplay/RenderConfig.java"));
  if(!config.contains("define(\"lowPerformanceMode\",false)")||config.contains("define(\"experimentalRendering\""))throw new AssertionError("renderer defaults");
  var faces=Files.readString(Path.of("src/main/java/dev/omnidisplay/ScreenFaces.java"));if(!faces.contains("RenderConfig.experimental()))throw ex;"))throw new AssertionError("low mode must reject open cabin fallback");
  System.out.println("PASS: continuous door fade, installed Create signal-eye hook, default surface renderer and low-mode open-cabin guard");
 }
}
