package dev.omnidisplay.client;
import dev.omnidisplay.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;
import org.lwjgl.glfw.GLFW;
public final class ModSettingsScreen extends Screen {
 private final Screen parent;private Button toggle,less,more;private Boolean pending;private StrengthSlider strength;
 private final Map<String,Integer> requests=new HashMap<>();
 private final Map<String,Button> effects=new LinkedHashMap<>();
 private int top,age;private final Map<String,Integer> ages=new HashMap<>();
 public ModSettingsScreen(Screen parent){super(Component.literal("Omni Display 设置"));this.parent=parent;}
 private boolean editable(){return minecraft.level!=null&&ServerRenderState.ready&&ServerRenderState.editable;}
 private int current(String key){return requests.getOrDefault(key,key.equals("strength")?Math.round(ServerRenderState.filter.getFloat(key)*100):ServerRenderState.filter.getBoolean(key)?1:0);}
 private void send(String key,int value){if(!editable())return;requests.put(key,value);ages.put(key,age);PacketDistributor.sendToServer(new RenderPackets.FilterRequest(key,value));}
 @Override protected void init(){
  top=Math.max(0,(height-246)/2);int left=width/2-150;
  toggle=addRenderableWidget(Button.builder(Component.empty(),b->{pending=!ServerRenderState.experimental;PacketDistributor.sendToServer(new RenderPackets.Request(pending));b.active=false;}).bounds(left,top+24,300,22).build());
  less=addRenderableWidget(Button.builder(Component.literal("◀"),b->strength.adjust(-1)).bounds(left,top+76,22,20).build());
  strength=addRenderableWidget(new StrengthSlider(left+26,top+76,248));
  more=addRenderableWidget(Button.builder(Component.literal("▶"),b->strength.adjust(1)).bounds(left+278,top+76,22,20).build());
  effects.clear();String[] keys={"tint","pixels","scanlines","edges"};
  for(int i=0;i<keys.length;i++){String key=keys[i];effects.put(key,addRenderableWidget(Button.builder(Component.empty(),b->send(key,1-current(key))).bounds(left+(i%2)*154,top+124+(i/2)*24,146,20).build()));}
  addRenderableWidget(Button.builder(Component.literal("完成"),b->onClose()).bounds(width/2-60,top+226,120,20).build());
 }
 @Override public void tick(){
  age++;requests.entrySet().removeIf(e->age-ages.getOrDefault(e.getKey(),age)>100||(e.getKey().equals("strength")?Math.round(ServerRenderState.filter.getFloat("strength")*100)==e.getValue():(ServerRenderState.filter.getBoolean(e.getKey())?1:0)==e.getValue()));
  if(strength!=null&&!strength.dirty&&!strength.isFocused())strength.sync(current("strength"));
 }
 @Override public boolean isPauseScreen(){return false;}
 @Override public void render(GuiGraphics g,int x,int y,float dt){
  renderBackground(g,x,y,dt);
  if(pending!=null&&ServerRenderState.experimental==pending)pending=null;
  toggle.setMessage(Component.literal("低性能模式："+(ServerRenderState.ready?(ServerRenderState.experimental?"关闭":"开启"):"未连接服务器")));toggle.active=editable()&&pending==null;
  strength.active=less.active=more.active=editable();
  effects.forEach((key,button)->{String label=switch(key){case "tint"->"冷色调";case "pixels"->"像素纹理";case "scanlines"->"扫描线";default->"屏幕边缘";};button.setMessage(Component.literal(label+"："+(current(key)!=0?"开启":"关闭")));button.active=editable();});
  super.render(g,x,y,dt);g.drawCenteredString(font,title,width/2,top+3,0xFFFFFF);
  g.drawCenteredString(font,"开启：整舱透视、门互锁，不支持开放座舱",width/2,top+53,0xBBBBBB);
  g.drawCenteredString(font,"0% 关闭滤镜；左右方向键微调 1%",width/2,top+103,0x99BBDD);
  g.drawCenteredString(font,ServerRenderState.experimental?"滤镜仅影响显示器画面":"低性能模式下滤镜和开放座舱不生效",width/2,top+184,0xE5C47B);
  g.drawCenteredString(font,minecraft.level==null?"进入世界后设置":ServerRenderState.editable?"服务器统一保存并同步设置":"仅服务器管理员可修改",width/2,top+205,0x99BBDD);
 }
 private final class StrengthSlider extends AbstractSliderButton {
  boolean dirty;
  StrengthSlider(int x,int y,int width){super(x,y,width,20,Component.empty(),current("strength")/100d);updateMessage();}
  private int percent(){return (int)Math.round(value*100);}
  void sync(int percent){value=Math.clamp(percent,0,100)/100d;updateMessage();}
  void adjust(int delta){if(!active)return;sync(percent()+delta);dirty=false;send("strength",percent());}
  @Override protected void updateMessage(){setMessage(Component.literal("滤镜强度："+percent()+"%"));}
  @Override protected void applyValue(){dirty=true;updateMessage();}
  private void commit(){if(dirty){dirty=false;send("strength",percent());}}
  @Override public void onRelease(double x,double y){super.onRelease(x,y);commit();}
  @Override public boolean keyPressed(int key,int scan,int modifiers){if(!active)return false;if(key==GLFW.GLFW_KEY_LEFT||key==GLFW.GLFW_KEY_RIGHT){adjust(key==GLFW.GLFW_KEY_LEFT?-1:1);return true;}return super.keyPressed(key,scan,modifiers);}
 }
 @Override public void onClose(){if(strength!=null)strength.commit();minecraft.setScreen(parent);}
 @net.neoforged.fml.common.EventBusSubscriber(modid=OmniDisplay.ID,value=net.neoforged.api.distmarker.Dist.CLIENT,bus=net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)
 public static final class Registration {
  @net.neoforged.bus.api.SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e){net.neoforged.fml.ModList.get().getModContainerById(OmniDisplay.ID).orElseThrow().registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,(container,parent)->new ModSettingsScreen(parent));}
 }
}
