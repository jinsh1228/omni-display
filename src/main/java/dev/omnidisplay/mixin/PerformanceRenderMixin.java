package dev.omnidisplay.mixin;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=GameRenderer.class,remap=false)
public abstract class PerformanceRenderMixin {
 @Unique private long omni$renderStart;
 @Inject(method="render",at=@At("HEAD")) private void omni$begin(DeltaTracker delta,boolean render,CallbackInfo ci){omni$renderStart=dev.omnidisplay.diagnostics.PerformanceDiagnostics.begin(dev.omnidisplay.diagnostics.PerformanceDiagnostics.Phase.RENDER_ALL);}
 @Inject(method="render",at=@At("RETURN")) private void omni$end(DeltaTracker delta,boolean render,CallbackInfo ci){dev.omnidisplay.diagnostics.PerformanceDiagnostics.end(dev.omnidisplay.diagnostics.PerformanceDiagnostics.Phase.RENDER_ALL,omni$renderStart);}
}
