package dev.omnidisplay;

import dev.omnidisplay.region.Enclosure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Server-authoritative, persisted surface assignment. No player opacity is stored here. */
public final class ScreenFaces {
 private static Enclosure.Cell cell(BlockPos p){return new Enclosure.Cell(p.getX(),p.getY(),p.getZ());}
 public static boolean isScreen(Level level,BlockPos pos){return OmniDisplay.isScreen(level.getBlockState(pos))||PanelStore.mask(level,pos)!=0;}
 public static boolean supportsFace(Level level,BlockPos pos,Direction face){
  var state=level.getBlockState(pos);
  if(state.is(OmniDisplay.TRAPDOOR.get()))return !state.getValue(CockpitTrapdoor.OPEN)&&face.getAxis()==CockpitTrapdoor.attached(state).getAxis();
  if(state.is(OmniDisplay.THIN_DISPLAY.get()))return face.getAxis()==state.getValue(ThinDisplayBlock.ATTACHED_FACE).getAxis();
  if(state.is(OmniDisplay.DOOR.get()))return face.getAxis()==state.getValue(DoorBlock.FACING).getAxis();
  return state.is(OmniDisplay.DISPLAY.get())||(PanelStore.mask(level,pos)&(1<<face.get3DDataValue()))!=0;
 }
 public static Enclosure.Result detect(Level level,Set<Enclosure.Cell> cells,BlockPos seed){
  Enclosure.Result shape;
  try{shape=Enclosure.detect(cells,cell(seed));}
  catch(IllegalArgumentException ex){if(!(level.isClientSide?dev.omnidisplay.client.ServerRenderState.experimental:RenderConfig.experimental()))throw ex;if(PanelStore.mask(level,seed)!=0)return open(level,cells,seed);if(level.getBlockEntity(seed) instanceof CabinEntity h&&h.settings.manualFaces!=0)return manual(level,cells,h.settings.manualFaces);throw ex;}
  for(var c:shape.walls()){
   var p=new BlockPos(c.x(),c.y(),c.z());var state=level.getBlockState(p);
   if(!state.is(OmniDisplay.THIN_DISPLAY.get())&&!(state.is(OmniDisplay.TRAPDOOR.get())&&!state.getValue(CockpitTrapdoor.OPEN)))continue;
   for(var d:Direction.values())if(shape.interior().contains(cell(p.relative(d)))&&!supportsFace(level,p,d))throw new IllegalArgumentException("轻薄显示器的宽面需要朝向座舱内部。");
  }
  return shape;
 }
 private static Enclosure.Result open(Level level,Set<Enclosure.Cell> cells,BlockPos seed){
  Set<BlockPos> visited=new HashSet<>();var queue=new ArrayDeque<BlockPos>();queue.add(seed);visited.add(seed);
  var faces=new ArrayList<dev.omnidisplay.region.OpenCabin.Face>();
  while(!queue.isEmpty()){
   var p=queue.removeFirst();int mask=PanelStore.mask(level,p);
   for(var d:Direction.values())if((mask&(1<<d.get3DDataValue()))!=0)faces.add(new dev.omnidisplay.region.OpenCabin.Face(cell(p),d.getStepX(),d.getStepY(),d.getStepZ()));
   for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++){
    var n=p.offset(x,y,z);if(cells.contains(cell(n))&&PanelStore.mask(level,n)!=0&&visited.add(n))queue.addLast(n);
   }
  }
  return dev.omnidisplay.region.OpenCabin.detect(faces);
 }
 private static Enclosure.Result manual(Level level,Set<Enclosure.Cell> cells,int mask){var faces=new ArrayList<dev.omnidisplay.region.OpenCabin.Face>();for(var c:cells){var p=new BlockPos(c.x(),c.y(),c.z());for(var d:Direction.values())if((mask&(1<<d.get3DDataValue()))!=0&&supportsFace(level,p,d))faces.add(new dev.omnidisplay.region.OpenCabin.Face(c,d.getStepX(),d.getStepY(),d.getStepZ()));}return dev.omnidisplay.region.OpenCabin.detect(faces);}
 public static boolean inward(Level level,Enclosure.Result shape,BlockPos pos,Direction face){
  return shape!=null&&supportsFace(level,pos,face)&&(shape.open()&&PanelStore.mask(level,pos)!=0?(PanelStore.mask(level,pos)&(1<<face.get3DDataValue()))!=0:shape.interior().contains(cell(pos.relative(face))));
 }
 public static Enclosure.Result scan(Level level,BlockPos seed){return detect(level,collect(level,seed),seed);}
 public static Set<Enclosure.Cell> collect(Level level,BlockPos seed){
   Set<Enclosure.Cell> cells=new HashSet<>();Set<BlockPos> visited=new HashSet<>();
   ArrayDeque<BlockPos> queue=new ArrayDeque<>();queue.add(seed);visited.add(seed);
   int lx=seed.getX(),ly=seed.getY(),lz=seed.getZ(),hx=lx,hy=ly,hz=lz;
   while(!queue.isEmpty()){
    var p=queue.remove();if(!isScreen(level,p))continue;
    cells.add(cell(p));if(cells.size()>4096)throw new IllegalArgumentException("显示器超过4096格");
    lx=Math.min(lx,p.getX());ly=Math.min(ly,p.getY());lz=Math.min(lz,p.getZ());
    hx=Math.max(hx,p.getX());hy=Math.max(hy,p.getY());hz=Math.max(hz,p.getZ());
    if((long)(hx-lx+3)*(hy-ly+3)*(hz-lz+3)>65536)throw new IllegalArgumentException("座舱范围过大");
    for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++){
     var n=p.offset(x,y,z);if(!visited.add(n))continue;
     // Do not load chunks merely to update a cosmetic surface.
     if(level.hasChunkAt(n)&&isScreen(level,n))queue.add(n);
    }
   }
   return cells;
 }
 public static void paint(Level level,Enclosure.Result enclosure){
   for(var c:enclosure.walls()){
    var pos=new BlockPos(c.x(),c.y(),c.z());BlockState state=level.getBlockState(pos),next=state;
    if(state.is(OmniDisplay.DISPLAY.get())||state.is(OmniDisplay.THIN_DISPLAY.get())||state.is(OmniDisplay.TRAPDOOR.get())){
     int mask=0;for(var dir:Direction.values())if(supportsFace(level,pos,dir)&&enclosure.interior().contains(cell(pos.relative(dir))))mask|=1<<dir.get3DDataValue();
     next=state.setValue(OmniDisplay.DisplayBlock.FACES,mask);
    }else if(state.is(OmniDisplay.DOOR.get())){
     var facing=state.getValue(DoorBlock.FACING);
     int mask=(enclosure.interior().contains(cell(pos.relative(facing)))?1:0)|(enclosure.interior().contains(cell(pos.relative(facing.getOpposite())))?2:0);
     next=state.setValue(CockpitDoor.SIDES,mask);
    }
    if(PanelStore.mask(level,pos)!=0){int mask=0;for(var dir:Direction.values())if(inward(level,enclosure,pos,dir))mask|=1<<dir.get3DDataValue();PanelStore.paint(level,pos,mask);}
    if(next!=state)level.setBlock(pos,next,Block.UPDATE_CLIENTS);
   }
 }
}
