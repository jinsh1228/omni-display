import dev.omnidisplay.region.AdaptiveRenderRules;
public class AdaptiveRenderTest {
 static void check(boolean value){if(!value)throw new AssertionError();}
 public static void main(String[] args){
  check(AdaptiveRenderRules.singleScene(false,true));
  check(!AdaptiveRenderRules.singleScene(false,false));
  check(!AdaptiveRenderRules.singleScene(true,true));
  check(!AdaptiveRenderRules.singleScene(true,false));
  check(AdaptiveRenderRules.doorClosed(false,0));
  for(float animation:new float[]{.0001f,.1f,.5f,1,Float.NaN,Float.POSITIVE_INFINITY})check(!AdaptiveRenderRules.doorClosed(false,animation));
  for(float animation:new float[]{0,.1f,.5f,1})check(!AdaptiveRenderRules.doorClosed(true,animation));
  var gpu=new PhotonHistoryTest.Memory();gpu.values.put(1,5);
  var textures=new dev.omnidisplay.region.DualSceneHistory(gpu);
  textures.beginNormal(java.util.List.of(new dev.omnidisplay.region.DualSceneHistory.Texture(0,1,8,8,1)));
  gpu.values.put(1,10);textures.captureNormal();textures.beginScreen();gpu.values.put(1,20);textures.finishScreen();
  check(gpu.values.get(1)==10);textures.promoteScreen();check(gpu.values.get(1)==20);textures.close();check(gpu.values.get(1)==20&&gpu.values.size()==1);
  var memory=new ShaderResourceHistoryTest.GPU();memory.values.put(1,5);
  var resources=new dev.omnidisplay.region.ShaderResourceHistory(memory);
  resources.beginNormal(java.util.List.of(new dev.omnidisplay.region.ShaderResourceHistory.Resource(0,1,0,0,0,0,0,64)));
  memory.values.put(1,10);resources.captureNormal();resources.beginScreen();memory.values.put(1,20);resources.finishScreen();
  resources.promoteScreen();check(memory.values.get(1)==20);resources.close();check(memory.values.size()==1);
  System.out.println("PASS: enclosed inside single scene; open/outside dual scene; opening/closing animation and invalid animation safeguards");
 }
}
