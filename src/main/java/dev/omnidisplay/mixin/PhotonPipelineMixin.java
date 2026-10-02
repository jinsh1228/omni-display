package dev.omnidisplay.mixin;

import dev.omnidisplay.client.IrisPhotonCompat;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional Iris hook: textures have been resized/cleared, but shader scene passes have not started. */
@Pseudo
@Mixin(targets="net.irisshaders.iris.pipeline.IrisRenderingPipeline",remap=false)
public abstract class PhotonPipelineMixin {
 @Inject(method="beginLevelRendering",at=@At(value="INVOKE",target="Lnet/irisshaders/iris/pipeline/CompositeRenderer;renderAll()V"))
 private void omni$history(CallbackInfo ci){IrisPhotonCompat.beforeWorld(this);}
 @Inject(method="destroy",at=@At("HEAD"))
 private void omni$release(CallbackInfo ci){IrisPhotonCompat.destroyed(this);}
}
