package dev.omnidisplay.mixin;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=Minecraft.class,remap=false)
public abstract class PerformanceFrameMixin {
 @Inject(method="runTick",at=@At("HEAD")) private void omni$begin(boolean render,CallbackInfo ci){dev.omnidisplay.diagnostics.PerformanceClient.beginFrame();}
 @Inject(method="runTick",at=@At("RETURN")) private void omni$end(boolean render,CallbackInfo ci){dev.omnidisplay.diagnostics.PerformanceClient.finishFrame();}
}
