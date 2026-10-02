package dev.omnidisplay.client;

import dev.omnidisplay.region.DualSceneHistory;
import dev.omnidisplay.region.ViewMatrixHistory;
import dev.omnidisplay.region.ShaderResourceHistory;
import net.minecraft.client.Minecraft;
import org.joml.*;
import org.lwjgl.opengl.GL;
import java.lang.reflect.*;
import java.util.*;

/** Optional Iris history adapter; Photon retains its specialized depth profile. */
public final class IrisPhotonCompat {
 private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("OmniDisplay/Shaders");
 private static final ViewMatrixHistory matrices=new ViewMatrixHistory();
 private static long viewFrame;
 private static int viewWidth,viewHeight;
 private static boolean announced;
 private static final ShaderResourceHistory resources=new ShaderResourceHistory(new ShaderResources());
 private static boolean photonProfile;
 private static String packName="";
 private static final DualSceneHistory history=new DualSceneHistory(new PhotonTextures());
 private static final Map<String,Method> methods=new HashMap<>();
 private static final Map<String,Field> fields=new HashMap<>();
 private static Class<?> iris;
 private static boolean singleScene;
 private static boolean searched,allowed=true,shaderActive,pair,screenPass,hookSeen,broken,reported;
 private static Object pipeline,notifier,cabin;
 private static Method shadow;
 private static final Vector2f jitter=new Vector2f();
 private static float scale=1,combinedA,combinedB;
 private static boolean lod;
 public static boolean photonPackLoaded(){try{return String.valueOf(call(Class.forName("net.irisshaders.iris.Iris"),"getCurrentPackName")).toLowerCase(Locale.ROOT).contains("photon-voxy-support");}catch(ReflectiveOperationException|RuntimeException absent){return false;}}
 public static boolean allowed(){return allowed;}
 public static boolean enabled(){return shaderActive;}
 public static void idle(){singleScene=false;history.close();resources.close();matrices.clear();cabin=null;announced=false;}
 public static void beginSingleScene(){
  if(!singleScene){history.promoteScreen();resources.promoteScreen();}
  idle();singleScene=true;
 }
 /** An invisible display returns to the ordinary view, not the transparent-view history. */
 public static void beginNormalScene(){restoreNormal();idle();singleScene=true;}
 public static boolean viewPair(){return pair&&shaderActive&&!broken;}
 public static boolean screenView(){return screenPass;}
 public static Matrix4fc previousMatrix(Object provider,Matrix4fc current,Matrix4fc seed){return matrices.previous(provider,screenPass,viewFrame,current,seed);}
 private static Method method(Class<?> type,String name,Class<?>... parameters)throws ReflectiveOperationException{
  String key=type.getName()+"#"+name+Arrays.toString(parameters);
  var result=methods.get(key);if(result==null){result=type.getMethod(name,parameters);result.setAccessible(true);methods.put(key,result);}return result;
 }
 private static Object call(Object receiver,String name,Class<?>[] parameters,Object... arguments)throws ReflectiveOperationException{
  Class<?> type=receiver instanceof Class<?> c?c:receiver.getClass();return method(type,name,parameters).invoke(receiver instanceof Class<?>?null:receiver,arguments);
 }
 private static Object call(Object receiver,String name)throws ReflectiveOperationException{return call(receiver,name,new Class<?>[0]);}
 private static Object field(Object receiver,String name)throws ReflectiveOperationException{
  String key=receiver.getClass().getName()+"#"+name;var result=fields.get(key);
  if(result==null){Class<?> type=receiver.getClass();while(type!=null){try{result=type.getDeclaredField(name);break;}catch(NoSuchFieldException ignored){type=type.getSuperclass();}}
   if(result==null)throw new NoSuchFieldException(key);result.setAccessible(true);fields.put(key,result);
  }return result.get(receiver);
 }
 /** Refresh only once at the caller boundary, not during block rendering. */
 public static void refresh(){
  try{
   if(!searched){searched=true;try{iris=Class.forName("net.irisshaders.iris.Iris");shadow=method(Class.forName("net.irisshaders.iris.shadows.ShadowRenderingState"),"areShadowsCurrentlyBeingRendered");}catch(ClassNotFoundException absent){iris=null;}}
   if(iris==null){shaderActive=false;allowed=true;return;}
   Object next=call(call(iris,"getPipelineManager"),"getPipelineNullable");
   if(next!=pipeline){reset();pipeline=next;}
   boolean shaders=next!=null&&next.getClass().getName().equals("net.irisshaders.iris.pipeline.IrisRenderingPipeline");
   if(!shaders){shaderActive=false;allowed=true;return;}
   String name=String.valueOf(call(iris,"getCurrentPackName")),version=String.valueOf(call(iris,"getVersion"));
   packName=name;photonProfile=name.toLowerCase(Locale.ROOT).contains("photon-voxy-support");
   shaderActive=version.startsWith("1.8.14");
   allowed=shaderActive&&!broken;
   if(!allowed&&!reported){reported=true;LOG.warn("Partition rendering uses normal-world fallback for shader pack {} / Iris {}; supported Iris interface is 1.8.14",name,version);}
  }catch(ReflectiveOperationException|RuntimeException error){disable(error);}
 }
 public static boolean shadowPass(){
  if(!shaderActive||shadow==null)return false;
  try{return (boolean)shadow.invoke(null);}catch(ReflectiveOperationException error){return false;}
 }
 public static void beginPair(){
  singleScene=false;
  if(!shaderActive)return;
  try{
   if(!GL.getCapabilities().OpenGL43)throw new IllegalStateException("Shader partition history copies require OpenGL 4.3");
   Object next=DisplayRenderer.settings()==null?null:DisplayRenderer.settings().id;
   var main=Minecraft.getInstance().getMainRenderTarget();
   if(!Objects.equals(cabin,next)||viewWidth!=main.width||viewHeight!=main.height){history.close();resources.close();matrices.clear();cabin=next;viewWidth=main.width;viewHeight=main.height;announced=false;}
   viewFrame++;
   notifier=call(pipeline,"getFrameUpdateNotifier");pair=true;screenPass=false;hookSeen=false;
  }catch(ReflectiveOperationException|RuntimeException error){disable(error);}
 }
 public static boolean skipFrameUpdate(Object current){return pair&&screenPass&&current==notifier;}
 /** Called after target resizing/clearing and immediately before Iris begins shader scene passes. */
 public static void beforeWorld(Object current){
  if(!pair||current!=pipeline||broken)return;
  try{
   if(screenPass){history.beginScreen();resources.beginScreen();}
   else{
    Object targets=field(current,"renderTargets");var settings=(Map<?,?>)field(targets,"targetSettingsMap");
    int count=(int)call(targets,"getRenderTargetCount");var textures=new ArrayList<DualSceneHistory.Texture>();
    for(int index=0;index<count;index++){
     Object target=call(targets,"get",new Class<?>[]{int.class},index),setting=settings.get(index);
     if(target==null||setting==null||(boolean)call(setting,"shouldClear"))continue;
     textures.add(PhotonTextures.describe(index*2,(int)call(target,"getMainTexture")));
     textures.add(PhotonTextures.describe(index*2+1,(int)call(target,"getAltTexture")));
    }
    history.beginNormal(textures);resources.beginNormal(extraResources(current));hookSeen=true;
   }
  }catch(ReflectiveOperationException|RuntimeException error){disable(error);}
 }
 public static boolean finishNormal(){
  if(!shaderActive)return true;
  if(!pair||!hookSeen||broken){if(!broken)disable(new IllegalStateException("Iris pipeline hook did not run"));return false;}
  try{history.captureNormal();resources.captureNormal();readProjection();return true;}catch(ReflectiveOperationException|RuntimeException error){disable(error);return false;}
 }
 public static void beginScreen(){if(pair)screenPass=true;}
 public static void finishPair(){
  try{if(pair&&!broken&&screenPass){try{history.finishScreen();}finally{resources.finishScreen();}if(!announced){announced=true;LOG.info("Independent shader view histories active: pack={}, depthProfile={}, {} matrix providers, {} persistent images/buffers, {}x{} viewport",packName,photonProfile?"Photon/Voxy":"standard Iris",matrices.providers(),resources.resources(),viewWidth,viewHeight);}}else restoreNormal();}
  catch(RuntimeException error){disable(error);}finally{pair=false;screenPass=false;}
 }
 public static void abortPair(){try{restoreNormal();}finally{pair=false;screenPass=false;}}
 public static void reset(){singleScene=false;history.close();resources.close();matrices.clear();viewWidth=viewHeight=0;announced=false;pair=false;screenPass=false;hookSeen=false;broken=false;reported=false;allowed=true;cabin=null;notifier=null;}
 public static void destroyed(Object current){if(current==pipeline){reset();pipeline=null;shaderActive=false;}}
 private static void disable(Throwable error){
  broken=true;allowed=false;LOG.error("Shader partition adapter failed; retaining normal world rendering",error);
  if(!reported){reported=true;var player=Minecraft.getInstance().player;if(player!=null)player.displayClientMessage(net.minecraft.network.chat.Component.literal("光影分区适配失败，已保留正常画面；请提供 latest.log。"),false);}
 }
 private static void restoreNormal(){try{history.restoreNormal();}finally{resources.restoreNormal();}}
 private static List<ShaderResourceHistory.Resource> extraResources(Object current)throws ReflectiveOperationException{
  var result=new ArrayList<ShaderResourceHistory.Resource>();int slot=0;
  var images=new ArrayList<>((Set<?>)field(current,"customImages"));
  images.sort(Comparator.comparing(image->{try{return String.valueOf(call(image,"getName"));}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}));
  for(var image:images){if((boolean)call(image,"shouldClear"))continue;
   result.add(ShaderResources.image(slot++,(int)call(image,"getId"),(int)call(call(image,"getTarget"),"getGlType")));
  }
  Object holder=field(current,"shaderStorageBufferHolder");
  if(holder!=null)for(Object buffer:(Object[])field(holder,"buffers")){if(buffer==null)continue;
   long size=((Number)call(buffer,"getSize")).longValue();if(size<=0)continue;
   result.add(new ShaderResourceHistory.Resource(slot++,(int)call(buffer,"getId"),0,0,0,0,0,size));
  }
  return result;
 }
 public static void singleDepthProfile()throws ReflectiveOperationException{if(shaderActive)readProjection();}
 private static void readProjection()throws ReflectiveOperationException{
  if(!photonProfile){scale=1;jitter.zero();lod=false;return;}
  Object pack=((Optional<?>)call(iris,"getCurrentPack")).orElseThrow();Object values=call(call(pack,"getShaderPackOptions"),"getOptionValues");
  boolean taa=(boolean)call(values,"getBooleanValueOrDefault",new Class<?>[]{String.class},"TAA");
  boolean taau=taa&&(boolean)call(values,"getBooleanValueOrDefault",new Class<?>[]{String.class},"TAAU");
  scale=taau?Float.parseFloat(String.valueOf(call(values,"getStringValueOrDefault",new Class<?>[]{String.class},"TAAU_RENDER_SCALE"))):1;
  jitter.zero();
  if(taa){Object variable=call(call(pipeline,"getCustomUniforms"),"getVariable",new Class<?>[]{String.class},"taa_offset");var offset=(Vector2fc)field(variable,"cached");jitter.set(offset).mul(taau?1:.66f);}
  lod=false;
  try{
   Object data=call(pipeline,"voxy$getPipelineData");
   if(data!=null&&field(data,"thePipeline")!=null){
    Class<?> config=Class.forName("me.cortex.voxy.client.config.VoxyConfig");Object current=config.getField("CONFIG").get(null);
    float far=16f*java.lang.Math.round(((Number)field(current,"sectionRenderDistance")).floatValue()*32f);
    Object captured=Class.forName("net.irisshaders.iris.uniforms.CapturedRenderingState").getField("INSTANCE").get(null);
    Matrix4fc projection=(Matrix4fc)call(captured,"getGbufferProjection");float near=projection.m32()/(projection.m22()-1);
    combinedA=(far+near)/(near-far);combinedB=2*far*near/(near-far);lod=true;
   }
  }catch(ClassNotFoundException|NoSuchMethodException absent){/* Voxy is optional. */}
 }
 public static int sourceDepth()throws ReflectiveOperationException{return (int)call(field(pipeline,"renderTargets"),"getDepthTexture");}
 public static int combinedDepth()throws ReflectiveOperationException{
  if(!lod)return sourceDepth();Object target=call(field(pipeline,"renderTargets"),"get",new Class<?>[]{int.class},15);
  if(target==null)throw new IllegalStateException("Photon Voxy combined depth buffer is missing");return (int)call(target,"getMainTexture");
 }
 public static Vector2fc jitter(){return jitter;}
 public static float scale(){return scale;}
 public static boolean hasLod(){return lod;}
 public static float combinedA(){return combinedA;}
 public static float combinedB(){return combinedB;}
}
