package dev.omnidisplay.mixin;
import dev.omnidisplay.client.Attachments;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=BlockEntityRenderDispatcher.class,remap=false)
public abstract class BlockEntityVisibilityMixin {
 @Inject(method="render",at=@At("HEAD"),cancellable=true)
 private void omni$hide(BlockEntity be,float tick,PoseStack pose,MultiBufferSource buffers,CallbackInfo ci){if(Attachments.hide(be))ci.cancel();}
}
