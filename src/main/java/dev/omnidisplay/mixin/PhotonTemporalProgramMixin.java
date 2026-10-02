package dev.omnidisplay.mixin;
import dev.omnidisplay.client.PhotonTemporalRuntime;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="net.irisshaders.iris.gl.program.Program",remap=false)
abstract class PhotonTemporalProgramMixin {
 @Shadow public abstract int getProgramId();
 @Inject(method="use",at=@At("RETURN")) private void omni$mode(CallbackInfo ci){PhotonTemporalRuntime.bind(getProgramId());}
 @Inject(method="destroyInternal",at=@At("HEAD")) private void omni$destroy(CallbackInfo ci){PhotonTemporalRuntime.destroyed(getProgramId());}
}
