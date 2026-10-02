package dev.omnidisplay.region;

import java.util.*;

/** Keeps two scene histories without changing the shader pack's texture IDs or framebuffers. */
public final class DualSceneHistory implements AutoCloseable {
 public record Texture(int slot,int id,int width,int height,int format){}
 public interface Backend {
  int create(Texture texture);
  void copy(int source,int destination,int width,int height);
  void delete(int texture);
 }
 private final Backend backend;
 private List<Texture> layout=List.of();
 private final Map<Integer,Integer> normal=new HashMap<>(),screen=new HashMap<>();
 private boolean captured;
 public DualSceneHistory(Backend backend){this.backend=backend;}
 public void beginNormal(List<Texture> textures){
  captured=false;
  if(layout.equals(textures))return;
  close();layout=List.copyOf(textures);
  try{for(var texture:layout){
   normal.put(texture.slot(),backend.create(texture));
   int saved=backend.create(texture);screen.put(texture.slot(),saved);
   backend.copy(texture.id(),saved,texture.width(),texture.height());
  }}catch(RuntimeException error){close();throw error;}
 }
 public void captureNormal(){save(normal);captured=true;}
 public void beginScreen(){if(!captured)throw new IllegalStateException("Normal scene must finish first");restore(screen);}
 public void finishScreen(){
  if(!captured)throw new IllegalStateException("No completed normal scene");
  try{save(screen);}finally{restore(normal);captured=false;}
 }
 public void restoreNormal(){if(captured){restore(normal);captured=false;}}
 public void promoteScreen(){restore(screen);captured=false;}
 private void save(Map<Integer,Integer> bank){for(var texture:layout)backend.copy(texture.id(),bank.get(texture.slot()),texture.width(),texture.height());}
 private void restore(Map<Integer,Integer> bank){for(var texture:layout)backend.copy(bank.get(texture.slot()),texture.id(),texture.width(),texture.height());}
 @Override public void close(){
  for(int id:normal.values())backend.delete(id);for(int id:screen.values())backend.delete(id);
  normal.clear();screen.clear();layout=List.of();captured=false;
 }
}
