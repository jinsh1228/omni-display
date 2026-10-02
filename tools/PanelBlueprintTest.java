import dev.omnidisplay.PanelBlueprint;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;

public final class PanelBlueprintTest {
 static void check(boolean b){if(!b)throw new AssertionError();}
 static CompoundTag plot(int... sections){
  var root=new CompoundTag();var chunks=new CompoundTag();var chunk=new CompoundTag();var list=new CompoundTag();
  for(int n:sections)list.put(Integer.toString(n),new CompoundTag());chunk.put("sections",list);chunks.put("0",chunk);root.put("chunks",chunks);return root;
 }
 public static void main(String[] args){
  var sourceOrigin=new BlockPos(24000000,-64,-23000000);var targetOrigin=new BlockPos(-22000000,0,25000000);
  var pos=sourceOrigin.offset(-17,137,33);var origin=pos.offset(-2,-3,-4);var fault=pos.offset(1,0,0);
  for(int mask=1;mask<64;mask++){
   var entry=new CompoundTag();entry.putLong("pos",pos.asLong());entry.putInt("mask",mask);entry.putInt("interior",mask&42);entry.putString("block","minecraft:glass");
   var settings=new CompoundTag();settings.putLong("origin",origin.asLong());settings.putLong("faultOrigin",fault.asLong());settings.putLong("radarLink",new BlockPos(3,2,1).asLong());settings.putInt("hudColor",0x123456);
   var clicks=new ListTag();var click=new CompoundTag();click.putLong("pos",pos.asLong());clicks.add(click);clicks.add(new CompoundTag());settings.put("clickOrigins",clicks);entry.put("cabin",settings);
   var local=PanelBlueprint.map(entry,p->p.subtract(sourceOrigin));var placed=PanelBlueprint.map(local,p->p.offset(targetOrigin));
   check(BlockPos.of(placed.getLong("pos")).equals(pos.subtract(sourceOrigin).offset(targetOrigin)));
   check(BlockPos.of(placed.getCompound("cabin").getLong("origin")).equals(origin.subtract(sourceOrigin).offset(targetOrigin)));
   check(BlockPos.of(placed.getCompound("cabin").getLong("faultOrigin")).equals(fault.subtract(sourceOrigin).offset(targetOrigin)));
   check(placed.getInt("mask")==mask&&placed.getInt("interior")== (mask&42));
   check(placed.getCompound("cabin").getLong("radarLink")==settings.getLong("radarLink"));
   check(placed.getCompound("cabin").getInt("hudColor")==0x123456);
   check(placed.getCompound("cabin").getList("clickOrigins",10).getCompound(1).isEmpty());
   check(PanelBlueprint.map(local,p->p.offset(sourceOrigin)).equals(entry));
   check(entry.getLong("pos")==pos.asLong());
   placed.putInt("mask",0);check(local.getInt("mask")==mask);
  }
  var source=plot(4,7,10);check(PanelBlueprint.minimumSection(source)==4);
  var portable=new CompoundTag();portable.putInt("section_min",4);source.put(PanelBlueprint.KEY,portable);
  check(PanelBlueprint.restoredYBase(source,-64)==-64);
  var shifted=plot(6,9,12);shifted.put(PanelBlueprint.KEY,portable.copy());check(PanelBlueprint.restoredYBase(shifted,0)==32);
  check(PanelBlueprint.minimumSection(new CompoundTag())==0);
  System.out.println("PASS: panel blueprint roundtrip, all 63 face masks, relocated plots, cross-dimension section shift, settings and copy isolation");
 }
}
