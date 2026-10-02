package dev.omnidisplay.mixin;

import dev.omnidisplay.client.PartitionRenderer;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The normal scene uses vanilla fallback renderers so hidden visual instances are restored. */
@Mixin(targets="dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl",remap=false)
public abstract class PartitionFlywheelMixin {
 @Inject(method="supportsVisualization",at=@At("HEAD"),cancellable=true)
 private static void omni$fallback(LevelAccessor level,CallbackInfoReturnable<Boolean> ci){if(PartitionRenderer.normalPass())ci.setReturnValue(false);}
 @Inject(method="get",at=@At("HEAD"),cancellable=true)
 private static void omni$normalScene(LevelAccessor level,CallbackInfoReturnable<Object> ci){if(PartitionRenderer.normalPass())ci.setReturnValue(null);}
}
