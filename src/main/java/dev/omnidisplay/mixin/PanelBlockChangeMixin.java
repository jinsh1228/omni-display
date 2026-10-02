package dev.omnidisplay.mixin;

import dev.omnidisplay.PanelStore;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=LevelChunk.class,remap=false)
public abstract class PanelBlockChangeMixin {
 @Inject(method="setBlockState",at=@At("RETURN"))
 private void omni$hostChanged(BlockPos pos,BlockState next,boolean moving,CallbackInfoReturnable<BlockState> ci){
  var previous=ci.getReturnValue();
  var level=((LevelChunk)(Object)this).getLevel();
  if(previous!=null&&previous!=next&&level.isClientSide){dev.omnidisplay.client.CabinContentsCache.changed(level,pos);dev.omnidisplay.client.HologramRenderer.changed(level,pos);}
  if(previous!=null&&previous!=next&&(dev.omnidisplay.OmniDisplay.isScreen(previous)||dev.omnidisplay.OmniDisplay.isScreen(next)||PanelStore.mask(((LevelChunk)(Object)this).getLevel(),pos)!=0))dev.omnidisplay.ScreenTopologyCache.invalidate(((LevelChunk)(Object)this).getLevel());
  if(previous!=null&&!previous.is(next.getBlock()))PanelStore.blockChanged(((LevelChunk)(Object)this).getLevel(),pos);
 }
}
