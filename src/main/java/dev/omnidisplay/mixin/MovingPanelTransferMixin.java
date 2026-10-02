package dev.omnidisplay.mixin;
import dev.omnidisplay.*;
import com.simibubi.create.content.contraptions.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=Contraption.class,remap=false)
public abstract class MovingPanelTransferMixin {
 @Inject(method="getBlockEntityNBT",at=@At("RETURN"),cancellable=true)
 private void omni$capture(Level level,BlockPos pos,CallbackInfoReturnable<CompoundTag> ci){
  var panel=PanelStore.movingData(level,pos);var old=ci.getReturnValue();
  if(panel==null&&(old==null||!old.contains("cabin")))return;
  var tag=old==null?new CompoundTag():old.copy();var anchor=((Contraption)(Object)this).anchor;
  if(panel!=null){var local=PanelBlueprint.map(panel,p->p.subtract(anchor));tag.put(MovingScreens.PANEL,local);}
  if(tag.contains("cabin")){var wrapper=new CompoundTag();wrapper.putLong("pos",pos.asLong());wrapper.put("cabin",tag.getCompound("cabin"));tag.put("cabin",PanelBlueprint.map(wrapper,p->p.subtract(anchor)).getCompound("cabin"));}
  ci.setReturnValue(tag);

 }
 @Inject(method="addBlock",at=@At("RETURN"))
 private void omni$updateTag(Level level,BlockPos pos,org.apache.commons.lang3.tuple.Pair<net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo,net.minecraft.world.level.block.entity.BlockEntity> captured,CallbackInfo ci){
  var contraption=(Contraption)(Object)this;var local=pos.subtract(contraption.anchor);var info=contraption.getBlocks().get(local);if(info!=null&&info.nbt()!=null&&(info.nbt().contains(MovingScreens.PANEL)||info.nbt().contains("cabin")))MovingScreens.updateTag(contraption,local,info.nbt());
 }
 @Inject(method="removeBlocksFromWorld",at=@At("HEAD"))
 private void omni$remove(Level level,BlockPos offset,CallbackInfo ci){
  if(level.isClientSide)return;var contraption=(Contraption)(Object)this;
  for(var info:contraption.getBlocks().values())if(info.nbt()!=null&&info.nbt().contains(MovingScreens.PANEL))PanelStore.extractMoving(level,info.pos().offset(offset));
 }
 @Inject(method="addBlocksToWorld",at=@At("RETURN"))
 private void omni$place(Level level,StructureTransform transform,CallbackInfo ci){
  if(level.isClientSide)return;var contraption=(Contraption)(Object)this;
  for(var info:contraption.getBlocks().values())if(info.nbt()!=null){
   var target=transform.apply(info.pos());
   if(info.nbt().contains(MovingScreens.PANEL))PanelStore.restoreMoving(level,target,info.pos(),info.nbt().getCompound(MovingScreens.PANEL),transform);
   if(info.nbt().contains("cabin")&&level.getBlockEntity(target) instanceof CabinEntity host){
    var wrapper=new CompoundTag();wrapper.putLong("pos",info.pos().asLong());wrapper.put("cabin",info.nbt().getCompound("cabin"));var mapped=PanelBlueprint.map(wrapper,transform::apply);var settings=CabinSettings.load(mapped.getCompound("cabin"),level.registryAccess());
    int mask=0;for(var face:net.minecraft.core.Direction.values())if((settings.manualFaces&(1<<face.get3DDataValue()))!=0)mask|=1<<transform.rotateFacing(face).get3DDataValue();settings.manualFaces=mask;
    settings.revision=Math.max(settings.revision+1,System.currentTimeMillis());host.apply(settings);
    if(info.state().is(OmniDisplay.THIN_DISPLAY.get()))level.setBlock(target,level.getBlockState(target).setValue(ThinDisplayBlock.ATTACHED_FACE,transform.rotateFacing(info.state().getValue(ThinDisplayBlock.ATTACHED_FACE))),2);
   }
  }
 }
}
