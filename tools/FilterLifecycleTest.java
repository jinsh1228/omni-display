import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
public class FilterLifecycleTest {
 public static void main(String[] args)throws Exception{
  var game=RuntimeHooksTest.read("net/minecraft/client/renderer/GameRenderer");boolean deferred=false;
  for(var method:game.methods)if(method.name.equals("renderLevel")){
   boolean rendered=false;for(var instruction:method.instructions){
    if(instruction instanceof MethodInsnNode call&&call.owner.equals("net/minecraft/client/renderer/LevelRenderer")&&call.name.equals("renderLevel"))rendered=true;
    if(instruction instanceof FieldInsnNode field&&field.name.equals("AFTER_LEVEL")){if(!rendered)throw new AssertionError("Unexpected event order");deferred=true;}
   }
  }
  if(!deferred)throw new AssertionError("Missing installed AFTER_LEVEL event");
  var renderer=RuntimeHooksTest.read("dev/omnidisplay/client/PartitionRenderer");
  var scenes=renderer.methods.stream().filter(m->m.name.equals("renderScenes")).findFirst().orElseThrow();
  var timed=renderer.methods.stream().filter(m->m.name.equals("timedWorld")).findFirst().orElseThrow();
  if(java.util.stream.StreamSupport.stream(timed.instructions.spliterator(),false).noneMatch(i->i instanceof MethodInsnNode c&&c.owner.equals("net/minecraft/client/renderer/LevelRenderer")&&c.name.equals("renderLevel")))throw new AssertionError("Timing bridge must preserve installed world renderer");
  int calls=0;AbstractInsnNode start=null;
  for(var instruction:scenes.instructions)if(instruction instanceof MethodInsnNode call&&call.owner.equals("dev/omnidisplay/client/PartitionRenderer")&&call.name.equals("timedWorld")){
   if(++calls==2){start=instruction.getNext();break;}
  }
  if(start==null)throw new AssertionError("Missing optimized world render");
  boolean returned=false;
  for(var instruction=start;instruction!=null;instruction=instruction.getNext()){
   if(instruction.getOpcode()==Opcodes.GOTO){instruction=((JumpInsnNode)instruction).label;continue;}
   if(instruction.getOpcode()==Opcodes.RETURN){returned=true;break;}
   if(instruction instanceof FieldInsnNode field&&field.getOpcode()==Opcodes.PUTSTATIC&&(field.name.equals("prepared")||field.name.equals("singlePass")))throw new AssertionError("Filter frame cleared before GameRenderer's AFTER_LEVEL event");
  }
  if(!returned)throw new AssertionError("Optimized path did not return normally");
  var compose=renderer.methods.stream().filter(m->m.name.equals("compose")).findFirst().orElseThrow();
  for(String flag:new String[]{"prepared","singlePass"}){
   boolean cleared=false;for(var instruction:compose.instructions)if(instruction instanceof FieldInsnNode field&&field.getOpcode()==Opcodes.PUTSTATIC&&field.name.equals(flag)&&instruction.getPrevious().getOpcode()==Opcodes.ICONST_0)cleared=true;
   if(!cleared)throw new AssertionError("Composition must release "+flag);
  }
  System.out.println("PASS: installed AFTER_LEVEL runs after world render; optimized filter state survives return and is cleared by composition");
 }
}
