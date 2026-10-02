import org.objectweb.asm.tree.*;
import java.util.*;
import dev.omnidisplay.MovingScreens;
import net.minecraft.nbt.CompoundTag;
public class MovingScreenHooksTest {
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args)throws Exception{
  var contraption=RuntimeHooksTest.read("com/simibubi/create/content/contraptions/Contraption");
  Map<String,String> methods=Map.of(
   "getBlockEntityNBT","(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/nbt/CompoundTag;",
   "addBlock","(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lorg/apache/commons/lang3/tuple/Pair;)V",
   "removeBlocksFromWorld","(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V",
   "addBlocksToWorld","(Lnet/minecraft/world/level/Level;Lcom/simibubi/create/content/contraptions/StructureTransform;)V");
  methods.forEach((name,desc)->check(contraption.methods.stream().filter(m->m.name.equals(name)&&m.desc.equals(desc)).count()==1));
  check(contraption.fields.stream().anyMatch(f->f.name.equals("updateTags")&&f.desc.equals("Ljava/util/Map;")));
  var entity=RuntimeHooksTest.read("com/simibubi/create/content/contraptions/AbstractContraptionEntity");
  check(entity.methods.stream().anyMatch(m->m.name.equals("handlePlayerInteraction")&&m.desc.equals("(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/InteractionHand;)Z")));
  var bearing=RuntimeHooksTest.read("com/simibubi/create/content/contraptions/bearing/MechanicalBearingBlockEntity");
  check(bearing.methods.stream().anyMatch(m->m.name.equals("getMovedContraption")&&m.desc.equals("()Lcom/simibubi/create/content/contraptions/ControlledContraptionEntity;")));
  var original=new CompoundTag();var cabin=new CompoundTag();cabin.putBoolean("power",true);cabin.putUUID("group",UUID.randomUUID());original.put("cabin",cabin);original.putLong("originOffset",123);original.putString("unrelatedInventory","preserve on server, do not copy into screen-only sync");
  var panel=new CompoundTag();panel.putInt("mask",42);panel.put("cabin",cabin.copy());original.put(MovingScreens.PANEL,panel);
  var onlyScreen=MovingScreens.metadata(original);check(onlyScreen.getAllKeys().equals(Set.of("cabin","originOffset",MovingScreens.PANEL)));check(onlyScreen.getCompound(MovingScreens.PANEL).getInt("mask")==42);onlyScreen.getCompound("cabin").putBoolean("power",false);check(original.getCompound("cabin").getBoolean("power"));
  var receiver=new CompoundTag();receiver.putString("unrelatedInventory","unchanged");receiver.merge(onlyScreen);check(receiver.getString("unrelatedInventory").equals("unchanged"));
  System.out.println("PASS: Create 6.0.10 moving-screen capture, placement, interaction, update-tag and bearing API signatures; isolated metadata sync and original block-data preservation");
 }
}
