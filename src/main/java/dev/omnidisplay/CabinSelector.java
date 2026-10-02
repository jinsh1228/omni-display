package dev.omnidisplay;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.network.chat.Component;
/** Right click starts/extends a group; sneak click removes a screen group; air clears the cursor. */
public final class CabinSelector extends Item {
 public CabinSelector(Properties p){super(p);}
 private static void notice(Player p,String text){p.displayClientMessage(Component.literal(text),true);}
 public InteractionResult useOn(UseOnContext ctx){
  if(ctx.getHand()!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;if(ctx.getLevel().isClientSide)return InteractionResult.SUCCESS;
  var player=ctx.getPlayer();if(player==null)return InteractionResult.PASS;var level=ctx.getLevel();var pos=ctx.getClickedPos();
  if(!RenderConfig.experimental()){notice(player,"低性能模式不支持手动座舱分组。");return InteractionResult.SUCCESS;}
  var mounted=HighlightStructures.mounted(level,pos);
  if(mounted!=null&&!MovingScreens.screens(mounted).isEmpty()){MovingScreens.prepare(mounted,ctx.getClickedFace());CabinManager.rebuild(level);var moving=CabinManager.moving(level,mounted);if(moving!=null)select(player,ctx.getItemInHand(),moving);return InteractionResult.SUCCESS;}
  if(!ScreenFaces.isScreen(level,pos)){notice(player,"请点击显示器或贴片屏幕。");return InteractionResult.SUCCESS;}
  var sub=HighlightStructures.at(level,pos);if(sub==null){notice(player,"座舱选择工具仅支持载具上的屏幕。");return InteractionResult.SUCCESS;}
  // Explicitly selecting a loose ordinary/thin display assigns its inward face from the clicked side.
  var host=PanelStore.hostAt(level,pos);if(host==null&&level.getBlockEntity(pos) instanceof CabinEntity h)host=h;
  if(host!=null&&!player.isShiftKeyDown()){
   var settings=host.settings.copy();if(settings.group==null)settings.group=java.util.UUID.randomUUID();
   if(PanelStore.mask(level,pos)==0&&ScreenFaces.supportsFace(level,pos,ctx.getClickedFace()))settings.manualFaces=1<<ctx.getClickedFace().get3DDataValue();
   settings.revision=Math.max(settings.revision+1,System.currentTimeMillis());host.apply(settings);
  }
  CabinManager.rebuild(level);var cabin=CabinManager.find(level,pos);if(cabin==null){notice(player,"未识别显示面，请从屏幕正面点击。");return InteractionResult.SUCCESS;}
  select(player,ctx.getItemInHand(),cabin);return InteractionResult.SUCCESS;
 }
 public static void select(Player player,ItemStack stack,CabinManager.Cabin cabin){
  var level=cabin.level;var sub=CabinManager.body(cabin);
  if(!RenderConfig.experimental()||sub==null){notice(player,"座舱选择工具仅支持非低性能模式下的载具屏幕。");return;}
  var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
  if(player.isShiftKeyDown()){cabin.settings.group=null;cabin.publish();tag.remove("first");notice(player,"已将这组屏幕移出手动座舱组。");}
  else{
   var first=tag.hasUUID("first")&&level.dimension().location().toString().equals(tag.getString("dimension"))?CabinManager.byId(level,tag.getUUID("first")):null;
   var firstSub=first==null?null:CabinManager.body(first);
   if(first!=null&&!HighlightStructures.connected((net.minecraft.server.level.ServerLevel)level,firstSub,sub)){notice(player,"只能合并通过关节或轴承连接的载具屏幕；右键空气重新选择。");return;}
   if(first!=null){if(!CabinManager.group(first,cabin)){notice(player,"座舱分组范围过大，请缩小选择范围。");return;}notice(player,"已加入同一座舱，开关机同步；动画分别执行。");}
   else{tag.putUUID("first",cabin.settings.id);tag.putString("dimension",level.dimension().location().toString());notice(player,"已选择第一组屏幕，继续右键同一载具上的其他屏幕。");}
  }
  stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return;
 }
 public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){var stack=player.getItemInHand(hand);if(!level.isClientSide){var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();tag.remove("first");stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));notice(player,"已清空工具选择；已建立的座舱组保留。Shift+右键屏幕可移出分组。");}return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);}
}
