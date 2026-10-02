package dev.omnidisplay.mixin;

import dev.omnidisplay.client.IrisPhotonCompat;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import java.util.function.Supplier;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Both suppliers mutate their history in get(), outside FrameUpdateNotifier. */
@Pseudo
@Mixin(targets={"net.irisshaders.iris.uniforms.MatrixUniforms$Previous","me.cortex.voxy.client.iris.VoxyUniforms$PreviousMat"},remap=false)
public abstract class PhotonPreviousMatrixMixin {
 @Shadow @Final private Supplier<Matrix4fc> parent;
 @Shadow private Matrix4f previous;
 @Inject(method="get()Lorg/joml/Matrix4fc;",at=@At("HEAD"),cancellable=true)
 private void omni$viewHistory(CallbackInfoReturnable<Matrix4fc> ci){
  if(!IrisPhotonCompat.viewPair())return;
  var current=parent.get();
  ci.setReturnValue(IrisPhotonCompat.previousMatrix(this,current,previous));
  // Keep the original supplier valid when partition rendering is stopped.
  if(!IrisPhotonCompat.screenView())previous=new Matrix4f(current);
 }
}
