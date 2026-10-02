package dev.omnidisplay;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public class CabinEntity extends BlockEntity {
 public CabinSettings settings=new CabinSettings();
 public CabinEntity(BlockPos p,BlockState s){super(OmniDisplay.ENTITY.get(),p,s);}
 @Override public void onLoad(){super.onLoad();if(level!=null&&!level.isClientSide)CabinManager.add(this);}
 @Override public void setRemoved(){CabinManager.remove(this);super.setRemoved();}
 @Override public void onChunkUnloaded(){CabinManager.remove(this);super.onChunkUnloaded();}
 public void apply(CabinSettings s){
  if(settings.id!=null&&settings.id.equals(s.id)&&settings.revision==s.revision)return;
  if(settings.manualFaces!=s.manualFaces)ScreenTopologyCache.invalidate(level);settings=s.copy();setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);
 }
 @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);t.put("cabin",settings.save(r));if(settings.origin!=null)t.putLong("originOffset",settings.origin.subtract(worldPosition).asLong());}
 @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);var fresh=CabinSettings.load(t.getCompound("cabin"),r);if(settings.manualFaces!=fresh.manualFaces)ScreenTopologyCache.invalidate(level);settings=fresh;if(t.contains("originOffset"))settings.origin=worldPosition.offset(net.minecraft.core.BlockPos.of(t.getLong("originOffset")));}
 @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveWithoutMetadata(r);}
 @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
