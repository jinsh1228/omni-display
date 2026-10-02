package dev.omnidisplay.region;
import java.util.*;

/** Discrete voxel enclosure: 26-connected screen group, 6-connected outside flood. */
public final class Enclosure {
 public record Cell(int x,int y,int z){
  // Dense plot-coordinate grids cluster badly with the record's linear 31-based hash.
  @Override public int hashCode(){long h=(long)x*0x9E3779B185EBCA87L^(long)y*0xC2B2AE3D27D4EB4FL^(long)z*0x165667B19E3779F9L;h=(h^(h>>>33))*0xff51afd7ed558ccdL;h=(h^(h>>>33))*0xc4ceb9fe1a85ec53L;h^=h>>>33;return (int)(h^(h>>>32));}
  Cell offset(int x,int y,int z){return new Cell(this.x+x,this.y+y,this.z+z);}}
 public record Result(Set<Cell> walls,Set<Cell> interior,Cell min,Cell max,boolean open,double centerX,double centerY,double centerZ){
  public Result(Set<Cell> walls,Set<Cell> interior,Cell min,Cell max){this(walls,interior,min,max,false,Double.NaN,Double.NaN,Double.NaN);}
 }
 private static final int[][] SIDES={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
 public static Result detect(Set<Cell> displays,Cell seed){
  if(!displays.contains(seed))throw new IllegalArgumentException("没有找到起始显示器。");
  Set<Cell> walls=new HashSet<>();ArrayDeque<Cell> queue=new ArrayDeque<>();walls.add(seed);queue.add(seed);
  int lx=seed.x,ly=seed.y,lz=seed.z,hx=lx,hy=ly,hz=lz;
  while(!queue.isEmpty()){
   Cell c=queue.removeFirst();
   for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++){
    if(x==0&&y==0&&z==0)continue;Cell p=c.offset(x,y,z);
    if(displays.contains(p)&&walls.add(p)){
     if(walls.size()>4096)throw new IllegalArgumentException("相连显示器超过4096块，请缩小范围。");
     lx=Math.min(lx,p.x);ly=Math.min(ly,p.y);lz=Math.min(lz,p.z);hx=Math.max(hx,p.x);hy=Math.max(hy,p.y);hz=Math.max(hz,p.z);
     if((long)(hx-lx+3)*(hy-ly+3)*(hz-lz+3)>65536)throw new IllegalArgumentException("扫描范围超过65536格，请缩小显示器组。");
     queue.addLast(p);
    }
   }
  }
  Cell min=new Cell(lx,ly,lz),max=new Cell(hx,hy,hz);
  int nx=hx-lx+3,ny=hy-ly+3,nz=hz-lz+3,total=nx*ny*nz;
  byte[] cells=new byte[total];
  for(Cell w:walls)cells[((w.y-ly+1)*nz+w.z-lz+1)*nx+w.x-lx+1]=1;
  int[] pending=new int[total];int head=0,tail=0;pending[tail++]=0;cells[0]=2;
  while(head<tail){
   int i=pending[head++],x=i%nx,z=(i/nx)%nz,y=i/(nx*nz);
   for(int[] d:SIDES){int xx=x+d[0],yy=y+d[1],zz=z+d[2];if(xx<0||xx>=nx||yy<0||yy>=ny||zz<0||zz>=nz)continue;
    int j=(yy*nz+zz)*nx+xx;if(cells[j]==0){cells[j]=2;pending[tail++]=j;}
   }
  }
  Set<Cell> inside=new HashSet<>();
  for(int i=0;i<total;i++)if(cells[i]==0){int x=i%nx,z=(i/nx)%nz,y=i/(nx*nz);inside.add(new Cell(x+lx-1,y+ly-1,z+lz-1));
   if(inside.size()>65536)throw new IllegalArgumentException("内部保留区超过65536格，请缩小座舱。");}
  if(inside.isEmpty())throw new IllegalArgumentException("未找到封闭内部：请补齐显示器外壳，普通门或普通方块不能代替显示器及座舱门边界。");
  return new Result(Set.copyOf(walls),Set.copyOf(inside),min,max);
 }
}
