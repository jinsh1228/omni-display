import dev.omnidisplay.region.ShaderResourceHistory;
import java.util.*;
public class ShaderResourceHistoryTest {
 static class GPU implements ShaderResourceHistory.Backend {
  final Map<Integer,Integer> values=new HashMap<>();int next=100;
  public int create(ShaderResourceHistory.Resource r){int id=next++;values.put(id,0);return id;}
  public void copy(ShaderResourceHistory.Resource r,int from,int to){values.put(to,Objects.requireNonNull(values.get(from)));}
  public void delete(ShaderResourceHistory.Resource r,int id){values.remove(id);}
 }
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args){
  var gpu=new GPU();gpu.values.put(1,10);gpu.values.put(2,20);
  var image=new ShaderResourceHistory.Resource(0,1,32879,8,4,8,34842,0);
  var ssbo=new ShaderResourceHistory.Resource(1,2,0,0,0,0,0,256);
  var layout=List.of(image,ssbo);var history=new ShaderResourceHistory(gpu);
  int priorImage=10,priorBuffer=20;
  for(int f=0;f<120;f++){
   history.beginNormal(layout);gpu.values.put(1,1000+f);gpu.values.put(2,2000+f);history.captureNormal();
   history.beginScreen();check(gpu.values.get(1)==priorImage&&gpu.values.get(2)==priorBuffer);
   priorImage=3000+f;priorBuffer=4000+f;gpu.values.put(1,priorImage);gpu.values.put(2,priorBuffer);
   history.finishScreen();check(gpu.values.get(1)==1000+f&&gpu.values.get(2)==2000+f);
  }
  history.beginNormal(layout);history.captureNormal();history.beginScreen();gpu.values.put(1,-1);gpu.values.put(2,-1);history.restoreNormal();
  check(gpu.values.get(1)==1119&&gpu.values.get(2)==2119);
  // A same-ID relative image resize and SSBO size change must invalidate both banks.
  layout=List.of(new ShaderResourceHistory.Resource(0,1,32879,16,8,16,34842,0),new ShaderResourceHistory.Resource(1,2,0,0,0,0,0,512));
  history.beginNormal(layout);check(gpu.values.size()==6);history.captureNormal();history.beginScreen();check(gpu.values.get(1)==1119);history.finishScreen();
  // Reload can replace resources with new IDs even when their shape is unchanged.
  gpu.values.put(3,99);history.beginNormal(List.of(new ShaderResourceHistory.Resource(0,3,32879,16,8,16,34842,0)));check(gpu.values.size()==5);
  history.captureNormal();history.beginScreen();check(gpu.values.get(3)==99);history.finishScreen();history.close();check(gpu.values.size()==3);
  // Partial allocation failure releases earlier snapshots, without deleting pack-owned IDs.
  var failing=new ShaderResourceHistory(new ShaderResourceHistory.Backend(){int made;
   public int create(ShaderResourceHistory.Resource r){if(++made==3)throw new IllegalStateException("OOM");return gpu.create(r);}
   public void copy(ShaderResourceHistory.Resource r,int a,int b){gpu.copy(r,a,b);}
   public void delete(ShaderResourceHistory.Resource r,int id){gpu.delete(r,id);}
  });
  try{failing.beginNormal(layout);throw new AssertionError();}catch(IllegalStateException expected){}
  check(gpu.values.size()==3);check(failing.resources()==0);
  System.out.println("PASS: 120 independent image/SSBO histories; abort restore; relative resize; resource replacement; allocation failure cleanup");
 }
}
