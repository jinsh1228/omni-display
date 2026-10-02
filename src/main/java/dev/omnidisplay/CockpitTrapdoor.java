package dev.omnidisplay;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

/** Vanilla hinged hatch geometry with shared cabin settings and screen surfaces. */
public final class CockpitTrapdoor extends TrapDoorBlock implements EntityBlock {
 public static final MapCodec<CockpitTrapdoor> CODEC=simpleCodec(CockpitTrapdoor::new);
 public CockpitTrapdoor(){this(BlockBehaviour.Properties.of().strength(2).noOcclusion());}
 public CockpitTrapdoor(BlockBehaviour.Properties properties){
  super(BlockSetType.COPPER,properties);
  registerDefaultState(defaultBlockState().setValue(OmniDisplay.DisplayBlock.FACES,0).setValue(CockpitDoor.LOCKED,false));
 }
 @Override public MapCodec<CockpitTrapdoor> codec(){return CODEC;}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(OmniDisplay.DisplayBlock.FACES,CockpitDoor.LOCKED);}
 @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new OmniDisplay.DisplayEntity(pos,state);}
 @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
  if(state.getValue(CockpitDoor.LOCKED)||player.isShiftKeyDown())return InteractionResult.SUCCESS;
  return super.useWithoutItem(state,level,pos,player,hit);
 }
 @Override protected void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moved){
  if(!state.getValue(CockpitDoor.LOCKED))super.neighborChanged(state,level,pos,block,from,moved);
 }
 @Override protected void onExplosionHit(BlockState state,Level level,BlockPos pos,Explosion explosion,java.util.function.BiConsumer<net.minecraft.world.item.ItemStack,BlockPos> drops){
  if(state.getValue(CockpitDoor.LOCKED)&&explosion.getBlockInteraction()==Explosion.BlockInteraction.TRIGGER_BLOCK)return;
  super.onExplosionHit(state,level,pos,explosion,drops);
 }
 @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
  if(!level.isClientSide&&!next.is(this))CabinManager.broken(level,pos);
  super.onRemove(state,level,pos,next,moving);
 }
 public static Direction attached(BlockState state){return state.getValue(OPEN)?state.getValue(FACING).getOpposite():state.getValue(HALF)==Half.TOP?Direction.UP:Direction.DOWN;}
 public static double plane(BlockState state,Direction normal){return dev.omnidisplay.region.TrapdoorGeometry.plane(attached(state).getAxisDirection()==Direction.AxisDirection.POSITIVE,normal.getAxisDirection()==Direction.AxisDirection.POSITIVE);}
}
