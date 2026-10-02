package dev.omnidisplay.mixin;
import dev.omnidisplay.MovingScreens;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.core.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=AbstractContraptionEntity.class,remap=false)
public abstract class MovingScreenInteractionMixin {
 @Inject(method="handlePlayerInteraction",at=@At("HEAD"),cancellable=true)
 private void omni$screen(Player player,BlockPos pos,Direction face,InteractionHand hand,CallbackInfoReturnable<Boolean> ci){if(MovingScreens.interact((AbstractContraptionEntity)(Object)this,player,pos,face,hand))ci.setReturnValue(true);}
}
