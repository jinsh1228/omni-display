package dev.omnidisplay.mixin;
import net.neoforged.neoforge.client.model.data.ModelDataManager;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Copycat material changes can update model data without changing block state. */
@Mixin(value=ModelDataManager.class,remap=false)
public abstract class HologramModelDataMixin {
 @Inject(method="requestRefresh",at=@At("HEAD"))
 private void omni$modelChanged(BlockEntity be,CallbackInfo ci){if(be!=null)dev.omnidisplay.client.HologramRenderer.changed(be.getLevel(),be.getBlockPos());}
}
