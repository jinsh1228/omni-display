package dev.omnidisplay.client;
import dev.omnidisplay.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Native-size text; deliberately no vanilla blur/background pass after rendering controls. */
public final class CabinScreen extends Screen {
 private final BlockPos anchor;private CabinSettings settings;private int members,left,top,page;private long revision;
 private final java.util.List<SettingSlider> sliders=new java.util.ArrayList<>();private Button power,wireless,mouseToggle,mouseRadio;
 private final java.util.List<Button> hudButtons=new java.util.ArrayList<>();private static final String[] HUD_NAMES={"航向刻度","俯仰刻度","地平线","速度 / m/s","高度 / 世界 Y","固定十字线"};
 public CabinScreen(CabinPackets.Data data){super(Component.literal("全周天座舱配置"));anchor=data.pos();update(data);}
 public static void receive(CabinPackets.Data data){
  var mc=Minecraft.getInstance();if(mc.level==null)return;
  if(data.tag().getBoolean("radarStatus")){RadarOverlay.receive(data);return;}
  if(data.tag().getBoolean("emergency")){DisplayRenderer.fault(data);return;}
  if(data.tag().getBoolean("tool")){if(data.open())mc.setScreen(new HighlightScreen(data));else if(mc.screen instanceof HighlightScreen h)h.update(data);return;}
  if(data.open())mc.setScreen(new CabinScreen(data));
  else if(mc.screen instanceof FrequencyPickerScreen f)f.parent.update(data);
  else if(mc.screen instanceof CabinScreen s&&s.anchor.equals(data.pos()))s.update(data);
 }
 private void update(CabinPackets.Data d){settings=CabinSettings.load(d.tag(),Minecraft.getInstance().level.registryAccess());members=d.tag().getInt("members");revision=settings.revision;}
 private void send(int op,int value){PacketDistributor.sendToServer(new CabinPackets.Action(anchor,op,value));}
 @Override protected void init(){
  sliders.clear();hudButtons.clear();power=wireless=mouseToggle=mouseRadio=null;left=(width-310)/2;top=Math.max(0,(height-226)/2);
  String[] tabs={"座舱","仪表","雷达"};
  for(int i=0;i<3;i++){final int n=i;addRenderableWidget(new PanelButton(left+10+i*98,top+32,94,18,tabs[i],()->{page=n;rebuildWidgets();},()->page==n));}
  if(page==0){
   power=addRenderableWidget(new PanelButton(left+205,top+63,90,20,"",()->send(1,0),()->settings.power));
   addSlider(3,"载具不透明度",0,100,left+15,top+96,280);
   addSlider(12,"高光不透明度",0,100,left+15,top+124,280);
   addSlider(18,"接缝不透明度",0,100,left+15,top+152,280);
   wireless=addRenderableWidget(new PanelButton(left+151,top+181,86,20,"",()->send(6,settings.wireless?0:1),()->settings.wireless));frequency(0,243,180);frequency(1,272,180);
  }else if(page==1){
   addSlider(13,"HUD 球大小",50,2000,left+15,top+65,280);
   addSlider(27,"HUD 亮度",0,300,left+15,top+92,280);
   for(int i=0;i<6;i++){final int bit=i;var button=new PanelButton(left+15+(i%2)*143,top+122+(i/2)*25,137,22,HUD_NAMES[i],()->send(19,bit),()->(settings.hudMask&(1<<bit))!=0);hudButtons.add(button);addRenderableWidget(button);}
  }else{
   mouseToggle=addRenderableWidget(new PanelButton(left+15,top+91,280,22,"",()->send(23,0),()->settings.mouseLock));
   mouseRadio=addRenderableWidget(new PanelButton(left+151,top+124,86,20,"",()->send(24,settings.mouseWireless?0:1),()->settings.mouseWireless));frequency(2,243,123);frequency(3,272,123);
  }
 }
 private void frequency(int side,int x,int y){addRenderableWidget(new FrequencySlot(left+x,top+y,side));}
 private final class FrequencySlot extends Button {
  final int side;
  FrequencySlot(int x,int y,int side){super(x,y,23,23,Component.literal(side%2==0?"A":"B"),b->minecraft.setScreen(new FrequencyPickerScreen(CabinScreen.this,anchor,side)),DEFAULT_NARRATION);this.side=side;}
  @Override public void renderWidget(GuiGraphics g,int mx,int my,float dt){
   int color=side%2==0?0xFFE76D7F:0xFF66B7FF;int x=getX(),y=getY();g.fill(x,y,x+23,y+23,isHoveredOrFocused()?0xFF273E50:0xFF101D2C);g.renderOutline(x,y,23,23,color);
   ItemStack item=switch(side){case 0->settings.first;case 1->settings.second;case 2->settings.mouseFirst;default->settings.mouseSecond;};
   if(item.isEmpty())g.drawCenteredString(font,side%2==0?"A":"B",x+11,y+7,color);else g.renderItem(item,x+3,y+3);
  }
 }
 static class PanelButton extends Button {
  final java.util.function.BooleanSupplier selected;
  PanelButton(int x,int y,int w,int h,String text,Runnable action,java.util.function.BooleanSupplier selected){super(x,y,w,h,Component.literal(text),b->action.run(),DEFAULT_NARRATION);this.selected=selected;}
  @Override public void renderWidget(GuiGraphics g,int mx,int my,float dt){
   int x=getX(),y=getY();boolean on=selected.getAsBoolean();g.fill(x,y,x+width,y+height,isHoveredOrFocused()?0xFF263B50:on?0xFF15353C:0xFF121F30);g.fill(x,y,x+2,y+height,on?0xFF7FE7DB:0xFF45546C);g.fill(x+2,y+height-1,x+width,y+height,0xFF34465A);g.drawCenteredString(Minecraft.getInstance().font,getMessage(),x+width/2,y+(height-8)/2,on?0xD3FFF5:0xAFBDCF);
  }
 }
 private int current(int op){return switch(op){case 3->settings.opacity;case 12->settings.highlightOpacity;case 13->settings.hudScale;case 18->settings.seamOpacity;case 27->settings.hudBrightness;default->0;};}
 private void addSlider(int op,String label,int min,int max,int x,int y,int w){var slider=new SettingSlider(op,label,min,max,x,y,w);sliders.add(slider);addRenderableWidget(slider);}
 @Override public void tick(){
  if(minecraft.level==null||minecraft.player==null){onClose();return;}
  CabinEntity be=minecraft.level.getBlockEntity(anchor) instanceof CabinEntity actual?actual:PanelStore.hostAt(minecraft.level,anchor);
  if(be!=null&&be.settings.revision>revision){settings=be.settings.copy();revision=settings.revision;}
  for(var s:sliders)if(!s.dragging)s.sync(current(s.op));
 }
 @Override public void render(GuiGraphics g,int mx,int my,float partial){
  g.fill(0,0,width,height,0xA0080E17);g.fill(left,top,left+310,top+226,0xFA0B1422);g.renderOutline(left,top,310,226,0xFF354760);
  g.fill(left,top,left+3,top+25,0xFF79E9DE);g.drawString(font,"OMNI / 全周天座舱",left+12,top+11,0xD8EEF6,false);g.drawString(font,"SYS.25",left+260,top+12,0x8997BB,false);
  g.fill(left+10,top+55,left+300,top+56,0xFF293A50);
  if(page==0){
   g.drawString(font,"显示系统",left+15,top+69,0xC5D3E2,false);power.setMessage(Component.literal(settings.power?"运行中":"待机"));
   wireless.setMessage(Component.literal(settings.wireless?"脉冲 · 开":"脉冲 · 关"));g.drawString(font,"无线电源",left+15,top+187,0x94A7BD,false);
  }else if(page==1){g.drawString(font,"多色仪表 / 按功能分色",left+15,top+190,0x94A7BD,false);}
  else{
   boolean linked=settings.radarLink!=null;g.fill(left+15,top+69,left+20,top+74,linked?0xFF7FE7DB:0xFFFFBC70);g.drawString(font,linked?"数据链接器已绑定":"未绑定数据链接器",left+28,top+68,0xC5D3E2,false);
   mouseToggle.setMessage(Component.literal("鼠标锁定目标    "+(settings.mouseLock?"开启":"关闭")));mouseRadio.setMessage(Component.literal(settings.mouseWireless?"脉冲 · 开":"脉冲 · 关"));g.drawString(font,"无线切换",left+15,top+130,0x94A7BD,false);
   g.drawString(font,"空手右键显示器或舱门，选择已探测目标",left+15,top+166,0x9CACBE,false);g.drawString(font,"拆除数据链接器即可解除绑定",left+15,top+185,0x9CACBE,false);
  }
  g.fill(left+10,top+207,left+300,top+208,0xFF293A50);g.drawString(font,"即时保存",left+15,top+215,0x698396,false);g.drawString(font,"ESC  返回",left+243,top+215,0x8CA3B5,false);
  for(var w:renderables)w.render(g,mx,my,partial);
 }
 @Override public boolean isPauseScreen(){return false;}
 private final class SettingSlider extends AbstractSliderButton{
  final int op,min,max;final String label;boolean dragging;
  SettingSlider(int op,String label,int min,int max,int x,int y,int w){super(x,y,w,20,Component.empty(),(current(op)-min)/(double)(max-min));this.op=op;this.label=label;this.min=min;this.max=max;updateMessage();}
  int number(){return min+(int)Math.round(value*(max-min));}
  void sync(int n){value=(n-min)/(double)(max-min);updateMessage();}
  protected void updateMessage(){setMessage(Component.literal(label+"："+number()+(op<=13||op==18||op==27?"%":"")));}
  @Override public void renderWidget(GuiGraphics g,int mx,int my,float dt){
   int x=getX(),y=getY();g.fill(x,y,x+width,y+height,0xFF101F30);g.drawString(font,getMessage(),x+6,y+3,0xC6DCE8,false);
   g.fill(x+6,y+16,x+width-6,y+18,0xFF293C52);int end=x+6+(int)((width-12)*value);g.fill(x+6,y+16,end,y+18,0xFF76DDD4);g.fill(end-1,y+14,end+2,y+20,0xFFD5FFF4);
  }
  protected void applyValue(){if(!dragging)send(op,number());}
  @Override public void onClick(double x,double y){dragging=true;super.onClick(x,y);}
  @Override public void onRelease(double x,double y){super.onRelease(x,y);dragging=false;send(op,number());}
 }
}
