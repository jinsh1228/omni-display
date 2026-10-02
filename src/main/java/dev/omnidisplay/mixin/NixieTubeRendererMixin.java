package dev.omnidisplay.mixin;
import com.simibubi.create.content.redstone.nixieTube.*;
import dev.omnidisplay.client.CabinInstruments;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=NixieTubeRenderer.class,remap=false)
public abstract class NixieTubeRendererMixin {
 @Inject(method="renderSafe(Lcom/simibubi/create/content/redstone/nixieTube/NixieTubeBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",at=@At("HEAD"))
 private void omni$mark(NixieTubeBlockEntity tube,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay,CallbackInfo ci){CabinInstruments.mark(tube);}
 @Redirect(method="renderAsSignal",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;getEyePosition(F)Lnet/minecraft/world/phys/Vec3;"))
 private Vec3 omni$signalEye(Entity entity,float partial,NixieTubeBlockEntity tube,float tick,PoseStack pose,MultiBufferSource buffers,int light,int overlay){return CabinInstruments.eye(tube,entity,partial);}
}
