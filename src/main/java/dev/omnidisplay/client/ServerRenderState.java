package dev.omnidisplay.client;
import dev.omnidisplay.SynaxisLinks;
import net.minecraft.nbt.*;
import java.util.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
@EventBusSubscriber(modid=dev.omnidisplay.OmniDisplay.ID,value=Dist.CLIENT)
public final class ServerRenderState {
 public static boolean ready,experimental,editable;
 public static CompoundTag filter=new CompoundTag();
 private static String dimension="";
 public static List<SynaxisLinks.Link> links=List.of();
 public static void receive(CompoundTag tag){filter=tag.getCompound("screenFilter").copy();ready=true;experimental=tag.getBoolean("experimental");editable=tag.getBoolean("editable");dimension=tag.getString("dimension");var result=new ArrayList<SynaxisLinks.Link>();var list=tag.getList("links",Tag.TAG_COMPOUND);for(int i=0;i<Math.min(list.size(),8192);i++){var e=list.getCompound(i);if(e.hasUUID("a")&&e.hasUUID("b"))result.add(new SynaxisLinks.Link(e.getUUID("a"),e.getUUID("b")));}links=List.copyOf(result);}
 public static List<SynaxisLinks.Link> currentLinks(){var level=net.minecraft.client.Minecraft.getInstance().level;return level!=null&&level.dimension().location().toString().equals(dimension)?links:List.of();}
 @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){ready=experimental=editable=false;filter=new CompoundTag();links=List.of();dimension="";ParticleVisibility.clear();}
}
