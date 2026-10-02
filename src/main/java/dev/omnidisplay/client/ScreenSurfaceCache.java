package dev.omnidisplay.client;
import dev.omnidisplay.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.*;
import java.util.*;
/** Surface descriptors stay local; camera and moving ship transforms are never cached. */
final class ScreenSurfaceCache {
 record Surface(DisplayRenderer.Body body,BlockPos pos,BlockState state,Direction face,double plane,boolean door,boolean hatch){}
 private record View(Vector3d local,BlockPos origin,Matrix4d clip){}
 private static dev.omnidisplay.region.IdentitySurfaceGroups<DisplayRenderer.Body,Surface> groups=new dev.omnidisplay.region.IdentitySurfaceGroups<>(List.of(),Surface::body);
 private static Object shape,world;private static long version=-1,tick=-1;private static List<Surface> surfaces=List.of();private static boolean incomplete;
 static void clear(){shape=world=null;surfaces=List.of();groups=new dev.omnidisplay.region.IdentitySurfaceGroups<>(surfaces,Surface::body);version=tick=-1;incomplete=false;}
 static List<Surface> faces(){
  long omni$perf=dev.omnidisplay.diagnostics.PerformanceDiagnostics.begin(dev.omnidisplay.diagnostics.PerformanceDiagnostics.Phase.SURFACE_CACHE);try{

  var mc=Minecraft.getInstance();var current=DisplayRenderer.bodies();if(mc.level==null||current.isEmpty())return List.of();long v=ScreenTopologyCache.version(mc.level),now=mc.level.getGameTime();
  if(world==mc.level&&!incomplete&&Objects.equals(shape,current)&&v==version&&now>=tick)return surfaces;
  shape=current;world=mc.level;version=v;tick=now;incomplete=false;var next=new ArrayList<Surface>();
  for(var body:current)for(var cell:body.shape().walls()){
   var p=new BlockPos(cell.x(),cell.y(),cell.z());if(body.moving()==null&&!mc.level.hasChunkAt(p)){incomplete=true;continue;}var state=body.level().getBlockState(p);if(body.moving()==null&&!ScreenFaces.isScreen(mc.level,p)){incomplete=true;continue;}
   boolean door=state.is(OmniDisplay.DOOR.get()),hatch=state.is(OmniDisplay.TRAPDOOR.get());
   for(var face:Direction.values())if(body.inward(p,face)){
    double plane=face.getAxisDirection()==Direction.AxisDirection.POSITIVE?1:0;
    if(state.is(OmniDisplay.THIN_DISPLAY.get()))plane=ThinDisplayBlock.plane(state,face);else if(hatch)plane=CockpitTrapdoor.plane(state,face);else if(door)plane=dev.omnidisplay.region.TrapdoorGeometry.plane(state.getValue(DoorBlock.FACING).getAxisDirection()!=Direction.AxisDirection.POSITIVE,face.getAxisDirection()==Direction.AxisDirection.POSITIVE);
    next.add(new Surface(body,p,state,face,plane,door,hatch));
   }
  }surfaces=List.copyOf(next);groups=new dev.omnidisplay.region.IdentitySurfaceGroups<>(surfaces,Surface::body);return surfaces;
 
  }finally{dev.omnidisplay.diagnostics.PerformanceDiagnostics.end(dev.omnidisplay.diagnostics.PerformanceDiagnostics.Phase.SURFACE_CACHE,omni$perf);}
 }
 static List<Surface> forBody(DisplayRenderer.Body body){return groups.matching(body,(cached,current)->cached.sub()==current.sub()&&cached.moving()==current.moving()&&cached.shape().equals(current.shape()));}
 static boolean potentiallyVisible(net.minecraft.client.Camera camera,Matrix4f view,Matrix4f projection){
  var owner=DisplayRenderer.owner();if(owner==null||DisplayRenderer.viewActive())return true;var list=faces();if(incomplete)return true;
  var eye=camera.getPosition();var views=new IdentityHashMap<DisplayRenderer.Body,View>();
  for(var s:list){
   owner=s.body().sub();if(owner.isRemoved()||s.body().moving()!=null&&s.body().moving().isRemoved())continue;
   var frame=views.computeIfAbsent(s.body(),body->{var pose=body.pose(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));var local=dev.omnidisplay.region.ScreenCoordinates.localEye(pose,new Vector3d(eye.x,eye.y,eye.z));var origin=body.origin();var relative=new Matrix4d().translation(-eye.x,-eye.y,-eye.z).mul(pose).translate(origin.getX(),origin.getY(),origin.getZ());return new View(local,origin,new Matrix4d(projection).mul(new Matrix4d(view)).mul(relative));});
   var local=frame.local();var origin=frame.origin();var clip=frame.clip();
   // Keep animated hatch bounds conservative until the next topology refresh.
   // Moving doors get a deliberately broad bound and no static back-face rejection.
   if(!s.door&&!dev.omnidisplay.region.PartitionSurface.frontFacing(s.face.get3DDataValue(),local.x-s.pos.getX(),local.y-s.pos.getY(),local.z-s.pos.getZ(),s.plane))continue;
   double x=s.pos.getX()-origin.getX(),y=s.pos.getY()-origin.getY(),z=s.pos.getZ()-origin.getZ(),pad=s.door?2:.02;
   if(dev.omnidisplay.region.ScreenVisibility.intersects(clip,x-pad,y-pad,z-pad,x+1+pad,y+1+pad,z+1+pad))return true;
  }return false;
 }
}
