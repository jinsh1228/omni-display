package dev.omnidisplay;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.slidingDoor.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
public final class CockpitDoor extends SlidingDoorBlock {
 public static final net.minecraft.world.level.block.state.properties.BooleanProperty LOCKED=net.minecraft.world.level.block.state.properties.BooleanProperty.create("cockpit_locked");
 public static final net.minecraft.world.level.block.state.properties.IntegerProperty SIDES=net.minecraft.world.level.block.state.properties.IntegerProperty.create("screen_sides",0,3);
 @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block,net.minecraft.world.level.block.state.BlockState> b){super.createBlockStateDefinition(b);b.add(SIDES,LOCKED);}
 @Override protected net.minecraft.world.InteractionResult useWithoutItem(net.minecraft.world.level.block.state.BlockState s,net.minecraft.world.level.Level l,net.minecraft.core.BlockPos p,net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit){
  if(s.getValue(LOCKED)||player.isShiftKeyDown())return net.minecraft.world.InteractionResult.SUCCESS;
  return super.useWithoutItem(s,l,p,player,hit);
 }
 public CockpitDoor(){super(BlockBehaviour.Properties.ofFullCopy(AllBlocks.BRASS_DOOR.get()),AllBlocks.BRASS_DOOR.get().type(),AllBlocks.BRASS_DOOR.get().isFoldingDoor());registerDefaultState(defaultBlockState().setValue(SIDES,0).setValue(LOCKED,false));}
 @Override public void setOpen(net.minecraft.world.entity.Entity entity,net.minecraft.world.level.Level level,net.minecraft.world.level.block.state.BlockState state,net.minecraft.core.BlockPos pos,boolean open){if(open&&state.getValue(LOCKED))return;super.setOpen(entity,level,state,pos,open);}
 @Override public void neighborChanged(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.Block block,net.minecraft.core.BlockPos from,boolean moved){if(state.getValue(LOCKED))return;super.neighborChanged(state,level,pos,block,from,moved);}
 @Override protected void onRemove(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState next,boolean moving){if(!level.isClientSide&&!next.is(this))CabinManager.broken(level,pos);super.onRemove(state,level,pos,next,moving);}
 @Override public BlockEntityType<? extends SlidingDoorBlockEntity> getBlockEntityType(){return OmniDisplay.DOOR_ENTITY.get();}
}
