package dev.omnidisplay.mixin;
import dev.omnidisplay.client.Attachments;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets="dev.engine_room.flywheel.impl.visualization.storage.EntityStorage",remap=false)
public abstract class VisualEntityVisibilityMixin {
 @Inject(method="willAccept(Lnet/minecraft/world/entity/Entity;)Z",at=@At("HEAD"),cancellable=true)
 private void omni$accept(Entity e,CallbackInfoReturnable<Boolean> cir){if(Attachments.hide(e))cir.setReturnValue(false);}
}
