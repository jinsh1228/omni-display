package dev.omnidisplay.diagnostics;
import dev.omnidisplay.OmniDisplay;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
@EventBusSubscriber(modid=OmniDisplay.ID)
public final class PerformanceServer {
 @SubscribeEvent(priority=EventPriority.HIGHEST) public static void pre(ServerTickEvent.Pre e){PerformanceDiagnostics.beginServer();}
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void post(ServerTickEvent.Post e){PerformanceDiagnostics.finishServer();}
}
