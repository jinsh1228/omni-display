package dev.omnidisplay.mixin;

import dev.omnidisplay.client.IrisPhotonCompat;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Two scene renders still represent one display frame; previous-camera uniforms advance only once. */
@Pseudo
@Mixin(targets="net.irisshaders.iris.uniforms.FrameUpdateNotifier",remap=false)
public abstract class PhotonFrameMixin {
 @Inject(method="onNewFrame",at=@At("HEAD"),cancellable=true)
 private void omni$oneFrame(CallbackInfo ci){if(IrisPhotonCompat.skipFrameUpdate(this))ci.cancel();}
}
