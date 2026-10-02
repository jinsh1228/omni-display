package dev.omnidisplay.mixin;
import dev.omnidisplay.client.*;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import java.util.UUID;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=Particle.class,remap=false,priority=1000)
abstract class ParticleSourceMixin implements ParticleSource {
 @Shadow protected double x,y,z;
 @Unique private UUID omni$source;
 @Unique private BlockPos omni$anchor;
 public UUID omni$source(){return omni$source;}
 public BlockPos omni$anchor(){return omni$anchor;}
 public void omni$source(UUID id,BlockPos pos){omni$source=id;omni$anchor=pos;}
 @Inject(method="<init>",at=@At("RETURN")) private void omni$capture(CallbackInfo ci){ParticleVisibility.capture((Particle)(Object)this,x,y,z);}
}
