package dev.omnidisplay;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import java.util.function.UnaryOperator;

/** Portable surface metadata; coordinates follow the plot's own chunk serialization. */
public final class PanelBlueprint {
 public static final String KEY="omni_display_panels";
 private PanelBlueprint(){}
 public static CompoundTag map(CompoundTag source,UnaryOperator<BlockPos> transform){
  var copy=source.copy();mapPosition(copy,"pos",transform);
  var cabin=copy.getCompound("cabin");mapPosition(cabin,"origin",transform);mapPosition(cabin,"faultOrigin",transform);
  for(var tag:cabin.getList("clickOrigins",Tag.TAG_COMPOUND))mapPosition((CompoundTag)tag,"pos",transform);
  // Radar links and highlighted regions are already relative to cabin origin.
  return copy;
 }
 private static void mapPosition(CompoundTag tag,String key,UnaryOperator<BlockPos> transform){if(tag.contains(key,Tag.TAG_LONG))tag.putLong(key,transform.apply(BlockPos.of(tag.getLong(key))).asLong());}
 public static int minimumSection(CompoundTag plot){
  int min=Integer.MAX_VALUE;var chunks=plot.getCompound("chunks");
  for(var key:chunks.getAllKeys())for(var section:chunks.getCompound(key).getCompound("sections").getAllKeys()){
   try{min=Math.min(min,Integer.parseInt(section));}catch(NumberFormatException ignored){}
  }
  return min==Integer.MAX_VALUE?0:min;
 }
 public static int restoredYBase(CompoundTag plot,int minimumBuildHeight){
  return minimumBuildHeight+16*(minimumSection(plot)-plot.getCompound(KEY).getInt("section_min"));
 }
}
