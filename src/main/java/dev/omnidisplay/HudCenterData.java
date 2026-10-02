package dev.omnidisplay;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** Relative eye coordinates survive both a changing cabin outline and relocated plots. */
public final class HudCenterData {
 public static Vec3 move(Vec3 center,BlockPos offset){return center==null?null:center.add(offset.getX(),offset.getY(),offset.getZ());}
 public static void save(CompoundTag tag,Vec3 center){
  if(center==null){tag.remove("hudCenter");return;}
  var h=new CompoundTag();h.putDouble("x",center.x);h.putDouble("y",center.y);h.putDouble("z",center.z);tag.put("hudCenter",h);
 }
 public static Vec3 load(CompoundTag tag){
  if(!tag.contains("hudCenter",10))return null;
  var h=tag.getCompound("hudCenter");double x=h.getDouble("x"),y=h.getDouble("y"),z=h.getDouble("z");
  return Double.isFinite(x)&&Double.isFinite(y)&&Double.isFinite(z)?new Vec3(x,y,z):null;
 }
}
