package dev.omnidisplay.mixin;
import dev.omnidisplay.client.*;
import net.minecraft.client.particle.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Optional AsyncParticles 21.1 GPU dispatch. Keep only vehicle particles on the CPU path. */
@Pseudo
@Mixin(targets="neoforge.fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior",remap=false)
abstract class AsyncParticleGpuMixin {
 @Inject(method="canRenderFast(Lnet/minecraft/client/particle/Particle;)Z",at=@At("HEAD"),cancellable=true)
 private void omni$particle(Particle particle,CallbackInfoReturnable<Boolean> ci){if(ParticleVisibility.vehicleParticle(particle))ci.setReturnValue(false);}
 @Inject(method="canRenderFast(Lnet/minecraft/client/particle/TextureSheetParticle;)Z",at=@At("HEAD"),cancellable=true)
 private void omni$sheet(TextureSheetParticle particle,CallbackInfoReturnable<Boolean> ci){if(ParticleVisibility.vehicleParticle(particle))ci.setReturnValue(false);}
}
