package dev.omnidisplay.region;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import java.util.IdentityHashMap;

/** Read-side matrix suppliers must advance once per view/frame, not once per shader query. */
public final class ViewMatrixHistory {
 private static final class State {
  long frame=Long.MIN_VALUE;
  Matrix4f current,previous;
 }
 private final IdentityHashMap<Object,State> normal=new IdentityHashMap<>(),screen=new IdentityHashMap<>();
 public Matrix4fc previous(Object provider,boolean screenView,long frame,Matrix4fc current,Matrix4fc seed){
  var bank=screenView?screen:normal;
  var state=bank.computeIfAbsent(provider,key->new State());
  if(state.frame!=frame){
   Matrix4fc initial=seed;
   if(screenView&&state.current==null){var other=normal.get(provider);if(other!=null&&other.frame==frame)initial=other.previous;}
   state.previous=new Matrix4f(state.current==null?initial:state.current);
   state.current=new Matrix4f(current);state.frame=frame;
  }
  // Never expose stored mutable matrices to Iris uniform upload code.
  return new Matrix4f(state.previous);
 }
 public int providers(){return normal.size()+screen.size();}
 public void clear(){normal.clear();screen.clear();}
}
