package dev.omnidisplay.mixin;
import dev.omnidisplay.client.Attachments;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=EntityRenderDispatcher.class,remap=false)
public abstract class EntityVisibilityMixin {
 @Inject(method="render",at=@At("HEAD"),cancellable=true)
 private void omni$hide(Entity e,double x,double y,double z,float yaw,float tick,PoseStack pose,MultiBufferSource buffers,int light,CallbackInfo ci){if(Attachments.hide(e))ci.cancel();}
}
