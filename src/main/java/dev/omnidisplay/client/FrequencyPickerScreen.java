package dev.omnidisplay.client;
import dev.omnidisplay.CabinPackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
public final class FrequencyPickerScreen extends Screen {
 final CabinScreen parent;private final BlockPos anchor;private final int side;private int left,top;
 FrequencyPickerScreen(CabinScreen p,BlockPos a,int s){super(Component.literal("FREQUENCY / "+(s%2==0?"A":"B")));parent=p;anchor=a;side=s;}
 private void choose(int slot){PacketDistributor.sendToServer(new CabinPackets.Action(anchor,side<2?4+side:23+side,slot));onClose();}
 protected void init(){left=(width-236)/2;top=(height-154)/2;
  for(int i=0;i<36;i++){final int slot=i;addRenderableWidget(new CabinScreen.PanelButton(left+28+i%9*20,top+34+i/9*20,19,19,"",()->choose(slot),()->false));}
  addRenderableWidget(new CabinScreen.PanelButton(left+10,top+124,64,20,"清空",()->choose(-1),()->false));
  addRenderableWidget(new CabinScreen.PanelButton(left+86,top+124,64,20,"副手",()->choose(40),()->false));
  addRenderableWidget(new CabinScreen.PanelButton(left+162,top+124,64,20,"返回",()->onClose(),()->false));
 }
 public void render(GuiGraphics g,int mx,int my,float dt){g.fill(0,0,width,height,0xA0080E17);g.fill(left,top,left+236,top+154,0xFF0B1422);g.renderOutline(left,top,236,154,side%2==0?0xFFE76D7F:0xFF66B7FF);g.drawString(font,title,left+10,top+12,0xD5F5FF,false);for(var w:renderables)w.render(g,mx,my,dt);
  for(int i=0;i<36;i++){int x=left+29+i%9*20,y=top+35+i/9*20;var item=minecraft.player.getInventory().getItem(i);if(!item.isEmpty()){g.renderItem(item,x,y);if(mx>=x&&mx<x+18&&my>=y&&my<y+18)g.renderTooltip(font,item,mx,my);}}
 }
 public void onClose(){minecraft.setScreen(parent);}public boolean isPauseScreen(){return false;}
}
