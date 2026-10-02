package dev.omnidisplay;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
/** Coordinates relative to the cabin's persisted reference, not world position. */
public record HighlightRegion(BlockPos min,BlockPos max,int opacity,java.util.UUID body,String kind){
 public HighlightRegion(BlockPos min,BlockPos max,int opacity){this(min,max,opacity,null,"");}
 public static HighlightRegion structure(java.util.UUID id,String kind,int alpha){return new HighlightRegion(BlockPos.ZERO,BlockPos.ZERO,Math.clamp(alpha,0,100),id,kind);}
 public static HighlightRegion blocks(java.util.UUID id,BlockPos a,BlockPos b,int alpha){var h=between(a,b,alpha);return new HighlightRegion(h.min,h.max,h.opacity,id,"blocks");}
 public boolean matches(BlockPos p){return p.getX()>=min.getX()&&p.getX()<=max.getX()&&p.getY()>=min.getY()&&p.getY()<=max.getY()&&p.getZ()>=min.getZ()&&p.getZ()<=max.getZ();}
 public HighlightRegion withOpacity(int alpha){return new HighlightRegion(min,max,Math.clamp(alpha,0,100),body,kind);}
 public static HighlightRegion between(BlockPos a,BlockPos b,int opacity){return new HighlightRegion(new BlockPos(Math.min(a.getX(),b.getX()),Math.min(a.getY(),b.getY()),Math.min(a.getZ(),b.getZ())),new BlockPos(Math.max(a.getX(),b.getX()),Math.max(a.getY(),b.getY()),Math.max(a.getZ(),b.getZ())),Math.clamp(opacity,0,100));}
 public boolean contains(BlockPos p){return body==null&&p.getX()>=min.getX()&&p.getX()<=max.getX()&&p.getY()>=min.getY()&&p.getY()<=max.getY()&&p.getZ()>=min.getZ()&&p.getZ()<=max.getZ();}
 public HighlightRegion move(BlockPos offset){return body==null?new HighlightRegion(min.offset(offset),max.offset(offset),opacity):this;}
 public CompoundTag save(){var t=new CompoundTag();t.putLong("min",min.asLong());t.putLong("max",max.asLong());t.putInt("opacity",opacity);if(body!=null){t.putUUID("body",body);t.putString("kind",kind);}return t;}
 public static HighlightRegion load(CompoundTag t){if(t.hasUUID("body")&&java.util.Set.of("ship","contraption","blocks").contains(t.getString("kind")))return t.getString("kind").equals("blocks")?blocks(t.getUUID("body"),BlockPos.of(t.getLong("min")),BlockPos.of(t.getLong("max")),t.getInt("opacity")):structure(t.getUUID("body"),t.getString("kind"),t.getInt("opacity"));return between(BlockPos.of(t.getLong("min")),BlockPos.of(t.getLong("max")),t.getInt("opacity"));}
}
