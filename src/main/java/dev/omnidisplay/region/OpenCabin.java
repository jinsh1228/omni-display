package dev.omnidisplay.region;
import java.util.*;

/** Axis-aligned screen rays fitted in relative coordinates, including parallel walls. */
public final class OpenCabin {
 public record Face(Enclosure.Cell block,int nx,int ny,int nz){
  double coordinate(int axis){return switch(axis){case 0->block.x()+.5+nx*.5;case 1->block.y()+.5+ny*.5;default->block.z()+.5+nz*.5;};}
  int normal(int axis){return axis==0?nx:axis==1?ny:nz;}
 }
 public static Enclosure.Result detect(List<Face> faces){
  if(faces.isEmpty())throw new IllegalArgumentException("没有贴片屏幕");
  var base=faces.get(0).block();double[] offset={base.x(),base.y(),base.z()},center=new double[3];
  double span=0;
  for(int a=0;a<3;a++){
   double low=Double.POSITIVE_INFINITY,high=Double.NEGATIVE_INFINITY;
   for(var f:faces){low=Math.min(low,f.coordinate(a)-offset[a]);high=Math.max(high,f.coordinate(a)-offset[a]);}span=Math.max(span,high-low);
  }
  double distance=Math.clamp(1.5+span*.25,2,6);
  for(int a=0;a<3;a++){
   double sum=0,weight=0;
   for(var f:faces){double w=f.normal(a)==0?1:.025;sum+=w*(f.coordinate(a)-offset[a]+f.normal(a)*distance);weight+=w;}
   center[a]=sum/weight;
  }
  // Keep the candidate in front of the sheets. Contradictory layouts converge to a compromise.
  for(int iteration=0;iteration<16;iteration++){
   double[] correction=new double[3];int violations=0;
   for(var f:faces){double front=0;for(int a=0;a<3;a++)front+=(center[a]-(f.coordinate(a)-offset[a]))*f.normal(a);
    if(front<.75){violations++;for(int a=0;a<3;a++)correction[a]+=(.75-front)*f.normal(a);}}
   if(violations==0)break;for(int a=0;a<3;a++)center[a]+=correction[a]/violations;
  }
  for(int a=0;a<3;a++)center[a]+=offset[a];
  Set<Enclosure.Cell> walls=new HashSet<>(),inside=new HashSet<>();for(var f:faces)walls.add(f.block());
  int radius=(int)Math.clamp(Math.ceil(span*.5+2),3,7);
  int cx=(int)Math.floor(center[0]),cy=(int)Math.floor(center[1]),cz=(int)Math.floor(center[2]);
  for(int x=cx-radius;x<=cx+radius;x++)for(int y=cy-radius;y<=cy+radius;y++)for(int z=cz-radius;z<=cz+radius;z++){
   var c=new Enclosure.Cell(x,y,z);if(walls.contains(c))continue;
   boolean front=true;for(var f:faces){double d=(x+.5-f.coordinate(0))*f.nx()+(y+.5-f.coordinate(1))*f.ny()+(z+.5-f.coordinate(2))*f.nz();if(d<-.01){front=false;break;}}
   if(front)inside.add(c);
  }
  // An unusual face arrangement must not prevent a single sheet from being switched on.
  if(inside.isEmpty())for(var f:faces){var b=f.block();var c=new Enclosure.Cell(b.x()+f.nx(),b.y()+f.ny(),b.z()+f.nz());if(!walls.contains(c))inside.add(c);}
  if(inside.isEmpty())throw new IllegalArgumentException("贴片没有可观看的空间");
  int lx=cx,ly=cy,lz=cz,hx=cx,hy=cy,hz=cz;
  for(var c:walls){lx=Math.min(lx,c.x());ly=Math.min(ly,c.y());lz=Math.min(lz,c.z());hx=Math.max(hx,c.x());hy=Math.max(hy,c.y());hz=Math.max(hz,c.z());}
  for(var c:inside){lx=Math.min(lx,c.x());ly=Math.min(ly,c.y());lz=Math.min(lz,c.z());hx=Math.max(hx,c.x());hy=Math.max(hy,c.y());hz=Math.max(hz,c.z());}
  return new Enclosure.Result(Set.copyOf(walls),Set.copyOf(inside),new Enclosure.Cell(lx,ly,lz),new Enclosure.Cell(hx,hy,hz),true,center[0],center[1],center[2]);
 }
}
