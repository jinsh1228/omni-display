import dev.omnidisplay.region.*;
import java.util.*;
import org.objectweb.asm.tree.*;
public final class CabinSelectionTest {
 static void check(boolean value){if(!value)throw new AssertionError();}
 public static void main(String[] args)throws Exception{
  var a=UUID.randomUUID();var b=UUID.randomUUID();check(SelectionRules.sameVehicle(true,a,a));check(!SelectionRules.sameVehicle(false,a,a));check(!SelectionRules.sameVehicle(true,a,b));check(!SelectionRules.sameVehicle(true,null,null));
  var wall=new Enclosure.Cell(0,0,0);var second=new Enclosure.Cell(10,0,0);var gap=new Enclosure.Cell(5,0,0);
  var first=new Enclosure.Result(Set.of(wall),Set.of(new Enclosure.Cell(1,0,0)),wall,new Enclosure.Cell(1,0,0),true,1.5,.5,.5);
  var many=new Enclosure.Result(Set.of(second,new Enclosure.Cell(10,1,0)),Set.of(new Enclosure.Cell(9,0,0)),new Enclosure.Cell(9,0,0),new Enclosure.Cell(10,1,0),true,8.5,1,.5);
  var merged=GroupedCabin.combine(List.of(first,many),many);check(merged.open()&&merged.walls().size()==3);check(!merged.walls().contains(gap)&&!merged.interior().contains(gap));check(merged.centerX()==many.centerX()&&merged.centerY()==many.centerY());check(merged.interior().containsAll(first.interior())&&merged.interior().containsAll(many.interior()));
  check(GroupedCabin.combine(List.of(many,first),many).equals(merged));
  var far=new Enclosure.Cell(10000,10000,10000);try{GroupedCabin.combine(List.of(first,new Enclosure.Result(Set.of(far),Set.of(far),far,far)),first);throw new AssertionError("unbounded group");}catch(IllegalArgumentException expected){}
  GroupedCabin.validateBodies(List.of(List.of(first),List.of(new Enclosure.Result(Set.of(far),Set.of(far),far,far))));
  try{GroupedCabin.validateBodies(List.of(List.of(first,new Enclosure.Result(Set.of(far),Set.of(far),far,far))));throw new AssertionError("same body must remain bounded");}catch(IllegalArgumentException expected){}
  var helper=RuntimeHooksTest.read("dev/omnidisplay/client/HighlightModels");boolean modelData=false,fullApi=false;
  for(var m:helper.methods)for(var ins:m.instructions)if(ins instanceof MethodInsnNode call){if(call.name.equals("getModelData"))modelData=true;if(call.name.equals("renderSingleBlock")&&call.desc.contains("ModelData"))fullApi=true;}
  check(modelData&&fullApi);
  var config=RuntimeHooksTest.read("dev/omnidisplay/RenderConfig");check(config.fields.stream().noneMatch(f->f.name.equals("FILTER")));
  var source=java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/dev/omnidisplay/RenderConfig.java"));check(source.contains("\"strength\",0.0,0.0,1.0"));
  System.out.println("PASS: same-vehicle and low-mode restrictions; disconnected screen gaps, dominant HUD center, model-data rendering and zero-strength defaults");
 }
}
