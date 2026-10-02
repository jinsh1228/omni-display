import dev.omnidisplay.diagnostics.PerformanceDiagnostics;
import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.tree.*;
public final class PerformanceDiagnosticsTest {
 public static void main(String[] args)throws Exception{
  var mc=RuntimeHooksTest.read("net/minecraft/client/Minecraft");if(mc.methods.stream().noneMatch(m->m.name.equals("runTick")&&m.desc.equals("(Z)V")))throw new AssertionError("Frame hook signature changed");
  var game=RuntimeHooksTest.read("net/minecraft/client/renderer/GameRenderer");if(game.methods.stream().noneMatch(m->m.name.equals("render")&&m.desc.equals("(Lnet/minecraft/client/DeltaTracker;Z)V")))throw new AssertionError("Render hook signature changed");
  for(String hook:new String[]{"PerformanceFrameMixin","PerformanceRenderMixin"}){var type=RuntimeHooksTest.read("dev/omnidisplay/mixin/"+hook);if(type.methods.stream().flatMap(m->java.util.stream.StreamSupport.stream(m.instructions.spliterator(),false)).noneMatch(i->i instanceof MethodInsnNode c&&c.owner.startsWith("dev/omnidisplay/diagnostics/")))throw new AssertionError("Missing timing hook");}
  var directory=Path.of("build/local/diagnostic-test-"+System.nanoTime());PerformanceDiagnostics.initialize(directory);
  var limit=System.nanoTime()+8_000_000_000L;Path log=null;
  while(System.nanoTime()<limit){try(var files=Files.walk(directory)){log=files.filter(f->f.getFileName().toString().equals("stutter.log")).findFirst().orElse(null);}if(log!=null&&Files.readString(log).contains("JFR rolling capture started"))break;Thread.sleep(100);}
  if(log==null)throw new AssertionError("Diagnostic file not created");
  PerformanceDiagnostics.beginFrame();long start=PerformanceDiagnostics.begin(PerformanceDiagnostics.Phase.HOLOGRAM_BUILD);Thread.sleep(5);PerformanceDiagnostics.end(PerformanceDiagnostics.Phase.HOLOGRAM_BUILD,start);Thread.sleep(220);PerformanceDiagnostics.finishFrame("test-cabin");
  limit=System.nanoTime()+5_000_000_000L;while(System.nanoTime()<limit&&!Files.readString(log).contains("JFR_SAVED"))Thread.sleep(100);
  String text=Files.readString(log);if(!text.contains("FRAME_STALL")||!text.contains("HOLOGRAM_BUILD=")||!text.contains("cpuMs=")||!text.contains("gcCount="))throw new AssertionError("Incomplete stall evidence: "+text);
  if(!text.contains("JFR rolling capture started")||!text.contains("JFR_SAVED"))throw new AssertionError("JFR capture/dump did not work: "+text);
  try(var files=Files.walk(directory)){if(files.noneMatch(f->f.toString().endsWith(".jfr")))throw new AssertionError("Missing captured JFR");}
  PerformanceDiagnostics.idle();if(PerformanceDiagnostics.begin(PerformanceDiagnostics.Phase.RADAR)!=0)throw new AssertionError("Idle diagnostics still enabled");
  System.out.println("PASS: installed frame/render hook signatures, asynchronous stall log with phase/CPU/GC evidence, rolling JFR and automatic dump, idle disable");
 }
}
