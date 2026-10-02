package dev.omnidisplay.mixin;
import dev.omnidisplay.client.Attachments;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets="dev.engine_room.flywheel.impl.visualization.storage.BlockEntityStorage",remap=false)
public abstract class VisualBlockVisibilityMixin {
 @Inject(method="willAccept(Lnet/minecraft/world/level/block/entity/BlockEntity;)Z",at=@At("HEAD"),cancellable=true)
 private void omni$accept(BlockEntity be,CallbackInfoReturnable<Boolean> cir){if(Attachments.hide(be))cir.setReturnValue(false);}
}
