package dev.omnidisplay.mixin;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=Particle.class,remap=false)
public interface ParticleAlphaAccessor {
 @Accessor("alpha") float omni$alpha();
 @Accessor("alpha") void omni$alpha(float value);
}
