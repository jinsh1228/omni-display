import dev.omnidisplay.region.*;
import java.util.*;
public class OpenCabinTest {
 static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
 static OpenCabin.Face face(int x,int y,int z,int nx,int ny,int nz){return new OpenCabin.Face(new Enclosure.Cell(x,y,z),nx,ny,nz);}
 static void near(double a,double b){check(Math.abs(a-b)<1e-6,a+" != "+b);}
 public static void main(String[] args){
  for(int[] n:new int[][]{{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}}){
   var single=OpenCabin.detect(List.of(face(0,0,0,n[0],n[1],n[2])));
   check(single.open()&&single.walls().size()==1&&!single.interior().isEmpty(),"single surface");
   check((single.centerX()-.5)*n[0]+(single.centerY()-.5)*n[1]+(single.centerZ()-.5)*n[2]>1.5,"center behind panel");
  }
  var plane=new ArrayList<OpenCabin.Face>();for(int x=-2;x<=2;x++)for(int y=0;y<3;y++)plane.add(face(x,y,-3,0,0,1));
  var flat=OpenCabin.detect(plane);near(flat.centerX(),.5);near(flat.centerY(),1.5);check(flat.centerZ()>-1,"flat viewing distance");
  check(flat.interior().stream().allMatch(c->c.z()>=-2),"flat back side included");
  var u=new ArrayList<>(plane);for(int z=-2;z<=2;z++)for(int y=0;y<3;y++){u.add(face(-3,y,z,1,0,0));u.add(face(3,y,z,-1,0,0));}
  var half=OpenCabin.detect(u);near(half.centerX(),.5);check(half.centerZ()>-2&&half.centerZ()<3,"U viewing center");
  var l=new ArrayList<>(plane);for(int z=-2;z<=2;z++)for(int y=0;y<3;y++)l.add(face(-3,y,z,1,0,0));
  var corner=OpenCabin.detect(l);check(corner.centerX()>-2&&corner.centerZ()>-2,"L center");
  for(int x=-2;x<=2;x++)for(int y=0;y<3;y++)u.add(face(x,y,3,0,0,-1));
  var ring=OpenCabin.detect(u);near(ring.centerX(),.5);near(ring.centerY(),1.5);near(ring.centerZ(),.5);
  check(ring.interior().contains(new Enclosure.Cell(0,6,0)),"open top missing");check(ring.interior().size()<=4096,"unbounded region");
  int dx=24000000,dz=-23000000;
  var moved=u.stream().map(f->face(f.block().x()+dx,f.block().y(),f.block().z()+dz,f.nx(),f.ny(),f.nz())).toList();
  var translated=OpenCabin.detect(moved);near(translated.centerX()-dx,ring.centerX());near(translated.centerZ()-dz,ring.centerZ());
  Collections.reverse(u);var reversed=OpenCabin.detect(u);near(reversed.centerX(),ring.centerX());near(reversed.centerZ(),ring.centerZ());
  System.out.println("PASS: six single faces, flat wall, L/U, open ring, large plot coordinates and order independence");
 }
}
