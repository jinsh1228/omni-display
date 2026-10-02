package dev.omnidisplay;

import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;

/** A self-supporting sheet at one boundary of its cell, with no support requirement. */
public final class ThinDisplayBlock extends OmniDisplay.DisplayBlock {
 public static final DirectionProperty ATTACHED_FACE=DirectionProperty.create("attached_face");
 public ThinDisplayBlock(){super(BlockBehaviour.Properties.of().strength(2).noOcclusion());registerDefaultState(defaultBlockState().setValue(ATTACHED_FACE,Direction.DOWN));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(ATTACHED_FACE);}
 @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(ATTACHED_FACE,c.getClickedFace().getOpposite());}
 @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){var face=s.getValue(ATTACHED_FACE);boolean positive=face.getAxisDirection()==Direction.AxisDirection.POSITIVE;double min=dev.omnidisplay.region.ThinGeometry.min(positive)*16,max=dev.omnidisplay.region.ThinGeometry.max(positive)*16;return switch(face.getAxis()){
  case Y->Block.box(0,min,0,16,max,16);
  case Z->Block.box(0,0,min,16,16,max);
  case X->Block.box(min,0,0,max,16,16);
 };}
 @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(ATTACHED_FACE,r.rotate(s.getValue(ATTACHED_FACE)));}
 @Override protected BlockState mirror(BlockState s,Mirror m){return s.setValue(ATTACHED_FACE,m.mirror(s.getValue(ATTACHED_FACE)));}
 public static double plane(BlockState s,Direction normal){
  Direction attached=s.getValue(ATTACHED_FACE);
  return dev.omnidisplay.region.ThinGeometry.plane(attached.getAxisDirection()==Direction.AxisDirection.POSITIVE,normal.getAxisDirection()==Direction.AxisDirection.POSITIVE);
 }
}
