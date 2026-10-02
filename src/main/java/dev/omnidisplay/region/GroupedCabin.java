package dev.omnidisplay.region;
import java.util.*;
/** Preserve actual screen surfaces and interior cells; gaps never become screen walls. */
public final class GroupedCabin {
 public static void validateBodies(Collection<? extends List<Enclosure.Result>> bodies){
  int count=0;for(var local:bodies){if(local.isEmpty())continue;count+=local.stream().mapToInt(s->s.walls().size()).sum();if(count>4096)throw new IllegalArgumentException("显示器超过4096格");combine(local,local.get(0));}
 }
 public static Enclosure.Result combine(List<Enclosure.Result> shapes,Enclosure.Result dominant){
  Set<Enclosure.Cell> walls=new HashSet<>(),inside=new HashSet<>();for(var shape:shapes){walls.addAll(shape.walls());inside.addAll(shape.interior());}
  int lx=Integer.MAX_VALUE,ly=lx,lz=lx,hx=Integer.MIN_VALUE,hy=hx,hz=hx;
  var bounds=new HashSet<>(walls);bounds.addAll(inside);for(var p:bounds){lx=Math.min(lx,p.x());ly=Math.min(ly,p.y());lz=Math.min(lz,p.z());hx=Math.max(hx,p.x());hy=Math.max(hy,p.y());hz=Math.max(hz,p.z());}
  if(walls.isEmpty())throw new IllegalArgumentException("Empty group");
  if(walls.size()>4096||(long)(hx-lx+3)*(hy-ly+3)*(hz-lz+3)>65536)throw new IllegalArgumentException("座舱分组范围过大，请缩小选择范围。");
  double x=dominant.centerX(),y=dominant.centerY(),z=dominant.centerZ();
  if(!dominant.open()){x=dominant.interior().stream().mapToDouble(p->p.x()+.5).average().orElse(0);y=dominant.interior().stream().mapToDouble(p->p.y()+.5).average().orElse(0);z=dominant.interior().stream().mapToDouble(p->p.z()+.5).average().orElse(0);}
  return new Enclosure.Result(Set.copyOf(walls),Set.copyOf(inside),new Enclosure.Cell(lx,ly,lz),new Enclosure.Cell(hx,hy,hz),true,x,y,z);
 }
}
