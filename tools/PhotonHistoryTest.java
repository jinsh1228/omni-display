import dev.omnidisplay.region.DualSceneHistory;
import java.util.*;

public final class PhotonHistoryTest {
 static final class Memory implements DualSceneHistory.Backend {
  final Map<Integer,Integer> values=new HashMap<>();int next=100,deleted;
  public int create(DualSceneHistory.Texture texture){values.put(++next,0);return next;}
  public void copy(int source,int destination,int width,int height){values.put(destination,Objects.requireNonNull(values.get(source)));}
  public void delete(int texture){values.remove(texture);deleted++;}
 }
 public static void main(String[] args){
  var gpu=new Memory();gpu.values.put(1,7);gpu.values.put(2,9);
  var history=new DualSceneHistory(gpu);
  var layout=List.of(new DualSceneHistory.Texture(10,1,1920,1080,1),new DualSceneHistory.Texture(11,2,960,540,2));
  for(int frame=0;frame<120;frame++){
   history.beginNormal(layout);
   if(gpu.values.get(1)!=(frame==0?7:1000+frame-1))throw new AssertionError("Normal history contaminated by screen scene");
   gpu.values.put(1,1000+frame);gpu.values.put(2,2000+frame);history.captureNormal();
   history.beginScreen();
   if(gpu.values.get(1)!=(frame==0?7:3000+frame-1))throw new AssertionError("Screen TAA reused the normal scene");
   gpu.values.put(1,3000+frame);gpu.values.put(2,4000+frame);history.finishScreen();
   if(gpu.values.get(1)!=1000+frame||gpu.values.get(2)!=2000+frame)throw new AssertionError("Normal history not restored after screen scene");
  }
  // Resize destroys both banks and seeds the new screen history from the current normal scene.
  gpu.values.put(1,77);gpu.values.put(2,88);
  history.beginNormal(List.of(new DualSceneHistory.Texture(10,1,1280,720,1),new DualSceneHistory.Texture(11,2,640,360,2)));
  if(gpu.deleted!=4)throw new AssertionError("Old resolution leaks GPU textures");
  gpu.values.put(1,99);history.captureNormal();history.beginScreen();
  if(gpu.values.get(1)!=77)throw new AssertionError("Resize retained stale history");
  gpu.values.put(1,-1);history.restoreNormal();if(gpu.values.get(1)!=99)throw new AssertionError("Failed screen render damaged normal history");
  try{history.beginScreen();throw new AssertionError("Reentrant/incomplete sequence accepted");}catch(IllegalStateException expected){}
  history.close();if(gpu.values.size()!=2||gpu.deleted!=8)throw new AssertionError("History buffers leaked on shader reload");
  System.out.println("PASS: 120 isolated normal/screen history frames; two texture formats/sizes; resize, aborted pass, ordering and cleanup");
 }
}
