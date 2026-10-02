package dev.omnidisplay;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;
@EventBusSubscriber(modid=OmniDisplay.ID)
public final class RenderControl {
 private static Boolean lastMode;
 private static final Map<UUID,CompoundTag> sent=new HashMap<>();
 public static boolean mayEdit(ServerPlayer p){return dev.omnidisplay.region.RenderRules.mayEdit(p.hasPermissions(2),p.server.isSingleplayerOwner(p.getGameProfile()));}
 public static void request(ServerPlayer p,boolean value){
  if(!mayEdit(p)){p.displayClientMessage(net.minecraft.network.chat.Component.literal("只有服务器管理员可以修改低性能模式。"),false);return;}
  RenderConfig.LOW_PERFORMANCE.set(!value);RenderConfig.SPEC.save();CabinManager.renderModeChanged();sent.clear();
 }
 public static void filterRequest(ServerPlayer player,String key,int value){
  if(!mayEdit(player)||!RenderConfig.SPEC.isLoaded())return;
  if(key.equals("strength")){if(value<0||value>100)return;RenderConfig.STRENGTH.set(value/100d);}
  else{
   if(value!=0&&value!=1)return;
   var setting=switch(key){case "tint"->RenderConfig.TINT;case "pixels"->RenderConfig.PIXELS;case "scanlines"->RenderConfig.SCANLINES;case "edges"->RenderConfig.EDGES;default->null;};
   if(setting==null)return;setting.set(value!=0);
  }
  RenderConfig.SPEC.save();sent.clear();
 }
 @SubscribeEvent public static void tick(ServerTickEvent.Post event){
  boolean mode=RenderConfig.experimental();if(lastMode==null||lastMode!=mode){lastMode=mode;CabinManager.renderModeChanged();sent.clear();}
  var server=event.getServer();if(server.getTickCount()%10!=0)return;
  Map<ServerLevel,List<SynaxisLinks.Link>> worlds=new IdentityHashMap<>();
  for(var p:server.getPlayerList().getPlayers()){
   var tag=new CompoundTag();RenderConfig.filterState(tag);tag.putBoolean("experimental",RenderConfig.experimental());tag.putBoolean("editable",mayEdit(p));tag.putString("dimension",p.serverLevel().dimension().location().toString());
   var list=new ListTag();for(var link:worlds.computeIfAbsent(p.serverLevel(),SynaxisLinks::read)){var e=new CompoundTag();e.putUUID("a",link.a());e.putUUID("b",link.b());list.add(e);}tag.put("links",list);
   if(!tag.equals(sent.get(p.getUUID()))||server.getTickCount()%100==0){PacketDistributor.sendToPlayer(p,new RenderPackets.State(tag));sent.put(p.getUUID(),tag);}
  }
  var online=new HashSet<UUID>();for(var p:server.getPlayerList().getPlayers())online.add(p.getUUID());sent.keySet().retainAll(online);
 }
 @SubscribeEvent public static void stopped(ServerStoppedEvent e){sent.clear();lastMode=null;}
}
