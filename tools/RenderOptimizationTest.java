import dev.omnidisplay.region.*;
import org.joml.Matrix4d;
import org.objectweb.asm.tree.*;
public final class RenderOptimizationTest {
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args)throws Exception{
  var p=new Matrix4d().perspective(java.lang.Math.PI/2,16d/9,.1,100);
  check(ScreenVisibility.intersects(p,-.5,-.5,-4,.5,.5,-3));
  check(!ScreenVisibility.intersects(p,100,-.5,-4,101,.5,-3));
  check(!ScreenVisibility.intersects(p,-.5,100,-4,.5,101,-3));
  check(!ScreenVisibility.intersects(p,-.5,-.5,2,.5,.5,3));
  check(ScreenVisibility.intersects(p,-1,-1,-.2,1,1,.2));
  check(ScreenVisibility.intersects(new Matrix4d().m00(Double.NaN),0,0,0,1,1,1));
  var transformed=new Matrix4d(p).translate(-24000000,64,23000000).translate(24000000,-64,-23000000);check(ScreenVisibility.intersects(transformed,-.5,-.5,-4,.5,.5,-3));
  for(int frame=0;frame<100;frame++){check(!ScreenVisibility.intersects(p,100,-.5,-4,101,.5,-3));check(ScreenVisibility.intersects(p,-.5,-.5,-4,.5,.5,-3));}
  check(TopologyCachePolicy.reusable(4,4,true,true,99,0));check(TopologyCachePolicy.reusable(4,4,true,true,100,0));check(TopologyCachePolicy.reusable(4,4,true,true,1000000,0));check(!TopologyCachePolicy.reusable(5,4,true,true,1,0));check(!TopologyCachePolicy.reusable(4,4,false,true,1,0));check(!TopologyCachePolicy.reusable(4,4,true,true,0,1));
  var mixin=RuntimeHooksTest.read("dev/omnidisplay/mixin/PanelBlockChangeMixin");check(mixin.methods.stream().flatMap(m->java.util.stream.StreamSupport.stream(m.instructions.spliterator(),false)).anyMatch(i->i instanceof MethodInsnNode c&&c.owner.equals("dev/omnidisplay/ScreenTopologyCache")&&c.name.equals("invalidate")));
  var renderer=RuntimeHooksTest.read("dev/omnidisplay/client/PartitionRenderer");var method=renderer.methods.stream().filter(m->m.name.equals("renderScenes")).findFirst().orElseThrow();int visible=-1,pair=-1,normalOnly=-1,index=0;
  for(var ins:method.instructions){if(ins instanceof MethodInsnNode c){if(c.name.equals("potentiallyVisible"))visible=index;if(c.name.equals("beginPair"))pair=index;}if(ins instanceof FieldInsnNode f&&f.name.equals("invisibleFrame")&&f.getOpcode()==179&&ins.getPrevious().getOpcode()==4)normalOnly=index;index++;}
  check(visible>=0&&normalOnly>visible&&pair>normalOnly);
  System.out.println("PASS: conservative frustum/near-plane clipping, per-frame re-entry, large coordinates, long-lived cache reuse, edit/mode/clock-reset invalidation and normal-only render branch");
 }
}
