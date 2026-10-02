package dev.omnidisplay;

import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid=OmniDisplay.ID)
public final class ScreenPanelItem extends Item {
 public ScreenPanelItem(Properties p){super(p);}
 @Override public void appendHoverText(ItemStack stack,Item.TooltipContext context,java.util.List<net.minecraft.network.chat.Component> text,TooltipFlag flag){
  text.add(net.minecraft.network.chat.Component.translatable("tooltip.omni_display.screen_panel.place"));
  text.add(net.minecraft.network.chat.Component.translatable("tooltip.omni_display.screen_panel.remove"));
  text.add(net.minecraft.network.chat.Component.translatable("tooltip.omni_display.screen_panel.configure"));
 }
 @Override public InteractionResult useOn(UseOnContext c){
  var player=c.getPlayer();var level=c.getLevel();var pos=c.getClickedPos();var face=c.getClickedFace();
  if(player==null||!player.mayBuild()||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,face,c.getItemInHand()))return InteractionResult.FAIL;
  if(level.isClientSide)return InteractionResult.SUCCESS;
  if(player.isShiftKeyDown())return PanelStore.detach(level,pos,face,!player.getAbilities().instabuild)?InteractionResult.CONSUME:InteractionResult.PASS;
  if(!PanelStore.attach(level,pos,face))return InteractionResult.PASS;
  if(!player.getAbilities().instabuild)c.getItemInHand().shrink(1);return InteractionResult.CONSUME;
 }
 // Run before the target's use method, so installing on a chest does not open it instead.
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void click(PlayerInteractEvent.RightClickBlock e){
  if(e.getUseItem()==net.neoforged.neoforge.common.util.TriState.FALSE)return;
  var face=e.getHitVec().getDirection();
  if(e.getItemStack().getItem() instanceof com.simibubi.create.content.equipment.wrench.WrenchItem
    &&(PanelStore.mask(e.getLevel(),e.getPos())&(1<<face.get3DDataValue()))!=0){
   var player=e.getEntity();var level=e.getLevel();
   e.setCanceled(true);
   if(!player.mayBuild()||!level.mayInteract(player,e.getPos())||!player.mayUseItemAt(e.getPos(),face,e.getItemStack())){e.setCancellationResult(InteractionResult.FAIL);return;}
   e.setCancellationResult(InteractionResult.SUCCESS);
   if(!level.isClientSide)PanelStore.detach(level,e.getPos(),face,!player.getAbilities().instabuild);
   return;
  }
  if(!e.getItemStack().is(OmniDisplay.PANEL_ITEM.get()))return;
  // Let the normal item-use path run after cancellable protection events.
  if(e.getUseItem()==net.neoforged.neoforge.common.util.TriState.FALSE)return;
  e.setUseBlock(net.neoforged.neoforge.common.util.TriState.FALSE);
  e.setUseItem(net.neoforged.neoforge.common.util.TriState.TRUE);
 }
}
