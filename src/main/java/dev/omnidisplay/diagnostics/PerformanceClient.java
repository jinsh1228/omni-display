package dev.omnidisplay.diagnostics;
import dev.omnidisplay.*;
import dev.omnidisplay.client.*;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
@EventBusSubscriber(modid=OmniDisplay.ID,value=Dist.CLIENT)
public final class PerformanceClient {
 private static boolean tracking;private static long tickStart;
 public static void beginFrame(){var mc=Minecraft.getInstance();tracking=mc.level!=null&&mc.screen==null;if(!tracking){PerformanceDiagnostics.idle();return;}PerformanceDiagnostics.initialize(mc.gameDirectory.toPath());PerformanceDiagnostics.beginFrame();}
 public static void finishFrame(){if(!tracking)return;var mc=Minecraft.getInstance();if(mc.level==null){PerformanceDiagnostics.idle();return;}var shape=DisplayRenderer.enclosure();var settings=DisplayRenderer.settings();PerformanceDiagnostics.finishFrame("tick="+mc.level.getGameTime()+" active="+DisplayRenderer.active()+" partition="+PartitionRenderer.enabled()+" shaderAdapter="+IrisPhotonCompat.enabled()+" walls="+(shape==null?0:shape.walls().size())+" interior="+(shape==null?0:shape.interior().size())+" bodies="+DisplayRenderer.bodies().size()+" panels="+PanelStore.entries(mc.level).size()+" topology="+ScreenTopologyCache.version(mc.level)+" power="+(settings!=null&&settings.power)+" animating="+(settings!=null&&settings.animating(mc.level.getGameTime())));}
 @SubscribeEvent(priority=EventPriority.HIGHEST) public static void pre(ClientTickEvent.Pre e){tickStart=PerformanceDiagnostics.begin(PerformanceDiagnostics.Phase.CLIENT_TICK);}
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void post(ClientTickEvent.Post e){PerformanceDiagnostics.end(PerformanceDiagnostics.Phase.CLIENT_TICK,tickStart);}
}
