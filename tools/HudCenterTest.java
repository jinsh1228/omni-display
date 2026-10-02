import dev.omnidisplay.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
public class HudCenterTest {
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args){
  var origin=new BlockPos(24000000,64,-23000000);var relative=new Vec3(2.25,1.625,3.75);
  var saved=new CompoundTag();saved.putLong("origin",origin.asLong());HudCenterData.save(saved,relative);
  check(HudCenterData.load(saved).equals(relative));
  var absolute=HudCenterData.move(relative,origin);var next=origin.offset(-3,2,7);
  check(HudCenterData.move(HudCenterData.move(relative,origin.subtract(next)),next).equals(absolute));
  var entry=new CompoundTag();entry.putLong("pos",origin.asLong());entry.put("cabin",saved);
  var moved=PanelBlueprint.map(entry,p->p.offset(-40000000,16,40000000));
  check(HudCenterData.load(moved.getCompound("cabin")).equals(relative));
  check(BlockPos.of(moved.getCompound("cabin").getLong("origin")).equals(origin.offset(-40000000,16,40000000)));
  var reset=saved.copy();HudCenterData.save(reset,null);check(HudCenterData.load(reset)==null);
  var invalid=saved.copy();invalid.getCompound("hudCenter").putDouble("x",Double.NaN);check(HudCenterData.load(invalid)==null);
  System.out.println("PASS: HUD center NBT reload, origin changes, blueprint relocation, reset and invalid values");
 }
}
