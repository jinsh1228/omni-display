package dev.omnidisplay;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
/** Optional integration entry point; no radar types in event signatures. */
@EventBusSubscriber(modid=OmniDisplay.ID)
public final class RadarHooks {
 public static boolean available(){return ModList.get().isLoaded("create_radar");}
 @SubscribeEvent(priority=EventPriority.HIGHEST) public static void click(PlayerInteractEvent.RightClickBlock e){
  if(available()&&e.getHand()==net.minecraft.world.InteractionHand.MAIN_HAND&&ScreenFaces.isScreen(e.getLevel(),e.getPos()))RadarBridge.place(e);
 }
}

