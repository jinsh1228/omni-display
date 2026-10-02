package dev.omnidisplay.mixin;

import dev.omnidisplay.PanelStore;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Toolgun and Sable storage both use these native plot serialization methods. */
@Mixin(value=ServerLevelPlot.class,remap=false)
public abstract class PanelPlotMixin {
 @Inject(method="save()Lnet/minecraft/nbt/CompoundTag;",at=@At("RETURN"))
 private void omni$savePanels(CallbackInfoReturnable<CompoundTag> ci){PanelStore.savePlot((ServerLevelPlot)(Object)this,ci.getReturnValue());}
 @Inject(method="load(Lnet/minecraft/nbt/CompoundTag;)V",at=@At("RETURN"))
 private void omni$loadPanels(CompoundTag tag,CallbackInfo ci){PanelStore.loadPlot((ServerLevelPlot)(Object)this,tag);}
}
