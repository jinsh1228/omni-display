package dev.omnidisplay.client;

import java.util.*;
import dev.omnidisplay.*;
import dev.omnidisplay.region.ScreenWave;
import net.minecraft.core.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.DoorBlock;
import com.mojang.blaze3d.vertex.*;
import org.joml.Vector3d;

/** Cached clipped hexagons with a shortest-path wave on the interior screen surface. */
public final class ScreenAnimation {
 private record Face(BlockPos pos,Direction dir){}
 private record Piece(Node node,List<double[]> polygon){}
 private static final class Node {
  final Vector3d p;final Direction normal;final List<Node> neighbours=new ArrayList<>();double distance=Double.POSITIVE_INFINITY,faultDistance;double[] clicks;double savedFault;
  Node(Vector3d p,Direction n){this.p=p;normal=n;}
 }
 private record QueueNode(Node node,double distance){}
 private record Bucket(int x,int y,int z){}
 private static final ResourceLocation WHITE=ResourceLocation.fromNamespaceAndPath(OmniDisplay.ID,"textures/block/wave_white.png");
 private static BlockPos[] clickOrigins=new BlockPos[0];
 private static BlockPos faultOrigin;private static Object selection;private static int heading;
 private static Map<Face,List<Piece>> faces=new HashMap<>();
 private static List<Node> nodes=new ArrayList<>();
 private record Cached(Map<Face,List<Piece>> faces,List<Node> nodes,int heading,BlockPos fault,BlockPos[] clicks){}
 private static final Map<Object,Cached> groupCache=new HashMap<>();
 private static void select(Object next){if(selection==next)return;if(selection!=null){if(groupCache.size()>32)groupCache.clear();groupCache.put(selection,new Cached(faces,nodes,heading,faultOrigin,clickOrigins));}var cached=groupCache.remove(next);selection=next;if(cached==null){faces=new HashMap<>();nodes=new ArrayList<>();clickOrigins=new BlockPos[0];heading=Integer.MIN_VALUE;faultOrigin=null;}else{faces=cached.faces;nodes=cached.nodes;heading=cached.heading;faultOrigin=cached.fault;clickOrigins=cached.clicks;}}
 public static void reset(){selection=null;faces=new HashMap<>();nodes=new ArrayList<>();groupCache.clear();}
 private static final double BUCKET=.4;
 private static Bucket bucket(Vector3d p){return new Bucket((int)Math.floor(p.x/BUCKET),(int)Math.floor(p.y/BUCKET),(int)Math.floor(p.z/BUCKET));}
 /** UVs match Minecraft cube faces and the existing panel texture. */
 private static Vector3d point(Direction face,double u,double v,double plane){
  return switch(face){
   case EAST->new Vector3d(plane,1-v,1-u);case WEST->new Vector3d(plane,1-v,u);
   case NORTH->new Vector3d(1-u,1-v,plane);case SOUTH->new Vector3d(u,1-v,plane);
   case UP->new Vector3d(u,plane,v);case DOWN->new Vector3d(u,plane,1-v);
  };
 }
 private static double plane(Direction face){return face.getAxisDirection()==Direction.AxisDirection.POSITIVE?1:0;}
 private static List<double[]> clipped(int col,int row){
  double x=col*16,y=row*16+(Math.floorMod(col,2))*8;
  List<double[]> p=new ArrayList<>(List.of(new double[]{x-11,y},new double[]{x-5,y-8},new double[]{x+5,y-8},new double[]{x+11,y},new double[]{x+5,y+8},new double[]{x-5,y+8}));
  for(int axis=0;axis<2;axis++)for(int edge=0;edge<2;edge++){
   List<double[]> out=new ArrayList<>();if(p.isEmpty())return p;double bound=edge==0?0:64;
   for(int i=0;i<p.size();i++){
    var a=p.get(i);var b=p.get((i+1)%p.size());boolean ai=edge==0?a[axis]>=bound:a[axis]<=bound,bi=edge==0?b[axis]>=bound:b[axis]<=bound;
    if(ai)out.add(a);if(ai!=bi){double t=(bound-a[axis])/(b[axis]-a[axis]);out.add(new double[]{a[0]+t*(b[0]-a[0]),a[1]+t*(b[1]-a[1])});}
   }p=out;
  }
  double area=0;for(int i=0;i<p.size();i++){var a=p.get(i);var b=p.get((i+1)%p.size());area+=a[0]*b[1]-b[0]*a[1];}
  return Math.abs(area)<.01?List.of():p;
 }
 private static void rebuild(int h,dev.omnidisplay.CabinSettings settings,dev.omnidisplay.region.Enclosure.Result shape,net.minecraft.world.level.Level sourceLevel,java.util.function.BiPredicate<BlockPos,Direction> inward){
  long omni$perf=dev.omnidisplay.diagnostics.PerformanceDiagnostics.begin(dev.omnidisplay.diagnostics.PerformanceDiagnostics.Phase.ANIMATION_BUILD);try{

  selection=shape;clickOrigins=settings.clickOrigins.clone();heading=h;faultOrigin=settings.faultOrigin;faces.clear();nodes.clear();
  if(shape==null)return;var mc=Minecraft.getInstance();
  var origin=shape.min();var dedup=new HashMap<String,Node>();
  var bounds=DisplayRenderer.hudBounds(shape,settings);var center=bounds.getCenter();var centerLocal=new Vector3d(center.x-origin.x(),center.y-origin.y(),center.z-origin.z());
  double angle=Math.toRadians(h);var forward=new Vector3d(Math.sin(angle),0,-Math.cos(angle));
  Vector3d target=null;double rayDistance=Double.POSITIVE_INFINITY;
  for(var c:shape.walls()){
   var pos=new BlockPos(c.x(),c.y(),c.z());var state=sourceLevel.getBlockState(pos);boolean door=state.is(OmniDisplay.DOOR.get());
   for(var dir:Direction.values()){
    if(inward!=null?!inward.test(pos,dir):!ScreenFaces.supportsFace(sourceLevel,pos,dir))continue;
    double surface=state.is(OmniDisplay.TRAPDOOR.get())?CockpitTrapdoor.plane(state,dir):state.is(OmniDisplay.THIN_DISPLAY.get())?ThinDisplayBlock.plane(state,dir):plane(dir);
    var n=pos.relative(dir);if(inward==null&&!ScreenFaces.inward(sourceLevel,shape,pos,dir))continue;
    if(door&&dir!=state.getValue(DoorBlock.FACING)&&dir!=state.getValue(DoorBlock.FACING).getOpposite())continue;
    var offset=new Vector3d(c.x()-origin.x(),c.y()-origin.y(),c.z()-origin.z());
    var normal=new Vector3d(dir.getStepX(),dir.getStepY(),dir.getStepZ());
    var faceCenter=point(dir,.5,.5,surface).add(offset);double denom=forward.dot(normal);
    if(denom<-.00001){double t=new Vector3d(faceCenter).sub(centerLocal).dot(normal)/denom;var hit=new Vector3d(forward).mul(t).add(centerLocal);
     if(t>0&&t<rayDistance&&Math.abs(hit.x-faceCenter.x)<=.50001&&Math.abs(hit.y-faceCenter.y)<=.50001&&Math.abs(hit.z-faceCenter.z)<=.50001){target=hit;rayDistance=t;}
    }
    var pieces=new ArrayList<Piece>();faces.put(new Face(pos,dir),pieces);
    for(int col=-1;col<=5;col++)for(int row=-1;row<=5;row++){
     var poly=clipped(col,row);if(poly.isEmpty())continue;
     var v=point(dir,col/4d,(row*16+Math.floorMod(col,2)*8)/64d,surface).add(offset);
     String key=dir.ordinal()+":"+Math.round(v.x*64)+":"+Math.round(v.y*64)+":"+Math.round(v.z*64);
     Node node=dedup.get(key);if(node==null){node=new Node(v,dir);dedup.put(key,node);nodes.add(node);}pieces.add(new Piece(node,poly));
    }
   }
  }
  if(nodes.isEmpty())return;
  // Spatial buckets connect neighbouring cells on a plane and across right-angle folds.
  // Opposite faces never connect through a wall. Door distance uses its closed unit face.
  Map<Bucket,List<Node>> grid=new HashMap<>();
  for(var node:nodes){var b=bucket(node.p);
   for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++){
    var others=grid.get(new Bucket(b.x+x,b.y+y,b.z+z));if(others==null)continue;
    for(var other:others){if(node.normal==other.normal.getOpposite())continue;
     if(node.normal==other.normal){int axis=node.normal.getAxis().ordinal();if(Math.abs(node.p.get(axis)-other.p.get(axis))>.001)continue;}
     double dist=node.p.distanceSquared(other.p);if(dist<.39*.39){node.neighbours.add(other);other.neighbours.add(node);}
    }
   }grid.computeIfAbsent(b,k->new ArrayList<>()).add(node);
  }
  if(target==null)target=new Vector3d(forward).mul(Math.max(bounds.getXsize(),bounds.getZsize())).add(centerLocal);
  final var start=target;Node source=nodes.stream().min(Comparator.comparingDouble(n->n.p.distanceSquared(start))).orElseThrow();source.distance=0;
  var queue=new PriorityQueue<QueueNode>(Comparator.comparingDouble(QueueNode::distance));queue.add(new QueueNode(source,0));
  while(!queue.isEmpty()){var q=queue.remove();if(q.distance!=q.node.distance)continue;
   for(var next:q.node.neighbours){double d=q.distance+q.node.p.distance(next.p);if(d<next.distance){next.distance=d;queue.add(new QueueNode(next,d));}}
  }
  double max=nodes.stream().filter(n->Double.isFinite(n.distance)).mapToDouble(n->n.distance).max().orElse(1);
  for(var node:nodes)if(!Double.isFinite(node.distance))node.distance=max+node.p.distance(source.p);
  max=nodes.stream().mapToDouble(n->n.distance).max().orElse(1);if(max<=0)max=1;
  for(var node:nodes)node.distance/=max;
  for(var node:nodes){node.clicks=new double[clickOrigins.length];Arrays.fill(node.clicks,Double.NaN);}
  for(int event=-1;event<clickOrigins.length;event++){
   BlockPos startPos=event<0?faultOrigin:clickOrigins[event];if(startPos==null)continue;
   var point=new Vector3d(startPos.getX()-origin.x()+.5,startPos.getY()-origin.y()+.5,startPos.getZ()-origin.z()+.5);
   Node broken=nodes.stream().min(Comparator.comparingDouble(n->n.p.distanceSquared(point))).orElseThrow();
   for(var node:nodes)node.faultDistance=Double.POSITIVE_INFINITY;broken.faultDistance=0;
   queue.clear();queue.add(new QueueNode(broken,0));
   while(!queue.isEmpty()){var q=queue.remove();if(q.distance!=q.node.faultDistance)continue;for(var next:q.node.neighbours){double d=q.distance+q.node.p.distance(next.p);if(d<next.faultDistance){next.faultDistance=d;queue.add(new QueueNode(next,d));}}}
   double far=nodes.stream().filter(n->Double.isFinite(n.faultDistance)).mapToDouble(n->n.faultDistance).max().orElse(1);
   for(var node:nodes)if(!Double.isFinite(node.faultDistance))node.faultDistance=far+node.p.distance(broken.p);
   far=Math.max(.001,nodes.stream().mapToDouble(n->n.faultDistance).max().orElse(1));for(var node:nodes)node.faultDistance/=far;
   if(event<0)for(var node:nodes)node.savedFault=node.faultDistance;else for(var node:nodes)node.clicks[event]=node.faultDistance;
  }
  for(var node:nodes){node.faultDistance=node.savedFault;node.neighbours.clear();}
 
  }finally{dev.omnidisplay.diagnostics.PerformanceDiagnostics.end(dev.omnidisplay.diagnostics.PerformanceDiagnostics.Phase.ANIMATION_BUILD,omni$perf);}
 }
 public static void draw(BlockPos pos,Direction worldFace,Direction modelFace,boolean door,PoseStack pose,MultiBufferSource buffers,float partial){
  draw(pos,worldFace,modelFace,door,pose,buffers,partial,1);
 }
 public static void draw(BlockPos pos,Direction worldFace,Direction modelFace,boolean door,PoseStack pose,MultiBufferSource buffers,float partial,float opacity){
  if(!DisplayRenderer.screen(pos))return;
  draw(pos,worldFace,modelFace,door,pose,buffers,partial,opacity,DisplayRenderer.surfaceSettings(pos),DisplayRenderer.surfaceShape(pos),Minecraft.getInstance().level);
 }
 public static void draw(BlockPos pos,Direction worldFace,Direction modelFace,boolean door,PoseStack pose,MultiBufferSource buffers,float partial,float opacity,CabinSettings settings,dev.omnidisplay.region.Enclosure.Result shape,net.minecraft.world.level.Level source){
  draw(pos,worldFace,modelFace,door,pose,buffers,partial,opacity,settings,shape,source,null);
 }
 public static void draw(BlockPos pos,Direction worldFace,Direction modelFace,boolean door,PoseStack pose,MultiBufferSource buffers,float partial,float opacity,CabinSettings settings,dev.omnidisplay.region.Enclosure.Result shape,net.minecraft.world.level.Level source,java.util.function.BiPredicate<BlockPos,Direction> inward){
  var mc=Minecraft.getInstance();if(settings==null||mc.level==null||!DisplayRenderer.active())return;
  double now=mc.level.getGameTime()+partial;if(!settings.animating(now))return;
  if(dev.omnidisplay.region.LargeCabinFade.enabled(shape)){
   drawUniform(pos,modelFace,door,pose,buffers,opacity,settings,source,now);return;
  }
  select(shape);
  if(selection!=shape||heading!=settings.waveHeading||!Objects.equals(faultOrigin,settings.faultOrigin)||!Arrays.equals(clickOrigins,settings.clickOrigins))rebuild(settings.waveHeading,settings,shape,source,inward);
  var pieces=faces.get(new Face(pos,worldFace));if(pieces==null)return;
  var consumer=buffers.getBuffer(RenderType.entityTranslucent(WHITE));
  double surface=door?(modelFace==Direction.EAST?3/16d:0):plane(modelFace);
  var state=source.getBlockState(pos);if(state.is(OmniDisplay.THIN_DISPLAY.get()))surface=ThinDisplayBlock.plane(state,modelFace);if(state.is(OmniDisplay.TRAPDOOR.get()))surface=CockpitTrapdoor.plane(state,modelFace);
  surface+=modelFace.getAxisDirection()==Direction.AxisDirection.POSITIVE?.0005:-.0005;
  for(var piece:pieces){
   double open=ScreenWave.amount(settings.waveBase,settings.waveTimes,settings.waveTargets,now,piece.node.distance,piece.node.faultDistance,settings.faultEvent,piece.node.clicks);
   int alpha=(int)Math.round((1-open)*255*opacity);
   if(alpha>0)polygon(consumer,pose,modelFace,surface,piece.polygon,4,6,9,alpha);
   int last=settings.waveTimes.length-1;if(last<0)continue;
   double progress=(now-settings.waveTimes[last]-ScreenWave.delay(settings.waveTargets[last],piece.node.distance,piece.node.faultDistance,last,settings.faultEvent,piece.node.clicks)*ScreenWave.SPREAD)/ScreenWave.FADE;
   double before=ScreenWave.amount(settings.waveBase,settings.waveTimes,settings.waveTargets,settings.waveTimes[last],piece.node.distance,piece.node.faultDistance,settings.faultEvent,piece.node.clicks);
   double flash=progress>0&&progress<1?Math.sin(progress*Math.PI)*Math.abs((settings.waveTargets[last]?1:0)-before):0;
   if(flash>.01){int glow=(int)(flash*255*opacity);var poly=piece.polygon;
    int color=last==settings.faultEvent?0xFF627C:settings.waveTargets[last]?(progress<.55?0x70E6DE:0xA398D7):0xFFC17E;
    for(int i=0;i<poly.size();i++){var a=poly.get(i);var b=poly.get((i+1)%poly.size());
     if((a[0]==b[0]&&(a[0]==0||a[0]==64))||(a[1]==b[1]&&(a[1]==0||a[1]==64)))continue;
     double dx=b[0]-a[0],dy=b[1]-a[1],len=Math.hypot(dx,dy);if(len<.01)continue;double nx=-dy/len*.25,ny=dx/len*.25;
     polygon(consumer,pose,modelFace,surface+(modelFace.getAxisDirection()==Direction.AxisDirection.POSITIVE?.001:-.001),List.of(a,b,new double[]{b[0]+nx,b[1]+ny},new double[]{a[0]+nx,a[1]+ny}),(color>>16)&255,(color>>8)&255,color&255,glow);
    }
   }
  }
 }
 private static void drawUniform(BlockPos pos,Direction face,boolean door,PoseStack pose,MultiBufferSource buffers,float opacity,CabinSettings settings,net.minecraft.world.level.Level source,double now){
  double open=dev.omnidisplay.region.LargeCabinFade.amount(settings.waveBase,settings.waveTimes,settings.waveTargets,now);
  int alpha=(int)Math.round((1-open)*255*opacity);if(alpha<=0)return;
  double surface=door?(face==Direction.EAST?3/16d:0):plane(face);
  var state=source.getBlockState(pos);if(state.is(OmniDisplay.THIN_DISPLAY.get()))surface=ThinDisplayBlock.plane(state,face);if(state.is(OmniDisplay.TRAPDOOR.get()))surface=CockpitTrapdoor.plane(state,face);
  surface+=face.getAxisDirection()==Direction.AxisDirection.POSITIVE?.0005:-.0005;
  var out=buffers.getBuffer(RenderType.entityTranslucent(WHITE));
  for(int i=0;i<4;i++){
   var v=point(face,i==1||i==2?1:0,i>=2?1:0,surface);
   out.addVertex(pose.last(),(float)v.x,(float)v.y,(float)v.z).setColor(4,6,9,alpha).setUv(.5f,.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose.last(),face.getStepX(),face.getStepY(),face.getStepZ());
  }
 }
 private static void polygon(VertexConsumer out,PoseStack pose,Direction dir,double plane,List<double[]> p,int r,int g,int b,int a){
  for(int i=1;i+1<p.size();i++)for(var uv:List.of(p.get(0),p.get(i),p.get(i+1),p.get(i+1))){var v=point(dir,uv[0]/64,uv[1]/64,plane);
   out.addVertex(pose.last(),(float)v.x,(float)v.y,(float)v.z).setColor(r,g,b,a).setUv(.5f,.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pose.last(),dir.getStepX(),dir.getStepY(),dir.getStepZ());
  }
 }
}
