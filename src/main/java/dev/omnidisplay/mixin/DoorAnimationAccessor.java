package dev.omnidisplay.mixin;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlockEntity;
import net.createmod.catnip.animation.LerpedFloat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=SlidingDoorBlockEntity.class,remap=false)
public interface DoorAnimationAccessor {
 @Accessor("animation") LerpedFloat omni$animation();
}
