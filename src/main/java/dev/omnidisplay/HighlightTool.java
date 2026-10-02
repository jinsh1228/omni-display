package dev.omnidisplay;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import net.neoforged.neoforge.network.PacketDistributor;
public final class HighlightTool extends Item {
 public HighlightTool(Properties p){super(p);}
 private static CompoundTag data(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
 private static void write(ItemStack stack,CompoundTag t){stack.set(DataComponents.CUSTOM_DATA,CustomData.of(t));}
 public static boolean bound(ItemStack stack,CabinManager.Cabin c){var tag=data(stack);return stack.is(OmniDisplay.HIGHLIGHT_TOOL.get())&&tag.hasUUID("cabin")&&tag.getUUID("cabin").equals(c.settings.id);}
 private static void notice(Player p,String s){p.displayClientMessage(Component.literal(s),true);}
 @Override public InteractionResult useOn(UseOnContext ctx){
  if(ctx.getHand()!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;
  if(ctx.getLevel().isClientSide)return InteractionResult.SUCCESS;
  if(!(ctx.getPlayer() instanceof ServerPlayer p))return InteractionResult.PASS;
  var level=ctx.getLevel();var stack=ctx.getItemInHand();var pos=ctx.getClickedPos();var tag=data(stack);
  var c=CabinManager.forHighlight(p,pos);
  if(c==null){notice(p,"目标载具及其连接结构上没有可用座舱，请先放置屏幕。");return InteractionResult.SUCCESS;}
  if(!tag.hasUUID("cabin")||!c.settings.id.equals(tag.getUUID("cabin"))||!level.dimension().location().toString().equals(tag.getString("dimension")))tag.remove("first");
  tag.putUUID("cabin",c.settings.id);tag.putString("dimension",level.dimension().location().toString());write(stack,tag);
  var owner=HighlightStructures.at(level,c.anchor.getBlockPos());var target=HighlightStructures.at(level,pos);
  int mode=tag.contains("mode")?tag.getInt("mode"):(tag.getBoolean("range")?1:0);
  if(mode==2){
   if(!HighlightStructures.connected(p.serverLevel(),owner,target)){notice(p,"请选取与座舱物理连接的结构。");return InteractionResult.SUCCESS;}
   var cannon=HighlightStructures.mounted(level,pos);
   var additions=new java.util.ArrayList<HighlightRegion>();
   if(cannon!=null){additions.add(target==owner?HighlightRegion.between(pos.subtract(c.settings.origin),pos.subtract(c.settings.origin),c.settings.highlightOpacity):HighlightRegion.structure(target.getUniqueId(),"ship",c.settings.highlightOpacity));additions.add(HighlightRegion.structure(cannon.getUUID(),"contraption",c.settings.highlightOpacity));}
   else additions.add(HighlightRegion.structure(target.getUniqueId(),"ship",c.settings.highlightOpacity));
   if(c.settings.highlights.size()+additions.size()>32){notice(p,"最多32个高光选区，请先删除旧选区。");return InteractionResult.SUCCESS;}
   c.settings.highlights.addAll(additions);c.publish();notice(p,cannon!=null?"已选中控制座和整个装配机构/火炮。":"已选中整个物理结构。");return InteractionResult.SUCCESS;
  }
  if(target==null||!HighlightStructures.connected(p.serverLevel(),owner,target)){notice(p,"请选取载具或物理连接结构上的方块。");return InteractionResult.SUCCESS;}
  var reference=target.getPlot().getCenterBlock();var relative=pos.subtract(reference);var first=relative;
  if(mode==1){
   if(!tag.contains("first")||!tag.hasUUID("firstBody")||!target.getUniqueId().equals(tag.getUUID("firstBody"))){tag.putLong("first",relative.asLong());tag.putUUID("firstBody",target.getUniqueId());write(stack,tag);notice(p,"已选框选起点，请点击对角方块。");return InteractionResult.SUCCESS;}
   first=BlockPos.of(tag.getLong("first"));tag.remove("first");write(stack,tag);
  }
  if(c.settings.highlights.size()>=32){notice(p,"最多32个高光选区，请先删除旧选区。");return InteractionResult.SUCCESS;}
  var region=HighlightRegion.blocks(target.getUniqueId(),first,relative,c.settings.highlightOpacity);
  long volume=((long)region.max().getX()-region.min().getX()+1)*((long)region.max().getY()-region.min().getY()+1)*((long)region.max().getZ()-region.min().getZ()+1);
  if(volume>65536){notice(p,"选区不能超过65536格。");return InteractionResult.SUCCESS;}
  c.settings.highlights.add(region);c.publish();notice(p,"高光选区已保存，不透明度 "+region.opacity()+"%。右键空气可修改。");return InteractionResult.SUCCESS;
 }
 @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
  var stack=player.getItemInHand(hand);if(hand!=InteractionHand.MAIN_HAND)return InteractionResultHolder.pass(stack);if(!level.isClientSide&&player instanceof ServerPlayer p){var t=data(stack);var c=CabinManager.forHighlight(p,null);if(c==null&&t.hasUUID("cabin")&&level.dimension().location().toString().equals(t.getString("dimension")))c=CabinManager.byId(level,t.getUUID("cabin"));if(c==null)notice(p,"请靠近载具屏幕或先右键选取目标。");else{t.putUUID("cabin",c.settings.id);t.putString("dimension",level.dimension().location().toString());write(stack,t);send(p,c,true);}}
  return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
 }
 public static void send(ServerPlayer p,CabinManager.Cabin c,boolean open){var tag=c.settings.save(p.registryAccess());var tool=data(p.getMainHandItem());tag.putBoolean("tool",true);tag.putBoolean("range",tool.getBoolean("range"));tag.putInt("mode",tool.contains("mode")?tool.getInt("mode"):(tool.getBoolean("range")?1:0));tag.putInt("toolOpacity",c.settings.highlightOpacity);PacketDistributor.sendToPlayer(p,new CabinPackets.Data(c.anchor.getBlockPos(),open,tag));}
 public static void action(ServerPlayer p,CabinManager.Cabin c,int op,int value){var stack=p.getMainHandItem();var tag=data(stack);
  if(op==8){c.settings.highlightOpacity=Math.clamp(value,0,100);tag.putInt("opacity",Math.clamp(value,0,100));write(stack,tag);}
  if(op==9){tag.putInt("mode",Math.clamp(value,0,2));tag.putBoolean("range",value==1);tag.remove("first");write(stack,tag);}
  if(op==10){int i=value/101,alpha=value%101;if(i>=0&&i<c.settings.highlights.size()&&alpha>=0){var old=c.settings.highlights.get(i);c.settings.highlights.set(i,old.withOpacity(alpha));}}
  if(op==11&&value>=0&&value<c.settings.highlights.size())c.settings.highlights.remove(value);
 }
}

