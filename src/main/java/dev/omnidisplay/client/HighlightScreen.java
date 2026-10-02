package dev.omnidisplay.client;
import dev.omnidisplay.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
public final class HighlightScreen extends Screen {
 private final BlockPos anchor;private CabinSettings settings;private int mode;private int opacity,selected=-1,page,left,top;
 HighlightScreen(CabinPackets.Data d){super(Component.literal("载具高光显示工具"));anchor=d.pos();update(d);}
 void update(CabinPackets.Data d){if(!anchor.equals(d.pos()))return;settings=CabinSettings.load(d.tag(),Minecraft.getInstance().level.registryAccess());mode=d.tag().contains("mode")?Math.clamp(d.tag().getInt("mode"),0,2):(d.tag().getBoolean("range")?1:0);opacity=d.tag().getInt("toolOpacity");if(selected>=settings.highlights.size())selected=-1;page=Math.min(page,Math.max(0,(settings.highlights.size()-1)/5));if(minecraft!=null)rebuildWidgets();}
 private void send(int op,int value){PacketDistributor.sendToServer(new CabinPackets.Action(anchor,op,value));}
 protected void init(){left=(width-320)/2;top=(height-238)/2;
  addRenderableWidget(Button.builder(Component.literal("模式："+new String[]{"单个方块","范围选择","物理结构"}[mode]),b->send(9,(mode+1)%3)).bounds(left+10,top+30,145,20).build());
  addRenderableWidget(Button.builder(Component.literal("新选区默认值"),b->{selected=-1;rebuildWidgets();}).bounds(left+165,top+30,145,20).build());
  int alpha=selected<0?opacity:settings.highlights.get(selected).opacity();addRenderableWidget(new Alpha(left+10,top+57,alpha));
  for(int row=0;row<5;row++){int i=page*5+row;if(i>=settings.highlights.size())break;var h=settings.highlights.get(i);int y=top+86+row*23;
   String label=(selected==i?"> ":"")+"选区 "+(i+1)+"  "+h.opacity()+"%  "+(h.body()!=null?(h.kind().equals("ship")?"物理结构":h.kind().equals("blocks")?"方块选区":"装配火炮/机构"):(h.max().getX()-h.min().getX()+1)+"×"+(h.max().getY()-h.min().getY()+1)+"×"+(h.max().getZ()-h.min().getZ()+1));
   addRenderableWidget(Button.builder(Component.literal(label),b->{selected=i;rebuildWidgets();}).bounds(left+10,y,244,20).build());
   addRenderableWidget(Button.builder(Component.literal("删除"),b->{selected=-1;send(11,i);}).bounds(left+260,y,50,20).build());
  }
  addRenderableWidget(Button.builder(Component.literal("上一页"),b->{page=Math.max(0,page-1);rebuildWidgets();}).bounds(left+10,top+208,75,20).build());
  addRenderableWidget(Button.builder(Component.literal("完成"),b->onClose()).bounds(left+122,top+208,75,20).build());
  addRenderableWidget(Button.builder(Component.literal("下一页"),b->{page=Math.min(Math.max(0,(settings.highlights.size()-1)/5),page+1);rebuildWidgets();}).bounds(left+235,top+208,75,20).build());
 }
 public void render(GuiGraphics g,int x,int y,float dt){g.fill(0,0,width,height,0x80202020);g.fill(left,top,left+320,top+238,0xFF18222D);g.drawString(font,title,left+10,top+12,0xD5F5FF,false);for(var w:renderables)w.render(g,x,y,dt);}
 public boolean isPauseScreen(){return false;}
 private class Alpha extends AbstractSliderButton {
  private boolean dragging;
  Alpha(int x,int y,int alpha){super(x,y,300,20,Component.empty(),alpha/100d);updateMessage();}
  protected void updateMessage(){setMessage(Component.literal((selected<0?"新选区":"选区 "+(selected+1))+"不透明度："+(int)Math.round(value*100)+"%"));}
  private void commit(){int a=(int)Math.round(value*100);send(selected<0?8:10,selected<0?a:selected*101+a);}
  protected void applyValue(){if(!dragging)commit();}
  public void onClick(double x,double y){dragging=true;super.onClick(x,y);}
  public void onRelease(double x,double y){super.onRelease(x,y);dragging=false;commit();}
 }
}
