package dev.omnidisplay.region;
import java.util.*;

/** Persistent image/SSBO state; descriptors include identity, type, size and format. */
public final class ShaderResourceHistory implements AutoCloseable {
 public record Resource(int slot,int id,int target,int width,int height,int depth,int format,long bytes){}
 public interface Backend {
  int create(Resource resource);
  void copy(Resource resource,int from,int to);
  void delete(Resource resource,int id);
 }
 private final Backend backend;
 private List<Resource> layout=List.of();
 private final Map<Integer,Integer> normal=new HashMap<>(),screen=new HashMap<>();
 private boolean captured;
 public ShaderResourceHistory(Backend backend){this.backend=backend;}
 public void beginNormal(List<Resource> resources){
  captured=false;
  if(layout.equals(resources))return;
  close();layout=List.copyOf(resources);
  try{for(var r:layout){
   normal.put(r.slot(),backend.create(r));
   int saved=backend.create(r);screen.put(r.slot(),saved);backend.copy(r,r.id(),saved);
  }}catch(RuntimeException error){close();throw error;}
 }
 public void captureNormal(){save(normal);captured=true;}
 public void beginScreen(){if(!captured)throw new IllegalStateException("Missing normal resource snapshot");restore(screen);}
 public void finishScreen(){
  if(!captured)throw new IllegalStateException("Missing normal resource snapshot");
  try{save(screen);}finally{restoreNormal();}
 }
 public void restoreNormal(){if(captured){restore(normal);captured=false;}}
 public void promoteScreen(){restore(screen);captured=false;}
 private void save(Map<Integer,Integer> bank){for(var r:layout)backend.copy(r,r.id(),bank.get(r.slot()));}
 private void restore(Map<Integer,Integer> bank){for(var r:layout)backend.copy(r,bank.get(r.slot()),r.id());}
 public int resources(){return layout.size();}
 @Override public void close(){
  for(var r:layout){var n=normal.get(r.slot());var s=screen.get(r.slot());if(n!=null)backend.delete(r,n);if(s!=null)backend.delete(r,s);}
  layout=List.of();normal.clear();screen.clear();captured=false;
 }
}
