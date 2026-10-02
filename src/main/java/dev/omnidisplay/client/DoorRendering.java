package dev.omnidisplay.client;
import dev.omnidisplay.OmniDisplay;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
@EventBusSubscriber(modid=OmniDisplay.ID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class DoorRendering {
 @SubscribeEvent public static void models(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional e){
  for(int i=0;i<6;i++)for(int side=0;side<2;side++)e.register(ScreenEdges.model("panel_"+i+"_"+side));
  for(int i=0;i<6;i++)e.register(ScreenEdges.model("panel_back_"+i));
  for(int i=1;i<64;i++)e.register(ScreenEdges.model("edges_"+i));
  for(int i=1;i<4;i++)e.register(ScreenEdges.model("door_edges_"+i));
 }
 @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){
  e.registerBlockEntityRenderer(OmniDisplay.DOOR_ENTITY.get(),ScreenEdges.Door::new);
  e.registerBlockEntityRenderer(OmniDisplay.ENTITY.get(),context->new ScreenEdges.Display());
  var models=com.simibubi.create.AllPartialModels.FOLDING_DOORS;
  var brass=models.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create","brass_door"));
  if(brass!=null)models.put(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(OmniDisplay.ID,"cockpit_door"),brass);
 }
}
