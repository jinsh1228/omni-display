import dev.omnidisplay.region.ViewMatrixHistory;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class ViewMatrixHistoryTest {
 private static void expect(Matrix4fc matrix,float value){if(Math.abs(matrix.m30()-value)>1e-5)throw new AssertionError("Wrong previous view: "+matrix.m30()+" expected "+value);}
 public static void main(String[] args){
  var history=new ViewMatrixHistory();Object provider=new Object();var seed=new Matrix4f().translation(-1,0,0);
  for(int frame=0;frame<120;frame++){
   var current=new Matrix4f().translation(frame,0,0);
   var previous=history.previous(provider,false,frame,current,seed);expect(previous,frame==0?-1:frame-1);
   // Simulate CustomUniforms.update() and multiple GPU programs asking in the same frame.
   for(int read=0;read<8;read++)expect(history.previous(provider,false,frame,new Matrix4f().translation(999,0,0),current),frame==0?-1:frame-1);
   var screen=new Matrix4f().translation(1000+frame,0,0);
   expect(history.previous(provider,true,frame,screen,current),frame==0?-1:999+frame);
   for(int read=0;read<8;read++)expect(history.previous(provider,true,frame,new Matrix4f().translation(-999,0,0),current),frame==0?-1:999+frame);
   ((Matrix4f)previous).translation(999,0,0);
   expect(history.previous(provider,false,frame,current,current),frame==0?-1:frame-1);
  }
  // Separate suppliers (e.g. projection/model-view and Iris/Voxy) must never share histories.
  expect(history.previous(new Object(),false,120,new Matrix4f(),seed),-1);
  history.clear();if(history.providers()!=0)throw new AssertionError("Reload leaks supplier histories");
  expect(history.previous(provider,false,121,new Matrix4f().translation(4,0,0),seed),-1);
  expect(history.previous(provider,true,121,new Matrix4f().translation(9,0,0),new Matrix4f().translation(4,0,0)),-1);
  System.out.println("PASS: 120 independent normal/screen matrix frames, repeated reads, mutable return isolation, Iris/Voxy supplier identity and reset");
 }
}
