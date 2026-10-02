package dev.omnidisplay.mixin;
import dev.omnidisplay.region.PhotonTemporalPatch;
import dev.omnidisplay.client.IrisPhotonCompat;
import java.util.Optional;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets="net.irisshaders.iris.shaderpack.programs.ProgramSource",remap=false)
abstract class PhotonTemporalSourceMixin {
 @Inject(method="getFragmentSource",at=@At("RETURN"),cancellable=true)
 private void omni$temporal(CallbackInfoReturnable<Optional<String>> ci){if(!IrisPhotonCompat.photonPackLoaded())return;var source=ci.getReturnValue();if(source.isEmpty())return;String before=source.get(),after=PhotonTemporalPatch.apply(before);if(!before.equals(after))ci.setReturnValue(Optional.of(after));}
}
