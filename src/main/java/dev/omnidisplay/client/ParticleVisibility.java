package dev.omnidisplay.client;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.mixinterface.particle.ParticleExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.*;
import net.minecraft.world.level.ChunkPos;
import org.joml.Vector3d;
public final class ParticleVisibility {
 public static void clear(){} // Ownership lives on particles and expires with their world.
 public static void capture(Particle particle,double x,double y,double z){
  if(!(particle instanceof ParticleSource source)||source.omni$source()!=null)return;
  var level=Minecraft.getInstance().level;if(level==null)return;var c=SubLevelContainer.getContainer(level);if(c==null)return;
  var pos=BlockPos.containing(x,y,z);ClientSubLevel sub=null;
  if(c.inBounds(pos)){var plot=c.getPlot(new ChunkPos(pos));if(plot!=null&&plot.getSubLevel() instanceof ClientSubLevel s)sub=s;}
  if(sub==null&&(Object)particle instanceof ParticleExtension extension&&extension.sable$getTrackingSubLevel() instanceof ClientSubLevel s){sub=s;var local=s.logicalPose().transformPositionInverse(new Vector3d(x,y,z));pos=BlockPos.containing(local.x,local.y,local.z);}
  if(sub!=null&&!sub.isRemoved())source.omni$source(sub.getUniqueId(),pos);
 }
 public static boolean vehicleParticle(Particle particle){var center=particle.getBoundingBox().getCenter();capture(particle,center.x,center.y,center.z);return particle instanceof ParticleSource source&&source.omni$source()!=null;}
 public static float factor(Particle particle){
  if(!DisplayRenderer.active()||PartitionRenderer.normalPass())return 1;
  var center=particle.getBoundingBox().getCenter();capture(particle,center.x,center.y,center.z);
  if(!(particle instanceof ParticleSource source)||source.omni$source()==null)return 1;
  for(var sub:Attachments.structures())if(sub.getUniqueId().equals(source.omni$source())){
   if(DisplayRenderer.member(sub)&&source.omni$anchor()!=null&&DisplayRenderer.keeps(source.omni$anchor()))return 1;
   return DisplayRenderer.opacity();
  }
  return 1;
 }
}
