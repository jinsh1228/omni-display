package dev.omnidisplay;
import net.neoforged.neoforge.common.ModConfigSpec;
/** Server-authoritative rendering and screen appearance. */
public final class RenderConfig {
 public static final ModConfigSpec SPEC;
 public static final ModConfigSpec.BooleanValue LOW_PERFORMANCE,TINT,PIXELS,SCANLINES,EDGES;
 public static final ModConfigSpec.DoubleValue STRENGTH,BRIGHTNESS,CONTRAST,BLACK_LIFT,PIXEL_STRENGTH,SCANLINE_STRENGTH,EDGE_STRENGTH;
 public static final ModConfigSpec.IntValue PIXEL_DENSITY,SCANLINE_DENSITY;
 static {
  var b=new ModConfigSpec.Builder();
  LOW_PERFORMANCE=b.comment("Low-performance mode: whole-cabin transparency, doors interlocked, no open cabins or screen filters. False uses the default display-surface renderer.").define("lowPerformanceMode",false);
  b.comment("Screen-only filter. Unavailable in lowPerformanceMode. All values are controlled by the server; ordinary world, door openings and HUD are unaffected.").push("screenFilter");
  STRENGTH=b.comment("Overall effect strength, 0 = original image.").defineInRange("strength",0.0,0.0,1.0);
  TINT=b.comment("Subtle cool cyan tint.").define("coolTint",true);
  BRIGHTNESS=b.comment("Brightness multiplier before overall strength is applied.").defineInRange("brightness",1.03,0.5,1.5);
  CONTRAST=b.comment("Contrast multiplier before overall strength is applied.").defineInRange("contrast",1.06,0.5,1.5);
  BLACK_LIFT=b.comment("Lift dark areas slightly to retain visibility.").defineInRange("blackLift",0.02,0.0,0.15);
  PIXELS=b.comment("Pixel grid fixed to each display surface; automatically fades when too small to resolve.").define("pixelTexture",true);
  PIXEL_DENSITY=b.comment("Pixel cells per block face.").defineInRange("pixelsPerBlock",96,8,256);
  PIXEL_STRENGTH=b.defineInRange("pixelStrength",0.12,0.0,0.5);
  SCANLINES=b.comment("Subtle scanlines fixed to each display surface.").define("scanlines",true);
  SCANLINE_DENSITY=b.defineInRange("scanlinesPerBlock",64,8,256);
  SCANLINE_STRENGTH=b.defineInRange("scanlineStrength",0.08,0.0,0.5);
  EDGES=b.comment("Slightly darken the perimeter of each panel.").define("edgeDimming",true);
  EDGE_STRENGTH=b.defineInRange("edgeStrength",0.10,0.0,0.5);
  b.pop();SPEC=b.build();
 }
 public static boolean experimental(){return !SPEC.isLoaded()||!LOW_PERFORMANCE.get();}
 public static void filterState(net.minecraft.nbt.CompoundTag tag){
  var f=new net.minecraft.nbt.CompoundTag();
  if(SPEC.isLoaded()){
   f.putBoolean("enabled",STRENGTH.get()>0);f.putBoolean("tint",TINT.get());f.putBoolean("pixels",PIXELS.get());f.putBoolean("scanlines",SCANLINES.get());f.putBoolean("edges",EDGES.get());
   f.putFloat("strength",STRENGTH.get().floatValue());f.putFloat("brightness",BRIGHTNESS.get().floatValue());f.putFloat("contrast",CONTRAST.get().floatValue());f.putFloat("blackLift",BLACK_LIFT.get().floatValue());
   f.putFloat("pixelStrength",PIXEL_STRENGTH.get().floatValue());f.putFloat("scanlineStrength",SCANLINE_STRENGTH.get().floatValue());f.putFloat("edgeStrength",EDGE_STRENGTH.get().floatValue());
   f.putInt("pixelDensity",PIXEL_DENSITY.get());f.putInt("scanlineDensity",SCANLINE_DENSITY.get());
  }
  tag.put("screenFilter",f);
 }
}
