import dev.omnidisplay.region.RepairSyncBudget;
import java.util.*;
import org.objectweb.asm.tree.*;
public final class PeriodicStutterTest {
 public static void main(String[] args)throws Exception{
  for(int size:new int[]{0,54,1946,4096,20000}){var pending=new ArrayDeque<Integer>();for(int i=0;i<size;i++)pending.add(i);var visited=new HashSet<Integer>();int budget=RepairSyncBudget.perTick(size);for(int tick=0;tick<100;tick++)for(int i=0;i<budget&&!pending.isEmpty();i++)if(!visited.add(pending.removeFirst()))throw new AssertionError();if(visited.size()!=size||!pending.isEmpty())throw new AssertionError();if(size==1946&&budget!=32)throw new AssertionError();}
  var type=Class.forName("net.neoforged.neoforge.client.model.data.ModelDataManager",false,PeriodicStutterTest.class.getClassLoader());var be=Class.forName("net.minecraft.world.level.block.entity.BlockEntity",false,PeriodicStutterTest.class.getClassLoader());if(type.getDeclaredMethod("requestRefresh",be).getReturnType()!=void.class)throw new AssertionError();
  var hook=RuntimeHooksTest.read("dev/omnidisplay/mixin/HologramModelDataMixin");if(hook.methods.stream().flatMap(m->java.util.stream.StreamSupport.stream(m.instructions.spliterator(),false)).noneMatch(i->i instanceof MethodInsnNode c&&c.owner.equals("dev/omnidisplay/client/HologramRenderer")&&c.name.equals("changed")))throw new AssertionError();
  System.out.println("PASS: bounded repair queues complete within 100 ticks, no duplicate sends; installed model-data refresh hook supported");
 }
}
